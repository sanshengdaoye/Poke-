package com.pocketbook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val theme by viewModel.themeMode.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            // 个性化
            SettingsGroup(title = "个性化") {
                ListItem(
                    headlineContent = { Text("深色模式") },
                    supportingContent = { 
                        Text(
                            when (theme) {
                                com.pocketbook.data.entity.ThemeMode.DARK -> "已开启"
                                com.pocketbook.data.entity.ThemeMode.LIGHT -> "已关闭"
                                else -> "跟随系统"
                            }
                        )
                    },
                    leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = theme == com.pocketbook.data.entity.ThemeMode.DARK,
                            onCheckedChange = { viewModel.toggleTheme(it) }
                        )
                    }
                )
            }

            // 数据管理
            SettingsGroup(title = "数据管理") {
                ListItem(
                    headlineContent = { Text("导出数据 (CSV)") },
                    supportingContent = { Text("导出到下载文件夹") },
                    leadingContent = { Icon(Icons.Default.TableChart, contentDescription = null) },
                    modifier = Modifier.clickable { viewModel.exportCSV() }
                )
                Divider(indent = 56.dp)
                ListItem(
                    headlineContent = { Text("导出数据 (JSON)") },
                    supportingContent = { Text("完整数据备份") },
                    leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
                    modifier = Modifier.clickable { viewModel.exportJSON() }
                )
                Divider(indent = 56.dp)
                ListItem(
                    headlineContent = { Text("清除全部数据") },
                    supportingContent = { Text("删除所有记账记录，不可恢复") },
                    leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showClearConfirm = true }
                )
            }

            // 安全与隐私
            SettingsGroup(title = "安全与隐私") {
                ListItem(
                    headlineContent = { Text("数据加密") },
                    supportingContent = { Text("SQLCipher AES-256 加密保护") },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                    trailingContent = {
                        Text(
                            "已启用",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                )
            }

            // 关于
            SettingsGroup(title = "关于") {
                ListItem(
                    headlineContent = { Text("记一笔") },
                    supportingContent = { Text("v1.0.0 · 纯净无广告 · 本地优先 · AI洞察") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 导出结果弹窗
    exportResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportResult() },
            title = { Text(if (result.isSuccess) "导出成功" else "导出失败") },
            text = {
                Text(
                    if (result.isSuccess)
                        "文件已保存到: ${result.getOrNull()?.absolutePath ?: ""}"
                    else
                        result.exceptionOrNull()?.message ?: "未知错误"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExportResult() }) {
                    Text("确定")
                }
            }
        )
    }

    // 清除数据确认
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清除") },
            text = { Text("此操作将删除所有记账记录、分类、预算等数据，不可恢复。确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            content()
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
