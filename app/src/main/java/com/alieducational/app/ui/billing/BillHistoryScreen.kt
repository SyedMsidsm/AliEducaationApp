package com.alieducational.app.ui.billing

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillHistoryScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReceipt: (String) -> Unit = {},
    onNavigateToCustomerAccount: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val allSales by viewModel.allSales.collectAsState()
    val allSaleLines by viewModel.allSaleLines.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("Today") } // Today, This Week, This Month, All Time, Custom
    var selectedStatus by remember { mutableStateOf("All") } // All, Paid, Due, Voided

    // Custom date range bounds
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    var selectedSaleForDetail by remember { mutableStateOf<SaleEntity?>(null) }
    var repaymentSaleTarget by remember { mutableStateOf<SaleEntity?>(null) }

    // Calculate time interval
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val (startTime, endTime) = when (selectedPeriod) {
        "Today" -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            Pair(start, calendar.timeInMillis)
        }
        "This Week" -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            Pair(start, now)
        }
        "This Month" -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            Pair(start, now)
        }
        "Custom" -> {
            val s = customStartDate ?: 0L
            val e = customEndDate ?: Long.MAX_VALUE
            Pair(s, e)
        }
        else -> Pair(0L, Long.MAX_VALUE) // All Time
    }

    val displayDateRangeText = remember(selectedPeriod, startTime, endTime) {
        val df = SimpleDateFormat("dd MMM yyyy", Locale.US)
        when (selectedPeriod) {
            "Today" -> df.format(Date())
            "This Week", "This Month", "Custom" -> {
                if (startTime == 0L && endTime == Long.MAX_VALUE) "All Time"
                else "${df.format(Date(startTime))} - ${df.format(Date(minOf(endTime, now)))}"
            }
            else -> "All recorded bills"
        }
    }

    // Filtered sales
    val filteredSales = remember(allSales, startTime, endTime, searchQuery, selectedStatus) {
        allSales.filter { sale ->
            val matchesTime = sale.timestamp in startTime..endTime
            val matchesSearch = if (searchQuery.isBlank()) true else {
                sale.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                        (sale.customerName?.contains(searchQuery, ignoreCase = true) == true)
            }
            val matchesStatus = when (selectedStatus) {
                "Paid" -> !sale.isVoided && sale.remainingCreditPaise == 0L
                "Due" -> !sale.isVoided && sale.remainingCreditPaise > 0L
                "Voided" -> sale.isVoided
                else -> true
            }
            matchesTime && matchesSearch && matchesStatus
        }
    }

    val completedBills = filteredSales.filter { !it.isVoided }
    val totalBilledPaise = completedBills.sumOf { it.netTotalPaise }
    val totalCollectedPaise = completedBills.sumOf { it.amountReceivedPaise }
    val totalDuesPaise = completedBills.sumOf { it.remainingCreditPaise }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bill History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            displayDateRangeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Period Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Today", "This Week", "This Month", "All Time", "Custom").forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = {
                                    selectedPeriod = period
                                    if (period == "Custom" && customStartDate == null) {
                                        val c = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val startCal = Calendar.getInstance().apply {
                                                    set(y, m, d, 0, 0, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                customStartDate = startCal.timeInMillis

                                                DatePickerDialog(
                                                    context,
                                                    { _, ey, em, ed ->
                                                        val endCal = Calendar.getInstance().apply {
                                                            set(ey, em, ed, 23, 59, 59)
                                                            set(Calendar.MILLISECOND, 999)
                                                        }
                                                        customEndDate = endCal.timeInMillis
                                                    },
                                                    y, m, d
                                                ).show()
                                            },
                                            c.get(Calendar.YEAR),
                                            c.get(Calendar.MONTH),
                                            c.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                },
                                label = { Text(period, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Search & Status Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by receipt # or customer name") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Paid", "Due", "Voided").forEach { st ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st },
                                label = { Text(st, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Financial Metrics Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Bills", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${completedBills.size}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Billed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        CostCalculator.formatPaiseToRupees(totalBilledPaise),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Collected", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(totalCollectedPaise),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining Dues", style = MaterialTheme.typography.labelSmall, color = if (totalDuesPaise > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        CostCalculator.formatPaiseToRupees(totalDuesPaise),
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalDuesPaise > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // List of Bills
                if (filteredSales.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No bills found in this period or filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSales, key = { it.id }) { sale ->
                        BillHistoryRow(
                            sale = sale,
                            onClick = { selectedSaleForDetail = sale },
                            onRepay = { repaymentSaleTarget = sale },
                            onOpenCustomer = {
                                sale.customerId?.let { cId -> onNavigateToCustomerAccount(cId) }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Bill Detail Dialog
    selectedSaleForDetail?.let { sale ->
        val saleLines = remember(sale.id, allSaleLines) {
            allSaleLines.filter { it.saleId == sale.id }
        }

        AlertDialog(
            onDismissRequest = { selectedSaleForDetail = null },
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sale.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (sale.isVoided) {
                            Text("VOIDED", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else if (sale.remainingCreditPaise > 0) {
                            Text("DUE", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Text("PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(sale.timestamp))
                    Text(dateFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Customer Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer: ${sale.customerName ?: "Walk-in Customer"}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (sale.customerId != null) {
                            TextButton(
                                onClick = {
                                    val cId = sale.customerId
                                    selectedSaleForDetail = null
                                    onNavigateToCustomerAccount(cId)
                                }
                            ) {
                                Text("View in Accounts", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Items Snapshot
                    Text("Items / Services:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        saleLines.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val name = line.productName ?: line.serviceName ?: "Item"
                                val qtyUnit = if (line.lineType == "SERVICE") "${line.quantityBase} ${line.unitLabel ?: "job"}"
                                else CostCalculator.formatQuantityWithGrouping(line.quantityBase, "pcs")
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(
                                        "$qtyUnit @ ${CostCalculator.formatPaiseToRupees(line.unitSellingPricePaise)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    CostCalculator.formatPaiseToRupees(line.lineNetPaise),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Financial Summary
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Total:", style = MaterialTheme.typography.bodySmall)
                        Text(CostCalculator.formatPaiseToRupees(sale.grossTotalPaise), style = MaterialTheme.typography.bodySmall)
                    }
                    if (sale.discountValuePaise > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                            Text("-${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Payable:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(CostCalculator.formatPaiseToRupees(sale.netTotalPaise), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Paid (${sale.paymentMethod}):", style = MaterialTheme.typography.bodySmall)
                        Text(CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise), style = MaterialTheme.typography.bodySmall)
                    }
                    if (sale.remainingCreditPaise > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Due:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD32F2F))
                            Text(CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD32F2F))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = sale.receiptNumber
                        selectedSaleForDetail = null
                        onNavigateToReceipt(num)
                    }
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View / Share Receipt")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedSaleForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Direct Repayment Dialog for this bill
    repaymentSaleTarget?.let { sale ->
        var amountStr by remember { mutableStateOf((sale.remainingCreditPaise / 100.0).toString()) }
        var paymentMethod by remember { mutableStateOf("CASH") }
        var notes by remember { mutableStateOf("Repayment for Bill ${sale.receiptNumber}") }

        AlertDialog(
            onDismissRequest = { repaymentSaleTarget = null },
            title = { Text("Receive Repayment for ${sale.receiptNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Customer: ${sale.customerName ?: "Customer"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        "Remaining Due on Bill: ${CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)}",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paymentMethod == "CASH",
                            onClick = { paymentMethod = "CASH" },
                            label = { Text("Cash") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = paymentMethod == "UPI",
                            onClick = { paymentMethod = "UPI" },
                            label = { Text("UPI") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = paymentMethod == "BANK",
                            onClick = { paymentMethod = "BANK" },
                            label = { Text("Bank") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Repayment Amount (₹) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Reference") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amtPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L
                        if (amtPaise > 0L && sale.customerId != null) {
                            viewModel.recordAccountPayment(
                                accountType = "CUSTOMER",
                                accountId = sale.customerId,
                                amountPaise = amtPaise,
                                paymentMethod = paymentMethod,
                                notes = notes.trim().ifBlank { null },
                                receiptRef = sale.receiptNumber,
                                onDone = {
                                    repaymentSaleTarget = null
                                }
                            )
                        }
                    }
                ) {
                    Text("Confirm Repayment")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { repaymentSaleTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BillHistoryRow(
    sale: SaleEntity,
    onClick: () -> Unit,
    onRepay: () -> Unit,
    onOpenCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        sale.receiptNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (sale.isVoided) {
                    Text(
                        "VOIDED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (sale.remainingCreditPaise > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "DUE ${CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFC62828)
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "PAID",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        sale.customerName ?: "Walk-in Customer",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(sale.timestamp))
                    Text(
                        dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        CostCalculator.formatPaiseToRupees(sale.netTotalPaise),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Paid: ${CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (sale.remainingCreditPaise > 0 && !sale.isVoided && sale.customerId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onRepay,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Repayment", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
