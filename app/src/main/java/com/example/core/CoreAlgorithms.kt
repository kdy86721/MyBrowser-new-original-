package com.example.core

/**
 * Single entry point for the browser's pure hot-path algorithms.
 *
 * Dispatches to the Rust implementation ([NativeBrowserCore]) when `libbmcore.so` is loaded, and to
 * the behaviourally identical [KotlinCoreAlgorithms] otherwise. The choice is made once at class
 * initialisation, so each call costs a single branch.
 */
object CoreAlgorithms {
    private val native: Boolean = NativeBrowserCore.isLoaded

    val isNative: Boolean get() = native

    val backendLabel: String =
        if (native) "RustCore (bmcore via JNI)" else "LocalKotlinCore (Pure Kotlin fallback)"

    private val securityStates = SecurityState.entries

    fun normalizeOrSearch(rawInput: String, homeUrl: String, queryTemplate: String): String =
        if (native) {
            NativeBrowserCore.normalizeOrSearch(rawInput, homeUrl, queryTemplate)
        } else {
            KotlinCoreAlgorithms.normalizeOrSearch(rawInput, homeUrl, queryTemplate)
        }

    fun formatDisplayUrl(url: String): String =
        if (native) NativeBrowserCore.formatDisplayUrl(url) else KotlinCoreAlgorithms.formatDisplayUrl(url)

    fun classifySecurity(url: String): SecurityState =
        if (native) {
            securityStates.getOrElse(NativeBrowserCore.classifySecurity(url)) { SecurityState.NEUTRAL }
        } else {
            KotlinCoreAlgorithms.classifySecurity(url)
        }

    fun isAdTrackerUrl(url: String): Boolean =
        if (native) NativeBrowserCore.isAdTrackerUrl(url) else KotlinCoreAlgorithms.isAdTrackerUrl(url)

    fun sanitizeUserAgent(rawUa: String): String =
        if (native) NativeBrowserCore.sanitizeUserAgent(rawUa) else KotlinCoreAlgorithms.sanitizeUserAgent(rawUa)

    fun sanitizeDownloadFileName(rawName: String?, mimeType: String?, fallbackStem: String): String =
        if (native) {
            NativeBrowserCore.sanitizeDownloadFileName(rawName.orEmpty(), mimeType.orEmpty(), fallbackStem)
        } else {
            KotlinCoreAlgorithms.sanitizeDownloadFileName(rawName, mimeType, fallbackStem)
        }

    fun sanitizeStateFileName(raw: String): String =
        if (native) NativeBrowserCore.sanitizeStateFileName(raw) else KotlinCoreAlgorithms.sanitizeStateFileName(raw)

    fun escapeJsSingleQuoted(value: String): String =
        if (native) NativeBrowserCore.escapeJsSingleQuoted(value) else KotlinCoreAlgorithms.escapeJsSingleQuoted(value)

    fun inferProviderSubtype(cliType: String, endpoint: String): String =
        if (native) {
            NativeBrowserCore.inferProviderSubtype(cliType, endpoint)
        } else {
            KotlinCoreAlgorithms.inferProviderSubtype(cliType, endpoint)
        }
}

/**
 * Pure-Kotlin twin of `rust/bmcore`. Keep the two implementations behaviourally in sync; the Rust
 * crate's unit tests and `ExampleUnitTest` exercise the same cases.
 */
internal object KotlinCoreAlgorithms {
    private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

    // Explicit ASCII classes instead of \s, \d and \b: Android's ICU regex engine treats those as
    // Unicode-aware while the JDK does not, and the Rust implementation must match both.
    private const val ASCII_SPACE = "[ \\t\\n\\u000B\\f\\r]"
    private val UA_WV_TOKEN = Regex(";$ASCII_SPACE*wv(?=\\))")
    private val UA_VERSION_TOKEN = Regex("(?<![A-Za-z0-9_])Version/[0-9]+\\.[0-9]+$ASCII_SPACE*")
    private val UA_MULTI_SPACE = Regex("$ASCII_SPACE{2,}")
    private val UNSAFE_DOWNLOAD_CHARS = Regex("[\\\\/:*?\"<>|]")
    private val UNSAFE_STATE_FILE_CHARS = Regex("[^a-zA-Z0-9._-]")

