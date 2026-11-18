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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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
 * Supports Gemini CLI, Claude CLI, Codex / OpenAI CLI, Kimi CLI, Ollama local CLI,
 * and Custom OpenAI-compatible CLI bridges with dynamic model discovery and zero GMS/Android dependencies.
 */
object AiCliHttpClient {

    private val jsonCodec = Json {
        ignoreUnknownKeys = true
    }

    suspend fun fetchAvailableModels(
        profile: AiCliProfile,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = validateEndpoint(profile.endpointOrCommand)
            validateRemoteKey(profile, endpoint)

            val (modelsUrl, headers) = buildModelsFetchSpec(profile, endpoint)
            val connection = (URL(modelsUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 20_000
                doInput = true
                setRequestProperty("Accept", "application/json")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            try {
                val statusCode = connection.responseCode
                val rawStream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val rawBody = rawStream?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                }.orEmpty()

                if (statusCode !in 200..299) {
                    val serverError = extractErrorMessage(rawBody)
                    val reason = when (statusCode) {
                        401, 403 -> "인증 실패 (HTTP $statusCode): API 키 또는 권한을 확인하세요."
                        404 -> "모델 목록 엔드포인트(HTTP 404)를 찾을 수 없습니다: $modelsUrl"
                        else -> "HTTP $statusCode 오류: ${serverError.ifBlank { rawBody.take(200) }}"
                    }
                    throw IllegalStateException(reason)
                }

                val parsedModels = parseAvailableModelsResponse(profile.cliType, rawBody)
                if (parsedModels.isEmpty()) {
                    throw IllegalStateException("서버에서 사용 가능한 모델 목록을 찾지 못했습니다.")
                }
                parsedModels
            } finally {
                connection.disconnect()
            }
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
        }
    }

    private fun executeChatPost(
        profile: AiCliProfile,
        endpoint: String,
        turns: List<AiChatTurn>,
    ): AiCliResponse {
        val (targetUrl, headers, requestBody) = buildHttpSpec(profile, endpoint, turns)
        val connection = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val statusCode = connection.responseCode
            val rawStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val rawBody = rawStream?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            }.orEmpty()

            if (statusCode !in 200..299) {
                val serverError = extractErrorMessage(rawBody)
                throw IllegalStateException(
                    "HTTP $statusCode 오류 (${profile.name}): ${serverError.ifBlank { rawBody.take(220) }}"
                )
            }

            val parsedText = extractResponseText(profile.cliType, rawBody).trim()
            if (parsedText.isEmpty()) {
                throw IllegalStateException("AI 응답 본문이 비어 있습니다.")
            }

            return AiCliResponse(
                text = parsedText,
                profileName = profile.name,
                model = profile.selectedModel,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun validateEndpoint(rawEndpoint: String): String {
        val endpoint = rawEndpoint.trim()
        require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            "유효한 HTTP/HTTPS AI 엔드포인트 URL이 필요합니다: $endpoint"
        }
        return endpoint
    }

    private fun validateRemoteKey(profile: AiCliProfile, endpoint: String) {
        val requiresRemoteKey = profile.cliType in setOf(
            AiCliProfile.CLI_TYPE_GEMINI,
            AiCliProfile.CLI_TYPE_CLAUDE,
            AiCliProfile.CLI_TYPE_OPENAI,
            AiCliProfile.CLI_TYPE_KIMI,
        ) && !isLocalLoopback(endpoint)

        if (requiresRemoteKey && profile.apiKey.isBlank()) {
            throw MissingApiKeyException(profile.name)
        }
    }

