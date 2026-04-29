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
import com.pocketbook.data.entity.Category
import com.pocketbook.data.entity.Transaction
import com.pocketbook.data.entity.TransactionType
import com.pocketbook.viewmodel.CategoryViewModel
import com.pocketbook.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordScreen(
    onSaveComplete: () -> Unit = {},
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories by categoryViewModel.categories.collectAsState()

    val filteredCategories = categories.filter { 
        if (selectedType == TransactionType.EXPENSE) it.type == com.pocketbook.data.entity.CategoryType.EXPENSE 
        else it.type == com.pocketbook.data.entity.CategoryType.INCOME 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 自然语言快速记账
        NaturalLanguageInput(
            categories = filteredCategories,
            onParsed = { parsedAmount, parsedCategory, parsedNote ->
                amount = parsedAmount
                selectedCategory = parsedCategory
                if (parsedNote.isNotBlank()) note = parsedNote
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TypeSelector(
            selectedType = selectedType,
            onTypeSelected = {
                selectedType = it
                selectedCategory = null
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AmountDisplay(
            amount = amount,
            type = selectedType
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "选择分类",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CategoryGrid(
            categories = filteredCategories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

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

        NumberPad(
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

        Button(
            onClick = {
                if (amount.isNotEmpty() && selectedCategory != null) {
                    val amountInCents = (amount.toDoubleOrNull() ?: 0.0) * 100
                    val transaction = Transaction(
                        bookId = "",
                        type = selectedType,
                        amount = amountInCents.toLong(),
                        categoryId = selectedCategory?.id,
                        date = selectedDate,
                        note = note.takeIf { it.isNotBlank() }
                    )
                    viewModel.createTransaction(transaction)
                    onSaveComplete()
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
fun TypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
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
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (type == TransactionType.EXPENSE) "支出" else "收入",
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun AmountDisplay(
    amount: String,
    type: TransactionType
) {
    val color = when (type) {
        TransactionType.EXPENSE -> Color(0xFFE53935)
        TransactionType.INCOME -> Color(0xFF43A047)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u00A5 ${if (amount.isEmpty()) "0.00" else amount}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
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
        modifier = Modifier.height(140.dp),
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
                    text = category.name.take(2),
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = label,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
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
            if (input.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "按回车或点击箭头自动解析",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun parseNaturalLanguage(
    input: String,
    categories: List<Category>
): Triple<String, Category?, String>? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    // Extract amount: find the last number (supports decimals)
    val amountRegex = Regex("""(\d+(?:\.\d{1,2})?)""")
    val amountMatch = amountRegex.findAll(trimmed).lastOrNull()
    val amountStr = amountMatch?.value ?: return null

    // Extract category keyword: text before the amount
    val beforeAmount = trimmed.substring(0, amountMatch.range.first).trim()
    val afterAmount = trimmed.substring(amountMatch.range.last + 1).trim()

    // Match category
    val matchedCategory = if (beforeAmount.isNotEmpty()) {
        categories.find { cat ->
            cat.name.contains(beforeAmount) || beforeAmount.contains(cat.name) ||
            // Fuzzy match common aliases
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
    } else null

    val note = if (afterAmount.isNotEmpty()) afterAmount else beforeAmount

    return Triple(amountStr, matchedCategory, note)
}
