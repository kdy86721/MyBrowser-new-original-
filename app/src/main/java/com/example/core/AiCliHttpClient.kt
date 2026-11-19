package com.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.math.roundToInt

data class AiCliResponse(
    val text: String,
    val profileName: String,
    val model: String,
)

@Serializable
data class AiChatTurn(
    val isUser: Boolean,
    val text: String,
)

class MissingApiKeyException(val profileName: String) :
    IllegalStateException("$profileName API 키가 설정되지 않았습니다.")

class MissingModelSelectionException(val profileName: String) :
    IllegalStateException("${profileName}의 사용할 모델이 선택되지 않았습니다. 'AI 설정'에서 연결 확인 및 모델 불러오기를 실행해 주세요.")

/**
 * Pure AOSP HttpURLConnection client executing on [Dispatchers.IO].
 * Supports Kimi API (Global/China), Kimi Code (`User-Agent: KimiCLI/1.0`), Local Kimi CLI bridge,
 * Gemini CLI, Claude CLI, Codex / OpenAI CLI, Ollama local CLI, and Custom OpenAI-compatible CLI bridges
 * with explicit timeouts, safe errorStream diagnostics, and zero GMS/Android dependencies.
 */
object AiCliHttpClient {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 20_000
    internal const val KIMI_CODE_USER_AGENT = "KimiCLI/1.0"
    internal val KIMI_CODE_FALLBACK_MODELS = listOf("kimi-for-coding", "kimi-k2-0711-preview")

    private val jsonCodec = Json {
        ignoreUnknownKeys = true
    }

    private data class RawHttpResult(
        val statusCode: Int,
        val body: String,
    )

