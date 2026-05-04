package com.gaojiluyin.ui.recording

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaojiluyin.service.RecordingState
import com.gaojiluyin.ui.theme.CompletedGreen
import com.gaojiluyin.ui.theme.RecordingRed

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onNavigateToDocument: (Long) -> Unit = {}
) {
    val state by viewModel.recordingState.collectAsState()
    val pipelineState by viewModel.pipelineState.collectAsState()
    val lastSavedId by viewModel.lastSavedId.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(state) {
        if (state is RecordingState.Completed) {
            val completed = state as RecordingState.Completed
            viewModel.onRecordingCompleted(completed.filePath, completed.fileSize, 0L)
        }
    }

    LaunchedEffect(pipelineState) {
        if (pipelineState is PipelineState.Completed) {
            val docId = (pipelineState as PipelineState.Completed).documentId
            onNavigateToDocument(docId)
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (state) {
                is RecordingState.Recording -> formatDuration((state as RecordingState.Recording).durationMs)
                is RecordingState.Paused -> formatDuration((state as RecordingState.Paused).durationMs)
                is RecordingState.Stopping -> "处理中…"
                is RecordingState.Completed -> "录音完成"
                is RecordingState.Error -> (state as RecordingState.Error).message
                else -> when (pipelineState) {
                    is PipelineState.Transcribing -> "正在转写…"
                    is PipelineState.Organizing -> "AI整理中…"
                    is PipelineState.Error -> (pipelineState as PipelineState.Error).message
                    else -> "准备录音"
                }
            },
            style = MaterialTheme.typography.headlineLarge,
            color = when {
                state is RecordingState.Recording -> RecordingRed
                pipelineState is PipelineState.Error -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onBackground
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (pipelineState is PipelineState.Transcribing || pipelineState is PipelineState.Organizing) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (pipelineState is PipelineState.Transcribing) "Whisper本地转写中…" else "Claude AI整理中…",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        when (state) {
            is RecordingState.Idle, is RecordingState.Completed, is RecordingState.Error -> {
                if (pipelineState !is PipelineState.Transcribing && pipelineState !is PipelineState.Organizing) {
                    FloatingActionButton(
                        onClick = {
                            if (hasPermission) viewModel.startRecording()
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.size(96.dp),
                        containerColor = RecordingRed,
                        contentColor = Color.White
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "开始录音",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            is RecordingState.Recording, is RecordingState.Paused -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (state is RecordingState.Recording) viewModel.pauseRecording()
                            else viewModel.resumeRecording()
                        }
                    ) {
                        Icon(
                            if (state is RecordingState.Recording) Icons.Default.Pause
                            else Icons.Default.PlayArrow,
                            contentDescription = if (state is RecordingState.Recording) "暂停" else "继续",
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(RecordingRed),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { viewModel.stopRecording() }) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "停止",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, millis)
}
