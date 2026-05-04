package com.gaojiluyin.data.remote.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAICompatibleApi(
    private val client: OkHttpClient,
    override val providerId: String,
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String
) : LlmApi {

    override suspend fun organizeTranscript(transcript: String): Result<OrganizedDocument> =
        withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("未配置 $providerId API Key"))
                }

                val url = "${baseUrl.trimEnd('/')}/chat/completions"

                val body = JSONObject().apply {
                    put("model", model)
                    put("max_tokens", 4096)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "以下是需要整理的录音转写文本：\n\n$transcript")
                        })
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.code != 200) {
                    return@withContext Result.failure(
                        Exception("$providerId API错误: ${response.code} - $responseBody")
                    )
                }

                val json = JSONObject(responseBody)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val doc = parseOrganizedDocument(content)
                Result.success(doc)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
