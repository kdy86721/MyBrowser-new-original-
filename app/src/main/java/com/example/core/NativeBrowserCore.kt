package com.example.core

/**
 * JNI bindings to the Rust `bmcore` library (`rust/bmcore`, shipped as `libbmcore.so`).
 *
 * Do not call these directly: go through [CoreAlgorithms], which falls back to
 * [KotlinCoreAlgorithms] when the library is missing (JVM unit tests, builds without cargo-ndk)
 * or was built for a different [EXPECTED_ABI_VERSION].
 */
internal object NativeBrowserCore {
    /** Must match `bmcore::ABI_VERSION`. */
    const val EXPECTED_ABI_VERSION = 1

    val isLoaded: Boolean = runCatching {
        System.loadLibrary("bmcore")
        abiVersion() == EXPECTED_ABI_VERSION
    }.getOrDefault(false)

    @JvmStatic external fun abiVersion(): Int

    @JvmStatic external fun normalizeOrSearch(rawInput: String, homeUrl: String, queryTemplate: String): String

    @JvmStatic external fun formatDisplayUrl(url: String): String

    /** Returns the [SecurityState] ordinal. */
    @JvmStatic external fun classifySecurity(url: String): Int

    @JvmStatic external fun isAdTrackerUrl(url: String): Boolean

    @JvmStatic external fun sanitizeUserAgent(rawUa: String): String

    @JvmStatic external fun sanitizeDownloadFileName(rawName: String, mimeType: String, fallbackStem: String): String

    @JvmStatic external fun sanitizeStateFileName(raw: String): String

    @JvmStatic external fun escapeJsSingleQuoted(value: String): String

    @JvmStatic external fun inferProviderSubtype(cliType: String, endpoint: String): String
}
