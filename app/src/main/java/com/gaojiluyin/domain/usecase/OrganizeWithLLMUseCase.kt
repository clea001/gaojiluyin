package com.gaojiluyin.domain.usecase

import android.util.Log
import com.gaojiluyin.data.local.db.entity.DocumentEntity
import com.gaojiluyin.data.remote.claude.ClaudeApi
import com.gaojiluyin.data.remote.claude.OrganizedDocument
import com.gaojiluyin.data.remote.openai.OpenAIApi
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrganizeWithLLMUseCase @Inject constructor(
    private val claudeApi: ClaudeApi,
    private val openAIApi: OpenAIApi
) {
    suspend fun execute(transcript: String): Result<OrganizedDocument> {
        val claudeResult = claudeApi.organizeTranscript(transcript)
        if (claudeResult.isSuccess) {
            return claudeResult
        }

        Log.w("LLM", "Claude failed, trying OpenAI", claudeResult.exceptionOrNull())
        val openAIResult = openAIApi.organizeTranscript(transcript)
        return openAIResult
    }

    fun toDocumentEntity(recordingId: Long, transcript: String, doc: OrganizedDocument, provider: String, model: String): DocumentEntity {
        return DocumentEntity(
            recordingId = recordingId,
            rawTranscript = transcript,
            title = doc.title,
            summary = doc.summary,
            keyPoints = JSONArray(doc.keyPoints).toString(),
            organizedContent = doc.organizedContent,
            tags = JSONArray(doc.tags).toString(),
            llmProvider = provider,
            llmModel = model
        )
    }
}
