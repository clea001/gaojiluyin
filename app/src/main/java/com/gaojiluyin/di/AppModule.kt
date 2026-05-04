package com.gaojiluyin.di

import com.gaojiluyin.data.remote.llm.ClaudeApi
import com.gaojiluyin.data.remote.llm.LlmApi
import com.gaojiluyin.data.remote.llm.LlmProvider
import com.gaojiluyin.data.remote.llm.LlmProviders
import com.gaojiluyin.data.remote.llm.OpenAICompatibleApi
import com.gaojiluyin.util.ApiKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLlmApis(
        client: OkHttpClient,
        apiKeyProvider: ApiKeyProvider
    ): List<LlmApi> {
        return LlmProviders.ALL.map { provider ->
            createLlmApi(client, apiKeyProvider, provider)
        }
    }

    private fun createLlmApi(
        client: OkHttpClient,
        apiKeyProvider: ApiKeyProvider,
        provider: LlmProvider
    ): LlmApi {
        val apiKey = apiKeyProvider.getApiKey(provider.id)
        val baseUrl = apiKeyProvider.getBaseUrl(provider.id)
        val model = apiKeyProvider.getModel(provider.id)

        return when (provider.apiFormat) {
            com.gaojiluyin.data.remote.llm.ApiFormat.OPENAI_COMPATIBLE -> OpenAICompatibleApi(
                client = client,
                providerId = provider.id,
                baseUrl = baseUrl,
                model = model,
                apiKey = apiKey
            )
            com.gaojiluyin.data.remote.llm.ApiFormat.CLAUDE -> ClaudeApi(
                client = client,
                providerId = provider.id,
                baseUrl = baseUrl,
                model = model,
                apiKey = apiKey
            )
        }
    }
}
