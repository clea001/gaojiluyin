package com.gaojiluyin.domain.usecase

import com.gaojiluyin.util.AudioConverter
import com.gaojiluyin.whisper.AudioPreprocessor
import com.gaojiluyin.whisper.WhisperEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscribeAudioUseCase @Inject constructor(
    private val whisperEngine: WhisperEngine,
    private val audioConverter: AudioConverter,
    private val audioPreprocessor: AudioPreprocessor
) {
    suspend fun execute(m4aPath: String, wavPath: String): Result<String> {
        return try {
            // Initialize whisper engine (loads model on first call)
            val initialized = whisperEngine.initialize()
            if (!initialized) {
                return Result.failure(Exception("Whisper模型加载失败，请检查模型文件"))
            }

            audioConverter.convertToWav(m4aPath, wavPath)
            val pcmData = audioPreprocessor.loadPcmAsFloat(wavPath)
            val transcript = whisperEngine.transcribe(pcmData)
            if (transcript.isBlank()) {
                Result.failure(Exception("转写结果为空"))
            } else {
                Result.success(transcript)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