    suspend fun fetchAvailableModels(
        profile: AiCliProfile,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = validateEndpoint(profile.endpointOrCommand)
            validateRemoteKey(profile, endpoint)

            val (modelsUrl, headers) = buildModelsFetchSpec(profile, endpoint)
            val response = executeGetRequest(url = modelsUrl, headers = headers)
            val isKimiCode = isKimiCodeMode(profile, endpoint)
            val supportsChatProbeFallback = isKimiCode || profile.cliType == AiCliProfile.CLI_TYPE_CUSTOM

            if (response.statusCode in 200..299) {
                val parsedModels = parseAvailableModelsResponse(profile.cliType, response.body)
                if (parsedModels.isNotEmpty()) {
                    return@runCatching parsedModels
                }
                if (isKimiCode) {
                    return@runCatching verifyByMinimalChatProbe(
                        profile = profile,
                        endpoint = endpoint,
                        fallbackModels = KIMI_CODE_FALLBACK_MODELS,
                    )
                }
                throw IllegalStateException("서버에서 사용 가능한 모델 목록을 찾지 못했습니다.")
            }

            if ((response.statusCode == 404 || response.statusCode == 405) && supportsChatProbeFallback) {
                val fallbackModels = if (isKimiCode) {
                    KIMI_CODE_FALLBACK_MODELS
                } else {
                    val existing = profile.selectedModel.trim()
                    if (existing.isNotEmpty()) listOf(existing) else listOf("default")
                }
                return@runCatching verifyByMinimalChatProbe(
                    profile = profile,
                    endpoint = endpoint,
                    fallbackModels = fallbackModels,
                )
            }

            throw IllegalStateException(
                formatHttpError(
                    statusCode = response.statusCode,
                    rawBody = response.body,
                    url = modelsUrl,
                    profileName = profile.name,
                )
            )
        }.recoverCatching { err ->
            throw mapNetworkException(err, profile.name)
        }
    }

    suspend fun requestPageAnalysis(
        profile: AiCliProfile,
        pageTitle: String,
        pageUrl: String,
        pageContentText: String,
        userQuestion: String? = null,
    ): Result<AiCliResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = validateEndpoint(profile.endpointOrCommand)
            validateRemoteKey(profile, endpoint)
            if (profile.selectedModel.isBlank()) {
                throw MissingModelSelectionException(profile.name)
            }

            val promptText = buildPromptPayload(
                pageTitle = pageTitle,
                pageUrl = pageUrl,
                pageContentText = pageContentText,
                userQuestion = userQuestion,
            )

            executeChatPost(
                profile = profile,
                endpoint = endpoint,
                turns = listOf(AiChatTurn(isUser = true, text = promptText)),
            )
        }.recoverCatching { err ->
            throw mapNetworkException(err, profile.name)
        }
    }

    suspend fun requestAiChat(
        profile: AiCliProfile,
        conversation: List<AiChatTurn>,
        pageTitle: String,
        pageUrl: String,
        pageContentText: String,
    ): Result<AiCliResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = validateEndpoint(profile.endpointOrCommand)
            validateRemoteKey(profile, endpoint)
            if (profile.selectedModel.isBlank()) {
                throw MissingModelSelectionException(profile.name)
            }

            val contextHeader = buildString {
                if (pageUrl.isNotBlank()) {
                    appendLine("[현재 브라우저 탭 컨텍스트]")
                    if (pageTitle.isNotBlank()) appendLine("제목: $pageTitle")
                    appendLine("URL: $pageUrl")
                    if (pageContentText.isNotBlank()) {
                        appendLine("페이지 본문 일부:")
                        appendLine(pageContentText.take(3500))
                    }
                    appendLine("---")
                }
            }

            val enrichedTurns = conversation.mapIndexed { idx, turn ->
                if (idx == 0 && turn.isUser && contextHeader.isNotBlank()) {
                    turn.copy(text = "$contextHeader${turn.text}")
                } else {
                    turn
                }
            }

            executeChatPost(
                profile = profile,
                endpoint = endpoint,
                turns = enrichedTurns,
            )
        }.recoverCatching { err ->
            throw mapNetworkException(err, profile.name)
        }
    }

    private fun verifyByMinimalChatProbe(
        profile: AiCliProfile,
        endpoint: String,
        fallbackModels: List<String>,
    ): List<String> {
        val probeModel = profile.selectedModel.trim().ifEmpty { fallbackModels.first() }
        val probeProfile = profile.copy(selectedModel = probeModel)
        val (chatUrl, headers, _) = buildHttpSpec(
            profile = probeProfile,
            cleanEndpoint = endpoint,
            turns = listOf(AiChatTurn(isUser = true, text = "ping")),
        )
        val probeTemp = sanitizeTemperature(0f, probeProfile.cliType, probeModel)
        val probeBody = buildJsonObject {
            put("model", probeModel)
            if (isReasoningModelWithoutTemperature(probeProfile.cliType, probeModel)) {
                put("max_completion_tokens", 1)
            } else {
                put("max_tokens", 1)
            }
            if (probeTemp != null) {
                put("temperature", probeTemp)
            }
            put("messages", buildJsonArray {
                addJsonObject {
                    put("role", "user")
                    put("content", "ping")
                }
            })
        }.toString()

        val probeResponse = executePostRequest(
            url = chatUrl,
            headers = headers,
            requestBody = probeBody,
        )

        if (probeResponse.statusCode in 200..299) {
            return fallbackModels
        }
        if (probeResponse.statusCode == 401 || probeResponse.statusCode == 403) {
            throw IllegalStateException(
                formatHttpError(
                    statusCode = probeResponse.statusCode,
                    rawBody = probeResponse.body,
                    url = chatUrl,
                    profileName = profile.name,
                )
            )
        }
        // For custom CLI bridges where /models returned 404/405 and default probe model isn't recognized (400/422),
        // auth succeeded and the endpoint is reachable so return fallbackModels to let the user pick/add a model.
        if (probeResponse.statusCode == 400 || probeResponse.statusCode == 422) {
            return fallbackModels
        }
        throw IllegalStateException(
            formatHttpError(
                statusCode = probeResponse.statusCode,
                rawBody = probeResponse.body,
                url = chatUrl,
                profileName = profile.name,
            )
        )
    }

    private fun executeChatPost(
        profile: AiCliProfile,
        endpoint: String,
        turns: List<AiChatTurn>,
    ): AiCliResponse {
        val (targetUrl, headers, requestBody) = buildHttpSpec(profile, endpoint, turns)
        val response = executePostRequest(
            url = targetUrl,
            headers = headers,
            requestBody = requestBody,
        )

        if (response.statusCode !in 200..299) {
            throw IllegalStateException(
                formatHttpError(
                    statusCode = response.statusCode,
                    rawBody = response.body,
                    url = targetUrl,
                    profileName = profile.name,
                )
            )
        }

        val parsedText = extractResponseText(profile.cliType, response.body).trim()
        if (parsedText.isEmpty()) {
            throw IllegalStateException("AI 응답 본문이 비어 있습니다.")
        }

        return AiCliResponse(
            text = parsedText,
            profileName = profile.name,
            model = profile.selectedModel,
        )
    }

    private fun executeGetRequest(
        url: String,
        headers: Map<String, String>,
    ): RawHttpResult {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            doInput = true
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            val statusCode = connection.responseCode
            val rawBody = readResponseBodySafely(connection, statusCode)
            RawHttpResult(statusCode = statusCode, body = rawBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun executePostRequest(
        url: String,
        headers: Map<String, String>,
        requestBody: String,
    ): RawHttpResult {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }
            val statusCode = connection.responseCode
            val rawBody = readResponseBodySafely(connection, statusCode)
            RawHttpResult(statusCode = statusCode, body = rawBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponseBodySafely(connection: HttpURLConnection, statusCode: Int): String {
        val stream = if (statusCode in 200..299) {
            runCatching { connection.inputStream }.getOrNull()
        } else {
            runCatching { connection.errorStream }.getOrNull()
        } ?: return ""

        return runCatching {
            stream.use { s ->
                BufferedReader(InputStreamReader(s, Charsets.UTF_8)).readText()
            }
        }.getOrDefault("")
    }

    private fun formatHttpError(
        statusCode: Int,
        rawBody: String,
        url: String,
        profileName: String,
    ): String {
        val serverError = extractErrorMessage(rawBody)
        val detail = serverError.ifBlank { rawBody.trim().take(200) }
        return when (statusCode) {
            400 -> "요청 파라미터 오류 (HTTP 400 · $profileName): ${detail.ifBlank { "모델명 또는 요청 형식을 확인하세요." }}"
            401, 403 -> "인증 실패 (HTTP $statusCode · $profileName): API 키 또는 권한을 확인하세요." +
                if (detail.isNotBlank()) " ($detail)" else ""
            404 -> "엔드포인트를 찾을 수 없습니다 (HTTP 404): $url"
            429 -> "요청 한도 초과 (HTTP 429 · $profileName): 잠시 후 다시 시도하세요." +
                if (detail.isNotBlank()) " ($detail)" else ""
            in 500..599 -> "AI 서버 내부 오류 (HTTP $statusCode · $profileName): ${detail.ifBlank { "서버 응답 지연" }}"
            else -> "HTTP $statusCode 오류 ($profileName): ${detail.ifBlank { "응답 본문 없음" }}"
        }
    }

    private fun mapNetworkException(err: Throwable, profileName: String): Throwable {
        if (err is MissingApiKeyException ||
            err is MissingModelSelectionException ||
            err is IllegalStateException ||
            err is IllegalArgumentException
        ) {
            return err
        }
        val msg = when (err) {
            is SocketTimeoutException ->
                "연결 시간 초과 ($profileName): 서버 응답이 20초 내에 도착하지 않았습니다. 엔드포인트 주소 및 네트워크 상태를 확인하세요."
            is UnknownHostException ->
                "호스트를 찾을 수 없습니다 ($profileName): 도메인 주소 또는 인터넷 연결을 확인하세요."
            is ConnectException ->
                "서버에 연결할 수 없습니다 ($profileName): 로컬 브릿지/Ollama 실행 여부 및 포트를 확인하세요."
            is SSLException ->
                "보안(TLS/SSL) 연결 실패 ($profileName): HTTPS 인증서 또는 엔드포인트 주소를 확인하세요."
            else ->
                "네트워크 오류 ($profileName): ${err.message ?: err.javaClass.simpleName}"
        }
        return IllegalStateException(msg, err)
    }

    internal fun normalizeBaseUrl(rawEndpoint: String): String =
        rawEndpoint.trim().trimEnd('/')

    private fun validateEndpoint(rawEndpoint: String): String {
        val endpoint = normalizeBaseUrl(rawEndpoint)
        require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            "유효한 HTTP/HTTPS AI 엔드포인트 URL이 필요합니다: $endpoint"
        }
        return endpoint
    }

    internal fun isKimiCodeMode(profile: AiCliProfile, normalizedEndpoint: String): Boolean =
        profile.cliType == AiCliProfile.CLI_TYPE_KIMI && (
            profile.providerSubtype == AiCliProfile.KIMI_SUBTYPE_CODE ||
                normalizedEndpoint.lowercase().contains("api.kimi.com/coding")
            )

    private fun validateRemoteKey(profile: AiCliProfile, endpoint: String) {
        val isLocal = isLocalLoopback(endpoint) ||
            (profile.cliType == AiCliProfile.CLI_TYPE_KIMI &&
                profile.providerSubtype == AiCliProfile.KIMI_SUBTYPE_LOCAL)

        val requiresRemoteKey = profile.cliType in setOf(
            AiCliProfile.CLI_TYPE_GEMINI,
            AiCliProfile.CLI_TYPE_CLAUDE,
            AiCliProfile.CLI_TYPE_OPENAI,
            AiCliProfile.CLI_TYPE_KIMI,
        ) && !isLocal

        if (requiresRemoteKey && profile.apiKey.isBlank()) {
            throw MissingApiKeyException(profile.name)
        }
    }

    internal fun buildModelsFetchSpec(
        profile: AiCliProfile,
        cleanEndpoint: String,
    ): Pair<String, Map<String, String>> {
        val trimmed = normalizeBaseUrl(cleanEndpoint)
        return when (profile.cliType) {
            AiCliProfile.CLI_TYPE_GEMINI -> {
                val baseWithoutAction = trimmed
                    .substringBefore(":generateContent")
                    .substringBefore("?")
                    .trimEnd('/')
                val modelsEndpoint = if (baseWithoutAction.endsWith("/models")) {
                    baseWithoutAction
                } else {
                    "$baseWithoutAction/models"
                }
                val encodedKey = URLEncoder.encode(profile.apiKey.trim(), "UTF-8")
                val fullUrl = if (profile.apiKey.isNotBlank()) {
                    "$modelsEndpoint?key=$encodedKey"
                } else {
                    modelsEndpoint
                }
                fullUrl to emptyMap()
            }

            AiCliProfile.CLI_TYPE_OLLAMA -> {
                val root = trimmed
                    .removeSuffix("/api/chat")
                    .removeSuffix("/api/generate")
                    .removeSuffix("/api/tags")
                    .trimEnd('/')
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }
                }
                "$root/api/tags" to headers
            }

            AiCliProfile.CLI_TYPE_CLAUDE -> {
                val base = trimmed
                    .removeSuffix("/messages")
                    .removeSuffix("/models")
                    .trimEnd('/')
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("x-api-key", profile.apiKey.trim())
                    }
                    put("anthropic-version", "2023-06-01")
                }
                "$base/models" to headers
            }

            else -> {
                // Kimi API, Kimi Code, Local Kimi CLI, OpenAI/Codex CLI, Custom OpenAI-compatible CLI bridges
                val base = trimmed
                    .removeSuffix("/chat/completions")
                    .removeSuffix("/completions")
                    .removeSuffix("/responses")
                    .removeSuffix("/models")
                    .trimEnd('/')
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }
                    if (isKimiCodeMode(profile, trimmed)) {
                        put("User-Agent", KIMI_CODE_USER_AGENT)
                    }
                }
                "$base/models" to headers
            }
        }
    }

    internal fun parseAvailableModelsResponse(
        cliType: String,
        rawJson: String,
    ): List<String> {
        val root = runCatching { jsonCodec.parseToJsonElement(rawJson).jsonObject }.getOrNull()
            ?: return emptyList()

        return when (cliType) {
            AiCliProfile.CLI_TYPE_GEMINI -> {
                val modelsArr = root["models"]?.jsonArray ?: return emptyList()
                modelsArr.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    val rawName = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                    if (rawName.isEmpty()) return@mapNotNull null
                    val methods = obj["supportedGenerationMethods"]?.jsonArray
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                        .orEmpty()
                    if (methods.isNotEmpty() && "generateContent" !in methods) {
                        return@mapNotNull null
                    }
                    rawName.removePrefix("models/")
                }.distinct()
            }

            AiCliProfile.CLI_TYPE_OLLAMA -> {
                val modelsArr = root["models"]?.jsonArray ?: return emptyList()
                modelsArr.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    (obj["name"] ?: obj["model"])?.jsonPrimitive?.contentOrNull?.trim()
                }.filter { it.isNotEmpty() }.distinct()
            }

            else -> {
                val dataArr = root["data"]?.jsonArray ?: root["models"]?.jsonArray ?: return emptyList()
                dataArr.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    (obj["id"] ?: obj["name"])?.jsonPrimitive?.contentOrNull?.trim()
                }.filter { it.isNotEmpty() }.distinct()
            }
        }
    }

    /**
     * Detects OpenAI reasoning models (o1 / o3 / o4 series) that reject custom `temperature` values
     * and expect `developer` instead of `system` role in Chat Completions payloads.
     */
    internal fun isReasoningModelWithoutTemperature(cliType: String, modelName: String): Boolean {
        val m = modelName.trim().lowercase()
        if (m.isEmpty()) return false
        val isOpenAiSeries = m == "o1" || m.startsWith("o1-") ||
            m == "o3" || m.startsWith("o3-") ||
            m == "o4" || m.startsWith("o4-")
        return isOpenAiSeries && (cliType == AiCliProfile.CLI_TYPE_OPENAI || cliType == AiCliProfile.CLI_TYPE_CUSTOM)
    }

    /**
     * Sanitizes and clamps temperature strictly into `[0.0, 1.0]` (rounded to 2 decimal places so
     * float-to-double rounding never produces `1.0000001`), preventing `HTTP 400: temperature must
     * not be greater than 1.000000` on Kimi CLI / Moonshot and strict OpenAI-compatible endpoints.
     * Returns `null` when the target reasoning model rejects the `temperature` parameter.
     */
    internal fun sanitizeTemperature(
        rawTemp: Float,
        cliType: String,
        modelName: String,
    ): Double? {
        if (isReasoningModelWithoutTemperature(cliType, modelName)) {
            return null
        }
        val m = modelName.trim().lowercase()
        if (cliType == AiCliProfile.CLI_TYPE_KIMI && m.contains("thinking")) {
            // kimi-k2-thinking / thinking models strictly require temperature <= 1.0 (recommended 1.0)
            return ((rawTemp.coerceIn(0f, 1.0f) * 100f).roundToInt() / 100.0).coerceIn(0.0, 1.0)
        }
        val clamped = rawTemp.coerceIn(0f, 1.0f)
        return ((clamped * 100f).roundToInt() / 100.0).coerceIn(0.0, 1.0)
    }

    internal fun buildHttpSpec(
        profile: AiCliProfile,
        cleanEndpoint: String,
        turns: List<AiChatTurn>,
    ): Triple<String, Map<String, String>, String> {
        val trimmed = normalizeBaseUrl(cleanEndpoint)
        val systemText = profile.systemPrompt.ifBlank { AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT }
        val cleanModel = profile.selectedModel.trim().removePrefix("models/")
        val sanitizedTemp = sanitizeTemperature(profile.temperature, profile.cliType, cleanModel)
        val cleanTurns = turns.filter { it.text.isNotBlank() }

        return when (profile.cliType) {
            AiCliProfile.CLI_TYPE_GEMINI -> {
                val baseUrl = if (trimmed.contains(":generateContent")) {
                    trimmed.substringBefore("?")
                } else {
                    val cleanBase = trimmed.substringBefore("?").trimEnd('/')
                    val modelsBase = if (cleanBase.endsWith("/models")) cleanBase else "$cleanBase/models"
                    "$modelsBase/$cleanModel:generateContent"
                }
                val url = if (profile.apiKey.isNotBlank()) {
                    val encodedKey = URLEncoder.encode(profile.apiKey.trim(), "UTF-8")
                    "$baseUrl?key=$encodedKey"
                } else {
                    baseUrl
                }
                val body = buildJsonObject {
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            addJsonObject { put("text", systemText) }
                        })
                    })
                    put("contents", buildJsonArray {
                        cleanTurns.forEach { turn ->
                            addJsonObject {
                                put("role", if (turn.isUser) "user" else "model")
                                put("parts", buildJsonArray {
                                    addJsonObject { put("text", turn.text) }
                                })
                            }
                        }
                    })
                    if (sanitizedTemp != null) {
                        put("generationConfig", buildJsonObject {
                            put("temperature", sanitizedTemp)
                        })
                    }
                }.toString()
                Triple(url, emptyMap(), body)
            }

            AiCliProfile.CLI_TYPE_CLAUDE -> {
                val base = trimmed
                    .removeSuffix("/messages")
                    .removeSuffix("/models")
                    .trimEnd('/')
                val url = "$base/messages"
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("x-api-key", profile.apiKey.trim())
                    }
                    put("anthropic-version", "2023-06-01")
                }
                val body = buildJsonObject {
                    put("model", cleanModel)
                    put("max_tokens", 1400)
                    if (sanitizedTemp != null) {
                        put("temperature", sanitizedTemp)
                    }
                    put("system", systemText)
                    put("messages", buildJsonArray {
                        cleanTurns.forEach { turn ->
                            addJsonObject {
                                put("role", if (turn.isUser) "user" else "assistant")
                                put("content", turn.text)
                            }
                        }
                    })
                }.toString()
                Triple(url, headers, body)
            }

            AiCliProfile.CLI_TYPE_OLLAMA -> {
                val url = if (trimmed.endsWith("/api/chat") || trimmed.endsWith("/api/generate")) {
                    trimmed
                } else {
                    val root = trimmed.removeSuffix("/api/tags").trimEnd('/')
                    "$root/api/chat"
                }
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }
                }
                val body = buildJsonObject {
                    put("model", cleanModel)
                    put("stream", false)
                    if (sanitizedTemp != null) {
                        put("options", buildJsonObject {
                            put("temperature", sanitizedTemp)
                        })
                    }
                    put("messages", buildJsonArray {
                        addJsonObject {
                            put("role", "system")
                            put("content", systemText)
                        }
                        cleanTurns.forEach { turn ->
                            addJsonObject {
                                put("role", if (turn.isUser) "user" else "assistant")
                                put("content", turn.text)
                            }
                        }
                    })
                }.toString()
                Triple(url, headers, body)
            }

            else -> {
                // Kimi API, Kimi Code, Local Kimi CLI, Codex / OpenAI CLI, Custom OpenAI-compatible CLI bridge
                val url = if (trimmed.endsWith("/chat/completions") ||
                    trimmed.endsWith("/completions") ||
                    trimmed.endsWith("/responses")
                ) {
                    trimmed
                } else {
                    val base = trimmed.removeSuffix("/models").trimEnd('/')
                    "$base/chat/completions"
                }
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }
                    if (isKimiCodeMode(profile, trimmed)) {
                        put("User-Agent", KIMI_CODE_USER_AGENT)
                    }
                }
                val isReasoningModel = isReasoningModelWithoutTemperature(profile.cliType, cleanModel)
                val body = buildJsonObject {
                    put("model", cleanModel)
                    if (sanitizedTemp != null) {
                        put("temperature", sanitizedTemp)
                    }
                    put("messages", buildJsonArray {
                        addJsonObject {
                            put("role", if (isReasoningModel) "developer" else "system")
                            put("content", systemText)
                        }
                        cleanTurns.forEach { turn ->
                            addJsonObject {
                                put("role", if (turn.isUser) "user" else "assistant")
                                put("content", turn.text)
                            }
                        }
                    })
                }.toString()
                Triple(url, headers, body)
            }
        }
    }

    private fun extractResponseText(cliType: String, rawJson: String): String {
        val root = runCatching { jsonCodec.parseToJsonElement(rawJson).jsonObject }.getOrNull()
            ?: return rawJson

        return when (cliType) {
            AiCliProfile.CLI_TYPE_GEMINI -> extractGeminiText(root)
                ?: extractFallbackAnyText(root)
                ?: ""
            AiCliProfile.CLI_TYPE_CLAUDE -> extractClaudeText(root)
                ?: extractFallbackAnyText(root)
                ?: ""
            AiCliProfile.CLI_TYPE_OLLAMA -> extractOllamaText(root)
                ?: extractFallbackAnyText(root)
                ?: ""
            else -> extractOpenAiText(root)
                ?: extractFallbackAnyText(root)
                ?: ""
        }
    }

    private fun extractGeminiText(root: JsonObject): String? {
        val candidates = root["candidates"]?.jsonArray ?: return null
        val first = candidates.firstOrNull()?.jsonObject ?: return null
        val parts = first["content"]?.jsonObject?.get("parts")?.jsonArray ?: return null
        val joined = parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            .joinToString("\n")
        return joined.takeIf { it.isNotBlank() }
    }

    private fun extractClaudeText(root: JsonObject): String? {
        val contentArr = root["content"]?.jsonArray ?: return null
        val joined = contentArr.mapNotNull { block ->
            block.jsonObject["text"]?.jsonPrimitive?.contentOrNull
        }.joinToString("\n")
        return joined.takeIf { it.isNotBlank() }
    }

    private fun extractOllamaText(root: JsonObject): String? {
        val msgContent = root["message"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        if (!msgContent.isNullOrBlank()) return msgContent
        return root["response"]?.jsonPrimitive?.contentOrNull
    }

    private fun extractOpenAiText(root: JsonObject): String? {
        val choices = root["choices"]?.jsonArray
        val firstChoice = choices?.firstOrNull()?.jsonObject
        val msgObj = firstChoice?.get("message")?.jsonObject
        val msgContent = msgObj?.get("content")?.jsonPrimitive?.contentOrNull
        if (!msgContent.isNullOrBlank()) return msgContent
        val reasoningContent = msgObj?.get("reasoning_content")?.jsonPrimitive?.contentOrNull
        if (!reasoningContent.isNullOrBlank()) return reasoningContent
        val choiceText = firstChoice?.get("text")?.jsonPrimitive?.contentOrNull
        if (!choiceText.isNullOrBlank()) return choiceText
        return root["output_text"]?.jsonPrimitive?.contentOrNull
    }

    private fun extractFallbackAnyText(root: JsonObject): String? {
        return extractOpenAiText(root)
            ?: extractGeminiText(root)
            ?: extractClaudeText(root)
            ?: extractOllamaText(root)
            ?: root["text"]?.jsonPrimitive?.contentOrNull
            ?: root["answer"]?.jsonPrimitive?.contentOrNull
    }

    private fun extractErrorMessage(rawJson: String): String {
        val root = runCatching { jsonCodec.parseToJsonElement(rawJson).jsonObject }.getOrNull()
            ?: return ""
        val errorObj = root["error"]
        return when {
            errorObj is JsonObject -> errorObj["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
            errorObj != null -> errorObj.jsonPrimitive.contentOrNull.orEmpty()
            else -> root["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }
    }

    private fun isLocalLoopback(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("http://127.0.0.1") ||
            lower.startsWith("http://localhost") ||
            lower.startsWith("http://10.0.2.2") ||
            lower.startsWith("http://[::1]")
    }

    private fun buildPromptPayload(
        pageTitle: String,
        pageUrl: String,
        pageContentText: String,
        userQuestion: String?,
    ): String {
        val clippedContent = pageContentText.take(6500)
        return buildString {
            appendLine("[웹페이지 정보]")
            appendLine("제목: ${pageTitle.ifBlank { "제목 없음" }}")
            appendLine("URL: $pageUrl")
            appendLine()
            if (!userQuestion.isNullOrBlank()) {
                appendLine("[사용자 질문]")
                appendLine(userQuestion.trim())
                appendLine()
            } else {
                appendLine("[요청]")
                appendLine("아래 웹페이지 내용을 바탕으로 한국어로 핵심 요약 3~5개와 주요 포인트를 깔끔하게 정리해 주세요.")
                appendLine()
            }
            appendLine("[페이지 본문 발췌]")
            appendLine(clippedContent.ifBlank { "(추출된 본문 텍스트 없음)" })
        }
    }
}
