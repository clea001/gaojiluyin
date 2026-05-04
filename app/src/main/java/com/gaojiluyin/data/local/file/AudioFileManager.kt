package com.gaojiluyin.data.local.file

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioDir: File
        get() = File(context.filesDir, "audio").also { it.mkdirs() }

    fun createRecordingFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(audioDir, "recording_${timestamp}.m4a")
    }

    fun createWavFile(m4aFile: File): File {
        val wavName = m4aFile.nameWithoutExtension + ".wav"
        return File(audioDir, wavName)
    }

    fun deleteFile(path: String) {
        File(path).delete()
    }

    fun getFileSize(path: String): Long = File(path).length()
}
