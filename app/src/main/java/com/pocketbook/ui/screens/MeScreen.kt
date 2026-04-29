package com.pocketbook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.viewmodel.SettingsViewModel

@Composable
fun MeScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme by viewModel.themeMode.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\u8bb0\u4e00\u7b14",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "v1.0.0",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            ListItem(
                headlineContent = { Text("\u6df1\u8272\u6a21\u5f0f") },
                supportingContent = { Text("\u8ddf\u968f\u7cfb\u7edf / \u6d3b\u52a8\u91cd\u542f\u751f\u6548") },
                leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = theme == com.pocketbook.data.entity.ThemeMode.DARK,
                        onCheckedChange = { viewModel.toggleTheme(it) }
                    )
                }
            )
        }

        item {
            ListItem(
                headlineContent = { Text("\u5bfc\u51fa\u6570\u636e (CSV)") },
                supportingContent = { Text("\u5bfc\u51fa\u5230\u4e0b\u8f7d\u6587\u4ef6\u5939") },
                leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                modifier = Modifier.clickable { viewModel.exportCSV() }
            )
        }

        item {
            ListItem(
                headlineContent = { Text("\u5bfc\u51fa\u6570\u636e (JSON)") },
                supportingContent = { Text("\u5b8c\u6574\u6570\u636e\u5907\u4efd") },
                leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
                modifier = Modifier.clickable { viewModel.exportJSON() }
            )
        }

        item {
            ListItem(
                headlineContent = { Text("\u6570\u636e\u52a0\u5bc6") },
                supportingContent = { Text("SQLCipher AES-256 \u52a0\u5bc6\u4fdd\u62a4") },
                leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                trailingContent = {
                    Text(
                        "\u5df2\u542f\u7528",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            )
        }

        item {
            ListItem(
                headlineContent = { Text("\u5173\u4e8e") },
                supportingContent = { Text("\u7eaf\u51c0\u65e0\u5e7f\u544a \u00b7 \u672c\u5730\u4f18\u5148 \u00b7 AI\u6d1e\u5bdf") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
    }

    exportResult?.let { result ->
        if (result.isSuccess) {
            val file = result.getOrNull()
            file?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.clearExportResult() },
                    title = { Text("\u5bfc\u51fa\u6210\u529f") },
                    text = { Text("\u6587\u4ef6\u5df2\u4fdd\u5b58\u5230: ${it.absolutePath}") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearExportResult() }) {
                            Text("\u786e\u5b9a")
                        }
                    }
                )
            }
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.clearExportResult() },
                title = { Text("\u5bfc\u51fa\u5931\u8d25") },
                text = { Text(result.exceptionOrNull()?.message ?: "\u672a\u77e5\u9519\u8bef") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearExportResult() }) {
                        Text("\u786e\u5b9a")
                    }
                }
            )
        }
    }
}
