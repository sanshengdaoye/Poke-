package com.pocketbook.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketbook.data.entity.Account
import com.pocketbook.data.entity.AccountType
import com.pocketbook.viewmodel.AccountViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val accountTransactions by viewModel.accountTransactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账户") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 总资产卡片
            TotalAssetsCard(totalBalance = totalBalance)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "账户列表",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (accounts.isEmpty()) {
                EmptyAccountState(onAdd = { showAddDialog = true })
            } else {
                accounts.forEach { account ->
                    AccountItem(
                        account = account,
                        onClick = { viewModel.selectAccount(account) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 选中账户的流水
            selectedAccount?.let { account ->
                Spacer(modifier = Modifier.height(16.dp))
                AccountDetailCard(
                    account = account,
                    transactions = accountTransactions,
                    onClose = { viewModel.clearSelection() }
                )
            }
        }
    }

    // 添加/编辑账户对话框
    if (showAddDialog || editingAccount != null) {
        AccountEditDialog(
            account = editingAccount,
            onDismiss = {
                showAddDialog = false
                editingAccount = null
            },
            onSave = { name, type ->
                if (editingAccount != null) {
                    viewModel.updateAccount(editingAccount!!.id, name, type)
                } else {
                    viewModel.createAccount(name, type)
                }
                showAddDialog = false
                editingAccount = null
            },
            onDelete = { accountId ->
                viewModel.deleteAccount(accountId)
                editingAccount = null
            }
        )
    }
}

@Composable
private fun TotalAssetsCard(totalBalance: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "净资产",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatMoney(totalBalance),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AccountItem(account: Account, onClick: () -> Unit) {
    val (icon, iconColor) = getAccountIconInfo(account.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getAccountTypeLabel(account.type),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatMoney(account.balance),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (account.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun AccountDetailCard(
    account: Account,
    transactions: List<com.pocketbook.data.entity.Transaction>,
    onClose: () -> Unit
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
                Text(
                    text = "${account.name} 流水",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Text(
                    text = "该账户暂无交易记录",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                transactions.take(5).forEach { tx ->
                    val isExpense = tx.type == com.pocketbook.data.entity.TransactionType.EXPENSE
                    val color = if (isExpense) Color(0xFFC62828) else Color(0xFF2E7D32)
                    val sign = if (isExpense) "-" else "+"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = tx.note ?: (if (isExpense) "支出" else "收入"),
                                fontSize = 13.sp
                            )
                            Text(
                                text = formatSimpleDate(tx.date),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$sign${formatMoney(tx.amount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountState(onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🏦", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "还没有账户",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加账户")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditDialog(
    account: Account?,
    onDismiss: () -> Unit,
    onSave: (String, AccountType) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var selectedType by remember { mutableStateOf(account?.type ?: AccountType.CASH) }
    val types = listOf(
        AccountType.CASH to "现金",
        AccountType.DEBIT_CARD to "银行卡",
        AccountType.CREDIT_CARD to "信用卡",
        AccountType.ALIPAY to "支付宝",
        AccountType.WECHAT_PAY to "微信支付",
        AccountType.OTHER to "其他"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "添加账户" else "编辑账户") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名称") },
                    placeholder = { Text("例如：工资卡") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "账户类型",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                types.forEach { (type, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Text(text = label, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, selectedType)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                if (account != null) {
                    TextButton(
                        onClick = { onDelete(account.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

private fun getAccountIconInfo(type: AccountType): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    return when (type) {
        AccountType.CASH -> Icons.Default.Money to Color(0xFF4CAF50)
        AccountType.DEBIT_CARD -> Icons.Default.CreditCard to Color(0xFF2196F3)
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard to Color(0xFFFF9800)
        AccountType.ALIPAY -> Icons.Default.AccountBalanceWallet to Color(0xFF00BCD4)
        AccountType.WECHAT_PAY -> Icons.Default.Chat to Color(0xFF4CAF50)
        AccountType.OTHER -> Icons.Default.AccountBalance to Color(0xFF9E9E9E)
    }
}

private fun getAccountTypeLabel(type: AccountType): String {
    return when (type) {
        AccountType.CASH -> "现金"
        AccountType.DEBIT_CARD -> "银行卡"
        AccountType.CREDIT_CARD -> "信用卡"
        AccountType.ALIPAY -> "支付宝"
        AccountType.WECHAT_PAY -> "微信支付"
        AccountType.OTHER -> "其他"
    }
}

private fun formatMoney(cents: Long): String {
    val yuan = cents / 100.0
    return NumberFormat.getCurrencyInstance(java.util.Locale.CHINA).format(yuan)
}

private fun formatSimpleDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
