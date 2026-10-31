package com.alieducational.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allProducts by viewModel.allProducts.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val allSaleLines by viewModel.allSaleLines.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val deviceRole by viewModel.deviceRole.collectAsState()

    val isOwner = deviceRole == "OWNER"
    var activeTab by remember { mutableStateOf("PRODUCTS") } // PRODUCTS or SERVICES
    var selectedPeriod by remember { mutableStateOf("All Time") } // "Today", "This Week", "This Month", "All Time"
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    // Calculate time boundaries
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val (startTime, endTime) = when (selectedPeriod) {
        "Today" -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            Pair(calendar.timeInMillis, now)
        }
        "This Week" -> {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            Pair(calendar.timeInMillis, now)
        }
        "This Month" -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            Pair(calendar.timeInMillis, now)
        }
        else -> Pair(0L, Long.MAX_VALUE) // All Time
    }

    // Filter non-voided sales & expenses in this period
    val periodSales = allSales.filter { !it.isVoided && it.timestamp in startTime..endTime }
    val periodSaleIds = periodSales.map { it.id }.toSet()
    val periodSaleLines = allSaleLines.filter { it.saleId in periodSaleIds }

    // Retail Product metrics (exclude service lines)
    val periodProductLines = periodSaleLines.filter { it.lineType != "SERVICE" }
    val productRevenuePaise: Long = periodProductLines.sumOf { it.lineNetPaise }
    val productCogsPaise: Long = periodProductLines.sumOf { line ->
        line.quantityBase.toLong() * (line.unitCostSnapshotPaise ?: 0L)
    }
    val productGrossProfitPaise: Long = productRevenuePaise - productCogsPaise
    val productGrossMarginPct = if (productRevenuePaise > 0) (productGrossProfitPaise.toDouble() / productRevenuePaise.toDouble()) * 100.0 else 0.0

    // Operating expenses: Exclude Pigmy savings transfers and exclude service supply expenses
    val periodGeneralExpenses = allExpenses.filter {
        !it.isVoided && !it.isIncome && !it.isSavingsTransfer && it.category != "SERVICE_EXPENSE" && it.timestamp in startTime..endTime
    }
    val totalGeneralExpensesPaise: Long = periodGeneralExpenses.sumOf { it.amountPaise }
    val productNetProfitPaise: Long = productGrossProfitPaise - totalGeneralExpensesPaise
    val productNetMarginPct = if (productRevenuePaise > 0) (productNetProfitPaise.toDouble() / productRevenuePaise.toDouble()) * 100.0 else 0.0

    // Potential inventory gross profit
    val expectedGrossProfitPaise: Long = allProducts.sumOf { p ->
        val potentialRevenue = p.currentSellableStock.toLong() * p.sellingPricePiecePaise
        val costValue = p.currentSellableStock.toLong() * p.movingAvgCostPaise
        potentialRevenue - costValue
    }

    // Services metrics
    val periodServiceLines = periodSaleLines.filter { it.lineType == "SERVICE" }
    val serviceRevenuePaise: Long = periodServiceLines.sumOf { it.lineNetPaise }
    val periodServiceExpenses = allExpenses.filter {
        !it.isVoided && !it.isIncome && it.category == "SERVICE_EXPENSE" && it.timestamp in startTime..endTime
    }
    val totalServiceExpensesPaise: Long = periodServiceExpenses.sumOf { it.amountPaise }
    val serviceDifferencePaise: Long = serviceRevenuePaise - totalServiceExpensesPaise

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales & Profit", fontWeight = FontWeight.Bold) },
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tab Switcher (Services tab visible only for Owner)
                if (isOwner) {
                    item {
                        TabRow(
                            selectedTabIndex = if (activeTab == "PRODUCTS") 0 else 1,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = activeTab == "PRODUCTS",
                                onClick = { activeTab = "PRODUCTS" },
                                text = { Text("Products", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = activeTab == "SERVICES",
                                onClick = { activeTab = "SERVICES" },
                                text = { Text("Services (Owner)", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                // Period Filter Tabs
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val periods = listOf("Today", "This Week", "This Month", "All Time")
                        items(periods) { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period, fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                if (activeTab == "PRODUCTS" || !isOwner) {
                    // ── PRODUCTS TAB CONTENT ───────────────────────────
                    item {
                        val isProfitPositive = productNetProfitPaise >= 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isProfitPositive) Color(0xFF1B3828) else Color(0xFF3E1B1B)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isProfitPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isProfitPositive) Color(0xFF75B890) else Color(0xFFE57373),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Product Net Profit ($selectedPeriod)",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f", productNetMarginPct)}%",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isProfitPositive) Color(0xFF75B890) else Color(0xFFE57373)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = CostCalculator.formatPaiseToRupees(productNetProfitPaise),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isProfitPositive) Color(0xFF75B890) else Color(0xFFE57373)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Gross Profit ${CostCalculator.formatPaiseToRupees(productGrossProfitPaise)} minus Expenses ${CostCalculator.formatPaiseToRupees(totalGeneralExpensesPaise)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Product Sales", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(productRevenuePaise),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Cost of Goods Sold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(productCogsPaise),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Inventory Gross Potential", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(expectedGrossProfitPaise),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Projected gross profit when current warehouse stock is fully sold at marked prices.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // ── SERVICES TAB CONTENT (OWNER ONLY) ───────────────
                    // Prominent Disclaimer Box
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2411)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFE4B765),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Not exact service profit. Supplies may be used across months, and some sales may be unpaid.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF0D69A)
                                )
                            }
                        }
                    }

                    // Services Summary Cards (3 metrics)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (serviceDifferencePaise >= 0) Color(0xFF1B3828) else Color(0xFF3E1B1B)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Revenue Minus Recorded Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CostCalculator.formatPaiseToRupees(serviceDifferencePaise),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (serviceDifferencePaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Service Sales", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            CostCalculator.formatPaiseToRupees(serviceRevenuePaise),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Service Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            CostCalculator.formatPaiseToRupees(totalServiceExpensesPaise),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFE57373)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recorded Service Expenses Header with "+ Add Expense"
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recorded Service Expenses",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Button(
                                onClick = { showAddExpenseDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (periodServiceExpenses.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                    Text("No service expenses recorded for $selectedPeriod", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(periodServiceExpenses) { exp ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${exp.expenseCategory ?: "Supply"} · ${exp.serviceTarget ?: "Shared"}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(exp.timestamp))
                                        Text(
                                            text = "$dateStr ${if (!exp.notes.isNullOrBlank()) "· " + exp.notes else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = CostCalculator.formatPaiseToRupees(exp.amountPaise),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFE57373)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { viewModel.voidExpense(exp.id, "Voided by user") },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.DeleteOutline,
                                                contentDescription = "Void",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddServiceExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { category, target, amountPaise, notes ->
                viewModel.recordServiceExpense(
                    category = category,
                    amountPaise = amountPaise,
                    serviceTarget = target,
                    notes = notes
                )
                showAddExpenseDialog = false
            }
        )
    }
}

@Composable
fun AddServiceExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (category: String, target: String, amountPaise: Long, notes: String?) -> Unit
) {
    val categories = listOf("Paper", "Ink / Toner", "Sticker sheets", "Machine repair", "Other")
    val targets = listOf("Printing", "Photocopying", "Sticker printing", "Shared")

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var selectedTarget by remember { mutableStateOf(targets.first()) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Service Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Supply Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Text("Service Target", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(targets) { tgt ->
                        FilterChip(
                            selected = selectedTarget == tgt,
                            onClick = { selectedTarget = tgt },
                            label = { Text(tgt, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onSave(selectedCategory, selectedTarget, (amt * 100).toLong(), notesText.trim().ifBlank { null })
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
