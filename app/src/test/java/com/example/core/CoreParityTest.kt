package com.example.core

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * Fuzzes the Rust (`libbmcore`) and Kotlin implementations of [CoreAlgorithms] against each other.
 *
 * Plain JVM unit tests have no native library, so this is a no-op there. To run it for real:
 * `cargo build --release` in `rust/bmcore`, then run the unit tests with
 * `-Djava.library.path=<repo>/rust/bmcore/target/release`.
 */
class CoreParityTest {
    private val fragments = listOf(
        "https://", "HTTP://", "http://", "www.", "WWW.", "about:", "data:", "file://", "localhost", "LocalHost",
        "example", ".com", ".", "..", "-", "/", "//", "?", "#", ":", ":8080", "192", "168", "256", "+1",
        " ", "  ", "\t", "\n", "a", "Z", "9", "_", "~", "%s", "네이버", ".한국", "😀", "é", "ß", "İ",
        "doubleclick.net", "ads.naver.com", "googletagmanager.com", "sub.", "Version/", "4.0", "12.34",
        "; wv)", ";wv", ")", "(", "'", "\\", "\"", "<", ">", "|", "*", "\r", "kimi", "moonshot.cn",
        "api.kimi.com/coding", "\u001C", "\u00A0", "\u0085", "\u2028", "\u3000", "٣", "http://127.0.0.1", "http://10.0.2.2", "downloadfile.bin", "image/png", ";",
    )

    private fun randomInput(rnd: Random): String =
        buildString { repeat(rnd.nextInt(0, 7)) { append(fragments[rnd.nextInt(fragments.size)]) } }

    @Test
    fun nativeAndKotlinImplementationsAgree() {
        println("CoreAlgorithms backend = ${CoreAlgorithms.backendLabel}")
        if (!CoreAlgorithms.isNative) return
        val rnd = Random(42)
        val k = KotlinCoreAlgorithms
        repeat(200_000) {
            val a = randomInput(rnd)
            val b = randomInput(rnd)
            val ctx = "a=<$a> b=<$b>"
            assertEquals(ctx, k.normalizeOrSearch(a, b, "https://s.example/?q=%s"), NativeBrowserCore.normalizeOrSearch(a, b, "https://s.example/?q=%s"))
            assertEquals(ctx, k.formatDisplayUrl(a), NativeBrowserCore.formatDisplayUrl(a))
            assertEquals(ctx, k.classifySecurity(a), CoreAlgorithms.classifySecurity(a))
            assertEquals(ctx, k.isAdTrackerUrl(a), NativeBrowserCore.isAdTrackerUrl(a))
            assertEquals(ctx, k.sanitizeUserAgent(a), NativeBrowserCore.sanitizeUserAgent(a))
            assertEquals(ctx, k.sanitizeDownloadFileName(a, b, "d"), NativeBrowserCore.sanitizeDownloadFileName(a, b, "d"))
            assertEquals(ctx, k.sanitizeStateFileName(a), NativeBrowserCore.sanitizeStateFileName(a))
            assertEquals(ctx, k.escapeJsSingleQuoted(a), NativeBrowserCore.escapeJsSingleQuoted(a))
            assertEquals(ctx, k.inferProviderSubtype("kimi", a), NativeBrowserCore.inferProviderSubtype("kimi", a))
        }
    }
}
