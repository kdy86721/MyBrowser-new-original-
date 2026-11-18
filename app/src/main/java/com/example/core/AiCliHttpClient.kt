package com.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

data class AiCliResponse(
    val text: String,
    val profileName: String,
    val model: String,
)

class MissingApiKeyException(val profileName: String) :
    IllegalStateException("$profileName API 키가 설정되지 않았습니다.")

/**
 * Pure AOSP HttpURLConnection client executing on [Dispatchers.IO].
 * Supports Gemini CLI, Claude CLI, Codex / OpenAI CLI, Ollama local CLI, and Custom REST bridges
 * with zero GMS or Android Context dependencies so it can migrate into Rust FFI later.
 */
object AiCliHttpClient {

    private val jsonCodec = Json {
        ignoreUnknownKeys = true
    }

    suspend fun requestPageAnalysis(
        profile: AiCliProfile,
        pageTitle: String,
        pageUrl: String,
        pageContentText: String,
        userQuestion: String? = null,
    ): Result<AiCliResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = profile.endpointOrCommand.trim()
            require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
                "유효한 HTTP/HTTPS AI 엔드포인트 URL이 필요합니다: $endpoint"
            }

            val requiresRemoteKey = profile.cliType in setOf(
                AiCliProfile.CLI_TYPE_GEMINI,
                AiCliProfile.CLI_TYPE_CLAUDE,
                AiCliProfile.CLI_TYPE_OPENAI,
            ) && !isLocalLoopback(endpoint)

            if (requiresRemoteKey && profile.apiKey.isBlank()) {
                throw MissingApiKeyException(profile.name)
            }

            val promptText = buildPromptPayload(
                pageTitle = pageTitle,
                pageUrl = pageUrl,
                pageContentText = pageContentText,
                userQuestion = userQuestion,
            )

            val (targetUrl, headers, requestBody) = buildHttpSpec(profile, promptText)
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

                AiCliResponse(
                    text = parsedText,
                    profileName = profile.name,
                    model = profile.selectedModel,
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun buildHttpSpec(
        profile: AiCliProfile,
        userPrompt: String,
    ): Triple<String, Map<String, String>, String> {
        val cleanEndpoint = profile.endpointOrCommand.trim()
        val systemText = profile.systemPrompt.ifBlank { AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT }
        val temp = profile.temperature.coerceIn(0f, 1.5f).toDouble()

        return when (profile.cliType) {
            AiCliProfile.CLI_TYPE_GEMINI -> {
                val url = if (cleanEndpoint.contains("generativelanguage.googleapis.com") &&
                    !cleanEndpoint.contains(":generateContent")
                ) {
                    "${cleanEndpoint.trimEnd('/')}/${profile.selectedModel}:generateContent"
                } else {
                    cleanEndpoint
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
                        addJsonObject {
                            put("role", "user")
                            put("parts", buildJsonArray {
                                addJsonObject { put("text", userPrompt) }
                            })
                        }
                    })
                    put("generationConfig", buildJsonObject {
                        put("temperature", temp)
                    })
                }.toString()
                Triple(url, headers, body)
            }

            AiCliProfile.CLI_TYPE_CLAUDE -> {
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("x-api-key", profile.apiKey.trim())
                    }
                    put("anthropic-version", "2023-06-01")
                }
                val body = buildJsonObject {
                    put("model", profile.selectedModel)
                    put("max_tokens", 1200)
                    put("temperature", temp.coerceAtMost(1.0))
                    put("system", systemText)
                    put("messages", buildJsonArray {
                        addJsonObject {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                    })
                }.toString()
                Triple(cleanEndpoint, headers, body)
            }

            AiCliProfile.CLI_TYPE_OLLAMA -> {
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }
                }
                val body = buildJsonObject {
                    put("model", profile.selectedModel)
                    put("stream", false)
                    put("options", buildJsonObject {
                        put("temperature", temp)
                    })
                    put("messages", buildJsonArray {
                        addJsonObject {
                            put("role", "system")
                            put("content", systemText)
                        }
                        addJsonObject {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                    })
                }.toString()
                Triple(cleanEndpoint, headers, body)
            }

            else -> {
                // Codex / OpenAI CLI & Custom OpenAI-compatible CLI bridge
                val headers = buildMap {
                    if (profile.apiKey.isNotBlank()) {
                        put("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }
                }
                val body = buildJsonObject {
                    put("model", profile.selectedModel)
                    put("temperature", temp)
                    put("messages", buildJsonArray {
                        addJsonObject {
                            put("role", "system")
                            put("content", systemText)
                        }
                        addJsonObject {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                    })
                }.toString()
                Triple(cleanEndpoint, headers, body)
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
