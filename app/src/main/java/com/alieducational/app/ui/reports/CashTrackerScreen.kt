package com.alieducational.app.ui.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AccessTime
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.TabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.service.CashTrackerImportPreview
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashTrackerScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allExpenses by viewModel.allExpenses.collectAsState()

    val context = LocalContext.current
    var isImporting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var importPreview by remember { mutableStateOf<CashTrackerImportPreview?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            val fileName = queryFileName(context, uri)
            viewModel.parseCashTrackerExcel(uri, fileName) { preview ->
                isImporting = false
                importPreview = preview
            }
        }
    }

    // 0: All, 1: Daily, 2: Weekly, 3: Monthly, 4: Yearly
    var selectedTab by remember { mutableIntStateOf(3) } // Default to Monthly (like in Picture 4)
    var anchorTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var isIncomeMode by remember { mutableStateOf(true) }
    var selectedItemForVoid by remember { mutableStateOf<ShopExpenseEntity?>(null) }

    // Date calculations based on anchor timestamp and selected tab
    val (startDate, endDate, rangeText) = remember(selectedTab, anchorTimestamp) {
        val cal = Calendar.getInstance().apply { timeInMillis = anchorTimestamp }
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)

        when (selectedTab) {
            0 -> { // All
                Triple(0L, Long.MAX_VALUE, "All Transactions")
            }
            1 -> { // Daily
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Triple(start, end, dateFormat.format(Date(start)))
            }
            2 -> { // Weekly (Monday to Sunday)
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Triple(start, end, "${dateFormat.format(Date(start))} -> ${dateFormat.format(Date(end))}")
            }
            3 -> { // Monthly (1st to last day)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Triple(start, end, "${dateFormat.format(Date(start))} -> ${dateFormat.format(Date(end))}")
            }
            else -> { // Yearly
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Triple(start, end, "${dateFormat.format(Date(start))} -> ${dateFormat.format(Date(end))}")
            }
        }
    }

    // Filter transactions in active range
    val filteredItems = remember(allExpenses, startDate, endDate) {
        allExpenses.filter { it.timestamp in startDate..endDate }
    }

    // Totals
    val totalIncomePaise = remember(filteredItems) {
        filteredItems.filter { it.isIncome }.sumOf { it.amountPaise }
    }
    val totalExpensePaise = remember(filteredItems) {
        filteredItems.filter { !it.isIncome }.sumOf { it.amountPaise }
    }
    val netBalancePaise = totalIncomePaise - totalExpensePaise

    // Group items by day (descending)
    val groupedByDay = remember(filteredItems) {
        val dayFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
        filteredItems.groupBy { dayFormat.format(Date(it.timestamp)) }
    }

    val tabTitles = listOf("All", "Daily", "Weekly", "Monthly", "Yearly")

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ali Educational",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Import Excel Button
                IconButton(
                    onClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "*/*"
                            )
                        )
                    },
                    enabled = !isImporting && !isExporting
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = "Import Excel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Share / Export via WhatsApp / Others Button
                IconButton(
                    onClick = {
                        val itemsToExport = if (filteredItems.isNotEmpty()) filteredItems else allExpenses
                        if (itemsToExport.isEmpty()) {
                            Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                        } else {
                            isExporting = true
                            viewModel.exportCashTrackerExcel(itemsToExport, rangeText) { shareIntent ->
                                isExporting = false
                                try {
                                    val chooser = Intent.createChooser(shareIntent, "Share Cash Book").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isImporting && !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share / Export (WhatsApp)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Period Tabs (All, Daily, Weekly, Monthly, Yearly)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            // Date Navigator Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = anchorTimestamp }
                        when (selectedTab) {
                            1 -> cal.add(Calendar.DAY_OF_YEAR, -1)
                            2 -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                            3 -> cal.add(Calendar.MONTH, -1)
                            4 -> cal.add(Calendar.YEAR, -1)
                        }
                        anchorTimestamp = cal.timeInMillis
                    },
                    enabled = selectedTab != 0
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous",
                        tint = if (selectedTab != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = anchorTimestamp }
                        when (selectedTab) {
                            1 -> cal.add(Calendar.DAY_OF_YEAR, 1)
                            2 -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                            3 -> cal.add(Calendar.MONTH, 1)
                            4 -> cal.add(Calendar.YEAR, 1)
                        }
                        anchorTimestamp = cal.timeInMillis
                    },
                    enabled = selectedTab != 0
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next",
                        tint = if (selectedTab != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // Table Header: Date | Income | Expense
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "Income",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF75B890),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Expense",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFE57373),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Transaction List Grouped by Date
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No entries in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + Income or - Expense below to add",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

                    groupedByDay.forEach { (dayString, itemsOnDay) ->
                        // Date Header
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = dayString,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Day's Entries
                        items(itemsOnDay) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedItemForVoid = entry }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Description & Time
                                Column(modifier = Modifier.weight(1.5f)) {
                                    val noteSuffix = if (!entry.notes.isNullOrBlank()) " - ${entry.notes}" else ""
                                    Text(
                                        text = "${entry.category}$noteSuffix",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = timeFormat.format(Date(entry.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Income Column
                                Text(
                                    text = if (entry.isIncome) CostCalculator.formatPaiseToRupees(entry.amountPaise) else "",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF75B890),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )

                                // Expense Column
                                Text(
                                    text = if (!entry.isIncome) CostCalculator.formatPaiseToRupees(entry.amountPaise) else "",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE57373),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            // Two Action Buttons: + Income & - Expense
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        isIncomeMode = true
                        showAddDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Income", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        isIncomeMode = false
                        showAddDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary Footer: Total Income | Total Expense | Balance
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF75B890)
                        )
                        Text(
                            text = CostCalculator.formatPaiseToRupees(totalIncomePaise),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF75B890)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Expense",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE57373)
                        )
                        Text(
                            text = CostCalculator.formatPaiseToRupees(totalExpensePaise),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE57373)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (netBalancePaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                        )
                        Text(
                            text = CostCalculator.formatPaiseToRupees(netBalancePaise),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (netBalancePaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                        )
                    }
                }
            }
        }
    }

    // Add Income / Expense Dialog with Editable Date & Time (F3)
    if (showAddDialog) {
        var amountStr by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var paymentMode by remember { mutableStateOf("CASH") }
        var note by remember { mutableStateOf("") }
        var entryTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
        val dialogContext = LocalContext.current

        val cal = Calendar.getInstance().apply { timeInMillis = entryTimestamp }
        val entryDateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(entryTimestamp))
        val entryTimeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(entryTimestamp))
        val isFuture = entryTimestamp > System.currentTimeMillis() + 60_000L

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (isIncomeMode) "Add Income (Cash In)" else "Add Expense (Cash Out)",
                    fontWeight = FontWeight.Bold,
                    color = if (isIncomeMode) Color(0xFF75B890) else Color(0xFFE57373)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Date & Time pickers
                    Text(
                        text = "Transaction Date & Time",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

                    if (isFuture) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Notice: This entry has a future date/time. It will be recorded with this timestamp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (₹) *") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Name *") },
                        placeholder = { Text(if (isIncomeMode) "e.g. Sales, Customer Payment" else "e.g. Tea, Rent, Pigmy, Kuree") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment Mode Chips
                    Text(
                        text = "Payment Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH" to "Cash", "GPAY" to "GPAY / UPI", "BANK" to "Bank").forEach { (key, label) ->
                            FilterChip(
                                selected = paymentMode == key,
                                onClick = { paymentMode = key },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Extra Note (Optional)") },
                        placeholder = { Text("Optional details") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L
                        if (amountPaise <= 0 || description.isBlank()) return@Button

                        val paymentNote = buildString {
                            append(paymentMode)
                            if (note.isNotBlank()) append(" - ${note.trim()}")
                        }

                        viewModel.recordShopExpense(
                            category = description.trim(),
                            amountPaise = amountPaise,
                            isIncome = isIncomeMode,
                            notes = paymentNote,
                            timestamp = entryTimestamp,
                            onDone = { showAddDialog = false }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncomeMode) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Void / Delete Entry Dialog
    selectedItemForVoid?.let { entry ->
        var voidReason by remember { mutableStateOf("Manual cancellation") }

        AlertDialog(
            onDismissRequest = { selectedItemForVoid = null },
            title = { Text("Cancel / Void Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to void this entry?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${entry.category}: ${CostCalculator.formatPaiseToRupees(entry.amountPaise)} (${if (entry.isIncome) "Income" else "Expense"})",
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isIncome) Color(0xFF75B890) else Color(0xFFE57373)
                    )
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason for voiding") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.voidExpense(entry.id, voidReason.trim()) {
                            selectedItemForVoid = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Void Entry")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedItemForVoid = null }) {
                    Text("Keep Entry")
                }
            }
        )
    }

    // Excel Import Confirmation / Error Dialog
    importPreview?.let { preview ->
        if (preview.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { importPreview = null },
                title = {
                    Text(
                        text = "Excel Import Notice",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = { Text(preview.errorMessage) },
                confirmButton = {
                    Button(onClick = { importPreview = null }) {
                        Text("OK")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { importPreview = null },
                title = {
                    Text(
                        text = "Import Cash Entries",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("File: ${preview.fileName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Total entries found: ${preview.items.size}", fontWeight = FontWeight.Bold)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Income (${preview.incomeCount}):", color = Color(0xFF75B890), fontWeight = FontWeight.SemiBold)
                            Text(CostCalculator.formatPaiseToRupees(preview.totalIncomePaise), color = Color(0xFF75B890), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expense (${preview.expenseCount}):", color = Color(0xFFE57373), fontWeight = FontWeight.SemiBold)
                            Text(CostCalculator.formatPaiseToRupees(preview.totalExpensePaise), color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                        }
                        if (preview.skippedRows > 0) {
                            Text(
                                "Skipped ${preview.skippedRows} empty / zero-value rows",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "These entries will be recorded in your Cash In & Out register.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.confirmCashTrackerImport(preview.items) {
                                val count = preview.items.size
                                importPreview = null
                                Toast.makeText(context, "Imported $count entries successfully", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Import (${preview.items.size})", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { importPreview = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String {
    var name = "cash_book.xlsx"
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        name = it.getString(idx)
                    }
                }
            }
        } catch (ignored: Exception) {}
    } else if (uri.path != null) {
        name = java.io.File(uri.path!!).name
    }
    return name
}
