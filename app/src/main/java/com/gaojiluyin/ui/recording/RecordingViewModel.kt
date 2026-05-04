package com.gaojiluyin.ui.recording

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaojiluyin.data.local.db.entity.RecordingEntity
import com.gaojiluyin.data.local.file.AudioFileManager
import com.gaojiluyin.data.remote.llm.LlmProviders
import com.gaojiluyin.data.repository.DocumentRepository
import com.gaojiluyin.data.repository.RecordingRepository
import com.gaojiluyin.domain.usecase.OrganizeWithLLMUseCase
import com.gaojiluyin.domain.usecase.TranscribeAudioUseCase
import com.gaojiluyin.service.AudioRecordingService
import com.gaojiluyin.service.RecordingState
import com.gaojiluyin.util.ApiKeyProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val app: Application,
    private val recordingRepository: RecordingRepository,
    private val documentRepository: DocumentRepository,
    private val audioFileManager: AudioFileManager,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val organizeWithLLMUseCase: OrganizeWithLLMUseCase,
    private val apiKeyProvider: ApiKeyProvider
) : AndroidViewModel(app) {

    val recordingState: StateFlow<RecordingState> = AudioRecordingService.state

    private val _pipelineState = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val pipelineState: StateFlow<PipelineState> = _pipelineState

    private val _lastSavedId = MutableStateFlow<Long?>(null)
    val lastSavedId: StateFlow<Long?> = _lastSavedId

    fun startRecording() {
        _pipelineState.value = PipelineState.Idle
        sendServiceAction(AudioRecordingService.ACTION_START)
    }

    fun pauseRecording() = sendServiceAction(AudioRecordingService.ACTION_PAUSE)
    fun resumeRecording() = sendServiceAction(AudioRecordingService.ACTION_RESUME)
    fun stopRecording() = sendServiceAction(AudioRecordingService.ACTION_STOP)

    fun onRecordingCompleted(filePath: String, fileSize: Long, durationMs: Long) {
        // Prevent duplicate processing
        if (_pipelineState.value != PipelineState.Idle) return

        // Reset service state immediately to prevent re-triggering
        AudioRecordingService.resetState()

        viewModelScope.launch {
            val timestamp = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())
            val entity = RecordingEntity(
                title = "录音 $timestamp",
                audioFilePath = filePath,
                durationMs = durationMs,
                fileSizeBytes = fileSize,
                status = "SAVING"
            )
            val id = recordingRepository.insertRecording(entity)
            _lastSavedId.value = id

            _pipelineState.value = PipelineState.Transcribing
            recordingRepository.updateStatus(id, "TRANSCRIBING")

            try {
                val inputFile = java.io.File(filePath)
                val wavFile = if (filePath.endsWith(".wav")) {
                    inputFile  // Already WAV, use directly
                } else {
                    audioFileManager.createWavFile(inputFile)  // Convert M4A to WAV
                }
                val transcribeResult = transcribeAudioUseCase.execute(filePath, wavFile.absolutePath)

                transcribeResult.fold(
                    onSuccess = { transcript ->
                        if (transcript.isBlank()) {
                            recordingRepository.updateStatus(id, "ERROR", "转写结果为空，请检查Whisper模型")
                            _pipelineState.value = PipelineState.Error("转写结果为空，请检查Whisper模型是否已下载")
                            return@fold
                        }

                        recordingRepository.updateRecording(
                            entity.copy(id = id, wavFilePath = wavFile.absolutePath, status = "ORGANIZING")
                        )

                        _pipelineState.value = PipelineState.Organizing
                        val llmResult = organizeWithLLMUseCase.execute(transcript)

                        llmResult.fold(
                            onSuccess = { doc ->
                                val providerId = apiKeyProvider.getPrimaryProviderId()
                                val providerInfo = LlmProviders.getById(providerId)
                                val docEntity = organizeWithLLMUseCase.toDocumentEntity(
                                    recordingId = id,
                                    transcript = transcript,
                                    doc = doc,
                                    provider = providerInfo?.name ?: providerId,
                                    model = apiKeyProvider.getModel(providerId)
                                )
                                documentRepository.insertDocument(docEntity)
                                recordingRepository.updateStatus(id, "COMPLETED")
                                _pipelineState.value = PipelineState.Completed(id)
                            },
                            onFailure = { error ->
                                // LLM failed, but save the transcript anyway
                                recordingRepository.updateRecording(
                                    entity.copy(id = id, wavFilePath = wavFile.absolutePath, status = "TRANSCRIBED")
                                )
                                recordingRepository.updateStatus(id, "ERROR", "LLM整理失败: ${error.message}")
                                _pipelineState.value = PipelineState.Error("AI整理失败: ${error.message}")
                            }
                        )
                    },
                    onFailure = { error ->
                        recordingRepository.updateStatus(id, "ERROR", "转写失败: ${error.message}")
                        _pipelineState.value = PipelineState.Error("转写失败: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                recordingRepository.updateStatus(id, "ERROR", "处理异常: ${e.message}")
                _pipelineState.value = PipelineState.Error("处理异常: ${e.message}")
            }
        }
    }

    fun resetState() {
        _lastSavedId.value = null
        _pipelineState.value = PipelineState.Idle
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(app, AudioRecordingService::class.java).apply {
            this.action = action
        }
        if (action == AudioRecordingService.ACTION_START) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }
}

sealed class PipelineState {
    data object Idle : PipelineState()
    data object Transcribing : PipelineState()
    data object Organizing : PipelineState()
    data class Completed(val documentId: Long) : PipelineState()
    data class Error(val message: String) : PipelineState()
}
