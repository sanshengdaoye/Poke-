package com.pocketbook.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.viewmodel.StatsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("总览", "分类", "趋势", "日历")

    Column(modifier = Modifier.fillMaxSize()) {
        // TopAppBar
        TopAppBar(
            title = { Text("统计") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Tab切换
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 14.sp) }
                )
            }
        }

        // Tab内容
        when (selectedTab) {
            0 -> OverviewTab(uiState)
            1 -> CategoryTab(uiState, viewModel)
            2 -> TrendTab(uiState, viewModel)
            3 -> CalendarTab(uiState, viewModel)
        }
    }
}

@Composable
private fun OverviewTab(uiState: StatsUiState) {
    val incomeColor = Color(0xFF2E7D32)
    val expenseColor = Color(0xFFC62828)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 收支对比卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "本月收支",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 柱状图
                BarChart(
                    income = uiState.monthIncome,
                    expense = uiState.monthExpense,
                    incomeColor = incomeColor,
                    expenseColor = expenseColor
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = incomeColor, label = "收入", amount = uiState.monthIncome)
                    LegendItem(color = expenseColor, label = "支出", amount = uiState.monthExpense)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 关键指标卡片
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "结余",
                value = formatMoney(uiState.monthIncome - uiState.monthExpense),
                valueColor = if (uiState.monthIncome >= uiState.monthExpense) incomeColor else expenseColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "日均支出",
                value = formatMoney(uiState.dailyAverageExpense),
                valueColor = expenseColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "储蓄率",
                value = "${(uiState.savingRate * 100).toInt()}%",
                valueColor = incomeColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "记账笔数",
                value = "${uiState.totalTransactionCount}笔",
                valueColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BarChart(
    income: Long,
    expense: Long,
    incomeColor: Color,
    expenseColor: Color
) {
    val maxValue = maxOf(income, expense, 1L)
    val incomeRatio = income.toFloat() / maxValue.toFloat()
    val expenseRatio = expense.toFloat() / maxValue.toFloat()

    val animatedIncome by animateFloatAsState(
        targetValue = incomeRatio,
        animationSpec = tween(1000),
        label = "income"
    )
    val animatedExpense by animateFloatAsState(
        targetValue = expenseRatio,
        animationSpec = tween(1000),
        label = "expense"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 收入柱
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = formatMoney(income),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = incomeColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(animatedIncome)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(incomeColor)
            )
        }

        // 支出柱
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = formatMoney(expense),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = expenseColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(animatedExpense)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(expenseColor)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, amount: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = formatMoney(amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryTab(uiState: StatsUiState, viewModel: StatsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (uiState.categoryBreakdown.isEmpty()) {
            EmptyState(message = "还没有支出记录")
        } else {
            // 饼图
            PieChart(
                data = uiState.categoryBreakdown,
                total = uiState.monthExpense
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 排行榜
            Text(
                text = "支出排行",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            uiState.categoryBreakdown.forEachIndexed { index, item ->
                CategoryRankItem(
                    rank = index + 1,
                    categoryName = item.categoryName,
                    amount = item.amount,
                    percent = item.percent,
                    emoji = item.emoji,
                    color = pieColors.getOrElse(index) { MaterialTheme.colorScheme.primary }
                )
                if (index < uiState.categoryBreakdown.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<CategoryBreakdownItem>,
    total: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "分类占比",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                var selectedIndex by remember { mutableStateOf(-1) }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (data.isEmpty() || total <= 0) return@Canvas

                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = (size.minDimension - 40.dp.toPx()) / 2
                    val holeRadius = radius * 0.55f

                    var startAngle = -90f

                    data.forEachIndexed { index, item ->
                        val sweepAngle = (item.amount.toFloat() / total.toFloat()) * 360f
                        val color = pieColors.getOrElse(index) { Color.Gray }
                        val isSelected = index == selectedIndex

                        drawArc(
                            color = if (isSelected) color.copy(alpha = 0.9f) else color.copy(alpha = 0.7f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(centerX - radius, centerY - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = if (isSelected) 32.dp.toPx() else 28.dp.toPx())
                        )

                        startAngle += sweepAngle
                    }

                    // 中心文字
                    val centerText = if (selectedIndex >= 0) {
                        val item = data[selectedIndex]
                        "${item.categoryName}\n${formatMoney(item.amount)}"
                    } else {
                        "总支出\n${formatMoney(total)}"
                    }
                    // 简化：不绘制中心文字，用下面的Text替代
                }

                // 中心文字
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedIndex >= 0) {
                        val item = data[selectedIndex]
                        Text(
                            text = item.categoryName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatMoney(item.amount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = pieColors.getOrElse(selectedIndex) { MaterialTheme.colorScheme.primary }
                        )
                        Text(
                            text = "${(item.percent * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "总支出",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(total),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图例（可点击）
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                maxItemsInEachRow = 4
            ) {
                data.forEachIndexed { index, item ->
                    val color = pieColors.getOrElse(index) { MaterialTheme.colorScheme.primary }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedIndex == index)
                                    color.copy(alpha = 0.15f)
                                else
                                    Color.Transparent
                            )
                            .clickable { selectedIndex = if (selectedIndex == index) -1 else index }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.emoji,
                            fontSize = 12.sp
                        )
                        Text(
                            text = item.categoryName,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRankItem(
    rank: Int,
    categoryName: String,
    amount: Long,
    percent: Float,
    emoji: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 图标+名称
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = categoryName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            // 进度条
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 金额+占比
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(percent * 100).toInt()}%",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrendTab(uiState: StatsUiState, viewModel: StatsViewModel) {
    var granularity by remember { mutableStateOf(0) } // 0=日, 1=周, 2=月
    val options = listOf("日", "周", "月")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 粒度切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = granularity == index,
                        onClick = { granularity = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        )
                    ) {
                        Text(label, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.trendData.isEmpty()) {
            EmptyState(message = "数据不足，多记几笔再来看趋势")
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "收支趋势",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 折线图
                    LineChart(
                        data = uiState.trendData,
                        incomeColor = Color(0xFF2E7D32),
                        expenseColor = Color(0xFFC62828)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 最高/最低标注
            val maxExpense = uiState.trendData.maxByOrNull { it.expense }
            val minExpense = uiState.trendData.minByOrNull { it.expense }

            if (maxExpense != null && maxExpense.expense > 0) {
                Text(
                    text = "支出峰值: ${maxExpense.label} ${formatMoney(maxExpense.expense)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (minExpense != null && minExpense.expense > 0) {
                Text(
                    text = "支出最低: ${minExpense.label} ${formatMoney(minExpense.expense)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<TrendDataItem>,
    incomeColor: Color,
    expenseColor: Color
) {
    val maxValue = maxOf(
        data.maxOfOrNull { it.income } ?: 0L,
        data.maxOfOrNull { it.expense } ?: 0L,
        1L
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
    ) {
        // X轴标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.take(7).forEach { item ->
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 折线 - 使用 drawBehind 绘制
        val density = androidx.compose.ui.platform.LocalDensity.current
        val expensePoints = remember(data) {
            data.map { it.expense.toFloat() / maxValue.toFloat() }
        }
        val incomePoints = remember(data) {
            data.map { it.income.toFloat() / maxValue.toFloat() }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(8.dp)
                .drawBehind {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    val stepX = chartWidth / maxOf(data.size - 1, 1)

                    // 支出线
                    if (data.size > 1) {
                        for (i in 0 until data.size - 1) {
                            val x1 = i * stepX
                            val x2 = (i + 1) * stepX
                            val y1 = chartHeight - expensePoints[i] * chartHeight
                            val y2 = chartHeight - expensePoints[i + 1] * chartHeight

                            this.drawLine(
                                color = expenseColor,
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = with(density) { 2.dp.toPx() }
                            )
                        }
                    }

                    // 收入线
                    if (data.size > 1) {
                        for (i in 0 until data.size - 1) {
                            val x1 = i * stepX
                            val x2 = (i + 1) * stepX
                            val y1 = chartHeight - incomePoints[i] * chartHeight
                            val y2 = chartHeight - incomePoints[i + 1] * chartHeight

                            this.drawLine(
                                color = incomeColor,
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = with(density) { 2.dp.toPx() }
                            )
                        }
                    }

                    // 数据点
                    data.forEachIndexed { index, item ->
                        val x = index * stepX
                        val yExpense = chartHeight - expensePoints[index] * chartHeight
                        val yIncome = chartHeight - incomePoints[index] * chartHeight

                        this.drawCircle(
                            color = expenseColor,
                            radius = with(density) { 3.dp.toPx() },
                            center = Offset(x, yExpense)
                        )
                        this.drawCircle(
                            color = incomeColor,
                            radius = with(density) { 3.dp.toPx() },
                            center = Offset(x, yIncome)
                        )
                    }
                }
        )
    }

    // 图例
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendItem(color = incomeColor, label = "收入", amount = 0)
        Spacer(modifier = Modifier.width(24.dp))
        LegendItem(color = expenseColor, label = "支出", amount = 0)
    }
}

@Composable
private fun CalendarTab(uiState: StatsUiState, viewModel: StatsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (uiState.calendarData.isEmpty()) {
            EmptyState(message = "还没有支出记录")
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "消费日历",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 简单热力图：用网格显示当月每天
                    val maxAmount = uiState.calendarData.maxOfOrNull { it.amount } ?: 1L

                    Column {
                        // 星期标题
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 日期网格
                        val weeks = uiState.calendarData.chunked(7)
                        weeks.forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                week.forEach { dayData ->
                                    val intensity = if (maxAmount > 0) {
                                        (dayData.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
                                    } else 0f

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (dayData.amount > 0) {
                                                    Color(0xFFC62828).copy(alpha = 0.1f + intensity * 0.7f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${dayData.day}",
                                                fontSize = 12.sp,
                                                fontWeight = if (dayData.amount > 0) FontWeight.Medium else FontWeight.Normal
                                            )
                                            if (dayData.amount > 0) {
                                                Text(
                                                    text = "${(dayData.amount / 100)}",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                // 补齐一周7天
                                repeat(7 - week.size) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📊",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMoney(cents: Long): String {
    val yuan = cents / 100.0
    return NumberFormat.getCurrencyInstance(java.util.Locale.CHINA).format(yuan)
}

// 饼图颜色
private val pieColors = listOf(
    Color(0xFF2196F3), // 蓝
    Color(0xFF4CAF50), // 绿
    Color(0xFFFF9800), // 橙
    Color(0xFFE91E63), // 粉
    Color(0xFF9C27B0), // 紫
    Color(0xFF00BCD4), // 青
    Color(0xFFFF5722), // 深橙
    Color(0xFF3F51B5), // 靛蓝
    Color(0xFF8BC34A), // 浅绿
    Color(0xFF795548)  // 棕
)

// Data classes for UI state
data class StatsUiState(
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
    val dailyAverageExpense: Long = 0,
    val savingRate: Float = 0f,
    val totalTransactionCount: Int = 0,
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val trendData: List<TrendDataItem> = emptyList(),
    val calendarData: List<CalendarDayItem> = emptyList()
)

data class CategoryBreakdownItem(
    val categoryName: String,
    val emoji: String,
    val amount: Long,
    val percent: Float,
    val transactionCount: Int
)

data class TrendDataItem(
    val label: String,
    val income: Long,
    val expense: Long
)

data class CalendarDayItem(
    val day: Int,
    val amount: Long,
    val hasData: Boolean
)
