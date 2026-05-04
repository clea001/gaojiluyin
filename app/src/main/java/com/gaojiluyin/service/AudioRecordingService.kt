package com.gaojiluyin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import com.gaojiluyin.MainActivity
import com.gaojiluyin.R
import com.gaojiluyin.data.local.file.AudioFileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class AudioRecordingService : Service() {

    @Inject lateinit var audioFileManager: AudioFileManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var outputStream: FileOutputStream? = null
    private var currentFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        _state.value = RecordingState.Recording(0L)
        currentFile = audioFileManager.createWavRecordingFile()
        outputStream = FileOutputStream(currentFile!!)

        val sampleRate = 16000  // Whisper expects 16kHz
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )

        // Write WAV header placeholder (will be updated on stop)
        writeWavHeader(outputStream!!, sampleRate, 1, 16)

        audioRecord?.startRecording()
        startForeground(NOTIFICATION_ID, createNotification("正在录音…"))

        recordingJob = scope.launch {
            val buffer = ByteArray(bufferSize)
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()

            while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0 && !_isPaused.value) {
                    outputStream?.write(buffer, 0, read)
                    totalBytes += read
                    val elapsed = System.currentTimeMillis() - startTime
                    _state.value = RecordingState.Recording(elapsed)
                }
            }
        }
    }

    private fun writeWavHeader(out: FileOutputStream, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        out.write("RIFF".toByteArray())
        out.write(intToLittleEndian(0))  // placeholder for file size
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(intToLittleEndian(16))
        out.write(shortToLittleEndian(1))  // PCM format
        out.write(shortToLittleEndian(channels.toShort()))
        out.write(intToLittleEndian(sampleRate))
        out.write(intToLittleEndian(byteRate))
        out.write(shortToLittleEndian(blockAlign.toShort()))
        out.write(shortToLittleEndian(bitsPerSample.toShort()))
        out.write("data".toByteArray())
        out.write(intToLittleEndian(0))  // placeholder for data size
    }

    private fun intToLittleEndian(value: Int): ByteArray =
        java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun shortToLittleEndian(value: Short): ByteArray =
        java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(value).array()

    private fun pauseRecording() {
        _isPaused.value = true
        _state.value = RecordingState.Paused((_state.value as? RecordingState.Recording)?.durationMs ?: 0L)
        updateNotification("录音已暂停")
    }

    private fun resumeRecording() {
        _isPaused.value = false
        _state.value = RecordingState.Recording((_state.value as? RecordingState.Paused)?.durationMs ?: 0L)
        updateNotification("正在录音…")
    }

    private fun stopRecording() {
        _state.value = RecordingState.Stopping
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        outputStream?.close()
        outputStream = null

        // Update WAV header with correct file size
        val file = currentFile
        if (file != null && file.exists() && file.length() > 44) {
            updateWavHeader(file)
            _state.value = RecordingState.Completed(file.absolutePath, file.length())
        } else {
            _state.value = RecordingState.Error("录音文件为空")
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateWavHeader(file: File) {
        try {
            val fileSize = file.length()
            val dataSize = fileSize - 44
            val raf = java.io.RandomAccessFile(file, "rw")
            // Update RIFF chunk size at offset 4
            raf.seek(4)
            raf.write(intToLittleEndian((fileSize - 8).toInt()))
            // Update data chunk size at offset 40
            raf.seek(40)
            raf.write(intToLittleEndian(dataSize.toInt()))
            raf.close()
        } catch (e: Exception) {
            // Header update failed, but file is still usable
        }
    }

    override fun onDestroy() {
        scope.cancel()
        audioRecord?.release()
        outputStream?.close()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, createNotification(text))
    }

    companion object {
        const val ACTION_START = "com.gaojiluyin.START"
        const val ACTION_PAUSE = "com.gaojiluyin.PAUSE"
        const val ACTION_RESUME = "com.gaojiluyin.RESUME"
        const val ACTION_STOP = "com.gaojiluyin.STOP"
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 1

        private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val state: StateFlow<RecordingState> = _state

        private val _isPaused = MutableStateFlow(false)

        fun resetState() {
            _state.value = RecordingState.Idle
            _isPaused.value = false
        }
    }
}

sealed class RecordingState {
    data object Idle : RecordingState()
    data class Recording(val durationMs: Long) : RecordingState()
    data class Paused(val durationMs: Long) : RecordingState()
    data object Stopping : RecordingState()
    data class Completed(val filePath: String, val fileSize: Long) : RecordingState()
    data class Error(val message: String) : RecordingState()
}
