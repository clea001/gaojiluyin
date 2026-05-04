package com.gaojiluyin.data.remote.claude

import com.gaojiluyin.util.ApiKeyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeApiImpl @Inject constructor(
    private val client: OkHttpClient,
    private val apiKeyProvider: ApiKeyProvider
) : ClaudeApi {

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val API_VERSION = "2023-06-01"

        private const val SYSTEM_PROMPT = """你是一个文档整理助手。请将以下录音转写文本整理成结构化文档。

请以JSON格式返回，包含以下字段：
- title: 简短标题
- summary: 100字以内的摘要
- key_points: 关键要点数组
- organized_content: 整理后的完整内容（使用Markdown格式）
- tags: 相关标签数组

只返回JSON，不要其他内容。"""
    }

    override suspend fun organizeTranscript(transcript: String): Result<OrganizedDocument> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = apiKeyProvider.getClaudeKey()
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("未配置Claude API Key"))
                }

                val body = JSONObject().apply {
                    put("model", apiKeyProvider.getClaudeModel())
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
                    .url(BASE_URL)
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

                val docJson = JSONObject(content.trim().removePrefix("```json").removeSuffix("```").trim())
                val doc = OrganizedDocument(
                    title = docJson.optString("title", "未命名"),
                    summary = docJson.optString("summary", ""),
                    keyPoints = jsonArrayToList(docJson.optJSONArray("key_points")),
                    organizedContent = docJson.optString("organized_content", ""),
                    tags = jsonArrayToList(docJson.optJSONArray("tags"))
                )
                Result.success(doc)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }
}
