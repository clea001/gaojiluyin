package com.gaojiluyin.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gaojiluyin.data.local.db.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE recording_id = :recordingId")
    suspend fun getDocumentByRecordingId(recordingId: Long): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Insert
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)
}
