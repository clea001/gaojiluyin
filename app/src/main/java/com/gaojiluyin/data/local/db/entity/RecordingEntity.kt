package com.gaojiluyin.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String,
    @ColumnInfo(name = "wav_file_path")
    val wavFilePath: String? = null,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long = 0,
    val status: String = "RECORDING",
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
