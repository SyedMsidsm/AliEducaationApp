package com.alieducational.app.ui.billing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import com.alieducational.app.ui.BulkResult
import com.alieducational.app.ui.theme.AliDimens
import com.alieducational.app.ui.theme.LocalAliStatusColors
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun utcPickerMillisToLocalStart(ms: Long): Long =
    Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun utcPickerMillisToLocalEnd(ms: Long): Long =
    Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
        .plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillHistoryScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReceipt: (String) -> Unit = {},
    onNavigateToCustomerAccount: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allSales by viewModel.allSales.collectAsState()
    val selectedBillIds by viewModel.selectedBillIds.collectAsState()
    var displayLimit by remember { mutableStateOf(100) }
    val deviceRole by viewModel.deviceRole.collectAsState()
    val isOwner = deviceRole == "OWNER"

    var showVoidConfirmDialog by remember { mutableStateOf(false) }
    var bulkResult by remember { mutableStateOf<BulkResult?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    BackHandler {
        if (selectedBillIds.isNotEmpty()) {
            viewModel.clearBillSelection()
        } else {
            onNavigateBack()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("Today") } // Today, This Week, This Month, All Time, Custom
    var selectedStatus by remember { mutableStateOf("All") } // All, Paid, Due, Voided

    // Custom date range bounds (in local epoch millis)
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    var selectedSaleForDetail by remember { mutableStateOf<SaleEntity?>(null) }
    var repaymentSaleTarget by remember { mutableStateOf<SaleEntity?>(null) }

    // Calculate time interval
    val now = System.currentTimeMillis()
    val (startTime, endTime) = remember(selectedPeriod, customStartDate, customEndDate, now) {
        val calendar = Calendar.getInstance()
        when (selectedPeriod) {
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
    }

    val displayDateRangeText = remember(selectedPeriod, startTime, endTime) {
        val df = SimpleDateFormat("dd MMM yyyy", Locale.US)
        when (selectedPeriod) {
            "Today" -> "Today • ${df.format(Date(now))}"
            "This Week", "This Month", "Custom" -> {
                if (startTime == 0L && endTime == Long.MAX_VALUE) "All Time"
                else "${df.format(Date(startTime))} - ${df.format(Date(minOf(endTime, now)))}"
            }
            else -> "All recorded bills"
        }
    }

    // Time + Search filtered sales (used for counts)
    val timeAndSearchFilteredSales = remember(allSales, startTime, endTime, searchQuery) {
        allSales.filter { sale ->
            val matchesTime = sale.timestamp in startTime..endTime
            val matchesSearch = if (searchQuery.isBlank()) true else {
                sale.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                        (sale.customerName?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesTime && matchesSearch
        }
    }

    // Filtered sales including status
    val filteredSales = remember(timeAndSearchFilteredSales, selectedStatus) {
        timeAndSearchFilteredSales.filter { sale ->
            when (selectedStatus) {
                "Paid" -> !sale.isVoided && sale.remainingCreditPaise == 0L
                "Due" -> !sale.isVoided && sale.remainingCreditPaise > 0L
                "Voided" -> sale.isVoided
                else -> true
            }
        }
    }

    val visibleSales = remember(filteredSales, displayLimit) {
        filteredSales.take(displayLimit)
    }

    // Status counts for chips
    val allCount = timeAndSearchFilteredSales.size
    val paidCount = remember(timeAndSearchFilteredSales) {
        timeAndSearchFilteredSales.count { !it.isVoided && it.remainingCreditPaise == 0L }
    }
    val dueCount = remember(timeAndSearchFilteredSales) {
        timeAndSearchFilteredSales.count { !it.isVoided && it.remainingCreditPaise > 0L }
    }
    val voidedCount = remember(timeAndSearchFilteredSales) {
        timeAndSearchFilteredSales.count { it.isVoided }
    }

    val completedBills = filteredSales.filter { !it.isVoided }
    val totalBilledPaise = completedBills.sumOf { it.netTotalPaise }
    val totalCollectedPaise = completedBills.sumOf { it.amountReceivedPaise }
    val totalDuesPaise = completedBills.sumOf { it.remainingCreditPaise }

    val statusColors = LocalAliStatusColors.current

    // Grouping by local date
    val groupedSales = remember(visibleSales) {
        visibleSales.groupBy { sale ->
            Instant.ofEpochMilli(sale.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    val todayDate = remember { LocalDate.now() }
    val yesterdayDate = remember { LocalDate.now().minusDays(1) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (selectedBillIds.isNotEmpty()) {
                TopAppBar(
                    title = {
                        Text("${selectedBillIds.size} selected", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearBillSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            viewModel.selectAllBills(filteredSales.map { it.id })
                        }) {
                            Text("Select All", fontWeight = FontWeight.SemiBold)
                        }
                        if (isOwner) {
                            IconButton(onClick = {
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null) {
                                    viewModel.stepUp.require(
                                        activity = activity,
                                        title = "Authenticate to void bills",
                                        onOk = { showVoidConfirmDialog = true },
                                        onFail = { /* cancel */ }
                                    )
                                } else {
                                    showVoidConfirmDialog = true
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Void Selected Bills",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
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
                    .padding(horizontal = AliDimens.spacingLg),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = AliDimens.spacingXs, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
            ) {
                item {
                    Spacer(modifier = Modifier.height(AliDimens.spacingXs))

                    // Period Selector Chips (Non-wrapping LazyRow)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                    ) {
                        items(listOf("Today", "This Week", "This Month", "All Time")) { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period, maxLines = 1, softWrap = false, fontSize = AliDimens.minChipTextSize) }
                            )
                        }
                        item {
                            val customChipLabel = if (customStartDate != null && customEndDate != null && selectedPeriod == "Custom") {
                                val df = SimpleDateFormat("dd MMM", Locale.US)
                                "${df.format(Date(customStartDate!!))} - ${df.format(Date(customEndDate!!))}"
                            } else {
                                "Custom"
                            }
                            FilterChip(
                                selected = selectedPeriod == "Custom",
                                onClick = {
                                    showDateRangePicker = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = if (selectedPeriod == "Custom" && customStartDate != null) {
                                    {
                                        IconButton(
                                            onClick = {
                                                customStartDate = null
                                                customEndDate = null
                                                selectedPeriod = "Today"
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear custom date", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                } else null,
                                label = { Text(customChipLabel, maxLines = 1, softWrap = false, fontSize = AliDimens.minChipTextSize) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AliDimens.spacingSm))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by receipt # or customer name") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(AliDimens.radiusSm)
                    )

                    Spacer(modifier = Modifier.height(AliDimens.spacingSm))

                    // Status Chips with counts
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                    ) {
                        val statusItems = listOf(
                            Pair("All", allCount),
                            Pair("Paid", paidCount),
                            Pair("Due", dueCount),
                            Pair("Voided", voidedCount)
                        )
                        items(statusItems) { (st, count) ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st },
                                label = { Text("$st ($count)", maxLines = 1, softWrap = false, fontSize = AliDimens.minChipTextSize) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AliDimens.spacingSm))

                    // Financial Hero Summary Card (Clickable to filter to Due)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStatus = if (selectedStatus == "Due") "All" else "Due"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(AliDimens.radiusMd)
                    ) {
                        Column(modifier = Modifier.padding(AliDimens.spacingLg)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "NET BILLED",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        CostCalculator.formatPaiseToRupees(totalBilledPaise),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "${completedBills.size} bills",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(AliDimens.spacingSm))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(AliDimens.spacingSm))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Collected", style = MaterialTheme.typography.labelSmall, color = statusColors.paidFg)
                                    Text(
                                        CostCalculator.formatPaiseToRupees(totalCollectedPaise),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = statusColors.paidFg
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining Dues", style = MaterialTheme.typography.labelSmall, color = statusColors.dueFg)
                                    Text(
                                        CostCalculator.formatPaiseToRupees(totalDuesPaise),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (totalDuesPaise > 0) statusColors.dueFg else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AliDimens.spacingXs))
                }

                // List of Bills or Empty State
                if (filteredSales.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AliDimens.spacingLg),
                            shape = RoundedCornerShape(AliDimens.radiusMd),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AliDimens.spacingXl),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AliDimens.spacingMd)
                            ) {
                                Icon(
                                    Icons.Filled.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = if (selectedPeriod == "Today") "No bills today" else "No bills found for the selected filter",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                ) {
                                    if (selectedPeriod != "This Week") {
                                        Button(
                                            onClick = { selectedPeriod = "This Week" },
                                            shape = RoundedCornerShape(AliDimens.radiusSm)
                                        ) {
                                            Text("View this week")
                                        }
                                    }
                                    if (searchQuery.isNotEmpty() || selectedStatus != "All" || selectedPeriod != "Today") {
                                        OutlinedButton(
                                            onClick = {
                                                searchQuery = ""
                                                selectedStatus = "All"
                                                selectedPeriod = "All Time"
                                            },
                                            shape = RoundedCornerShape(AliDimens.radiusSm)
                                        ) {
                                            Text("Clear filters")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Grouped by day with stickyHeaders
                    groupedSales.forEach { (localDate, daySales) ->
                        val dayLabel = when (localDate) {
                            todayDate -> "Today"
                            yesterdayDate -> "Yesterday"
                            else -> localDate.format(dateFormatter)
                        }
                        val dayTotalPaise = daySales.filter { !it.isVoided }.sumOf { it.netTotalPaise }

                        stickyHeader(key = "header_${localDate}") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AliDimens.spacingXs),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(AliDimens.radiusSm))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = AliDimens.spacingMd, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = CostCalculator.formatPaiseToRupees(dayTotalPaise),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        items(daySales, key = { it.id }) { sale ->
                            val isSelected = selectedBillIds.contains(sale.id)
                            BillHistoryRow(
                                sale = sale,
                                isSelected = isSelected,
                                isInSelectionMode = selectedBillIds.isNotEmpty(),
                                onToggleSelect = { viewModel.toggleBillSelection(sale.id) },
                                onLongClick = {
                                    if (isOwner) {
                                        viewModel.toggleBillSelection(sale.id)
                                    }
                                },
                                onClick = { selectedSaleForDetail = sale },
                                onRepay = { repaymentSaleTarget = sale },
                                onOpenCustomer = {
                                    sale.customerId?.let { cId -> onNavigateToCustomerAccount(cId) }
                                }
                            )
                        }
                    }

                    if (filteredSales.size > displayLimit) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AliDimens.spacingMd),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { displayLimit += 100 }
                                ) {
                                    Text("Load More (${filteredSales.size - displayLimit} remaining)")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AliDimens.spacingXl))
                }
            }
        }
    }

    // Material 3 Date Range Picker Dialog
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startUtc = dateRangePickerState.selectedStartDateMillis
                        val endUtc = dateRangePickerState.selectedEndDateMillis
                        if (startUtc != null && endUtc != null) {
                            customStartDate = utcPickerMillisToLocalStart(startUtc)
                            customEndDate = utcPickerMillisToLocalEnd(endUtc)
                            selectedPeriod = "Custom"
                            showDateRangePicker = false
                        }
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null &&
                            dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                title = {
                    Text(
                        "Select Date Range",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }

    // Bill Detail Dialog
    selectedSaleForDetail?.let { sale ->
        var saleLines by remember(sale.id) { mutableStateOf<List<SaleLineEntity>>(emptyList()) }
        LaunchedEffect(sale.id) {
            saleLines = viewModel.getSaleLines(sale.id)
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
                            Text("VOIDED", color = statusColors.voidFg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else if (sale.remainingCreditPaise > 0) {
                            Text("DUE", color = statusColors.dueFg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Text("PAID", color = statusColors.paidFg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(sale.timestamp))
                    Text(dateFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
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
                            Text("Discount:", style = MaterialTheme.typography.bodySmall, color = statusColors.paidFg)
                            Text("-${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)}", style = MaterialTheme.typography.bodySmall, color = statusColors.paidFg)
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
                            Text("Remaining Due:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = statusColors.dueFg)
                            Text(CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = statusColors.dueFg)
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
                        color = statusColors.dueFg,
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
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
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

    // Bulk Void Confirmation Dialog
    if (showVoidConfirmDialog) {
        val selectedSales = remember(selectedBillIds, allSales) {
            allSales.filter { it.id in selectedBillIds }
        }
        val totalAmountPaise = remember(selectedSales) {
            selectedSales.sumOf { it.netTotalPaise }
        }
        var reasonText by remember { mutableStateOf("") }
        var conditionChoice by remember { mutableStateOf("SELLABLE") } // SELLABLE, DAMAGED, NONE
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showVoidConfirmDialog = false },
            title = {
                Text("Void ${selectedBillIds.size} Bills", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Total Amount: ${CostCalculator.formatPaiseToRupees(totalAmountPaise)}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "This will restore stock (if selected), reverse customer dues, and mark these bills as VOIDED in the audit trail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Stock restoration condition:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = conditionChoice == "SELLABLE",
                                onClick = { conditionChoice = "SELLABLE" }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sellable (Restores to active stock)", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = conditionChoice == "DAMAGED",
                                onClick = { conditionChoice = "DAMAGED" }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Damaged (Logs as damaged write-off)", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = conditionChoice == "NONE",
                                onClick = { conditionChoice = "NONE" }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("No stock change", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = {
                            reasonText = it
                            if (errorMsg != null) errorMsg = null
                        },
                        label = { Text("Void Reason (Required) *") },
                        isError = errorMsg != null,
                        supportingText = errorMsg?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = reasonText.trim()
                        if (trimmed.isBlank()) {
                            errorMsg = "Reason is required"
                            return@Button
                        }
                        showVoidConfirmDialog = false
                        viewModel.voidSales(
                            ids = selectedBillIds.toList(),
                            stockCondition = conditionChoice,
                            reason = trimmed
                        ) { result ->
                            bulkResult = result
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Confirm Void")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showVoidConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Void Result Dialog
    bulkResult?.let { res ->
        AlertDialog(
            onDismissRequest = { bulkResult = null },
            title = {
                Text("Bulk Void Completed", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Successfully voided: ${res.done} bills")
                    if (res.skipped.isNotEmpty()) {
                        Text(
                            "Skipped: ${res.skipped.size} bills",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(res.skipped) { (id, reason) ->
                                Text(
                                    "• Bill $id: $reason",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { bulkResult = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillHistoryRow(
    sale: SaleEntity,
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
    onRepay: () -> Unit,
    onOpenCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalAliStatusColors.current

    val stripeColor = when {
        sale.isVoided -> statusColors.voidFg
        sale.remainingCreditPaise > 0 -> statusColors.dueFg
        else -> statusColors.paidFg
    }

    val (pillBg, pillFg, pillText) = when {
        sale.isVoided -> Triple(statusColors.voidBg, statusColors.voidFg, "Voided")
        sale.remainingCreditPaise > 0 -> Triple(
            statusColors.dueBg,
            statusColors.dueFg,
            "Due ${CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)}"
        )
        else -> Triple(statusColors.paidBg, statusColors.paidFg, "Paid")
    }

    val timeFormatted = remember(sale.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(sale.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isInSelectionMode) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(AliDimens.radiusMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 4 dp Status Stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AliDimens.spacingMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() }
                    )
                    Spacer(modifier = Modifier.width(AliDimens.spacingSm))
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Top line: Receipt # • Time and Status Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                sale.receiptNumber,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                " • $timeFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Status Pill with icon + text
                        Surface(
                            shape = RoundedCornerShape(AliDimens.radiusPill),
                            color = pillBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                when {
                                    sale.isVoided -> {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = pillFg
                                        )
                                    }
                                    sale.remainingCreditPaise > 0 -> {
                                        Icon(
                                            Icons.Filled.Payment,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = pillFg
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = pillFg
                                        )
                                    }
                                }
                                Text(
                                    pillText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = pillFg
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Middle line: Customer name and Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            sale.customerName ?: "Walk-in Customer",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            CostCalculator.formatPaiseToRupees(sale.netTotalPaise),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom line: Payment Method icon/label and Received amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val methodLabel = when (sale.paymentMethod) {
                            "GPAY", "UPI" -> "UPI"
                            "CARD" -> "Card"
                            "BANK" -> "Bank"
                            "CREDIT" -> "Credit"
                            else -> "Cash"
                        }
                        Text(
                            "• $methodLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (sale.remainingCreditPaise > 0) {
                            Text(
                                "Paid: ${CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (sale.remainingCreditPaise > 0 && !sale.isVoided && sale.customerId != null) {
                        Spacer(modifier = Modifier.height(AliDimens.spacingSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = onRepay,
                                shape = RoundedCornerShape(AliDimens.radiusSm),
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
    }
}
