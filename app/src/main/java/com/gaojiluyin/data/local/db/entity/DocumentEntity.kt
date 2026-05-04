package com.gaojiluyin.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(
        entity = RecordingEntity::class,
        parentColumns = ["id"],
        childColumns = ["recording_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["recording_id"], unique = true)]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "recording_id")
    val recordingId: Long,
    @ColumnInfo(name = "raw_transcript")
    val rawTranscript: String,
    val title: String,
    val summary: String,
    @ColumnInfo(name = "key_points")
    val keyPoints: String,
    @ColumnInfo(name = "organized_content")
    val organizedContent: String,
    val tags: String,
    @ColumnInfo(name = "llm_provider")
    val llmProvider: String,
    @ColumnInfo(name = "llm_model")
    val llmModel: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
