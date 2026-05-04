package com.gaojiluyin.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.gaojiluyin.data.remote.llm.LlmProvider
import com.gaojiluyin.data.remote.llm.LlmProviders
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "api_keys",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("api_keys_fallback", Context.MODE_PRIVATE)
        }
    }

    // Primary provider selection
    fun getPrimaryProviderId(): String =
        prefs.getString(KEY_PRIMARY_PROVIDER, LlmProviders.getDefault().id)
            ?: LlmProviders.getDefault().id

    fun setPrimaryProviderId(id: String) {
        prefs.edit().putString(KEY_PRIMARY_PROVIDER, id).apply()
    }

    // Per-provider API key
    fun getApiKey(providerId: String): String =
        prefs.getString("key_$providerId", "") ?: ""

    fun setApiKey(providerId: String, key: String) {
        prefs.edit().putString("key_$providerId", key).apply()
    }

    // Per-provider model
    fun getModel(providerId: String): String {
        val provider = LlmProviders.getById(providerId)
        val default = provider?.defaultModel ?: ""
        return prefs.getString("model_$providerId", default) ?: default
    }

    fun setModel(providerId: String, model: String) {
        prefs.edit().putString("model_$providerId", model).apply()
    }

    // Per-provider base URL
    fun getBaseUrl(providerId: String): String {
        val provider = LlmProviders.getById(providerId)
        val default = provider?.defaultBaseUrl ?: ""
        return prefs.getString("url_$providerId", default) ?: default
    }

    fun setBaseUrl(providerId: String, url: String) {
        prefs.edit().putString("url_$providerId", url).apply()
    }

    // Get all providers that have an API key configured
    fun getConfiguredProviderIds(): List<String> {
        return LlmProviders.ALL.filter { provider ->
            getApiKey(provider.id).isNotBlank()
        }.map { it.id }
    }

    // Check if a specific provider is configured
    fun isProviderConfigured(providerId: String): Boolean =
        getApiKey(providerId).isNotBlank()

    // Legacy compatibility (for migration)
    fun getClaudeKey(): String = getApiKey("claude")
    fun getOpenAIKey(): String = getApiKey("gpt")

    companion object {
        private const val KEY_PRIMARY_PROVIDER = "primary_provider"
    }
}
