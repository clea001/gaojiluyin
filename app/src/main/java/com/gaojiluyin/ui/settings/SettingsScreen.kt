package com.gaojiluyin.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaojiluyin.data.remote.llm.LlmProvider
import com.gaojiluyin.data.remote.update.UpdateState
import com.gaojiluyin.ui.update.UpdateDialog
import com.gaojiluyin.ui.update.UpdateViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val primaryProviderId by viewModel.primaryProviderId.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val editingProviderId by viewModel.editingProviderId.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val model by viewModel.model.collectAsState()
    val testState by viewModel.testState.collectAsState()
    val updateState by updateViewModel.updateState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Primary provider selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("LLM 提供商配置", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Text("主提供商", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                ProviderDropdown(
                    providers = providers,
                    selectedId = primaryProviderId,
                    onSelect = { viewModel.selectPrimaryProvider(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Provider list / editing
        if (editingProviderId != null) {
            ProviderEditCard(
                providerName = providers.find { it.id == editingProviderId }?.name ?: "",
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                testState = testState,
                onApiKeyChange = { viewModel.updateApiKey(it) },
                onBaseUrlChange = { viewModel.updateBaseUrl(it) },
                onModelChange = { viewModel.updateModel(it) },
                onSave = { viewModel.save() },
                onTest = { viewModel.testConnection() },
                onCancel = { viewModel.cancelEditing() }
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("其他提供商", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    providers.forEach { provider ->
                        val isConfigured = viewModel.isProviderConfigured(provider.id)
                        val isPrimary = provider.id == primaryProviderId

                        ProviderRow(
                            provider = provider,
                            isConfigured = isConfigured,
                            isPrimary = isPrimary,
                            onEdit = { viewModel.startEditing(provider.id) }
                        )
                        if (provider != providers.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Update section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("应用更新", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Text("当前版本: 1.0.0", style = MaterialTheme.typography.bodyMedium)

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

@Composable
private fun ProviderDropdown(
    providers: List<LlmProvider>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = providers.find { it.id == selectedId }?.name ?: selectedId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = selectedName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        providers.forEach { provider ->
            DropdownMenuItem(
                text = { Text(provider.name) },
                onClick = {
                    onSelect(provider.id)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun ProviderRow(
    provider: LlmProvider,
    isConfigured: Boolean,
    isPrimary: Boolean,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(100.dp)
            )
            if (isConfigured) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已配置",
                    tint = Color(0xFF43A047),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text("已配置", style = MaterialTheme.typography.bodySmall, color = Color(0xFF43A047))
            } else {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "未配置",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text("未配置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            if (isPrimary) {
                Text(
                    " [主]",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        TextButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text("编辑")
        }
    }
}

@Composable
private fun ProviderEditCard(
    providerName: String,
    apiKey: String,
    baseUrl: String,
    model: String,
    testState: TestState,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("编辑 $providerName", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("API 地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = model,
                onValueChange = onModelChange,
                label = { Text("模型") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                    enabled = testState !is TestState.Testing && apiKey.isNotBlank()
                ) {
                    if (testState is TestState.Testing) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp), strokeWidth = 2.dp)
                    }
                    Text("测试连接")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
            }

            when (testState) {
                is TestState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF43A047))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("连接成功", color = Color(0xFF43A047), style = MaterialTheme.typography.bodySmall)
                    }
                }
                is TestState.Error -> {
                    Text(
                        "连接失败: ${testState.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {}
            }
        }
    }
}
