package com.gaojiluyin.domain.usecase

import android.util.Log
import com.gaojiluyin.data.local.db.entity.DocumentEntity
import com.gaojiluyin.data.remote.llm.LlmApi
import com.gaojiluyin.data.remote.llm.OrganizedDocument
import com.gaojiluyin.util.ApiKeyProvider
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrganizeWithLLMUseCase @Inject constructor(
    private val llmApis: List<@JvmSuppressWildcards LlmApi>,
    private val apiKeyProvider: ApiKeyProvider
) {
    suspend fun execute(transcript: String): Result<OrganizedDocument> {
        val primaryId = apiKeyProvider.getPrimaryProviderId()
        val primaryApi = llmApis.find { it.providerId == primaryId }

        // Try primary provider first
        if (primaryApi != null) {
            val result = primaryApi.organizeTranscript(transcript)
            if (result.isSuccess) return result
            Log.w("LLM", "Primary provider $primaryId failed, trying fallbacks", result.exceptionOrNull())
        }

        // Try other configured providers as fallback
        for (api in llmApis) {
            if (api.providerId == primaryId) continue
            if (!apiKeyProvider.isProviderConfigured(api.providerId)) continue

            val result = api.organizeTranscript(transcript)
            if (result.isSuccess) {
                Log.i("LLM", "Fallback provider ${api.providerId} succeeded")
                return result
            }
            Log.w("LLM", "Provider ${api.providerId} failed", result.exceptionOrNull())
        }

        return Result.failure(Exception("所有LLM提供商均失败，请检查API配置"))
    }

    fun toDocumentEntity(
        recordingId: Long,
        transcript: String,
        doc: OrganizedDocument,
        provider: String,
        model: String
    ): DocumentEntity {
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
