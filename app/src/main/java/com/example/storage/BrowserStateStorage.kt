package com.example.storage

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import com.example.core.CoreAlgorithms
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Pure AOSP atomic file persistence for browser configuration, AI CLI profiles, open tabs,
 * and per-tab WebView navigation history Bundles across background process death.
 *
 * Writes the config atomically (`filesDir/browser_config.json`, validated with `org.json` on load)
 * and stores Android `Parcel` binary snapshots (`filesDir/tab_states/{tabId}.state`), with zero GMS
 * or external dependencies.
 */
class BrowserStateStorage(
    private val filesDir: File,
) {
    constructor(context: Context) : this(context.applicationContext.filesDir)

    private val configFile: File = File(filesDir, CONFIG_FILE_NAME)
    private val configBackupFile: File = File(filesDir, CONFIG_BACKUP_FILE_NAME)
    private val tabStatesDir: File = File(filesDir, TAB_STATES_DIR_NAME)
    private val ioLock = Any()

    /**
     * Atomically writes the serialized browser configuration, AI profiles, tabs, history, and bookmarks
     * to `browser_config.json`.
     *
     * The payload comes straight from the core's kotlinx.serialization exporter, so instead of a full
     * `org.json` parse + re-serialization on every save, the metadata fields are spliced into the
     * top-level object and only a cheap structural check is performed. The previous file is rotated
     * to the backup slot with a rename rather than a byte-for-byte copy.
     */
    fun saveConfigJson(rawStateJson: String): Boolean = synchronized(ioLock) {
        runCatching {
            val body = rawStateJson.trim()
            if (!body.startsWith('{') || !body.endsWith('}') || !body.contains(NON_EMPTY_TABS_MARKER)) {
                return@runCatching false
            }
            val payload = buildString(body.length + 64) {
                append("{\"schemaVersion\":").append(SCHEMA_VERSION)
                append(",\"savedAtEpochMs\":").append(System.currentTimeMillis())
                append(',')
                append(body, 1, body.length)
            }

            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }

            val tmpFile = File(filesDir, "$CONFIG_FILE_NAME.tmp")
            FileOutputStream(tmpFile).use { fos ->
                fos.write(payload.toByteArray(Charsets.UTF_8))
                fos.flush()
                runCatching { fos.fd.sync() }
            }

            // Rotate the last known-good file into the backup slot (atomic rename, no copy).
            if (configFile.exists() && configFile.length() > 0L) {
                if (!configFile.renameTo(configBackupFile)) {
                    runCatching { configFile.copyTo(configBackupFile, overwrite = true) }
                }
            }

            if (!tmpFile.renameTo(configFile)) {
                tmpFile.copyTo(configFile, overwrite = true)
                tmpFile.delete()
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Synchronously loads `browser_config.json` (falling back to backup file if primary was corrupted).
     */
    fun loadConfigJson(): String? = synchronized(ioLock) {
        readValidatedJsonFile(configFile) ?: readValidatedJsonFile(configBackupFile)
    }

    /**
     * Atomically marshals a tab's `WebView.saveState(bundle)` to `filesDir/tab_states/{tabId}.state`.
     */
    fun saveTabWebViewState(tabId: String, bundle: Bundle): Boolean = synchronized(ioLock) {
        if (bundle.isEmpty) return@synchronized false
        runCatching {
            if (!tabStatesDir.exists()) {
                tabStatesDir.mkdirs()
            }
            val bytes = marshalBundle(bundle) ?: return@runCatching false
            val target = tabStateFile(tabId)
            val tmpFile = File(tabStatesDir, "${target.name}.tmp")
            FileOutputStream(tmpFile).use { fos ->
                fos.write(bytes)
                fos.flush()
                runCatching { fos.fd.sync() }
            }
            if (!tmpFile.renameTo(target)) {
                tmpFile.copyTo(target, overwrite = true)
                tmpFile.delete()
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Loads a previously persisted `WebView` history `Bundle` for [tabId] from disk, or null if absent.
     */
    fun loadTabWebViewState(tabId: String): Bundle? = synchronized(ioLock) {
        val file = tabStateFile(tabId)
        if (!file.exists() || file.length() <= 0L) return@synchronized null
        runCatching {
            val bytes = file.readBytes()
            unmarshalBundle(bytes)
        }.getOrNull()
    }

    /**
     * Deletes the persisted WebView state file for a closed or incognito tab.
     */
    fun deleteTabWebViewState(tabId: String) = synchronized(ioLock) {
        runCatching {
            val file = tabStateFile(tabId)
            if (file.exists()) file.delete()
            val tmp = File(tabStatesDir, "${file.name}.tmp")
            if (tmp.exists()) tmp.delete()
        }
        Unit
    }

    /**
     * Removes any stale `.state` files whose tab IDs are no longer open (or are incognito).
     */
    fun pruneStaleTabStates(activeNonIncognitoTabIds: Set<String>) = synchronized(ioLock) {
        if (!tabStatesDir.exists()) return@synchronized
        val expectedNames = activeNonIncognitoTabIds.map { "${sanitizeFileName(it)}.state" }.toSet()
        tabStatesDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name !in expectedNames) {
                runCatching { file.delete() }
            }
        }
    }

    private fun readValidatedJsonFile(file: File): String? {
        if (!file.exists() || file.length() <= 0L) return null
        return runCatching {
            val text = file.readText(Charsets.UTF_8)
            val parsed = JSONObject(text)
            val tabs = parsed.optJSONArray("tabs")
            if (tabs != null && tabs.length() > 0) text else null
        }.getOrNull()
    }

    private fun tabStateFile(tabId: String): File =
        File(tabStatesDir, "${sanitizeFileName(tabId)}.state")

    private fun sanitizeFileName(raw: String): String = CoreAlgorithms.sanitizeStateFileName(raw)

    private fun marshalBundle(bundle: Bundle): ByteArray? {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            parcel.marshall()
        } catch (_: Throwable) {
            null
        } finally {
            parcel.recycle()
        }
    }

    private fun unmarshalBundle(bytes: ByteArray): Bundle? {
        if (bytes.isEmpty()) return null
        val parcel = Parcel.obtain()
        return try {
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            parcel.readBundle(BrowserStateStorage::class.java.classLoader)
        } catch (_: Throwable) {
            null
        } finally {
            parcel.recycle()
        }
    }

    companion object {
        private const val SCHEMA_VERSION = 2
        /** Compact kotlinx.serialization output of a non-empty `tabs` array. */
        private const val NON_EMPTY_TABS_MARKER = "\"tabs\":[{"
        const val CONFIG_FILE_NAME = "browser_config.json"
        private const val CONFIG_BACKUP_FILE_NAME = "browser_config.json.bak"
        const val TAB_STATES_DIR_NAME = "tab_states"
    }
}
