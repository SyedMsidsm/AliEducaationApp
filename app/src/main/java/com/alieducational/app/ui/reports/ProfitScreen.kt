package com.alieducational.app.ui.reports

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ProfitTrendBucket(
    val label: String,
    val startTime: Long,
    val endTime: Long,
    val salesPaise: Long,
    val cogsPaise: Long,
    val grossProfitPaise: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val allSaleLines by viewModel.allSaleLines.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val deviceRole by viewModel.deviceRole.collectAsState()

    val isOwner = deviceRole == "OWNER"
    var activeTab by remember { mutableStateOf("PRODUCTS") } // PRODUCTS, SERVICES, COMBINED
    var selectedPeriod by remember { mutableStateOf("All Time") } // Today, This Week, This Month, All Time, Custom

    // Custom date bounds
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // Calculate time boundaries
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val (startTime, endTime) = when (selectedPeriod) {
        "Today" -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val s = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            Pair(s, calendar.timeInMillis)
        }
        "This Week" -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            Pair(calendar.timeInMillis, now)
        }
        "This Month" -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            Pair(calendar.timeInMillis, now)
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
            else -> "All recorded history"
        }
    }

    // ── FINANCIAL AGGREGATIONS ──────────────────────────────────────
    // 1. Sales & COGS in period (non-voided)
    val periodSales = remember(allSales, startTime, endTime) {
        allSales.filter { !it.isVoided && it.timestamp in startTime..endTime }
    }
    val periodSaleIds = remember(periodSales) { periodSales.map { it.id }.toSet() }
    val periodSaleLines = remember(allSaleLines, periodSaleIds) {
        allSaleLines.filter { it.saleId in periodSaleIds }
    }

    // Products (retail) lines
    val periodProductLines = remember(periodSaleLines) {
        periodSaleLines.filter { it.lineType != "SERVICE" }
    }
    val productRevenuePaise = remember(periodProductLines) {
        periodProductLines.sumOf { it.lineNetPaise }
    }
    val productCogsPaise = remember(periodProductLines) {
        periodProductLines.sumOf { line ->
            line.quantityBase.toLong() * (line.unitCostSnapshotPaise ?: 0L)
        }
    }
    val productGrossProfitPaise = productRevenuePaise - productCogsPaise
    val productGrossMarginPct = if (productRevenuePaise > 0) {
        (productGrossProfitPaise.toDouble() / productRevenuePaise.toDouble()) * 100.0
    } else 0.0

    // Services lines
    val periodServiceLines = remember(periodSaleLines) {
        periodSaleLines.filter { it.lineType == "SERVICE" }
    }
    val serviceRevenuePaise = remember(periodServiceLines) {
        periodServiceLines.sumOf { it.lineNetPaise }
    }

    // 2. Canonical Expenses & Refunds in period
    // Non-voided, non-savings-transfer expenses
    val periodActiveExpenses = remember(allExpenses, startTime, endTime) {
        allExpenses.filter { !it.isVoided && !it.isSavingsTransfer && it.timestamp in startTime..endTime }
    }

    // Product expenses & refunds
    val productExpenses = remember(periodActiveExpenses) {
        periodActiveExpenses.filter { !it.isIncome && it.classification == "PRODUCTS" }
    }
    val grossProductExpensesPaise = remember(productExpenses) { productExpenses.sumOf { it.amountPaise } }
    val productRefunds = remember(periodActiveExpenses) {
        periodActiveExpenses.filter { it.isIncome && it.isExpenseRefund && it.classification == "PRODUCTS" }
    }
    val productRefundsPaise = remember(productRefunds) { productRefunds.sumOf { it.amountPaise } }
    val netProductExpensesPaise = grossProductExpensesPaise - productRefundsPaise
    val productResultAfterExpensesPaise = productGrossProfitPaise - netProductExpensesPaise

    // Service expenses & refunds
    val serviceExpenses = remember(periodActiveExpenses) {
        periodActiveExpenses.filter { !it.isIncome && (it.classification == "SERVICES" || it.category == "SERVICE_EXPENSE" || it.serviceTarget != null) }
    }
    val grossServiceExpensesPaise = remember(serviceExpenses) { serviceExpenses.sumOf { it.amountPaise } }
    val serviceRefunds = remember(periodActiveExpenses) {
        periodActiveExpenses.filter { it.isIncome && it.isExpenseRefund && (it.classification == "SERVICES" || it.category == "SERVICE_EXPENSE" || it.serviceTarget != null) }
    }
    val serviceRefundsPaise = remember(serviceRefunds) { serviceRefunds.sumOf { it.amountPaise } }
    val netServiceExpensesPaise = grossServiceExpensesPaise - serviceRefundsPaise
    val serviceResultAfterExpensesPaise = serviceRevenuePaise - netServiceExpensesPaise

    // Shared expenses & refunds
    val sharedExpenses = remember(periodActiveExpenses) {
        periodActiveExpenses.filter {
            !it.isIncome && it.classification != "PRODUCTS" && it.classification != "SERVICES" &&
                    it.category != "SERVICE_EXPENSE" && it.serviceTarget == null
        }
    }
    val grossSharedExpensesPaise = remember(sharedExpenses) { sharedExpenses.sumOf { it.amountPaise } }
    val sharedRefunds = remember(periodActiveExpenses) {
        periodActiveExpenses.filter {
            it.isIncome && it.isExpenseRefund && it.classification != "PRODUCTS" && it.classification != "SERVICES" &&
                    it.category != "SERVICE_EXPENSE" && it.serviceTarget == null
        }
    }
    val sharedRefundsPaise = remember(sharedRefunds) { sharedRefunds.sumOf { it.amountPaise } }
    val netSharedExpensesPaise = grossSharedExpensesPaise - sharedRefundsPaise

    // Combined Net Operating Result:
    // Product Gross Profit + Net Service Revenue - (Net Product Expenses + Net Service Expenses + Net Shared Expenses)
    val totalNetOperatingExpensesPaise = netProductExpensesPaise + netServiceExpensesPaise + netSharedExpensesPaise
    val combinedRecordedOperatingResultPaise = productGrossProfitPaise + serviceRevenuePaise - totalNetOperatingExpensesPaise

    // Potential inventory gross profit from current unsold positive stock (A06)
    val positiveProducts = remember(allProducts) {
        allProducts.filter { it.currentSellableStock > 0 }
    }
    val shortageProducts = remember(allProducts) {
        allProducts.filter { it.currentSellableStock < 0 }
    }
    val expectedGrossProfitPaise = remember(positiveProducts) {
        positiveProducts.sumOf { p ->
            val potentialRevenue = p.currentSellableStock.toLong() * p.sellingPricePiecePaise
            val costValue = p.currentSellableStock.toLong() * p.movingAvgCostPaise
            (potentialRevenue - costValue).coerceAtLeast(0L)
        }
    }

    // ── BUCKETED TREND DATA (Section 6 Graph A) ──────────────────────
    val trendBuckets = remember(periodSales, periodSaleLines, startTime, endTime, selectedPeriod) {
        generateTrendBuckets(periodSales, periodSaleLines, startTime, endTime, selectedPeriod)
    }

    var selectedBucketDetail by remember { mutableStateOf<ProfitTrendBucket?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sales & Profit", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Period Filter Chips Row
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val periods = listOf("Today", "This Week", "This Month", "All Time", "Custom")
                        items(periods) { period ->
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
                                label = { Text(period, fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                // Tab Switcher (Products, Services, Combined)
                if (isOwner) {
                    item {
                        TabRow(
                            selectedTabIndex = when (activeTab) {
                                "PRODUCTS" -> 0
                                "SERVICES" -> 1
                                else -> 2
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = activeTab == "PRODUCTS",
                                onClick = { activeTab = "PRODUCTS" },
                                text = { Text("Products", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                            Tab(
                                selected = activeTab == "SERVICES",
                                onClick = { activeTab = "SERVICES" },
                                text = { Text("Services", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                            Tab(
                                selected = activeTab == "COMBINED",
                                onClick = { activeTab = "COMBINED" },
                                text = { Text("Combined", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                        }
                    }
                }

                // ── TAB CONTENT: PRODUCTS ──────────────────────────────────────────
                if (activeTab == "PRODUCTS" || !isOwner) {
                    // 1. Prominent Gross Profit Card (with Gross Margin %)
                    item {
                        val isGrossProfitPositive = productGrossProfitPaise >= 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isGrossProfitPositive) Color(0xFF1B3828) else Color(0xFF3E1B1B)
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
                                            imageVector = if (isGrossProfitPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isGrossProfitPositive) Color(0xFF75B890) else Color(0xFFE57373),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Product Gross Profit",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = if (productRevenuePaise > 0) {
                                            "Margin: ${String.format(Locale.US, "%.2f", productGrossMarginPct)}%"
                                        } else "Margin: Not applicable",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isGrossProfitPositive) Color(0xFF75B890) else Color(0xFFE57373)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = CostCalculator.formatPaiseToRupees(productGrossProfitPaise),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isGrossProfitPositive) Color(0xFF75B890) else Color(0xFFE57373)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Net Sales ${CostCalculator.formatPaiseToRupees(productRevenuePaise)} minus COGS ${CostCalculator.formatPaiseToRupees(productCogsPaise)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. Sales and COGS 2-Column Cards
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

                    // 3. Product Expenses and Result after Product Expenses
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
                                    Column {
                                        Text(
                                            "Recorded Product Expenses",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (productRefundsPaise > 0) {
                                            Text(
                                                "Gross ${CostCalculator.formatPaiseToRupees(grossProductExpensesPaise)} - Refunds ${CostCalculator.formatPaiseToRupees(productRefundsPaise)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        CostCalculator.formatPaiseToRupees(netProductExpensesPaise),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFE57373)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Result After Product Expenses",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        CostCalculator.formatPaiseToRupees(productResultAfterExpensesPaise),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (productResultAfterExpensesPaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                                    )
                                }
                                Text(
                                    "Excludes Shared shop expenses (see Combined Overview for whole-shop result).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 4. Sales & Gross Profit Trend Graph (Section 6 A)
                    item {
                        SalesProfitTrendChart(
                            buckets = trendBuckets,
                            onBucketClick = { bucket -> selectedBucketDetail = bucket }
                        )
                    }

                    // 5. Expense Breakdown Chart (Section 6 B)
                    item {
                        ExpenseBreakdownChart(
                            expenses = productExpenses,
                            refunds = productRefunds,
                            title = "Product Expenses Breakdown"
                        )
                    }

                    // 6. Expected Gross Profit from Current Stock Card
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
                                    Text(
                                        "Expected Profit from Current Stock",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        CostCalculator.formatPaiseToRupees(expectedGrossProfitPaise),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Projected gross profit on unsold warehouse inventory if sold at current retail price.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (shortageProducts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "⚠️ Shortage warning: ${shortageProducts.size} product(s) have negative stock.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ── TAB CONTENT: SERVICES ──────────────────────────────────────────
                if (activeTab == "SERVICES" && isOwner) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (serviceResultAfterExpensesPaise >= 0) Color(0xFF1B3828) else Color(0xFF3E1B1B)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    "Service Revenue Minus Service Expenses",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    CostCalculator.formatPaiseToRupees(serviceResultAfterExpensesPaise),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (serviceResultAfterExpensesPaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Service Revenue ${CostCalculator.formatPaiseToRupees(serviceRevenuePaise)} minus Service Costs ${CostCalculator.formatPaiseToRupees(netServiceExpensesPaise)}",
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
                                    Text("Service Revenue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(serviceRevenuePaise),
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
                                    Text("Service Costs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(netServiceExpensesPaise),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ExpenseBreakdownChart(
                            expenses = serviceExpenses,
                            refunds = serviceRefunds,
                            title = "Services Expenses Breakdown"
                        )
                    }
                }

                // ── TAB CONTENT: COMBINED ──────────────────────────────────────────
                if (activeTab == "COMBINED" && isOwner) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (combinedRecordedOperatingResultPaise >= 0) Color(0xFF1B3828) else Color(0xFF3E1B1B)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    "Combined Recorded Operating Result",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    CostCalculator.formatPaiseToRupees(combinedRecordedOperatingResultPaise),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (combinedRecordedOperatingResultPaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Product GP + Service Rev - All Net Operating Expenses (Products + Services + Shared)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Operating Summary Breakdown", fontWeight = FontWeight.Bold)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("1. Product Gross Profit:", style = MaterialTheme.typography.bodySmall)
                                    Text(CostCalculator.formatPaiseToRupees(productGrossProfitPaise), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF75B890))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("2. Service Revenue:", style = MaterialTheme.typography.bodySmall)
                                    Text(CostCalculator.formatPaiseToRupees(serviceRevenuePaise), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF75B890))
                                }

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("3. Product Expenses (Net):", style = MaterialTheme.typography.bodySmall)
                                    Text("-${CostCalculator.formatPaiseToRupees(netProductExpensesPaise)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE57373))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("4. Service Expenses (Net):", style = MaterialTheme.typography.bodySmall)
                                    Text("-${CostCalculator.formatPaiseToRupees(netServiceExpensesPaise)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE57373))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("5. Shared Expenses (Net):", style = MaterialTheme.typography.bodySmall)
                                    Text("-${CostCalculator.formatPaiseToRupees(netSharedExpensesPaise)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE57373))
                                }

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Net Operating Result:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(combinedRecordedOperatingResultPaise),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (combinedRecordedOperatingResultPaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ExpenseBreakdownChart(
                            expenses = periodActiveExpenses.filter { !it.isIncome },
                            refunds = periodActiveExpenses.filter { it.isIncome && it.isExpenseRefund },
                            title = "All Shop Expenses Breakdown"
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Bucket Detail Dialog
    selectedBucketDetail?.let { bucket ->
        AlertDialog(
            onDismissRequest = { selectedBucketDetail = null },
            title = { Text(bucket.label, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sales:")
                        Text(CostCalculator.formatPaiseToRupees(bucket.salesPaise), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("COGS:")
                        Text(CostCalculator.formatPaiseToRupees(bucket.cogsPaise), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Profit:")
                        Text(
                            CostCalculator.formatPaiseToRupees(bucket.grossProfitPaise),
                            fontWeight = FontWeight.Bold,
                            color = if (bucket.grossProfitPaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedBucketDetail = null }) { Text("OK") }
            }
        )
    }
}

// ── TREND BUCKET GENERATOR ───────────────────────────────────────────
fun generateTrendBuckets(
    sales: List<com.alieducational.app.data.local.entity.SaleEntity>,
    lines: List<com.alieducational.app.data.local.entity.SaleLineEntity>,
    startTime: Long,
    endTime: Long,
    period: String
): List<ProfitTrendBucket> {
    if (sales.isEmpty()) return emptyList()

    val saleLinesMap = lines.groupBy { it.saleId }

    if (period == "Today") {
        val totalSales = sales.sumOf { it.netTotalPaise }
        val productLines = lines.filter { it.lineType != "SERVICE" }
        val totalCogs = productLines.sumOf { it.quantityBase.toLong() * (it.unitCostSnapshotPaise ?: 0L) }
        return listOf(
            ProfitTrendBucket(
                label = "Today",
                startTime = startTime,
                endTime = endTime,
                salesPaise = totalSales,
                cogsPaise = totalCogs,
                grossProfitPaise = totalSales - totalCogs
            )
        )
    }

    // Group sales into days
    val df = SimpleDateFormat("dd MMM", Locale.US)
    val dayBuckets = mutableMapOf<String, MutableList<com.alieducational.app.data.local.entity.SaleEntity>>()

    sales.sortedBy { it.timestamp }.forEach { s ->
        val key = df.format(Date(s.timestamp))
        dayBuckets.getOrPut(key) { mutableListOf() }.add(s)
    }

    return dayBuckets.map { (dateStr, daySales) ->
        val daySaleIds = daySales.map { it.id }.toSet()
        val dayLines = lines.filter { it.saleId in daySaleIds && it.lineType != "SERVICE" }
        val sPaise = daySales.sumOf { it.netTotalPaise }
        val cPaise = dayLines.sumOf { it.quantityBase.toLong() * (it.unitCostSnapshotPaise ?: 0L) }
        ProfitTrendBucket(
            label = dateStr,
            startTime = daySales.minOf { it.timestamp },
            endTime = daySales.maxOf { it.timestamp },
            salesPaise = sPaise,
            cogsPaise = cPaise,
            grossProfitPaise = sPaise - cPaise
        )
    }
}

// ── TREND CHART COMPOSABLE ──────────────────────────────────────────
@Composable
fun SalesProfitTrendChart(
    buckets: List<ProfitTrendBucket>,
    onBucketClick: (ProfitTrendBucket) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sales & Gross Profit Trend", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFE5A93C), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF75B890), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gross Profit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (buckets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions in this period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(buckets) {
                                detectTapGestures { offset ->
                                    val bucketWidth = size.width / buckets.size
                                    val index = (offset.x / bucketWidth).toInt().coerceIn(0, buckets.size - 1)
                                    onBucketClick(buckets[index])
                                }
                            }
                    ) {
                        val maxSales = buckets.maxOfOrNull { it.salesPaise }?.coerceAtLeast(1L) ?: 1L
                        val maxProfit = buckets.maxOfOrNull { it.grossProfitPaise }?.coerceAtLeast(1L) ?: 1L
                        val minProfit = buckets.minOfOrNull { it.grossProfitPaise }?.coerceAtMost(0L) ?: 0L

                        val overallMax = maxOf(maxSales, maxProfit)
                        val overallMin = minProfit

                        val totalSpan = (overallMax - overallMin).toFloat().coerceAtLeast(1f)
                        val zeroY = size.height * (overallMax.toFloat() / totalSpan)

                        // Draw Zero Baseline
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.5f),
                            start = Offset(0f, zeroY),
                            end = Offset(size.width, zeroY),
                            strokeWidth = 1.dp.toPx()
                        )

                        val bucketWidth = size.width / buckets.size
                        val barWidth = (bucketWidth * 0.35f).coerceAtLeast(4f)

                        buckets.forEachIndexed { index, b ->
                            val centerX = index * bucketWidth + bucketWidth / 2f

                            // Sales Bar
                            val salesHeight = (b.salesPaise.toFloat() / totalSpan) * size.height
                            drawRect(
                                color = Color(0xFFE5A93C),
                                topLeft = Offset(centerX - barWidth - 2f, zeroY - salesHeight),
                                size = Size(barWidth, salesHeight)
                            )

                            // Profit Bar (positive upwards, negative downwards)
                            val profitHeight = (kotlin.math.abs(b.grossProfitPaise).toFloat() / totalSpan) * size.height
                            val profitTop = if (b.grossProfitPaise >= 0) zeroY - profitHeight else zeroY
                            val profitColor = if (b.grossProfitPaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)

                            drawRect(
                                color = profitColor,
                                topLeft = Offset(centerX + 2f, profitTop),
                                size = Size(barWidth, profitHeight)
                            )
                        }
                    }
                }

                // Bucket Labels Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val sampleLabels = if (buckets.size <= 5) buckets else listOf(buckets.first(), buckets[buckets.size / 2], buckets.last())
                    sampleLabels.forEach { b ->
                        Text(b.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ── EXPENSE BREAKDOWN CHART (Section 6 B) ────────────────────────────
@Composable
fun ExpenseBreakdownChart(
    expenses: List<ShopExpenseEntity>,
    refunds: List<ShopExpenseEntity>,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            if (expenses.isEmpty()) {
                Text(
                    "No recorded expenses in this scope/period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Group by category, calculating net expense (expenses minus refunds)
                val catExpenses = expenses.groupBy { it.category }
                val catRefunds = refunds.groupBy { it.category }
                val allCats = (catExpenses.keys + catRefunds.keys).distinct()

                val categoryTotals = allCats.map { cat ->
                    val gross = catExpenses[cat]?.sumOf { it.amountPaise } ?: 0L
                    val ref = catRefunds[cat]?.sumOf { it.amountPaise } ?: 0L
                    cat to (gross - ref)
                }.sortedByDescending { it.second }

                val maxAmount = categoryTotals.maxOfOrNull { kotlin.math.abs(it.second) }?.coerceAtLeast(1L) ?: 1L

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryTotals.forEach { (cat, netPaise) ->
                        val fraction = (kotlin.math.abs(netPaise).toFloat() / maxAmount.toFloat()).coerceIn(0.05f, 1f)
                        val isNegative = netPaise < 0

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    CostCalculator.formatPaiseToRupees(netPaise),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNegative) Color(0xFF75B890) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(6.dp)
                                        .background(
                                            if (isNegative) Color(0xFF75B890) else Color(0xFFE57373),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
