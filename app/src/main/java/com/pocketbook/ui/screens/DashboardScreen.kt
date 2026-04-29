package com.pocketbook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNavigateToRecord: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToBudget: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val todayExpense by viewModel.todayExpense.collectAsState()
    val todayIncome by viewModel.todayIncome.collectAsState()
    val weekExpense by viewModel.weekExpense.collectAsState()
    val monthExpense by viewModel.monthExpense.collectAsState()
    val monthIncome by viewModel.monthIncome.collectAsState()
    val monthBudget by viewModel.monthBudget.collectAsState()
    val monthBudgetUsed by viewModel.monthBudgetUsed.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val insights by viewModel.insights.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "记一笔",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault()).format(Date()),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(16.dp)
        ) {
            // Today's summary card
            TodaySummaryCard(
                expense = todayExpense,
                income = todayIncome,
                onAddClick = onNavigateToRecord
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly overview
            MonthlyOverviewCard(
                expense = monthExpense,
                income = monthIncome,
                budget = monthBudget,
                budgetUsed = monthBudgetUsed,
                onClick = onNavigateToBudget
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    title = "本周支出",
                    amount = weekExpense,
                    icon = Icons.Default.DateRange,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "本月结余",
                    amount = monthIncome - monthExpense,
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f),
                    tintColor = if (monthIncome >= monthExpense) Color(0xFF43A047) else Color(0xFFE53935)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent transactions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近流水",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text("查看全部", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (recentTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "还没有记账记录",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                recentTransactions.take(5).forEach { transaction ->
                    DashboardTransactionItem(transaction = transaction)
                    if (transaction != recentTransactions.take(5).last()) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Insights
            if (insights.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "智能洞察",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text("${insights.size}条新建议", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                insights.take(3).forEach { insight ->
                    InsightCard(insight = insight)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TodaySummaryCard(
    expense: Long,
    income: Long,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日收支",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                FilledTonalButton(
                    onClick = onAddClick,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("记一笔", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "支出",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatAmount(expense)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (expense > 0) Color(0xFFE53935) else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "收入",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatAmount(income)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (income > 0) Color(0xFF43A047) else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyOverviewCard(
    expense: Long,
    income: Long,
    budget: Long,
    budgetUsed: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本月概览",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("支出", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatAmount(expense),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("收入", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatAmount(income),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF43A047)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("结余", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatAmount(income - expense),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (income >= expense) Color(0xFF43A047) else Color(0xFFE53935)
                    )
                }
            }

            if (budget > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("预算进度", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${(budgetUsed * 100 / budget)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (budgetUsed > budget) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (budgetUsed.toFloat() / budget.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (budgetUsed > budget) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuickStatCard(
    title: String,
    amount: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tintColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatAmount(amount),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
        }
    }
}

private fun formatAmount(amount: Long): String {
    val yuan = amount / 100
    val fen = kotlin.math.abs(amount % 100)
    val sign = if (amount < 0) "-" else ""
    return "${sign}¥${yuan}.${String.format("%02d", fen)}"
}
