package com.gaojiluyin.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaojiluyin.data.remote.update.UpdateState
import com.gaojiluyin.ui.update.UpdateDialog
import com.gaojiluyin.ui.update.UpdateViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val claudeKey by viewModel.claudeKey.collectAsState()
    val openaiKey by viewModel.openaiKey.collectAsState()
    val claudeModel by viewModel.claudeModel.collectAsState()
    val updateState by updateViewModel.updateState.collectAsState()

    var claudeKeyInput by remember(claudeKey) { mutableStateOf(claudeKey) }
    var openaiKeyInput by remember(openaiKey) { mutableStateOf(openaiKey) }
    var claudeModelInput by remember(claudeModel) { mutableStateOf(claudeModel) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("API 配置", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = claudeKeyInput,
                    onValueChange = { claudeKeyInput = it },
                    label = { Text("Claude API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = claudeModelInput,
                    onValueChange = { claudeModelInput = it },
                    label = { Text("Claude 模型") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = openaiKeyInput,
                    onValueChange = { openaiKeyInput = it },
                    label = { Text("OpenAI API Key (备用)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.setClaudeKey(claudeKeyInput)
                        viewModel.setOpenAIKey(openaiKeyInput)
                        viewModel.setClaudeModel(claudeModelInput)
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }

                if (saved) {
                    Text(
                        "已保存",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("应用更新", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "当前版本: 1.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { updateViewModel.checkForUpdate() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = updateState !is UpdateState.Checking
                ) {
                    if (updateState is UpdateState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        Text("检查中...")
                    } else {
                        Text("检查更新")
                    }
                }

                when (val state = updateState) {
                    is UpdateState.UpToDate -> {
                        Text(
                            "已是最新版本",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    is UpdateState.Error -> {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        UpdateDialog(
            viewModel = updateViewModel,
            onDismiss = {}
        )
    }
}
