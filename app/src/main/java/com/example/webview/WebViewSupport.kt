package com.example.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * Pure-AOSP / Zero-GMS WebView runtime helpers:
 * - Chromium renderer pre-warming via an idle offscreen WebView
 * - Mobile Chrome User-Agent sanitization (strips `; wv` to bypass Google OAuth WebView blocks)
 * - Safe `intent://`, `tel:`, `mailto:`, `sms:`, `geo:`, and custom app scheme resolution
 * - Native DownloadManager + `blob:` / `data:` URL download support via MediaStore (API 29+)
 */
object WebViewWarmupPool {
    private val lock = Any()
    private var prewarmedWebView: WebView? = null
    private var warmupScheduled = false

    @Volatile
    private var cachedCleanMobileUa: String? = null

    /**
     * Schedules creation of 1 hidden offscreen WebView on the main looper idle queue so the
     * Chromium renderer process is warmed up before the first tab mounts.
     */
    fun schedulePrewarm(appContext: Context) {
        synchronized(lock) {
            if (warmupScheduled || prewarmedWebView != null) return
            warmupScheduled = true
        }

        val appCtx = appContext.applicationContext
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            val queue = Looper.myQueue()
            queue.addIdleHandler {
                prewarmOnMainThread(appCtx)
                false
            }
        }
    }

    private fun prewarmOnMainThread(appContext: Context) {
        synchronized(lock) {
            if (prewarmedWebView != null) return
        }
        val created = runCatching {
            val wrappedContext = MutableContextWrapper(appContext)
            WebView(wrappedContext).apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = false
                }
                val cleanUa = getCleanMobileUserAgent(appContext, settings.userAgentString)
                if (cleanUa != null) {
                    settings.userAgentString = cleanUa
                }
            }
        }.getOrNull() ?: return

        synchronized(lock) {
            if (prewarmedWebView == null) {
                prewarmedWebView = created
            } else {
                runCatching { created.destroy() }
            }
        }
    }

    /**
     * Returns the pre-warmed WebView (rebound to the active Activity context) if available,
     * otherwise creates a fresh hardware-accelerated WebView.
     */
    fun acquireOrCreate(context: Context): WebView {
        val reused = synchronized(lock) {
            val existing = prewarmedWebView
            prewarmedWebView = null
            existing
        }
        if (reused != null) {
            val wrapper = reused.context as? MutableContextWrapper
            if (wrapper != null) {
                wrapper.baseContext = context
                return reused
            }
            runCatching { reused.destroy() }
        }
        return WebView(MutableContextWrapper(context)).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = false
            }
        }
    }

    /**
     * Strips `; wv` and legacy `Version/x.y` WebView tokens from the default Chromium UA so
     * OAuth providers (including Google Sign-In) recognize the browser as standard mobile Chrome.
     */
    fun getCleanMobileUserAgent(context: Context, fallbackRawUa: String? = null): String? {
        cachedCleanMobileUa?.let { return it }
        val raw = runCatching {
            fallbackRawUa?.takeIf { it.isNotBlank() } ?: WebSettings.getDefaultUserAgent(context)
        }.getOrNull() ?: return null
        val sanitized = sanitizeMobileUserAgent(raw)
        cachedCleanMobileUa = sanitized
        return sanitized
    }

    fun sanitizeMobileUserAgent(rawUa: String): String {
        return rawUa
            .replace(Regex(";\\s*wv(?=\\))"), "")
            .replace(Regex("\\bVersion/\\d+\\.\\d+\\s*"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }
}

object WebViewExternalNavigator {

    /**
     * Handles non-HTTP(S) schemes safely:
     * - `blob:` -> delegates to JS Blob download bridge
     * - `intent://` -> launches target app, falls back to `browser_fallback_url`, then market/Play web URL
     * - `tel:`, `mailto:`, `sms:`, `geo:`, `market://`, custom schemes -> launches with ActivityNotFoundException guard
     */
    fun handleUrl(
        context: Context,
        webView: WebView?,
        uri: Uri,
    ): Boolean {
        val urlStr = uri.toString()
        val scheme = uri.scheme?.lowercase().orEmpty()

        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "javascript") {
            return false
        }

        if (scheme == "blob") {
            if (webView != null) {
                triggerBlobDownload(
                    webView = webView,
                    blobUrl = urlStr,
                    contentDisposition = null,
                    mimeType = null,
                )
            }
            return true
        }

        if (scheme == "intent") {
            return handleIntentScheme(context, webView, urlStr)
        }

        val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val launched = runCatching {
            context.startActivity(viewIntent)
            true
        }.getOrDefault(false)

        if (!launched) {
            Toast.makeText(
                context,
                "이 링크를 열 수 있는 앱이 설치되어 있지 않습니다.",
                Toast.LENGTH_SHORT,
            ).show()
        }
        return true
    }

    private fun handleIntentScheme(
        context: Context,
        webView: WebView?,
        urlStr: String,
    ): Boolean {
        val parsedIntent = runCatching {
            Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                component = null
                selector = null
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }.getOrNull()

        if (parsedIntent == null) {
            Toast.makeText(
                context,
                "잘못된 외부 앱 링크 형식입니다.",
                Toast.LENGTH_SHORT,
            ).show()
            return true
        }

        val launchedDirectly = try {
            context.startActivity(parsedIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
        if (launchedDirectly) return true

        val fallbackUrl = parsedIntent.getStringExtra("browser_fallback_url")
        if (!fallbackUrl.isNullOrBlank() &&
            (fallbackUrl.startsWith("http://", ignoreCase = true) ||
                fallbackUrl.startsWith("https://", ignoreCase = true))
        ) {
            webView?.loadUrl(fallbackUrl)
            return true
        }

        val targetPackage = parsedIntent.`package`?.takeIf { it.isNotBlank() }
        if (targetPackage != null) {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$targetPackage"),
            ).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val marketOpened = runCatching {
                context.startActivity(marketIntent)
                true
            }.getOrDefault(false)
            if (marketOpened) return true

            // Zero-GMS AOSP fallback: load store web page inside the browser instead of crashing
            if (webView != null) {
                webView.loadUrl("https://play.google.com/store/apps/details?id=$targetPackage")
                return true
            }
        }

        Toast.makeText(
            context,
            "연결된 앱이 설치되어 있지 않습니다.",
            Toast.LENGTH_SHORT,
        ).show()
        return true
    }
}

const val BLOB_DOWNLOAD_JS_BRIDGE_NAME = "MyBrowserBlobBridge"

private val downloadIoExecutor = Executors.newSingleThreadExecutor()
private val mainThreadHandler = Handler(Looper.getMainLooper())

/**
 * JavascriptInterface bridging `blob:` and `data:` downloads from WebView into
 * MediaStore.Downloads (API 29+, zero storage permissions) or standard Downloads folder.
 */
class BlobDownloadBridge(
    context: Context,
) {
    private val appContext = context.applicationContext

    @JavascriptInterface
    fun onBlobDownloaded(
        dataUrl: String?,
        suggestedFileName: String?,
        mimeType: String?,
    ) {
        if (dataUrl.isNullOrBlank()) {
            showToast("다운로드할 파일 데이터를 가져오지 못했습니다.")
            return
        }
        saveDataUrlToDownloads(
            context = appContext,
            dataUrl = dataUrl,
            suggestedFileName = suggestedFileName,
            fallbackMimeType = mimeType,
        )
    }

    @JavascriptInterface
    fun onBlobDownloadFailed(reason: String?) {
        showToast("파일 다운로드 실패: ${reason?.take(60).orEmpty().ifBlank { "알 수 없는 오류" }}")
    }

    private fun showToast(message: String) {
        mainThreadHandler.post {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Unified download dispatcher supporting `http(s)://`, `blob:`, and `data:` URLs.
 */
fun dispatchWebViewDownload(
    context: Context,
    webView: WebView?,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?,
) {
    val trimmed = url.trim()
    when {
        trimmed.startsWith("blob:", ignoreCase = true) -> {
            if (webView != null) {
                triggerBlobDownload(
                    webView = webView,
                    blobUrl = trimmed,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                )
            } else {
                Toast.makeText(context, "Blob 다운로드를 처리할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        trimmed.startsWith("data:", ignoreCase = true) -> {
            val fileName = guessSafeFileName(
                url = trimmed,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
            )
            saveDataUrlToDownloads(
                context = context.applicationContext,
                dataUrl = trimmed,
                suggestedFileName = fileName,
                fallbackMimeType = mimeType,
            )
        }
        trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) -> {
            enqueueHttpDownload(
                context = context,
                url = trimmed,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
            )
        }
        else -> {
            Toast.makeText(context, "지원되지 않는 다운로드 주소입니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun triggerBlobDownload(
    webView: WebView,
    blobUrl: String,
    contentDisposition: String?,
    mimeType: String?,
) {
    val guessedName = guessSafeFileName(
        url = blobUrl,
        contentDisposition = contentDisposition,
        mimeType = mimeType,
    )
    val safeUrlLiteral = escapeJsSingleQuoted(blobUrl)
    val safeNameLiteral = escapeJsSingleQuoted(guessedName)
    val safeMimeLiteral = escapeJsSingleQuoted(mimeType.orEmpty())

    val script = """
        (function() {
          try {
            if (!window.$BLOB_DOWNLOAD_JS_BRIDGE_NAME) { return; }
            fetch('$safeUrlLiteral')
              .then(function(res) {
                if (!res.ok) { throw new Error('HTTP ' + res.status); }
                return res.blob();
              })
              .then(function(blob) {
                var reader = new FileReader();
                reader.onloadend = function() {
                  var resolvedMime = (blob && blob.type) ? blob.type : '$safeMimeLiteral';
                  window.$BLOB_DOWNLOAD_JS_BRIDGE_NAME.onBlobDownloaded(reader.result || '', '$safeNameLiteral', resolvedMime);
                };
                reader.onerror = function() {
                  window.$BLOB_DOWNLOAD_JS_BRIDGE_NAME.onBlobDownloadFailed('파일 읽기 오류');
                };
                reader.readAsDataURL(blob);
              })
              .catch(function(err) {
                window.$BLOB_DOWNLOAD_JS_BRIDGE_NAME.onBlobDownloadFailed(String(err || 'fetch error'));
              });
          } catch (e) {
            if (window.$BLOB_DOWNLOAD_JS_BRIDGE_NAME) {
              window.$BLOB_DOWNLOAD_JS_BRIDGE_NAME.onBlobDownloadFailed(String(e));
            }
          }
        })();
    """.trimIndent()

    webView.post {
        runCatching {
            webView.evaluateJavascript(script, null)
        }
    }
}

private fun enqueueHttpDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?,
) {
    val parsedUri = runCatching { Uri.parse(url) }.getOrNull()
    if (parsedUri == null) {
        Toast.makeText(context, "지원되지 않는 다운로드 주소입니다.", Toast.LENGTH_SHORT).show()
        return
    }

    val fileName = guessSafeFileName(url, contentDisposition, mimeType)
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    if (downloadManager == null) {
        Toast.makeText(context, "다운로드 관리자를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }

    val request = DownloadManager.Request(parsedUri).apply {
        if (!mimeType.isNullOrBlank()) {
            setMimeType(mimeType)
        }
        val cookie = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
        if (!cookie.isNullOrBlank()) {
            addRequestHeader("Cookie", cookie)
        }
        val effectiveUa = userAgent?.takeIf { it.isNotBlank() }
            ?: WebViewWarmupPool.getCleanMobileUserAgent(context)
        if (!effectiveUa.isNullOrBlank()) {
            addRequestHeader("User-Agent", effectiveUa)
        }
        setTitle(fileName)
        setDescription(url)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        try {
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        } catch (_: Throwable) {
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
    }

    runCatching {
        downloadManager.enqueue(request)
    }.onSuccess {
        Toast.makeText(context, "다운로드 시작: $fileName", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "다운로드를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

fun saveDataUrlToDownloads(
    context: Context,
    dataUrl: String,
    suggestedFileName: String?,
    fallbackMimeType: String?,
) {
    downloadIoExecutor.execute {
        val result = runCatching {
            val commaIndex = dataUrl.indexOf(',')
            if (commaIndex <= 0) error("잘못된 데이터 URL")
            val header = dataUrl.substring(0, commaIndex)
            val payload = dataUrl.substring(commaIndex + 1)

            val headerMime = header
                .removePrefix("data:")
                .substringBefore(';')
                .trim()
                .takeIf { it.contains('/') }
            val resolvedMime = headerMime
                ?: fallbackMimeType?.takeIf { it.contains('/') }
                ?: "application/octet-stream"

            val bytes = if (header.contains(";base64", ignoreCase = true)) {
                Base64.decode(payload, Base64.DEFAULT)
            } else {
                Uri.decode(payload).toByteArray(Charsets.UTF_8)
            }

            val finalFileName = sanitizeDownloadFileName(
                rawName = suggestedFileName,
                mimeType = resolvedMime,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Downloads.MIME_TYPE, resolvedMime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("MediaStore 항목 생성 실패")
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        out.write(bytes)
                        out.flush()
                    } ?: error("출력 스트림 열기 실패")
                    val doneValues = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(itemUri, doneValues, null, null)
                } catch (e: Throwable) {
                    runCatching { resolver.delete(itemUri, null, null) }
                    throw e
                }
            } else {
                val targetDir = runCatching {
                    @Suppress("DEPRECATION")
                    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!publicDir.exists()) publicDir.mkdirs()
                    if (publicDir.canWrite()) publicDir else null
                }.getOrNull() ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                if (!targetDir.exists()) targetDir.mkdirs()
                val outFile = File(targetDir, finalFileName)
                FileOutputStream(outFile).use { out ->
                    out.write(bytes)
                    out.flush()
                }
            }
            finalFileName
        }

        mainThreadHandler.post {
            result.onSuccess { savedName ->
                Toast.makeText(context, "다운로드 완료: $savedName", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "파일 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun guessSafeFileName(
    url: String,
    contentDisposition: String?,
    mimeType: String?,
): String {
    val guessed = runCatching {
        URLUtil.guessFileName(url, contentDisposition, mimeType)
    }.getOrNull()
    return sanitizeDownloadFileName(guessed, mimeType)
}

private fun sanitizeDownloadFileName(
    rawName: String?,
    mimeType: String?,
): String {
    val cleaned = rawName
        ?.trim()
        ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        ?.takeIf { it.isNotBlank() && it != "downloadfile.bin" }

    if (cleaned != null) return cleaned

    val ext = when (mimeType?.lowercase()?.substringBefore(';')?.trim()) {
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "application/pdf" -> "pdf"
        "text/plain" -> "txt"
        "text/csv" -> "csv"
        "application/json" -> "json"
        "application/zip" -> "zip"
        "video/mp4" -> "mp4"
        "audio/mpeg" -> "mp3"
        else -> "bin"
    }
    return "download_${System.currentTimeMillis()}.$ext"
}

private fun escapeJsSingleQuoted(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
