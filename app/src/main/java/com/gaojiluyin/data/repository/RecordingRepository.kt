package com.gaojiluyin.data.repository

import com.gaojiluyin.data.local.db.dao.RecordingDao
import com.gaojiluyin.data.local.db.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val recordingDao: RecordingDao
) {
    fun getAllRecordings(): Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    suspend fun getRecordingById(id: Long): RecordingEntity? = recordingDao.getRecordingById(id)

    suspend fun insertRecording(recording: RecordingEntity): Long = recordingDao.insertRecording(recording)

    suspend fun updateRecording(recording: RecordingEntity) = recordingDao.updateRecording(recording)

    suspend fun updateStatus(id: Long, status: String, error: String? = null) =
        recordingDao.updateStatus(id, status, error)

    suspend fun deleteRecording(recording: RecordingEntity) = recordingDao.deleteRecording(recording)

    suspend fun deleteRecordingById(id: Long) = recordingDao.deleteRecordingById(id)
}
