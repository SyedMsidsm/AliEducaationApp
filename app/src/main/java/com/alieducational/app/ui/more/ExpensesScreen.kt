package com.alieducational.app.ui.more

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Text
import com.alieducational.app.domain.calculation.CostCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.ui.AliViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.allExpenses.collectAsState()
    val allExpenses = entries.filter { !it.isVoided && !it.isSavingsTransfer && (!it.isIncome || it.isExpenseRefund) }
    val grossExpenses = allExpenses.filter { !it.isIncome }.sumOf { it.amountPaise }
    val refunds = allExpenses.filter { it.isExpenseRefund }.sumOf { it.amountPaise }
    var showAddDialog by remember { mutableStateOf(false) }
    val statusColors = com.alieducational.app.ui.theme.LocalAliStatusColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop Expenses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense / Income")
            }
        }
    ) { padding ->
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gross expenses: ${com.alieducational.app.domain.calculation.CostCalculator.formatPaiseToRupees(grossExpenses)}\nLinked refunds: ${com.alieducational.app.domain.calculation.CostCalculator.formatPaiseToRupees(refunds)}\nNet expenses: ${com.alieducational.app.domain.calculation.CostCalculator.formatPaiseToRupees(grossExpenses - refunds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (allExpenses.isEmpty()) {
                    item {
                        Text(
                            text = "No non-sale expenses or income recorded yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(allExpenses) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = expense.category,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!expense.notes.isNullOrBlank()) {
                                        Text(
                                            text = expense.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(expense.timestamp))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${if (expense.isIncome) "+" else "-"}${CostCalculator.formatPaiseToRupees(expense.amountPaise)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (expense.isIncome) statusColors.paidFg else statusColors.dueFg
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        var isIncome by remember { mutableStateOf(false) }
        var category by remember { mutableStateOf("Rent") }
        var amountStr by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var categoryExpanded by remember { mutableStateOf(false) }
        var entryTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
        val dialogContext = LocalContext.current

        val expenseCategories = listOf("Rent", "Electricity", "Staff Salary", "Tea & Refreshments", "Maintenance", "Packaging Supplies", "Other Expense")
        val incomeCategories = listOf("Capital Deposit", "Owner Cash Injection", "Old Loan Recovery", "Scrap Sale", "Other Income")

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isIncome) "Record Other Money Received" else "Record Shop Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isIncome = false; category = "Rent" }) {
                            RadioButton(selected = !isIncome, onClick = { isIncome = false; category = "Rent" })
                            Text("Expense")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isIncome = true; category = "Capital Deposit" }) {
                            RadioButton(selected = isIncome, onClick = { isIncome = true; category = "Capital Deposit" })
                            Text("Other Income")
                        }
                    }

                    // Date & Time pickers
                    val cal = Calendar.getInstance().apply { timeInMillis = entryTimestamp }
                    val entryDateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(entryTimestamp))
                    val entryTimeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(entryTimestamp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    dialogContext,
                                    { _, y, m, d ->
                                        val c = Calendar.getInstance().apply {
                                            timeInMillis = entryTimestamp
                                            set(Calendar.YEAR, y)
                                            set(Calendar.MONTH, m)
                                            set(Calendar.DAY_OF_MONTH, d)
                                        }
                                        entryTimestamp = c.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(entryDateStr, fontSize = 12.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    dialogContext,
                                    { _, hour, minute ->
                                        val c = Calendar.getInstance().apply {
                                            timeInMillis = entryTimestamp
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                        }
                                        entryTimestamp = c.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(entryTimeStr, fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            val options = if (isIncome) incomeCategories else expenseCategories
                            options.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        category = opt
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Description") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L
                        if (amountPaise <= 0) return@Button

                        viewModel.recordShopExpense(
                            category = category,
                            amountPaise = amountPaise,
                            isIncome = isIncome,
                            notes = notes.trim().ifBlank { null },
                            timestamp = entryTimestamp,
                            onDone = { showAddDialog = false }
                        )
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}