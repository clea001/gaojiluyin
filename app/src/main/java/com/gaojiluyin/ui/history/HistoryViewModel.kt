package com.gaojiluyin.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaojiluyin.data.local.db.entity.RecordingEntity
import com.gaojiluyin.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    val recordings: StateFlow<List<RecordingEntity>> = recordingRepository.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(recording)
        }
    }
}
