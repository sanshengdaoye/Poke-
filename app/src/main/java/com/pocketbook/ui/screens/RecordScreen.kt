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
    var insights by remember { mutableStateOf<List<Insight>>(emptyList()) }
    var showInsights by remember { mutableStateOf(false) }

    val categories by categoryViewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val filteredCategories = categories.filter {
        if (selectedType == TransactionType.EXPENSE)
            it.type == com.pocketbook.data.entity.CategoryType.EXPENSE
        else
            it.type == com.pocketbook.data.entity.CategoryType.INCOME
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // NLP智能记账输入
        var nlpInput by remember { mutableStateOf("") }
        var nlpPreview by remember { mutableStateOf("") }
        val nlpParser = remember { com.pocketbook.service.NlpParser() }

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
                    value = nlpInput,
                    onValueChange = { 
                        nlpInput = it
                        if (it.isNotBlank()) {
                            val result = nlpParser.parse(it, filteredCategories)
                            nlpPreview = if (result != null && result.category != null) {
                                "${result.category.name} ¥${result.amount}"
                            } else if (result != null) {
                                "¥${result.amount}（未识别分类）"
                            } else ""
                        } else {
                            nlpPreview = ""
                        }
                    },
                    placeholder = { Text("试试：午餐35、打车20、工资5000...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    trailingIcon = {
                        if (nlpInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val result = nlpParser.parse(nlpInput, filteredCategories)
                                    if (result != null) {
                                        amount = result.amount
                                        selectedCategory = result.category
                                        if (result.note.isNotBlank() && result.note != result.category?.name) {
                                            note = result.note
                                        }
                                        if (result.category != null) {
                                            selectedType = if (result.type == com.pocketbook.data.entity.CategoryType.INCOME) 
                                                TransactionType.INCOME else TransactionType.EXPENSE
                                        }
                                        nlpInput = ""
                                        nlpPreview = ""
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "解析",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
                if (nlpPreview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "识别结果: $nlpPreview",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 类型切换
        TypeSelector(selectedType = selectedType, onTypeSelected = {
            selectedType = it
            selectedCategory = null
        })

        Spacer(modifier = Modifier.height(16.dp))

        // 金额显示
        AmountDisplay(amount = amount, type = selectedType)

        Spacer(modifier = Modifier.height(12.dp))

        // 分类网格
        Text("选择分类", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        CategoryGrid(
            categories = filteredCategories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 账户选择
        if (accounts.isNotEmpty()) {
            Text("选择账户", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            AccountSelector(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onAccountSelected = { selectedAccount = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 日期和备注
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.DateRange, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(formatDate(selectedDate), fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("备注...", fontSize = 13.sp) },
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 数字键盘
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
            },
            onZeroClick = {
                if (amount.isNotEmpty() && amount.length < 10) amount += "0"
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 确认按钮
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
                    ) { generatedInsights ->
                        insights = generatedInsights
                        if (generatedInsights.isNotEmpty()) {
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
            Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("确认记账", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }

    // Insight Dialog
    if (showInsights) {
        AlertDialog(
            onDismissRequest = { showInsights = false; onSaveComplete() },
            icon = { Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("消费洞察") },
            text = {
                Column {
                    insights.forEach { insight ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (insight.severity) {
                                    com.pocketbook.data.entity.InsightSeverity.HIGH -> Color(0xFFC62828).copy(alpha = 0.1f)
                                    com.pocketbook.data.entity.InsightSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(insight.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(insight.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInsights = false; onSaveComplete() }) {
                    Text("知道了")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun AccountSelector(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.take(4).forEach { account ->
            val isSelected = account.id == selectedAccount?.id
            val color = if (account.color != null) Color(account.color) else MaterialTheme.colorScheme.primary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onAccountSelected(account) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    account.name.take(2),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "¥${account.balance / 100}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
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
                isSelected && type == TransactionType.EXPENSE -> Color(0xFFC62828)
                isSelected && type == TransactionType.INCOME -> Color(0xFF2E7D32)
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
                    .padding(vertical = 6.dp),
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
        TransactionType.EXPENSE -> Color(0xFFC62828)
        TransactionType.INCOME -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "¥ ${if (amount.isEmpty()) "0.00" else amount}",
            fontSize = 42.sp,
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
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
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
                    .padding(vertical = 5.dp, horizontal = 2.dp)
            ) {
                Text(
                    text = category.name.take(2),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 1.dp)
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
fun NumberPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onDotClick: () -> Unit,
    onZeroClick: () -> Unit
) {
    val rows = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf(".", "0", "DEL")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { row ->
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
                                "DEL" -> onDeleteClick()
                                "." -> onDotClick()
                                "0" -> onZeroClick()
                                else -> onNumberClick(label)
                            }
                        }

                    Box(modifier = modifier, contentAlignment = Alignment.Center) {
                        when (label) {
                            "DEL" -> Icon(
                                imageVector = Icons.Filled.Backspace,
                                contentDescription = "删除",
                                modifier = Modifier.size(20.dp)
                            )
                            else -> Text(
                                text = label,
                                fontSize = 20.sp,
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
    return SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
}
