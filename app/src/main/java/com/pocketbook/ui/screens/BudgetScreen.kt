package com.pocketbook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.data.entity.Budget
import com.pocketbook.viewmodel.BudgetViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsState()
    val totalBudget by viewModel.totalBudget.collectAsState()
    val totalUsed by viewModel.totalUsed.collectAsState()
    val categories by viewModel.categoryBudgets.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算管理") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加预算")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 总预算环形图
            TotalBudgetCard(
                totalBudget = totalBudget,
                totalUsed = totalUsed
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "分类预算",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (categories.isEmpty()) {
                EmptyBudgetState(onAdd = { showAddDialog = true })
            } else {
                categories.forEach { item ->
                    CategoryBudgetItem(
                        categoryName = item.categoryName,
                        emoji = item.emoji,
                        budgetAmount = item.budgetAmount,
                        usedAmount = item.usedAmount,
                        percent = item.percent,
                        onEdit = { editingBudget = item.rawBudget }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // 添加/编辑预算对话框
    if (showAddDialog || editingBudget != null) {
        BudgetEditDialog(
            budget = editingBudget,
            onDismiss = {
                showAddDialog = false
                editingBudget = null
            },
            onSave = { categoryId, amount ->
                if (editingBudget != null) {
                    viewModel.updateBudget(editingBudget!!.id, amount)
                } else {
                    viewModel.createBudget(categoryId, amount)
                }
                showAddDialog = false
                editingBudget = null
            },
            onDelete = { budgetId ->
                viewModel.deleteBudget(budgetId)
                editingBudget = null
            }
        )
    }
}

@Composable
private fun TotalBudgetCard(totalBudget: Long, totalUsed: Long) {
    val percent = if (totalBudget > 0) totalUsed.toFloat() / totalBudget.toFloat() else 0f
    val isOver = percent > 1f
    val remaining = totalBudget - totalUsed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "本月预算",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 环形进度
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                CanvasRing(percent = percent.coerceIn(0f, 1f), isOver = isOver)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isOver) "超支" else "剩余",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMoney(abs(remaining)),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BudgetMetric("总预算", totalBudget)
                BudgetMetric("已用", totalUsed)
                BudgetMetric("剩余", remaining.coerceAtLeast(0))
            }
        }
    }
}

@Composable
private fun CanvasRing(percent: Float, isOver: Boolean) {
    val color = if (isOver) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val strokeWidth = 12.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerX = size.width / 2
                val centerY = size.height / 2

                // 背景环
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 进度环
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * percent,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
    )
}

@Composable
private fun BudgetMetric(label: String, amount: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatMoney(amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryBudgetItem(
    categoryName: String,
    emoji: String,
    budgetAmount: Long,
    usedAmount: Long,
    percent: Float,
    onEdit: () -> Unit
) {
    val isOver = usedAmount > budgetAmount
    val isWarning = percent > 0.8f && !isOver

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = categoryName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${(percent * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isOver -> Color(0xFFC62828)
                        isWarning -> Color(0xFFF57C00)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    isOver -> Color(0xFFC62828)
                    isWarning -> Color(0xFFF57C00)
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已用 ${formatMoney(usedAmount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "预算 ${formatMoney(budgetAmount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyBudgetState(onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📊", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "还没有设置预算",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加预算")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditDialog(
    budget: com.pocketbook.data.entity.Budget?,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
    onDelete: (String) -> Unit
) {
    var amount by remember { mutableStateOf(budget?.amount?.let { it / 100.0 }?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) "添加预算" else "编辑预算") },
        text = {
            Column {
                Text(
                    text = "预算金额（元）",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = { Text("例如：1000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountInCents = (amount.toDoubleOrNull() ?: 0.0) * 100
                    if (amountInCents > 0) {
                        onSave(budget?.categoryId ?: "", amountInCents.toLong())
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                if (budget != null) {
                    TextButton(
                        onClick = { onDelete(budget.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

private fun formatMoney(cents: Long): String {
    val yuan = cents / 100.0
    return NumberFormat.getCurrencyInstance(java.util.Locale.CHINA).format(yuan)
}

// Data class for category budget display
data class CategoryBudgetDisplay(
    val categoryName: String,
    val emoji: String,
    val budgetAmount: Long,
    val usedAmount: Long,
    val percent: Float,
    val rawBudget: com.pocketbook.data.entity.Budget
)
