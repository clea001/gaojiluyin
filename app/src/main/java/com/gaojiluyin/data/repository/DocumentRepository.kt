package com.gaojiluyin.data.repository

import com.gaojiluyin.data.local.db.dao.DocumentDao
import com.gaojiluyin.data.local.db.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao
) {
    fun getAllDocuments(): Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    suspend fun getDocumentByRecordingId(recordingId: Long): DocumentEntity? =
        documentDao.getDocumentByRecordingId(recordingId)

    suspend fun insertDocument(document: DocumentEntity): Long = documentDao.insertDocument(document)

    suspend fun updateDocument(document: DocumentEntity) = documentDao.updateDocument(document)
}
