package com.gaojiluyin.whisper

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var contextPtr: Long = 0L
    private var isInitialized = false
    private var libraryLoaded = false

    private fun ensureLibraryLoaded() {
        if (!libraryLoaded) {
            try {
                System.loadLibrary("whisper_jni")
                libraryLoaded = true
                Log.i("Whisper", "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("Whisper", "Failed to load native library", e)
                throw e
            }
        }
    }

    suspend fun initialize(modelFileName: String = "ggml-base.bin"): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true

        try {
            ensureLibraryLoaded()
        } catch (e: UnsatisfiedLinkError) {
            Log.e("Whisper", "Library load failed", e)
            return@withContext false
        }

        val modelFile = copyModelFromAssets(modelFileName)
        if (modelFile == null) {
            Log.e("Whisper", "Model file not found in assets")
            return@withContext false
        }

        Log.i("Whisper", "Initializing with model: ${modelFile.absolutePath}, size: ${modelFile.length()}")
        contextPtr = nativeInit(modelFile.absolutePath)
        isInitialized = contextPtr != 0L
        Log.i("Whisper", "Initialized: $isInitialized, contextPtr: $contextPtr")
        isInitialized
    }

    suspend fun transcribe(audioData: FloatArray): String = withContext(Dispatchers.Default) {
        if (!isInitialized || contextPtr == 0L) {
            return@withContext ""
        }
        nativeTranscribe(contextPtr, audioData)
    }

    fun release() {
        if (isInitialized && contextPtr != 0L) {
            nativeFree(contextPtr)
            contextPtr = 0L
            isInitialized = false
        }
    }

    private fun copyModelFromAssets(fileName: String): File? {
        val modelDir = File(context.filesDir, "models")
        modelDir.mkdirs()
        val modelFile = File(modelDir, fileName)

        if (modelFile.exists()) return modelFile

        return try {
            context.assets.open("models/$fileName").use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
            modelFile
        } catch (e: Exception) {
            null
        }
    }

    private external fun nativeInit(modelPath: String): Long
    private external fun nativeTranscribe(contextPtr: Long, audioData: FloatArray): String
    private external fun nativeFree(contextPtr: Long)
}
