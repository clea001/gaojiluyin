package com.gaojiluyin.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gaojiluyin.data.local.db.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY created_at DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Insert
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("UPDATE recordings SET status = :status, error_message = :error, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String? = null, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)
}
