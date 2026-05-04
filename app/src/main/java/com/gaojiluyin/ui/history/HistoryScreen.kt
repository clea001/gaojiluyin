package com.gaojiluyin.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaojiluyin.data.local.db.entity.RecordingEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onRecordingClick: (Long) -> Unit = {}
) {
    val recordings by viewModel.recordings.collectAsState()

    if (recordings.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("暂无录音记录", style = MaterialTheme.typography.bodyLarge)
            Text("点击录音按钮开始", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recordings, key = { it.id }) { recording ->
                RecordingItem(
                    recording = recording,
                    onClick = { onRecordingClick(recording.id) },
                    onDelete = { viewModel.deleteRecording(recording) }
                )
            }
        }
    }
}

@Composable
private fun RecordingItem(
    recording: RecordingEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatDate(recording.createdAt),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "状态: ${recording.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (recording.status == "COMPLETED") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
