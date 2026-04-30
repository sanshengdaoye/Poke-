package com.pocketbook.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.viewmodel.DashboardViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddClick: () -> Unit,
    onViewTransactions: () -> Unit,
    onViewStats: () -> Unit,
    onViewBudget: () -> Unit,
    onViewAccounts: () -> Unit,
    onViewInsights: () -> Unit,
    onSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "记一笔",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "记账",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 总览卡片
            OverviewCard(
                monthBalance = uiState.monthBalance,
                monthIncome = uiState.monthIncome,
                monthExpense = uiState.monthExpense,
                budgetUsedPercent = uiState.budgetUsedPercent,
                isBudgetOver = uiState.isBudgetOver
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 消费健康评分卡片（新用户显示欢迎引导）
            if (uiState.isEmpty) {
                WelcomeCard()
            } else {
                HealthScoreCard(
                    score = uiState.healthScore,
                    budgetExecution = uiState.budgetExecutionRate,
                    spendingVolatility = uiState.spendingVolatility,
                    savingRate = uiState.savingRate
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 今日流水卡片
            TodayTransactionsCard(
                todayExpense = uiState.todayExpense,
                todayCount = uiState.todayTransactionCount,
                recentTransactions = uiState.recentTransactions,
                onViewAll = onViewTransactions
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI洞察卡片
            uiState.latestInsight?.let { insight ->
                InsightCard(
                    title = insight.title,
                    description = insight.description,
                    type = insight.type,
                    onViewAll = onViewInsights
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 快捷入口网格
            QuickAccessGrid(
                onStats = onViewStats,
                onBudget = onViewBudget,
                onAccounts = onViewAccounts,
                onInsights = onViewInsights
            )

            Spacer(modifier = Modifier.height(80.dp)) // FAB底部安全区
        }
    }
}

@Composable
private fun OverviewCard(
    monthBalance: Long,
    monthIncome: Long,
    monthExpense: Long,
    budgetUsedPercent: Float,
    isBudgetOver: Boolean
) {
    val incomeColor = Color(0xFF2E7D32)
    val expenseColor = Color(0xFFC62828)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "本月结余",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMoney(monthBalance),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "收入",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMoney(monthIncome),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = incomeColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "支出",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMoney(monthExpense),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = expenseColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 预算进度条
            LinearProgressIndicator(
                progress = { budgetUsedPercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isBudgetOver) expenseColor else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isBudgetOver)
                    "预算已超支 ${(budgetUsedPercent * 100).toInt()}%"
                else
                    "预算已用 ${(budgetUsedPercent * 100).toInt()}%",
                fontSize = 11.sp,
                color = if (isBudgetOver) expenseColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "👋 欢迎开始记账",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "记完第一笔后，这里会显示你的消费健康度",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "试试用智能记账：输入\"午餐35\"",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HealthScoreCard(
    score: Int,
    budgetExecution: Float,
    spendingVolatility: String,
    savingRate: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "消费健康度",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 环形评分
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularScoreRing(score = score)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            score >= 80 -> "良好"
                            score >= 60 -> "一般"
                            else -> "需改善"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 三个子指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("预算执行", "${(budgetExecution * 100).toInt()}%")
                MetricItem("消费波动", spendingVolatility)
                MetricItem("储蓄率", "${(savingRate * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun CircularScoreRing(score: Int) {
    val color = when {
        score >= 80 -> Color(0xFF2E7D32)
        score >= 60 -> Color(0xFFF57C00)
        else -> Color(0xFFC62828)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1000),
        label = "score"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background ring
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress ring
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
    )
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayTransactionsCard(
    todayExpense: Long,
    todayCount: Int,
    recentTransactions: List<TransactionItem>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "今日流水",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${todayCount}笔支出 · ${formatMoney(todayExpense)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onViewAll) {
                    Text("查看全部 →", fontSize = 12.sp)
                }
            }

            if (recentTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentTransactions.take(3).forEach { transaction ->
                        TransactionMiniCard(transaction)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "今天还没有记账",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionMiniCard(transaction: TransactionItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = transaction.emoji,
            fontSize = 24.sp
        )
        Text(
            text = transaction.category,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatMoney(transaction.amount),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFC62828)
        )
    }
}

@Composable
private fun InsightCard(
    title: String,
    description: String,
    type: InsightType,
    onViewAll: () -> Unit
) {
    val (icon, cardColor) = when (type) {
        InsightType.OVERSPEND_WARNING -> "⚠️" to Color(0xFFFFEBEE)
        InsightType.SAVING_SUGGESTION -> "💡" to Color(0xFFE3F2FD)
        InsightType.SAVING_MILESTONE -> "🎯" to Color(0xFFE8F5E9)
        InsightType.IMPULSE_DETECTION -> "⚡" to Color(0xFFFFF3E0)
        else -> "📊" to Color(0xFFF3E5F5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewAll() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAccessGrid(
    onStats: () -> Unit,
    onBudget: () -> Unit,
    onAccounts: () -> Unit,
    onInsights: () -> Unit
) {
    val items = listOf(
        Triple("统计报表", Icons.Default.BarChart, onStats),
        Triple("预算管理", Icons.Default.PieChart, onBudget),
        Triple("账户总览", Icons.Default.AccountBalance, onAccounts),
        Triple("AI洞察", Icons.Default.Lightbulb, onInsights)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { (label, icon, onClick) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = label, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatMoney(cents: Long): String {
    val yuan = cents / 100.0
    return NumberFormat.getCurrencyInstance(java.util.Locale.CHINA).format(yuan)
}

// Data classes for UI state
data class TransactionItem(
    val id: String,
    val emoji: String,
    val category: String,
    val amount: Long
)

enum class InsightType {
    OVERSPEND_WARNING,
    TREND_ANALYSIS,
    SAVING_SUGGESTION,
    SAVING_MILESTONE,
    IMPULSE_DETECTION,
    BUDGET_HEALTH,
    DAILY_BUDGET_ALERT
}
