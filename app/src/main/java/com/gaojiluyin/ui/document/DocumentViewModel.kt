package com.gaojiluyin.ui.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaojiluyin.data.local.db.entity.DocumentEntity
import com.gaojiluyin.data.local.db.entity.RecordingEntity
import com.gaojiluyin.data.repository.DocumentRepository
import com.gaojiluyin.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _recording = MutableStateFlow<RecordingEntity?>(null)
    val recording: StateFlow<RecordingEntity?> = _recording

    private val _document = MutableStateFlow<DocumentEntity?>(null)
    val document: StateFlow<DocumentEntity?> = _document

    fun load(recordingId: Long) {
        viewModelScope.launch {
            _recording.value = recordingRepository.getRecordingById(recordingId)
            _document.value = documentRepository.getDocumentByRecordingId(recordingId)
        }
    }
}