    fun normalizeOrSearch(rawInput: String, homeUrl: String, queryTemplate: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return homeUrl

        when {
            trimmed.startsWith("https://", ignoreCase = true) -> return "https://" + trimmed.substring(8)
            trimmed.startsWith("http://", ignoreCase = true) -> return "http://" + trimmed.substring(7)
            trimmed.startsWith("about:", ignoreCase = true) ||
                trimmed.startsWith("file://", ignoreCase = true) ||
                trimmed.startsWith("data:", ignoreCase = true) -> return trimmed
        }

        if (trimmed.none { it.isWhitespace() }) {
            val hostPart = trimmed.substringBefore('/').substringBefore('?')
            val hostWithoutPort = hostPart.substringBefore(':')
            val portPart = hostPart.substringAfter(':', "")
            val validPort = portPart.all { it in '0'..'9' }
            if (validPort && isLocalOrIpHost(hostWithoutPort)) return "http://$trimmed"
            if (validPort && looksLikeDomain(hostWithoutPort)) return "https://$trimmed"
        }

        return queryTemplate.replace("%s", percentEncodeForm(trimmed))
    }

    fun formatDisplayUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        val withoutScheme = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
            else -> trimmed
        }
        val withoutWww = if (withoutScheme.startsWith("www.", ignoreCase = true)) {
            withoutScheme.substring(4)
        } else {
            withoutScheme
        }
        return withoutWww.trimEnd('/')
    }

    fun classifySecurity(url: String): SecurityState {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("https://", ignoreCase = true) -> SecurityState.SECURE
            trimmed.startsWith("http://", ignoreCase = true) -> SecurityState.INSECURE
            else -> SecurityState.NEUTRAL
        }
    }

    fun isAdTrackerUrl(url: String): Boolean {
        var rest = url.trim()
        if (rest.startsWith("https://", ignoreCase = true)) rest = rest.substring(8)
        if (rest.startsWith("http://", ignoreCase = true)) rest = rest.substring(7)
        val host = rest
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .lowercase()
        if (host.isEmpty()) return false
        if (host in AD_TRACKER_DOMAINS) return true

        // Walk parent domains: one O(1) HashSet lookup per subdomain level.
        var dotIdx = host.indexOf('.')
        while (dotIdx >= 0 && dotIdx < host.lastIndex) {
            if (host.substring(dotIdx + 1) in AD_TRACKER_DOMAINS) return true
            dotIdx = host.indexOf('.', dotIdx + 1)
        }
        return false
    }

    fun sanitizeUserAgent(rawUa: String): String =
        rawUa
            .replace(UA_WV_TOKEN, "")
            .replace(UA_VERSION_TOKEN, "")
            .replace(UA_MULTI_SPACE, " ")
            .trim()

    fun sanitizeDownloadFileName(rawName: String?, mimeType: String?, fallbackStem: String): String {
        val cleaned = rawName
            ?.trim()
            ?.replace(UNSAFE_DOWNLOAD_CHARS, "_")
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
        return "$fallbackStem.$ext"
    }

    fun sanitizeStateFileName(raw: String): String = raw.replace(UNSAFE_STATE_FILE_CHARS, "_")

    fun escapeJsSingleQuoted(value: String): String {
        val sb = StringBuilder(value.length + 8)
        for (c in value) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\'' -> sb.append("\\'")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun inferProviderSubtype(cliType: String, endpoint: String): String {
        if (cliType != AiCliProfile.CLI_TYPE_KIMI) return ""
        val lower = endpoint.trim().lowercase()
        return when {
            lower.contains("api.kimi.com/coding") -> AiCliProfile.KIMI_SUBTYPE_CODE
            lower.contains("moonshot.cn") -> AiCliProfile.KIMI_SUBTYPE_CHINA
            lower.startsWith("http://127.0.0.1") ||
                lower.startsWith("http://localhost") ||
                lower.startsWith("http://10.0.2.2") -> AiCliProfile.KIMI_SUBTYPE_LOCAL
            else -> AiCliProfile.KIMI_SUBTYPE_GLOBAL
        }
    }

    private fun isLocalOrIpHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        val parts = host.split('.')
        return parts.size == 4 && parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255
        }
    }

    private fun looksLikeDomain(host: String): Boolean {
        if (host.length < 3 || !host.contains('.')) return false
        if (host.startsWith('.') || host.endsWith('.')) return false
        val labels = host.split('.')
        val tld = labels.last()
        if (tld.length < 2 || !tld.all { it.isLetter() }) return false
        return labels.all { label ->
            label.isNotEmpty() &&
                !label.startsWith('-') &&
                !label.endsWith('-') &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    private fun percentEncodeForm(input: String): String {
        val bytes = input.encodeToByteArray()
        val sb = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            val code = b.toInt() and 0xFF
            val c = code.toChar()
            when {
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' ||
                    c == '-' || c == '_' || c == '.' || c == '~' -> sb.append(c)
                c == ' ' -> sb.append('+')
                else -> {
                    sb.append('%')
                    sb.append(HEX_DIGITS[code ushr 4])
                    sb.append(HEX_DIGITS[code and 0x0F])
                }
            }
        }
        return sb.toString()
    }

    /** Must contain exactly the entries of `AD_TRACKER_DOMAINS` in `rust/bmcore/src/ad_block.rs`. */
    val AD_TRACKER_DOMAINS: HashSet<String> = hashSetOf(
        "33across.com",
        "acecounter.com",
        "ad.daum.net",
        "ad.naver.com",
        "ad.planbplus.co.kr",
        "ad4989.co.kr",
        "adbay.co.kr",
        "adbrix.io",
        "adcr.naver.com",
        "adfit.kakao.com",
        "adform.net",
        "adition.com",
        "admaru.com",
        "admixer.co.kr",
        "adnmore.co.kr",
        "adnxs.com",
        "adop.cc",
        "ads-twitter.com",
        "ads.naver.com",
        "ads.tiktok.com",
        "adservice.google.com",
        "adsrvr.org",
        "advertising.com",
        "amazon-adsystem.com",
        "bidswitch.net",
        "casalemedia.com",
        "cauly.co.kr",
        "clickadu.com",
        "clickmon.co.kr",
        "criteo.com",
        "criteo.net",
        "dable.io",
        "doubleclick.net",
        "exoclick.com",
        "fsn.co.kr",
        "google-analytics.com",
        "googleadservices.com",
        "googlesyndication.com",
        "googletagmanager.com",
        "igaworks.com",
        "inmobi.com",
        "interworksmedia.co.kr",
        "media.net",
        "mediacategory.com",
        "mgid.com",
        "moatads.com",
        "mobon.net",
        "nasmedia.co.kr",
        "netinsight.co.kr",
        "openx.net",
        "outbrain.com",
        "pagead2.googlesyndication.com",
        "planbplus.co.kr",
        "popads.net",
        "propellerads.com",
        "pubmatic.com",
        "quantserve.com",
        "realclick.co.kr",
        "revcontent.com",
        "rubiconproject.com",
        "scorecardresearch.com",
        "sharethrough.com",
        "siape.veta.naver.com",
        "smartadserver.com",
        "taboola.com",
        "tappx.com",
        "targetpush.co.kr",
        "teads.tv",
        "triplelift.com",
        "widerplanet.com",
        "yieldmo.com",
    )
}
