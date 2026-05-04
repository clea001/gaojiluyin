package com.gaojiluyin.ui.document

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DocumentScreen(
    recordingId: Long,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val recording by viewModel.recording.collectAsState()
    val document by viewModel.document.collectAsState()

    LaunchedEffect(recordingId) {
        viewModel.load(recordingId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = recording?.title ?: "加载中…",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "状态: ${recording?.status ?: ""}",
            style = MaterialTheme.typography.bodySmall,
            color = if (recording?.status == "COMPLETED") MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (document != null) {
            val doc = document!!

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("摘要", style = MaterialTheme.typography.titleMedium)
                    Text(doc.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("整理内容", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(doc.organizedContent, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("原始转写", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(doc.rawTranscript, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (recording != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("原始转写", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无文档 — Whisper转写和LLM整理功能将在后续版本中启用",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
