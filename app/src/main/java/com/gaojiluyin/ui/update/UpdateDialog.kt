package com.gaojiluyin.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaojiluyin.data.remote.update.UpdateState
import com.gaojiluyin.data.remote.update.VersionInfo

@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val state by viewModel.updateState.collectAsState()

    when (val currentState = state) {
        is UpdateState.Available -> {
            UpdateAvailableDialog(
                info = currentState.info,
                onDownload = { viewModel.startDownload(currentState.info.apkUrl) },
                onSkip = {
                    if (!currentState.info.forceUpdate) onDismiss()
                }
            )
        }
        is UpdateState.Downloading -> {
            DownloadProgressDialog(progress = currentState.progress)
        }
        is UpdateState.Downloaded -> {
            InstallDialog(
                onInstall = { viewModel.installUpdate(currentState.file) },
                onLater = onDismiss
            )
        }
        else -> {}
    }
}

@Composable
private fun UpdateAvailableDialog(
    info: VersionInfo,
    onDownload: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("发现新版本 ${info.versionName}") },
        text = {
            Column {
                Text("更新内容:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(info.changelog, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "大小: ${formatFileSize(info.apkSize)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Text("立即更新")
            }
        },
        dismissButton = {
            if (!info.forceUpdate) {
                OutlinedButton(onClick = onSkip) {
                    Text("稍后再说")
                }
            }
        }
    )
}

@Composable
private fun DownloadProgressDialog(progress: Int) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("正在下载更新") },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("$progress%", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun InstallDialog(
    onInstall: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("下载完成") },
        text = { Text("新版本已下载完成，是否立即安装？") },
        confirmButton = {
            Button(onClick = onInstall) {
                Text("立即安装")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onLater) {
                Text("稍后安装")
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