    private fun buildModelsFetchSpec(
        profile: AiCliProfile,
        cleanEndpoint: String,
    ): Pair<String, Map<String, String>> {
        val trimmed = cleanEndpoint.trimEnd('/')
        return when (profile.cliType) {
            AiCliProfile.CLI_TYPE_GEMINI -> {
                val baseWithoutAction = trimmed
                    .substringBefore(":generateContent")
                    .trimEnd('/')
                val modelsEndpoint = if (baseWithoutAction.endsWith("/models")) {
                    baseWithoutAction
                } else {
                    "$baseWithoutAction/models"
                }
                val encodedKey = URLEncoder.encode(profile.apiKey.trim(), "UTF-8")
                val fullUrl = if (profile.apiKey.isNotBlank()) {
                    if (modelsEndpoint.contains('?')) {
                        "$modelsEndpoint&key=$encodedKey"
                    } else {
                        "$modelsEndpoint?key=$encodedKey"
                    }
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
                // Kimi CLI, OpenAI/Codex CLI, Custom OpenAI-compatible CLI bridges
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

    private fun buildHttpSpec(
        profile: AiCliProfile,
        cleanEndpoint: String,
        turns: List<AiChatTurn>,
    ): Triple<String, Map<String, String>, String> {
        val trimmed = cleanEndpoint.trimEnd('/')
        val systemText = profile.systemPrompt.ifBlank { AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT }
        val temp = profile.temperature.coerceIn(0f, 1.5f).toDouble()
        val cleanModel = profile.selectedModel.trim().removePrefix("models/")

        return when (profile.cliType) {
            AiCliProfile.CLI_TYPE_GEMINI -> {
                val url = if (trimmed.contains(":generateContent")) {
                    trimmed
                } else {
                    val modelsBase = if (trimmed.endsWith("/models")) trimmed else "$trimmed/models"
                    "$modelsBase/$cleanModel:generateContent"
                }
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("x-goog-api-key", profile.apiKey.trim())
                    }
                }
                val body = buildJsonObject {
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            addJsonObject { put("text", systemText) }
                        })
                    })
                    put("contents", buildJsonArray {
                        turns.forEach { turn ->
                            addJsonObject {
                                put("role", if (turn.isUser) "user" else "model")
                                put("parts", buildJsonArray {
                                    addJsonObject { put("text", turn.text) }
                                })
                            }
                        }
                    })
                    put("generationConfig", buildJsonObject {
                        put("temperature", temp)
                    })
                }.toString()
                Triple(url, headers, body)
            }

            AiCliProfile.CLI_TYPE_CLAUDE -> {
                val url = if (trimmed.endsWith("/messages")) {
                    trimmed
                } else {
                    "$trimmed/messages"
                }
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("x-api-key", profile.apiKey.trim())
                    }
                    put("anthropic-version", "2023-06-01")
                }
                val body = buildJsonObject {
                    put("model", cleanModel)
                    put("max_tokens", 1400)
                    put("temperature", temp.coerceAtMost(1.0))
                    put("system", systemText)
                    put("messages", buildJsonArray {
                        turns.forEach { turn ->
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
                    "${trimmed.removeSuffix("/api/tags")}/api/chat"
                }
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }
                }
                val body = buildJsonObject {
                    put("model", cleanModel)
                    put("stream", false)
                    put("options", buildJsonObject {
                        put("temperature", temp)
                    })
                    put("messages", buildJsonArray {
                        addJsonObject {
                            put("role", "system")
                            put("content", systemText)
                        }
                        turns.forEach { turn ->
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
                // Kimi CLI, Codex / OpenAI CLI, and Custom OpenAI-compatible CLI bridge
                val url = if (trimmed.endsWith("/chat/completions") ||
                    trimmed.endsWith("/completions") ||
                    trimmed.endsWith("/responses")
                ) {
                    trimmed
                } else {
                    "${trimmed.removeSuffix("/models")}/chat/completions"
                }
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }
                }
                val body = buildJsonObject {
                    put("model", cleanModel)
                    put("temperature", temp)
                    put("messages", buildJsonArray {
                        addJsonObject {
                            put("role", "system")
                            put("content", systemText)
                        }
                        turns.forEach { turn ->
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
        val msgContent = firstChoice?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        if (!msgContent.isNullOrBlank()) return msgContent
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
