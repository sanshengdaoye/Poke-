package com.pocketbook.ui.screens

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
import androidx.compose.material.icons.outlined.*
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
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.viewmodel.CategoryViewModel
import com.pocketbook.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onSaveComplete: () -> Unit
) {
    val viewModel: RecordViewModel = hiltViewModel()
    val categoryViewModel: CategoryViewModel = hiltViewModel()

    val accounts by viewModel.accounts.collectAsState()
    val expenseCategories by categoryViewModel.expenseCategories.collectAsState()
    val incomeCategories by categoryViewModel.incomeCategories.collectAsState()

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }

    val filteredCategories = if (selectedType == TransactionType.EXPENSE) expenseCategories else incomeCategories

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    if (showDatePicker) {
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

    if (showAccountSelector && accounts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showAccountSelector = false },
            title = { Text("选择账户") },
            text = {
                Column {
                    accounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccount = account
                                    showAccountSelector = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = account.name,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedAccount?.id == account.id) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountSelector = false }) {
                    Text("关闭")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // NLP快速记账输入
        NaturalLanguageInput(
            categories = filteredCategories,
            onParsed = { parsedAmount, parsedCategory, parsedNote ->
                amount = parsedAmount
                selectedCategory = parsedCategory
                if (parsedNote.isNotBlank()) note = parsedNote
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 收支类型切换
        TypeSelector(
            selectedType = selectedType,
            onTypeSelected = {
                selectedType = it
                selectedCategory = null
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 金额显示
        AmountDisplay(
            amount = amount,
            type = selectedType
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 账户选择器
        AccountSelector(
            selectedAccount = selectedAccount,
            onClick = { showAccountSelector = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 分类选择
        Text(
            text = "选择分类",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CompactCategoryGrid(
            categories = filteredCategories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 日期和备注
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

        // 数字键盘
        CompactNumberPad(
            onNumberClick = { digit ->
                if (amount.length < 10) {
                    amount += digit
                }
            },
            onDeleteClick = {
                if (amount.isNotEmpty()) {
                    amount = amount.dropLast(1)
                }
            },
            onDotClick = {
                if (!amount.contains(".")) {
                    amount += if (amount.isEmpty()) "0." else "."
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 确认按钮
        Button(
            onClick = {
                if (amount.isNotEmpty()) {
                    // 如果没有选分类，尝试用默认分类
                    val finalCategoryId = selectedCategory?.id
                    viewModel.createTransaction(
                        amountStr = amount,
                        type = selectedType,
                        categoryId = finalCategoryId,
                        accountId = selectedAccount?.id,
                        date = selectedDate,
                        note = note.takeIf { it.isNotBlank() }
                    ) { insights ->
                        onSaveComplete()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = amount.isNotEmpty(), // 不再强制要求分类
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("确认记账", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AccountSelector(
    selectedAccount: Account?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedAccount?.name ?: "选择账户（可选）",
                    fontSize = 14.sp,
                    color = if (selectedAccount != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun CompactCategoryGrid(
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
            val isSelected = selectedCategory?.id == category.id
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
                    text = category.name.take(2),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = category.name,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactNumberPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onDotClick: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "del")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    val modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            when (key) {
                                "del" -> onDeleteClick()
                                "." -> onDotClick()
                                else -> onNumberClick(key)
                            }
                        }

                    Box(
                        modifier = modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "del") {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "删除",
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                fontSize = 18.sp,
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
fun NaturalLanguageInput(
    categories: List<Category>,
    onParsed: (amount: String, category: Category?, note: String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "智能记账",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("试试输入：午餐35、打车20、工资5000...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                trailingIcon = {
                    if (input.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val result = parseNaturalLanguage(input, categories)
                                if (result != null) {
                                    onParsed(result.first, result.second, result.third)
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

@Composable
private fun TypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        val types = listOf(
            TransactionType.EXPENSE to "支出",
            TransactionType.INCOME to "收入",
            TransactionType.TRANSFER to "转账"
        )
        types.forEach { (type, label) ->
            val isSelected = selectedType == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AmountDisplay(
    amount: String,
    type: TransactionType
) {
    val prefix = when (type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "="
    }
    val color = when (type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (amount.isEmpty()) "0.00" else "$prefix$amount",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun parseNaturalLanguage(
    input: String,
    categories: List<Category>
): Triple<String, Category?, String>? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    val amountRegex = Regex("""(\d+(?:\.\d{1,2})?)""")
    val amountMatch = amountRegex.findAll(trimmed).lastOrNull()
    val amountStr = amountMatch?.value ?: return null

    val beforeAmount = trimmed.substring(0, amountMatch.range.first).trim()
    val afterAmount = trimmed.substring(amountMatch.range.last + 1).trim()

    val matchedCategory = if (beforeAmount.isNotEmpty()) {
        categories.find { cat ->
            cat.name.contains(beforeAmount) || beforeAmount.contains(cat.name) ||
            (cat.name == "餐饮" && (beforeAmount.contains("餐") || beforeAmount.contains("吃") ||
                                    beforeAmount.contains("饭") || beforeAmount.contains("食") ||
                                    beforeAmount.contains("午餐") || beforeAmount.contains("晚餐") ||
                                    beforeAmount.contains("早餐") || beforeAmount.contains("外卖"))) ||
            (cat.name == "交通" && (beforeAmount.contains("车") || beforeAmount.contains("路") ||
                                    beforeAmount.contains("地铁") || beforeAmount.contains("公交") ||
                                    beforeAmount.contains("打车") || beforeAmount.contains("滴滴") ||
                                    beforeAmount.contains("油费") || beforeAmount.contains("加油"))) ||
            (cat.name == "购物" && (beforeAmount.contains("买") || beforeAmount.contains("购") ||
                                    beforeAmount.contains("东西") || beforeAmount.contains("淘宝") ||
                                    beforeAmount.contains("京东") || beforeAmount.contains("衣服"))) ||
            (cat.name == "娱乐" && (beforeAmount.contains("玩") || beforeAmount.contains("电影") ||
                                    beforeAmount.contains("游戏") || beforeAmount.contains("唱") ||
                                    beforeAmount.contains("奶茶") || beforeAmount.contains("咖啡"))) ||
            (cat.name == "住房" && (beforeAmount.contains("房") || beforeAmount.contains("租") ||
                                    beforeAmount.contains("贷") || beforeAmount.contains("物业") ||
                                    beforeAmount.contains("水电"))) ||
            (cat.name == "医疗" && (beforeAmount.contains("药") || beforeAmount.contains("病") ||
                                    beforeAmount.contains("医") || beforeAmount.contains("挂号") ||
                                    beforeAmount.contains("体检"))) ||
            (cat.name == "教育" && (beforeAmount.contains("学") || beforeAmount.contains("课") ||
                                    beforeAmount.contains("书") || beforeAmount.contains("培训") ||
                                    beforeAmount.contains("考试"))) ||
            (cat.name == "通讯" && (beforeAmount.contains("话") || beforeAmount.contains("网") ||
                                    beforeAmount.contains("流量") || beforeAmount.contains("宽带") ||
                                    beforeAmount.contains("手机"))) ||
            (cat.name == "人情" && (beforeAmount.contains("礼") || beforeAmount.contains("红包") ||
                                    beforeAmount.contains("请客") || beforeAmount.contains("份子"))) ||
            (cat.name == "工资" && (beforeAmount.contains("薪") || beforeAmount.contains("工资") ||
                                    beforeAmount.contains("收入") || beforeAmount.contains("奖金") ||
                                    beforeAmount.contains("兼职")))
        }
    } else {
        // 如果没有关键词但有分类列表，默认选第一个
        categories.firstOrNull()
    }

    // 如果还是没匹配到，用第一个分类兜底，保证能记账
    val finalCategory = matchedCategory ?: categories.firstOrNull()

    val note = if (afterAmount.isNotEmpty()) afterAmount else beforeAmount

    return Triple(amountStr, finalCategory, note)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
