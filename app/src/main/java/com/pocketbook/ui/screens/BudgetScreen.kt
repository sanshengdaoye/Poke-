package com.pocketbook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.data.entity.Budget
import com.pocketbook.data.entity.BudgetPeriod
import com.pocketbook.data.entity.BudgetType
import com.pocketbook.viewmodel.BudgetViewModel

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "预算管理",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            FilledTonalIconButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "添加预算")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "还没有设置预算",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加预算")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets) { budget ->
                    BudgetItem(
                        budget = budget,
                        spent = viewModel.getSpentAmount(budget),
                        onDelete = { viewModel.deleteBudget(budget) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { amount, period ->
                viewModel.createBudget(amount, period)
            }
        )
    }
}

@Composable
fun BudgetItem(
    budget: Budget,
    spent: Long,
    onDelete: () -> Unit
) {
    val budgetAmount = budget.amount
    val progress = if (budgetAmount > 0) spent.toFloat() / budgetAmount else 0f
    val isOverBudget = spent > budgetAmount

    val progressColor = when {
        progress >= 1f -> Color(0xFFE53935)
        progress >= 0.8f -> Color(0xFFFFA000)
        else -> Color(0xFF43A047)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (budget.type == BudgetType.TOTAL) "月度总预算" else "分类预算",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥ ${spent / 100}.${String.format("%02d", spent % 100)} / ¥ ${budgetAmount / 100}.${String.format("%02d", budgetAmount % 100)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isOverBudget) {
                    "已超支 ¥ ${(spent - budgetAmount) / 100}.${String.format("%02d", (spent - budgetAmount) % 100)}"
                } else {
                    "还剩 ¥ ${(budgetAmount - spent) / 100}.${String.format("%02d", (budgetAmount - spent) % 100)} (${((1 - progress) * 100).toInt()}%)"
                },
                fontSize = 12.sp,
                color = if (isOverBudget) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, BudgetPeriod) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加预算") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("预算金额 (¥)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "周期",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BudgetPeriod.values().forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = {
                                Text(
                                    when (period) {
                                        BudgetPeriod.WEEK -> "周"
                                        BudgetPeriod.MONTH -> "月"
                                        BudgetPeriod.YEAR -> "年"
                                        else -> "自定义"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountInCents = (amount.toDoubleOrNull() ?: 0.0) * 100
                    if (amountInCents > 0) {
                        onConfirm(amountInCents.toLong(), selectedPeriod)
                        onDismiss()
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
