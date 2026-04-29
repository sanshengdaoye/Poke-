package com.pocketbook.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.data.entity.Account
import com.pocketbook.data.entity.Category
import com.pocketbook.data.entity.Insight
import com.pocketbook.data.entity.Transaction
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.viewmodel.CategoryViewModel
import com.pocketbook.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordScreen(
    onSaveComplete: () -> Unit = {},
    viewModel: RecordViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }
    var latestInsights by remember { mutableStateOf<List<Insight>>(emptyList()) }

    val categories by categoryViewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val filteredCategories = categories.filter {
        if (selectedType == TransactionType.EXPENSE) it.type == com.pocketbook.data.entity.CategoryType.EXPENSE
        else it.type == com.pocketbook.data.entity.CategoryType.INCOME
    }

    // Auto-select first account if none selected
    LaunchedEffect(accounts) {
        if (selectedAccount == null && accounts.isNotEmpty()) {
            selectedAccount = accounts.first()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = if (selectedType == TransactionType.EXPENSE) "记一笔支出" else "记一笔收入",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Type selector
            TypeSelector(
                selectedType = selectedType,
                onTypeSelected = {
                    selectedType = it
                    selectedCategory = null
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Amount display
            AmountDisplay(amount = amount, type = selectedType)

            Spacer(modifier = Modifier.height(20.dp))

            // NLP Smart Input
            SmartInputBar(
                categories = filteredCategories,
                accounts = accounts,
                onParsed = { parsedAmount, parsedCategory, parsedAccount, parsedNote ->
                    amount = parsedAmount
                    parsedCategory?.let { selectedCategory = it }
                    parsedAccount?.let { selectedAccount = it }
                    if (parsedNote.isNotBlank()) note = parsedNote
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category grid
            Text(
                text = "选择分类",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            CategoryGrid(
                categories = filteredCategories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Account selector
            if (accounts.isNotEmpty()) {
                Text(
                    text = "选择账户",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AccountSelector(
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    onAccountSelected = { selectedAccount = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Date & Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(formatDate(selectedDate), fontSize = 13.sp)
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("备注...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .height(48.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Number pad
            NumberPad(
                onNumberClick = { digit ->
                    if (amount.length < 10) amount += digit
                },
                onDeleteClick = {
                    if (amount.isNotEmpty()) amount = amount.dropLast(1)
                },
                onDotClick = {
                    if (!amount.contains(".")) {
                        amount += if (amount.isEmpty()) "0." else "."
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm button
            Button(
                onClick = {
                    if (amount.isNotEmpty() && selectedCategory != null) {
                        viewModel.createTransaction(
                            amountStr = amount,
                            type = selectedType,
                            categoryId = selectedCategory?.id,
                            accountId = selectedAccount?.id,
                            date = selectedDate,
                            note = note.takeIf { it.isNotBlank() }
                        ) { insights ->
                            if (insights.isNotEmpty()) {
                                latestInsights = insights
                                showInsights = true
                            } else {
                                onSaveComplete()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = amount.isNotEmpty() && selectedCategory != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("确认记账", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Insight overlay
        AnimatedVisibility(
            visible = showInsights,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            InsightOverlay(
                insights = latestInsights,
                onDismiss = {
                    showInsights = false
                    onSaveComplete()
                }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SmartInputBar(
    categories: List<Category>,
    accounts: List<Account>,
    onParsed: (amount: String, category: Category?, account: Account?, note: String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "智能记账",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("试试：午餐35、打车20、工资5000...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                trailingIcon = {
                    if (input.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val result = parseSmartInput(input, categories, accounts)
                                if (result != null) {
                                    onParsed(result.first, result.second, result.third, result.fourth)
                                    input = ""
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "解析",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

private fun parseSmartInput(
    input: String,
    categories: List<Category>,
    accounts: List<Account>
): Quadruple<String, Category?, Account?, String>? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    val amountRegex = Regex("""(\d+(?:\.\d{1,2})?)""")
    val amountMatch = amountRegex.findAll(trimmed).lastOrNull()
    val amountStr = amountMatch?.value ?: return null

    val beforeAmount = trimmed.substring(0, amountMatch.range.first).trim()
    val afterAmount = trimmed.substring(amountMatch.range.last + 1).trim()

    val matchedCategory = categories.find { cat ->
        val keywords = when (cat.name) {
            "餐饮" -> listOf("餐", "吃", "饭", "食", "午餐", "晚餐", "早餐", "外卖", "咖啡", "奶茶", "面包")
            "交通" -> listOf("车", "路", "地铁", "公交", "打车", "滴滴", "油费", "加油", "高铁", "飞机")
            "购物" -> listOf("买", "购", "东西", "淘宝", "京东", "衣服", "鞋", "包", "超市")
            "娱乐" -> listOf("玩", "电影", "游戏", "唱", "奶茶", "咖啡", "酒吧", "KTV")
            "住房" -> listOf("房", "租", "贷", "物业", "水电", "煤", "宽带", "装修")
            "医疗" -> listOf("药", "病", "医", "挂号", "体检", "医院", "诊所")
            "教育" -> listOf("学", "课", "书", "培训", "考试", "学费", "资料")
            "通讯" -> listOf("话", "网", "流量", "宽带", "手机", "话费", "电信")
            "人情" -> listOf("礼", "红包", "请客", "份子", "婚礼", "生日")
            "工资" -> listOf("薪", "工资", "收入", "奖金", "兼职", "补贴", "提成")
            else -> listOf(cat.name)
        }
        keywords.any { beforeAmount.contains(it) }
    }

    val matchedAccount = accounts.find { acc ->
        val keywords = when (acc.name) {
            "现金" -> listOf("现金", "钱包")
            "支付宝" -> listOf("支付宝", "阿里", "淘宝")
            "微信" -> listOf("微信", "微信支付", "零钱")
            "银行卡" -> listOf("银行卡", "借记卡", "储蓄卡")
            "信用卡" -> listOf("信用卡", "花呗", "白条")
            else -> listOf(acc.name)
        }
        keywords.any { trimmed.contains(it) }
    }

    val note = if (afterAmount.isNotEmpty()) afterAmount else beforeAmount

    return Quadruple(amountStr, matchedCategory, matchedAccount, note)
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun TypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
        types.forEach { type ->
            val isSelected = type == selectedType
            val bgColor = when {
                isSelected && type == TransactionType.EXPENSE -> Color(0xFFE53935)
                isSelected && type == TransactionType.INCOME -> Color(0xFF43A047)
                else -> Color.Transparent
            }
            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (type == TransactionType.EXPENSE) "支出" else "收入",
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun AmountDisplay(amount: String, type: TransactionType) {
    val color = when (type) {
        TransactionType.EXPENSE -> Color(0xFFE53935)
        TransactionType.INCOME -> Color(0xFF43A047)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u00A5 ${if (amount.isEmpty()) "0.00" else amount}",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.height(120.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.id == selectedCategory?.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = category.icon?.take(1) ?: category.name.take(1),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = category.name,
                    fontSize = 11.sp,
                    maxLines = 1,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AccountSelector(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(52.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(accounts) { account ->
            val isSelected = account.id == selectedAccount?.id
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onAccountSelected(account) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.name,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun NumberPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onDotClick: () -> Unit
) {
    val buttons = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf(".", "0", "\u232B")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { label ->
                    val modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            when (label) {
                                "\u232B" -> onDeleteClick()
                                "." -> onDotClick()
                                else -> onNumberClick(label)
                            }
                        }

                    Box(
                        modifier = modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        if (label == "\u232B") {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Delete",
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = label,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsightOverlay(
    insights: List<Insight>,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "消费洞察",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            insights.take(3).forEach { insight ->
                InsightCardItem(insight = insight)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("知道了", fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun InsightCardItem(insight: com.pocketbook.data.entity.Insight) {
    val (icon, tint) = when (insight.type) {
        com.pocketbook.data.entity.InsightType.ANOMALY ->
            Pair(Icons.Default.Warning, Color(0xFFFF9800))
        com.pocketbook.data.entity.InsightType.FORECAST ->
            Pair(Icons.Default.TrendingUp, MaterialTheme.colorScheme.primary)
        com.pocketbook.data.entity.InsightType.RECOMMENDATION ->
            Pair(Icons.Default.Lightbulb, Color(0xFF4CAF50))
        com.pocketbook.data.entity.InsightType.SCORE ->
            Pair(Icons.Default.Star, Color(0xFF2196F3))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = insight.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (insight.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = insight.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
