package com.pocketbook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(16.dp)
    ) {
        TypeSelector(
            selectedType = selectedType,
            onTypeSelected = { 
                selectedType = it
                selectedCategory = null
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AmountDisplay(
            amount = amount,
            type = selectedType
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true }
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(formatDate(selectedDate))
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("添加备注...") },
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (amount.isNotEmpty() && selectedCategory != null) {
                    val amountInCents = (amount.toDoubleOrNull() ?: 0.0) * 100
                    val transaction = Transaction(
                        bookId = "", // Will be set by ViewModel or repository
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
                .height(56.dp),
            enabled = amount.isNotEmpty() && selectedCategory != null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("确认记账", fontSize = 18.sp, fontWeight = FontWeight.Medium)
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
        modifier = Modifier.height(200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.id == selectedCategory?.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(8.dp)
            ) {
                Text(
                    text = category.name.take(2),
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = category.name,
                    fontSize = 12.sp,
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
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
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
