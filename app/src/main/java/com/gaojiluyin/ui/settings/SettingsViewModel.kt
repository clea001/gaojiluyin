package com.gaojiluyin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaojiluyin.data.remote.llm.LlmProvider
import com.gaojiluyin.data.remote.llm.LlmProviders
import com.gaojiluyin.util.ApiKeyProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider,
    private val client: OkHttpClient
) : ViewModel() {

    private val _primaryProviderId = MutableStateFlow(apiKeyProvider.getPrimaryProviderId())
    val primaryProviderId: StateFlow<String> = _primaryProviderId

    private val _providers = MutableStateFlow(LlmProviders.ALL)
    val providers: StateFlow<List<LlmProvider>> = _providers

    // Currently editing provider
    private val _editingProviderId = MutableStateFlow<String?>(null)
    val editingProviderId: StateFlow<String?> = _editingProviderId

    // Fields for the editing provider
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl

    private val _model = MutableStateFlow("")
    val model: StateFlow<String> = _model

    // Test connection state
    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState

    fun selectPrimaryProvider(id: String) {
        _primaryProviderId.value = id
        apiKeyProvider.setPrimaryProviderId(id)
    }

    fun startEditing(providerId: String) {
        _editingProviderId.value = providerId
        _apiKey.value = apiKeyProvider.getApiKey(providerId)
        _baseUrl.value = apiKeyProvider.getBaseUrl(providerId)
        _model.value = apiKeyProvider.getModel(providerId)
        _testState.value = TestState.Idle
    }

    fun cancelEditing() {
        _editingProviderId.value = null
        _testState.value = TestState.Idle
    }

    fun updateApiKey(key: String) { _apiKey.value = key }
    fun updateBaseUrl(url: String) { _baseUrl.value = url }
    fun updateModel(model: String) { _model.value = model }

    fun save() {
        val providerId = _editingProviderId.value ?: return
        apiKeyProvider.setApiKey(providerId, _apiKey.value)
        apiKeyProvider.setBaseUrl(providerId, _baseUrl.value)
        apiKeyProvider.setModel(providerId, _model.value)
        _editingProviderId.value = null
        _testState.value = TestState.Idle
    }

    fun testConnection() {
        val providerId = _editingProviderId.value ?: return
        val provider = LlmProviders.getById(providerId) ?: return

        viewModelScope.launch {
            _testState.value = TestState.Testing
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = "${_baseUrl.value.trimEnd('/')}/chat/completions"
                    val body = """{"model":"${_model.value}","max_tokens":10,"messages":[{"role":"user","content":"hi"}]}"""
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer ${_apiKey.value}")
                        .addHeader("Content-Type", "application/json")
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }

                if (result.code == 200) {
                    _testState.value = TestState.Success
                } else {
                    _testState.value = TestState.Error("HTTP ${result.code}")
                }
            } catch (e: Exception) {
                _testState.value = TestState.Error(e.message ?: "连接失败")
            }
        }
    }

    fun isProviderConfigured(providerId: String): Boolean =
        apiKeyProvider.isProviderConfigured(providerId)
}

sealed class TestState {
    data object Idle : TestState()
    data object Testing : TestState()
    data object Success : TestState()
    data class Error(val message: String) : TestState()
}
