package com.pocketbook.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val categoryStats by viewModel.categoryStats.collectAsState()
    val monthlyTrend by viewModel.monthlyTrend.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SummaryRow(
                income = totalIncome,
                expense = totalExpense
            )
        }

        if (categoryStats.isNotEmpty()) {
            item {
                Text(
                    text = "\u652f\u51fa\u5206\u7c7b\u5360\u6bd4",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                PieChart(
                    data = categoryStats.map { it.name to it.amount },
                    colors = categoryStats.map { it.color },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }

        if (monthlyTrend.isNotEmpty()) {
            item {
                Text(
                    text = "\u8fd1\u4e2a\u6708\u8d8b\u52bf",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                TrendChart(
                    data = monthlyTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        if (categoryStats.isNotEmpty()) {
            item {
                Text(
                    text = "\u5206\u7c7b\u6392\u884c\u699c",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(categoryStats.sortedByDescending { it.amount }) { stat ->
                CategoryRankItem(
                    name = stat.name,
                    amount = stat.amount,
                    percentage = stat.percentage,
                    color = stat.color
                )
            }
        }
    }
}

@Composable
fun SummaryRow(
    income: Long,
    expense: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            label = "\u672c\u6708\u6536\u5165",
            amount = income,
            color = Color(0xFF43A047),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "\u672c\u6708\u652f\u51fa",
            amount = expense,
            color = Color(0xFFE53935),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\u00A5 ${amount / 100}.${String.format("%02d", amount % 100)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun PieChart(
    data: List<Pair<String, Long>>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.second }.toFloat()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp)
            ) {
                var startAngle = -90f
                data.forEachIndexed { index, (_, value) ->
                    val sweepAngle = if (total > 0) (value / total) * 360f else 0f
                    val color = colors.getOrElse(index) { Color.Gray }
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                data.forEachIndexed { index, (name, value) ->
                    val percentage = if (total > 0) (value * 100 / total).toInt() else 0
                    val color = colors.getOrElse(index) { Color.Gray }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$name $percentage%",
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendChart(
    data: List<Pair<String, Pair<Long, Long>>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (data.isEmpty()) return@Canvas

            val maxValue = data.maxOf { maxOf(it.second.first, it.second.second) }.coerceAtLeast(1)
            val padding = 40f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2
            val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)

            // Grid lines
            for (i in 0..4) {
                val y = padding + chartHeight * i / 4
                drawLine(
                    color = Color.LightGray,
                    start = Offset(padding, y),
                    end = Offset(size.width - padding, y),
                    strokeWidth = 1f
                )
            }

            // Income line (green)
            val incomePoints = data.mapIndexed { index, item ->
                val x = padding + index * stepX
                val y = padding + chartHeight * (1 - item.second.first.toFloat() / maxValue)
                Offset(x, y)
            }
            drawLineSeries(incomePoints, Color(0xFF81C784))

            // Expense line (red)
            val expensePoints = data.mapIndexed { index, item ->
                val x = padding + index * stepX
                val y = padding + chartHeight * (1 - item.second.second.toFloat() / maxValue)
                Offset(x, y)
            }
            drawLineSeries(expensePoints, Color(0xFFE57373))

            // Data points
            incomePoints.forEach { drawCircle(Color(0xFF81C784), 5f, it) }
            expensePoints.forEach { drawCircle(Color(0xFFE57373), 5f, it) }
        }
    }
}

private fun DrawScope.drawLineSeries(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    for (i in 0 until points.size - 1) {
        drawLine(
            color = color,
            start = points[i],
            end = points[i + 1],
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawCircle(color: Color, radius: Float, center: Offset) {
    drawCircle(color = color, radius = radius, center = center)
}

@Composable
fun CategoryRankItem(
    name: String,
    amount: Long,
    percentage: Float,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(
                    progress = { (percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "\u00A5 ${amount / 100}.${String.format("%02d", amount % 100)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE53935)
            )
        }
    }
}
