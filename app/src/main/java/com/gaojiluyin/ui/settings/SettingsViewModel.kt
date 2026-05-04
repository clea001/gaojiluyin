package com.gaojiluyin.ui.settings

import androidx.lifecycle.ViewModel
import com.gaojiluyin.util.ApiKeyProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) : ViewModel() {

    private val _claudeKey = MutableStateFlow("")
    val claudeKey: StateFlow<String> = _claudeKey

    private val _openaiKey = MutableStateFlow("")
    val openaiKey: StateFlow<String> = _openaiKey

    private val _claudeModel = MutableStateFlow("")
    val claudeModel: StateFlow<String> = _claudeModel

    init {
        _claudeKey.value = apiKeyProvider.getClaudeKey()
        _openaiKey.value = apiKeyProvider.getOpenAIKey()
        _claudeModel.value = apiKeyProvider.getClaudeModel()
    }

    fun setClaudeKey(key: String) {
        _claudeKey.value = key
        apiKeyProvider.setClaudeKey(key)
    }

    fun setOpenAIKey(key: String) {
        _openaiKey.value = key
        apiKeyProvider.setOpenAIKey(key)
    }

    fun setClaudeModel(model: String) {
        _claudeModel.value = model
        apiKeyProvider.setClaudeModel(model)
    }
}
