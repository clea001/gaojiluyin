package com.gaojiluyin.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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

    fun getClaudeKey(): String = prefs.getString(KEY_CLAUDE, "") ?: ""
    fun setClaudeKey(key: String) = prefs.edit().putString(KEY_CLAUDE, key).apply()

    fun getOpenAIKey(): String = prefs.getString(KEY_OPENAI, "") ?: ""
    fun setOpenAIKey(key: String) = prefs.edit().putString(KEY_OPENAI, key).apply()

    fun getClaudeModel(): String = prefs.getString(KEY_CLAUDE_MODEL, "claude-sonnet-4-20250514") ?: "claude-sonnet-4-20250514"
    fun setClaudeModel(model: String) = prefs.edit().putString(KEY_CLAUDE_MODEL, model).apply()

    companion object {
        private const val KEY_CLAUDE = "claude_api_key"
        private const val KEY_OPENAI = "openai_api_key"
        private const val KEY_CLAUDE_MODEL = "claude_model"
    }
}
