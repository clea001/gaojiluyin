package com.gaojiluyin.domain.usecase

import android.util.Log
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
    suspend fun execute(inputPath: String, wavPath: String): Result<String> {
        return try {
            Log.i("Transcribe", "Starting transcription: input=$inputPath, wav=$wavPath")

            // Initialize whisper engine (loads model on first call)
            val initialized = whisperEngine.initialize()
            if (!initialized) {
                Log.e("Transcribe", "Whisper engine initialization failed")
                return Result.failure(Exception("Whisper模型加载失败，请检查模型文件"))
            }

            // Only convert if input is not already WAV
            if (!inputPath.endsWith(".wav")) {
                Log.i("Transcribe", "Converting M4A to WAV...")
                audioConverter.convertToWav(inputPath, wavPath)
                Log.i("Transcribe", "WAV conversion done, file size: ${java.io.File(wavPath).length()}")
            } else {
                Log.i("Transcribe", "Input is already WAV, skipping conversion")
            }

            Log.i("Transcribe", "Loading PCM data...")
            val pcmData = audioPreprocessor.loadPcmAsFloat(wavPath)
            Log.i("Transcribe", "PCM data loaded: ${pcmData.size} samples")

            Log.i("Transcribe", "Running whisper transcription...")
            val transcript = whisperEngine.transcribe(pcmData)
            Log.i("Transcribe", "Transcription result: ${transcript.take(100)}...")

            if (transcript.isBlank()) {
                Log.w("Transcribe", "Transcription result is empty")
                Result.failure(Exception("转写结果为空"))
            } else {
                Result.success(transcript)
            }
        } catch (e: Exception) {
            Log.e("Transcribe", "Transcription failed", e)
            Result.failure(e)
        }
    }
}
