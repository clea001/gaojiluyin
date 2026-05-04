package com.gaojiluyin.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaojiluyin.data.remote.update.UpdateManager
import com.gaojiluyin.data.remote.update.UpdateState
import com.gaojiluyin.data.remote.update.VersionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: UpdateManager
) : ViewModel() {

    val updateState: StateFlow<UpdateState> = updateManager.updateState

    fun checkForUpdate() {
        viewModelScope.launch {
            updateManager.checkForUpdate()
        }
    }

    fun startDownload(apkUrl: String) {
        updateManager.startDownload(apkUrl)
    }

    fun installUpdate(file: File) {
        updateManager.installUpdate(file)
    }
}
