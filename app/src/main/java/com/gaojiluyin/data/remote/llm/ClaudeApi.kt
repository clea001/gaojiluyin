package com.gaojiluyin.data.remote.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ClaudeApi(
    private val client: OkHttpClient,
    override val providerId: String = "claude",
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String
) : LlmApi {

    companion object {
        private const val API_VERSION = "2023-06-01"
    }

    override suspend fun organizeTranscript(transcript: String): Result<OrganizedDocument> =
        withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("未配置 Claude API Key"))
                }

                val url = "${baseUrl.trimEnd('/')}/messages"

                val body = JSONObject().apply {
                    put("model", model)
                    put("max_tokens", 4096)
                    put("system", SYSTEM_PROMPT)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "以下是需要整理的录音转写文本：\n\n$transcript")
                        })
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", API_VERSION)
                    .addHeader("content-type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.code != 200) {
                    return@withContext Result.failure(
                        Exception("Claude API错误: ${response.code} - $responseBody")
                    )
                }

                val json = JSONObject(responseBody)
                val content = json.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")

                val doc = parseOrganizedDocument(content)
                Result.success(doc)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
