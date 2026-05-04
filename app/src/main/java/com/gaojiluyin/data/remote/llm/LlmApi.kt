package com.gaojiluyin.data.remote.llm

interface LlmApi {
    val providerId: String
    suspend fun organizeTranscript(transcript: String): Result<OrganizedDocument>
}

data class OrganizedDocument(
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val organizedContent: String,
    val tags: List<String>
)

internal const val SYSTEM_PROMPT = """你是一个文档整理助手。请将以下录音转写文本整理成结构化文档。

请以JSON格式返回，包含以下字段：
- title: 简短标题
- summary: 100字以内的摘要
- key_points: 关键要点数组
- organized_content: 整理后的完整内容（使用Markdown格式）
- tags: 相关标签数组

只返回JSON，不要其他内容。"""

internal fun parseOrganizedDocument(content: String): OrganizedDocument {
    val cleanContent = content.trim().removePrefix("```json").removeSuffix("```").trim()
    val docJson = org.json.JSONObject(cleanContent)
    return OrganizedDocument(
        title = docJson.optString("title", "未命名"),
        summary = docJson.optString("summary", ""),
        keyPoints = jsonArrayToList(docJson.optJSONArray("key_points")),
        organizedContent = docJson.optString("organized_content", ""),
        tags = jsonArrayToList(docJson.optJSONArray("tags"))
    )
}

internal fun jsonArrayToList(array: org.json.JSONArray?): List<String> {
    if (array == null) return emptyList()
    return (0 until array.length()).map { array.getString(it) }
}
