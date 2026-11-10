package com.alieducational.app.ui.reports

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.alieducational.app.ui.theme.AliDimens
import com.alieducational.app.ui.theme.LocalAliStatusColors



import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Checkbox
import com.alieducational.app.ui.BulkResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.SuggestionChip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.ui.res.painterResource
import com.alieducational.app.R
import com.alieducational.app.data.local.entity.CashCategoryEntity
import com.alieducational.app.data.local.entity.PigmySchemeEntity
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
import com.alieducational.app.util.AttachmentManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CashTrackerScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allExpenses by viewModel.allExpenses.collectAsState()
    val selectedCashEntryIds by viewModel.selectedCashEntryIds.collectAsState()
    val deviceRole by viewModel.deviceRole.collectAsState()
    val isOwner = deviceRole == "OWNER"

    var showBulkVoidConfirmDialog by remember { mutableStateOf(false) }
    var bulkVoidResult by remember { mutableStateOf<BulkResult?>(null) }

    BackHandler(enabled = selectedCashEntryIds.isNotEmpty()) {
        viewModel.clearCashEntrySelection()
    }

    val activeCategories by viewModel.activeCashCategories.collectAsState()
    val allPigmySchemes by viewModel.allPigmySchemes.collectAsState()
    val activePigmySchemes = remember(allPigmySchemes) { allPigmySchemes.filter { !it.isArchived } }

    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CashCategoryEntity?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isImporting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var importPreview by remember { mutableStateOf<CashTrackerImportPreview?>(null) }
    var showExportPreviewDialog by remember { mutableStateOf(false) }
    var pendingExportItems by remember { mutableStateOf<List<ShopExpenseEntity>>(emptyList()) }
    var pendingExportTitle by remember { mutableStateOf("All Transactions") }
    var pendingExportFromDate by remember { mutableStateOf<Long?>(null) }
    var pendingExportToDate by remember { mutableStateOf<Long?>(null) }
    var pendingExportSummary by remember { mutableStateOf("All Categories, Active Transactions") }

    val createCashDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            viewModel.saveCashTrackerToUri(
                items = pendingExportItems,
                uri = uri,
                periodTitle = pendingExportTitle,
                fromDate = pendingExportFromDate,
                toDate = pendingExportToDate,
                activeFiltersSummary = pendingExportSummary,
                onSuccess = {
                    isExporting = false
                    android.widget.Toast.makeText(context, "Cash Book saved successfully to device", android.widget.Toast.LENGTH_SHORT).show()
                },
                onError = { msg ->
                    isExporting = false
                    android.widget.Toast.makeText(context, "Failed to save: $msg", android.widget.Toast.LENGTH_LONG).show()
                }
            )
        } else {
            isExporting = false
        }
    }

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
    var selectedTab by remember { mutableIntStateOf(3) } // Default to Monthly
    var anchorTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) } // null = All Categories

    var showAddDialog by remember { mutableStateOf(false) }
    var isIncomeMode by remember { mutableStateOf(true) }

    val widgetAction by viewModel.widgetActionTrigger.collectAsState()
    androidx.compose.runtime.LaunchedEffect(widgetAction) {
        when (widgetAction) {
            com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_CASH_IN -> {
                isIncomeMode = true
                showAddDialog = true
                viewModel.consumeWidgetAction()
            }
            com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_CASH_OUT -> {
                isIncomeMode = false
                showAddDialog = true
                viewModel.consumeWidgetAction()
            }
        }
    }
    var itemToEdit by remember { mutableStateOf<ShopExpenseEntity?>(null) }
    var selectedItemForVoid by remember { mutableStateOf<ShopExpenseEntity?>(null) }
    var viewingPhotoFile by remember { mutableStateOf<File?>(null) }

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

    // Filter transactions in active range and category filter
    val filteredItems = remember(allExpenses, startDate, endDate, selectedCategoryFilter) {
        allExpenses.filter { item ->
            val inDate = item.timestamp in startDate..endDate
            val matchesCategory = if (selectedCategoryFilter == null) {
                true
            } else {
                item.categoryId == selectedCategoryFilter || item.category.equals(selectedCategoryFilter, ignoreCase = true)
            }
            inDate && matchesCategory
        }
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
            if (selectedCashEntryIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.clearCashEntrySelection() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel selection",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${selectedCashEntryIds.size} selected",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        viewModel.selectAllCashEntries(filteredItems.map { it.id })
                    }) {
                        Text("Select All", fontWeight = FontWeight.SemiBold)
                    }
                    if (isOwner) {
                        IconButton(onClick = {
                            val activity = context as? androidx.fragment.app.FragmentActivity
                            if (activity != null) {
                                viewModel.stepUp.require(
                                    activity = activity,
                                    title = "Authenticate to void cash entries",
                                    onOk = { showBulkVoidConfirmDialog = true },
                                    onFail = { /* no-op */ }
                                )
                            } else {
                                showBulkVoidConfirmDialog = true
                            }
                        }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Void Selected Entries",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
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
                    text = "Cash In & Out",
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

                // Share / Export Excel Button
                IconButton(
                    onClick = {
                        val itemsToExport = if (filteredItems.isNotEmpty()) filteredItems else allExpenses
                        if (itemsToExport.isEmpty()) {
                            Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                        } else {
                            showExportPreviewDialog = true
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
                            contentDescription = "Share / Export Excel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                    enabled = selectedTab != 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                }

                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
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
                    enabled = selectedTab != 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                }
            }

            // Category Filter Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All Categories (${allExpenses.size})", fontSize = 11.sp) }
                )

                activeCategories.filter { it.name != "Pigmy" }.forEach { cat ->
                    val count = allExpenses.count { it.categoryId == cat.id || it.category.equals(cat.name, ignoreCase = true) }
                    FilterChip(
                        selected = selectedCategoryFilter == cat.id || selectedCategoryFilter == cat.name,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat.id || selectedCategoryFilter == cat.name) null else cat.id
                        },
                        label = { Text("${cat.name} ($count)", fontSize = 11.sp) }
                    )
                }

                activePigmySchemes.forEach { scheme ->
                    val count = allExpenses.count { it.categoryId == scheme.id || it.category.equals(scheme.name, ignoreCase = true) }
                    FilterChip(
                        selected = selectedCategoryFilter == scheme.id || selectedCategoryFilter == scheme.name,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == scheme.id || selectedCategoryFilter == scheme.name) null else scheme.id
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_pigmy_coin),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        label = { Text("${scheme.name} ($count)", fontSize = 11.sp) }
                    )
                }

                IconButton(
                    onClick = { showManageCategoriesDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Manage Categories",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Table Header: Description / Details & Amount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content List
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

                        // Day's Entries (Section 2 Layout)
                        // Day's Entries (Section 2 Layout)
                        items(itemsOnDay, key = { it.id }) { entry ->
                            var menuExpanded by remember { mutableStateOf(false) }
                            val isSelected = selectedCashEntryIds.contains(entry.id)
                            val isInSelectionMode = selectedCashEntryIds.isNotEmpty()

                            val displayDescription = if (!entry.notes.isNullOrBlank()) entry.notes else entry.category
                            val displayPayment = when (entry.paymentMethod.uppercase()) {
                                "CASH" -> "Cash"
                                "GPAY", "UPI" -> "UPI"
                                "BANK" -> "Bank"
                                else -> entry.paymentMethod
                            }
                            val metaText = "${entry.category} · $displayPayment · ${timeFormat.format(Date(entry.timestamp))}"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color.Transparent
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (isInSelectionMode) {
                                                viewModel.toggleCashEntrySelection(entry.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (isOwner) {
                                                viewModel.toggleCashEntrySelection(entry.id)
                                            }
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isInSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleCashEntrySelection(entry.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayDescription,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = metaText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (entry.isSavingsTransfer) {
                                            Card(
                                                shape = RoundedCornerShape(3.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                                            ) {
                                                Text(
                                                    text = "Pigmy",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFE65100),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        if (!entry.attachmentUri.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(3.dp))
                                                    .clickable {
                                                        val f = AttachmentManager.getAttachmentFile(context, entry.attachmentUri)
                                                        if (f != null && f.exists()) {
                                                            viewingPhotoFile = f
                                                        } else {
                                                            Toast.makeText(context, "Attachment file not found", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.Image,
                                                    contentDescription = "View Photo",
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = (if (entry.isIncome) "+" else "-") + CostCalculator.formatPaiseToRupees(entry.amountPaise),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (entry.isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                    textAlign = TextAlign.End
                                )

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.MoreVert,
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                menuExpanded = false
                                                itemToEdit = entry
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Cancel / Reverse") },
                                            onClick = {
                                                menuExpanded = false
                                                selectedItemForVoid = entry
                                            }
                                        )
                                    }
                                }
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
                        containerColor = Color(0xFF2E7D32),
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
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary Footer
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
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = CostCalculator.formatPaiseToRupees(totalIncomePaise),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
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
                            color = Color(0xFFD32F2F)
                        )
                        Text(
                            text = CostCalculator.formatPaiseToRupees(totalExpensePaise),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD32F2F)
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
                            text = "Register Net",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (netBalancePaise >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                        Text(
                            text = CostCalculator.formatPaiseToRupees(netBalancePaise),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (netBalancePaise >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }

    // Add Income / Expense Dialog (Refactored to CashEntryForm & Pinned Actions)
    if (showAddDialog) {
        var amountStr by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var paymentMode by remember { mutableStateOf("CASH") }
        var note by remember { mutableStateOf("") }
        var entryTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

        val defaultCat = activeCategories.firstOrNull { it.name == "General" } ?: activeCategories.firstOrNull()
        var selectedCatId by remember { mutableStateOf(defaultCat?.id ?: "") }
        var selectedCatName by remember { mutableStateOf(defaultCat?.name ?: "General") }

        var noteSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
        LaunchedEffect(description, isIncomeMode) {
            noteSuggestions = viewModel.getNoteSuggestions(isIncomeMode, description)
        }

        var showCreateCategoryDialog by remember { mutableStateOf(false) }

        var attachedPhotoUri by remember { mutableStateOf<Uri?>(null) }
        var tempCameraFile by remember { mutableStateOf<File?>(null) }

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            val tempFile = tempCameraFile
            if (success && tempFile != null && tempFile.exists() && tempFile.length() > 0) {
                attachedPhotoUri = Uri.fromFile(tempFile)
            }
        }

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                attachedPhotoUri = uri
            }
        }

        val dialogContext = LocalContext.current
        val parsedPaise = CostCalculator.parseRupeesToPaise(amountStr)
        val canSave = parsedPaise != null && parsedPaise > 0L

        fun doSave(keepOpen: Boolean) {
            val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L
            if (amountPaise <= 0L) return

            coroutineScope.launch {
                var persistedPath: String? = null
                if (attachedPhotoUri != null) {
                    persistedPath = AttachmentManager.persistImageAttachment(context, attachedPhotoUri!!)
                }

                val selectedPigmyScheme = activePigmySchemes.firstOrNull {
                    it.id == selectedCatId || it.name.equals(selectedCatName, ignoreCase = true)
                }

                if (selectedPigmyScheme != null) {
                    val pigmyNote = buildString {
                        if (description.isNotBlank() && description.trim() != selectedPigmyScheme.name) {
                            append(description.trim())
                        }
                        if (note.isNotBlank()) {
                            if (isNotEmpty()) append(" - ")
                            append(note.trim())
                        }
                    }.ifBlank { null }

                    if (isIncomeMode) {
                        val destination = if (paymentMode == "CASH") "CASH" else "BANK"
                        viewModel.recordPigmyWithdrawal(
                            schemeId = selectedPigmyScheme.id,
                            amountPaise = amountPaise,
                            destination = destination,
                            notes = pigmyNote,
                            withdrawalTimestamp = entryTimestamp,
                            attachmentUri = persistedPath,
                            onDone = {
                                if (keepOpen) {
                                    amountStr = ""
                                    description = ""
                                    note = ""
                                    attachedPhotoUri = null
                                } else {
                                    showAddDialog = false
                                }
                            },
                            onError = { errMsg ->
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        val paymentSource = if (paymentMode == "CASH") "CASH" else "BANK"
                        viewModel.recordPigmyDeposit(
                            schemeId = selectedPigmyScheme.id,
                            amountPaise = amountPaise,
                            paymentSource = paymentSource,
                            notes = pigmyNote,
                            paidTimestamp = entryTimestamp,
                            attachmentUri = persistedPath,
                            onDone = {
                                if (keepOpen) {
                                    amountStr = ""
                                    description = ""
                                    note = ""
                                    attachedPhotoUri = null
                                } else {
                                    showAddDialog = false
                                }
                            }
                        )
                    }
                } else {
                    val entryNotes = buildString {
                        if (description.isNotBlank()) append(description.trim())
                        if (note.isNotBlank()) {
                            if (isNotEmpty()) append(" - ")
                            append(note.trim())
                        }
                    }.ifBlank { null }

                    viewModel.recordShopExpense(
                        category = selectedCatName,
                        amountPaise = amountPaise,
                        isIncome = isIncomeMode,
                        notes = entryNotes,
                        timestamp = entryTimestamp,
                        categoryId = selectedCatId.ifBlank { null },
                        attachmentUri = persistedPath,
                        paymentMethod = paymentMode,
                        onDone = {
                            if (keepOpen) {
                                amountStr = ""
                                description = ""
                                note = ""
                                attachedPhotoUri = null
                            } else {
                                showAddDialog = false
                            }
                        }
                    )
                }
            }
        }

        Dialog(
            onDismissRequest = { showAddDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .padding(AliDimens.spacingSm),
                shape = RoundedCornerShape(AliDimens.radiusLg),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Title Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AliDimens.spacingLg, vertical = AliDimens.spacingMd),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isIncomeMode) "Add Income (Cash In)" else "Add Expense (Cash Out)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isIncomeMode) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                        IconButton(onClick = { showAddDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider()

                    // Scrollable Form Content
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = AliDimens.spacingLg, vertical = AliDimens.spacingMd)
                    ) {
                        item {
                            CashEntryForm(
                                isIncome = isIncomeMode,
                                amountStr = amountStr,
                                onAmountChange = { amountStr = it },
                                categoryName = selectedCatName,
                                categoryId = selectedCatId,
                                onCategoryChange = { id, name ->
                                    selectedCatId = id
                                    selectedCatName = name
                                },
                                activeCategories = activeCategories,
                                activePigmySchemes = activePigmySchemes,
                                onOpenManageCategories = { showManageCategoriesDialog = true },
                                onOpenNewCategory = { showCreateCategoryDialog = true },
                                paymentMode = paymentMode,
                                onPaymentModeChange = { paymentMode = it },
                                entryTimestamp = entryTimestamp,
                                onTimestampChange = { entryTimestamp = it },
                                description = description,
                                onDescriptionChange = { description = it },
                                noteSuggestions = noteSuggestions,
                                note = note,
                                onNoteChange = { note = it },
                                attachedPhotoUri = attachedPhotoUri,
                                onTakeCamera = {
                                    val (camUri, file) = AttachmentManager.createTempCameraUri(dialogContext)
                                    tempCameraFile = file
                                    cameraLauncher.launch(camUri)
                                },
                                onPickGallery = { galleryLauncher.launch("image/*") },
                                onRemovePhoto = { attachedPhotoUri = null }
                            )
                        }
                    }

                    HorizontalDivider()

                    // Pinned Bottom Actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AliDimens.spacingLg, vertical = AliDimens.spacingSm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                        ) {
                            OutlinedButton(
                                onClick = { showAddDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(AliDimens.radiusSm)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { doSave(keepOpen = false) },
                                enabled = canSave,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(AliDimens.radiusSm),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isIncomeMode) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = { doSave(keepOpen = true) },
                            enabled = canSave,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save & add another")
                        }
                    }
                }
            }

            // Sub-dialog: Add New Category
            if (showCreateCategoryDialog) {
                var newCatName by remember { mutableStateOf("") }
                var catError by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { showCreateCategoryDialog = false },
                    title = { Text("New Category") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Create a new organizational category for cash entries.")
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = {
                                    newCatName = it
                                    catError = null
                                },
                                label = { Text("Category Name *") },
                                placeholder = { Text("e.g. Repairs, Shipping, Tea") },
                                singleLine = true,
                                isError = catError != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (catError != null) {
                                Text(catError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val trimmed = newCatName.trim()
                                if (trimmed.isBlank()) {
                                    catError = "Category name cannot be empty"
                                    return@Button
                                }
                                if (activeCategories.any { it.name.equals(trimmed, ignoreCase = true) }) {
                                    catError = "Category with this name already exists"
                                    return@Button
                                }
                                viewModel.createCashCategory(trimmed) { newId ->
                                    selectedCatId = newId
                                    selectedCatName = trimmed
                                    showCreateCategoryDialog = false
                                }
                            }
                        ) {
                            Text("Create & Select")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateCategoryDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    // Dialog: Manage / Remove Categories
    if (showManageCategoriesDialog) {
        AlertDialog(
            onDismissRequest = {
                showManageCategoriesDialog = false
                categoryToDelete = null
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Categories", fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        showManageCategoriesDialog = false
                        categoryToDelete = null
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Remove categories you no longer need. Past transactions will keep their recorded category name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    val removableCategories = activeCategories.filter { it.name != "Pigmy" }
                    if (removableCategories.isEmpty()) {
                        Text(
                            text = "No categories available to remove.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(removableCategories) { cat ->
                                val count = allExpenses.count { it.categoryId == cat.id || it.category.equals(cat.name, ignoreCase = true) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cat.name,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = if (count > 0) "$count transaction(s)" else "No transactions",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { categoryToDelete = cat },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.DeleteOutline,
                                                contentDescription = "Remove ${cat.name}",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManageCategoriesDialog = false
                        categoryToDelete = null
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Confirmation dialog for removing a specific category
    categoryToDelete?.let { cat ->
        val count = allExpenses.count { it.categoryId == cat.id || it.category.equals(cat.name, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Remove Category?") },
            text = {
                Text(
                    "Are you sure you want to remove \"${cat.name}\"?\n\n" +
                    "It will no longer appear in category options for new transactions." +
                    if (count > 0) " Past records ($count transactions) will safely retain this name." else ""
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCashCategory(cat.id) {
                            categoryToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View Attachment Full Photo Dialog
    viewingPhotoFile?.let { file ->
        val bitmap = remember(file) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                android.util.Log.w("CashTrackerScreen", "Failed to decode photo: ${e.message}", e)
                null
            }
        }

        AlertDialog(
            onDismissRequest = { viewingPhotoFile = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Receipt Evidence Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { viewingPhotoFile = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Receipt Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text("Unable to render image file.", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "File: ${file.name} (${file.length() / 1024} KB)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewingPhotoFile = null }) {
                    Text("Close")
                }
            }
        )
    }

    // ── EDIT TRANSACTION DIALOG (Section 3 - Refactored to CashEntryForm) ──────
    itemToEdit?.let { targetEntry ->
        var editAmountStr by remember {
            mutableStateOf(
                if (targetEntry.amountPaise % 100L == 0L) {
                    (targetEntry.amountPaise / 100L).toString()
                } else {
                    String.format(Locale.US, "%.2f", targetEntry.amountPaise / 100.0)
                }
            )
        }
        var editCategoryName by remember { mutableStateOf(targetEntry.category) }
        var editCategoryId by remember {
            mutableStateOf(
                targetEntry.categoryId ?: activeCategories.firstOrNull { it.name.equals(targetEntry.category, ignoreCase = true) }?.id ?: ""
            )
        }
        var editPaymentMode by remember { mutableStateOf(targetEntry.paymentMethod) }
        var editDescription by remember { mutableStateOf(targetEntry.notes ?: "") }
        var editNote by remember { mutableStateOf("") }
        var editTimestamp by remember { mutableLongStateOf(targetEntry.timestamp) }
        var editNoteSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

        LaunchedEffect(editDescription) {
            editNoteSuggestions = viewModel.getNoteSuggestions(targetEntry.isIncome, editDescription)
        }

        val canSaveEdit = CostCalculator.parseRupeesToPaise(editAmountStr)?.let { it > 0L } == true

        Dialog(
            onDismissRequest = { itemToEdit = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.88f)
                    .padding(AliDimens.spacingSm),
                shape = RoundedCornerShape(AliDimens.radiusLg),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AliDimens.spacingLg, vertical = AliDimens.spacingMd),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit Transaction",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (targetEntry.isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                        IconButton(onClick = { itemToEdit = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = AliDimens.spacingLg, vertical = AliDimens.spacingMd)
                    ) {
                        item {
                            CashEntryForm(
                                isIncome = targetEntry.isIncome,
                                amountStr = editAmountStr,
                                onAmountChange = { editAmountStr = it },
                                categoryName = editCategoryName,
                                categoryId = editCategoryId,
                                onCategoryChange = { id, name ->
                                    editCategoryId = id
                                    editCategoryName = name
                                },
                                activeCategories = activeCategories,
                                activePigmySchemes = activePigmySchemes,
                                onOpenManageCategories = { showManageCategoriesDialog = true },
                                onOpenNewCategory = {},
                                paymentMode = editPaymentMode,
                                onPaymentModeChange = { editPaymentMode = it },
                                entryTimestamp = editTimestamp,
                                onTimestampChange = { editTimestamp = it },
                                description = editDescription,
                                onDescriptionChange = { editDescription = it },
                                noteSuggestions = editNoteSuggestions,
                                note = editNote,
                                onNoteChange = { editNote = it },
                                attachedPhotoUri = null,
                                onTakeCamera = {},
                                onPickGallery = {},
                                onRemovePhoto = {}
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AliDimens.spacingLg, vertical = AliDimens.spacingMd),
                        horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                    ) {
                        OutlinedButton(
                            onClick = { itemToEdit = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(AliDimens.radiusSm)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val parsedPaise = CostCalculator.parseRupeesToPaise(editAmountStr) ?: 0L
                                if (parsedPaise <= 0L) {
                                    Toast.makeText(context, "Enter a valid positive amount", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.updateShopExpense(
                                    id = targetEntry.id,
                                    amountPaise = parsedPaise,
                                    category = editCategoryName,
                                    paymentMethod = editPaymentMode,
                                    notes = editDescription.trim().ifBlank { null },
                                    timestamp = editTimestamp,
                                    attachmentUri = targetEntry.attachmentUri
                                ) {
                                    itemToEdit = null
                                }
                            },
                            enabled = canSaveEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(AliDimens.radiusSm)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
                        color = if (entry.isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                    if (entry.isSavingsTransfer) {
                        Text(
                            text = "• Notice: This is a linked Pigmy savings transfer. Voiding here will maintain accounting integrity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                    if (!entry.attachmentUri.isNullOrBlank()) {
                        Text(
                            text = "• Attached photo evidence will be retained in history for audit purposes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            Text("Income (${preview.incomeCount}):", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                            Text(CostCalculator.formatPaiseToRupees(preview.totalIncomePaise), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expense (${preview.expenseCount}):", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                            Text(CostCalculator.formatPaiseToRupees(preview.totalExpensePaise), color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
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
                            "These entries will be recorded in your Cash In & Out register with their respective categories.",
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
    // Bulk Void Confirmation Dialog
    if (showBulkVoidConfirmDialog) {
        val selectedEntries = remember(selectedCashEntryIds, allExpenses) {
            allExpenses.filter { it.id in selectedCashEntryIds }
        }
        val totalPaise = remember(selectedEntries) {
            selectedEntries.sumOf { it.amountPaise }
        }
        var reasonText by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showBulkVoidConfirmDialog = false },
            title = {
                Text("Void ${selectedCashEntryIds.size} Cash Entries", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Total Amount: ${CostCalculator.formatPaiseToRupees(totalPaise)}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Refund entries will be voided before parent expenses. Voiding will reverse cash/bank totals safely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        viewModel.voidExpenses(selectedCashEntryIds.toList(), trimmed) { res ->
                            bulkVoidResult = res
                            showBulkVoidConfirmDialog = false
                        }
                    },
                    enabled = !viewModel.isVoidingExpenses && reasonText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (viewModel.isVoidingExpenses) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voiding...")
                    } else {
                        Text("Confirm Void")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBulkVoidConfirmDialog = false },
                    enabled = !viewModel.isVoidingExpenses
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Void Result Dialog
    bulkVoidResult?.let { res ->
        AlertDialog(
            onDismissRequest = { bulkVoidResult = null },
            title = { Text("Bulk Void Completed", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Successfully voided: ${res.done} entries")
                    if (res.skipped.isNotEmpty()) {
                        Text(
                            "Skipped: ${res.skipped.size} entries",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(res.skipped) { (id, reason) ->
                                Text(
                                    "• Entry $id: $reason",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { bulkVoidResult = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showExportPreviewDialog) {
        CashTrackerExportPreviewDialog(
            allExpenses = allExpenses,
            activeCategories = activeCategories,
            onDismiss = { showExportPreviewDialog = false },
            onExportAndShare = { items, periodTitle, fromDate, toDate, summary, isShare ->
                showExportPreviewDialog = false
                if (isShare) {
                    isExporting = true
                    viewModel.exportCashTrackerExcel(
                        items = items,
                        periodTitle = periodTitle,
                        fromDate = fromDate,
                        toDate = toDate,
                        activeFiltersSummary = summary
                    ) { shareIntent ->
                        isExporting = false
                        try {
                            val chooser = Intent.createChooser(shareIntent, "Share Cash Book").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooser)
                        } catch (e: Exception) {
                            android.util.Log.w("CashTracker", "Failed to share cash records", e)
                            Toast.makeText(context, "Failed to share cash records. Please retry.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    pendingExportItems = items
                    pendingExportTitle = periodTitle
                    pendingExportFromDate = fromDate
                    pendingExportToDate = toDate
                    pendingExportSummary = summary
                    val dfFile = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val fromStr = if (fromDate != null && fromDate > 0L) dfFile.format(Date(fromDate)) else "start"
                    val toStr = if (toDate != null && toDate < Long.MAX_VALUE) dfFile.format(Date(toDate)) else dfFile.format(Date())
                    val defaultName = "Ali_Educational_Cash_${fromStr}_to_${toStr}.xlsx"
                    createCashDocumentLauncher.launch(defaultName)
                }
            }
        )
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
        } catch (e: Exception) {
            android.util.Log.w("CashTrackerScreen", "Failed to query file name: ${e.message}", e)
        }
    } else uri.path?.let {
        name = java.io.File(it).name
    }
    return name
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashTrackerExportPreviewDialog(
    allExpenses: List<ShopExpenseEntity>,
    activeCategories: List<CashCategoryEntity>,
    onDismiss: () -> Unit,
    onExportAndShare: (
        items: List<ShopExpenseEntity>,
        periodTitle: String,
        fromDate: Long?,
        toDate: Long?,
        summary: String,
        isShare: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf("MONTH") } // TODAY, WEEK, MONTH, ALL, CUSTOM
    var selectedDirection by remember { mutableStateOf("ALL") } // ALL, IN, OUT
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) } // empty = All

    val now = System.currentTimeMillis()
    var fromTimestamp by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }
    var toTimestamp by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        mutableLongStateOf(cal.timeInMillis)
    }

    val (effectiveStart, effectiveEnd, rangeTitle) = remember(selectedPeriod, fromTimestamp, toTimestamp) {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
        when (selectedPeriod) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                val e = cal.timeInMillis
                Triple(s, e, "Today (${fmt.format(Date(s))})")
            }
            "WEEK" -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                val e = cal.timeInMillis
                Triple(s, e, "This Week (${fmt.format(Date(s))} to ${fmt.format(Date(e))})")
            }
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                val e = cal.timeInMillis
                Triple(s, e, "This Month (${fmt.format(Date(s))} to ${fmt.format(Date(e))})")
            }
            "ALL" -> {
                Triple(0L, Long.MAX_VALUE, "All Time")
            }
            else -> { // CUSTOM
                Triple(fromTimestamp, toTimestamp, "${fmt.format(Date(fromTimestamp))} to ${fmt.format(Date(toTimestamp))}")
            }
        }
    }

    val matchingItems = remember(allExpenses, effectiveStart, effectiveEnd, selectedDirection, selectedCategories) {
        allExpenses.filter { item ->
            val inDate = item.timestamp in effectiveStart..effectiveEnd
            val matchesDir = when (selectedDirection) {
                "IN" -> item.isIncome
                "OUT" -> !item.isIncome
                else -> true
            }
            val matchesCat = if (selectedCategories.isEmpty()) true else {
                selectedCategories.contains(item.category)
            }
            inDate && matchesDir && matchesCat
        }
    }

    val inflowPaise = remember(matchingItems) { matchingItems.filter { it.isIncome }.sumOf { it.amountPaise } }
    val outflowPaise = remember(matchingItems) { matchingItems.filter { !it.isIncome }.sumOf { it.amountPaise } }
    val netMovementPaise = inflowPaise - outflowPaise

    val filtersSummary = buildString {
        append("Period: $rangeTitle | Direction: $selectedDirection")
        if (selectedCategories.isEmpty()) append(" | Categories: All")
        else append(" | Categories: ${selectedCategories.joinToString(", ")}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export & Share Cash Book", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Quick Periods", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("TODAY" to "Today", "WEEK" to "This Week", "MONTH" to "This Month", "ALL" to "All Time", "CUSTOM" to "Custom").forEach { (code, label) ->
                            FilterChip(
                                selected = selectedPeriod == code,
                                onClick = { selectedPeriod = code },
                                label = { Text(label, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }

                if (selectedPeriod == "CUSTOM") {
                    item {
                        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val c = Calendar.getInstance().apply { timeInMillis = fromTimestamp }
                                    DatePickerDialog(context, { _, y, m, d ->
                                        val cal = Calendar.getInstance().apply {
                                            set(y, m, d, 0, 0, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        fromTimestamp = cal.timeInMillis
                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("From: ${fmt.format(Date(fromTimestamp))}", fontSize = 11.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    val c = Calendar.getInstance().apply { timeInMillis = toTimestamp }
                                    DatePickerDialog(context, { _, y, m, d ->
                                        val cal = Calendar.getInstance().apply {
                                            set(y, m, d, 23, 59, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }
                                        toTimestamp = cal.timeInMillis
                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("To: ${fmt.format(Date(toTimestamp))}", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }

                item {
                    Text("Direction", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedDirection == "ALL",
                            onClick = { selectedDirection = "ALL" },
                            label = { Text("All") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedDirection == "IN",
                            onClick = { selectedDirection = "IN" },
                            label = { Text("Cash In") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedDirection == "OUT",
                            onClick = { selectedDirection = "OUT" },
                            label = { Text("Cash Out") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Categories Filter", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategories.isEmpty(),
                            onClick = { selectedCategories = emptySet() },
                            label = { Text("All Categories", fontSize = 11.5.sp) }
                        )
                        activeCategories.forEach { cat ->
                            val isSel = selectedCategories.contains(cat.name)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedCategories = if (isSel) selectedCategories - cat.name else selectedCategories + cat.name
                                },
                                label = { Text(cat.name, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }

                item {
                    // Preview Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Export Preview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Range: $rangeTitle", style = MaterialTheme.typography.bodySmall)
                            Text("Matching Active Records: ${matchingItems.size}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Inflow Total:", style = MaterialTheme.typography.bodySmall)
                                Text(CostCalculator.formatPaiseToRupees(inflowPaise), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Outflow Total:", style = MaterialTheme.typography.bodySmall)
                                Text(CostCalculator.formatPaiseToRupees(outflowPaise), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD32F2F))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Selected Net Movement:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    CostCalculator.formatPaiseToRupees(netMovementPaise),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (netMovementPaise >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
                        val periodTitle = when (selectedPeriod) {
                            "TODAY" -> "Today"
                            "WEEK" -> "This Week"
                            "MONTH" -> "This Month"
                            "ALL" -> "All Time"
                            else -> "${fmt.format(Date(effectiveStart))} to ${fmt.format(Date(effectiveEnd))}"
                        }
                        onExportAndShare(
                            matchingItems,
                            periodTitle,
                            if (selectedPeriod == "ALL") null else effectiveStart,
                            if (selectedPeriod == "ALL") null else effectiveEnd,
                            filtersSummary,
                            false
                        )
                    }
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to Device", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
                        val periodTitle = when (selectedPeriod) {
                            "TODAY" -> "Today"
                            "WEEK" -> "This Week"
                            "MONTH" -> "This Month"
                            "ALL" -> "All Time"
                            else -> "${fmt.format(Date(effectiveStart))} to ${fmt.format(Date(effectiveEnd))}"
                        }
                        onExportAndShare(
                            matchingItems,
                            periodTitle,
                            if (selectedPeriod == "ALL") null else effectiveStart,
                            if (selectedPeriod == "ALL") null else effectiveEnd,
                            filtersSummary,
                            true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CashEntryForm(
    isIncome: Boolean,
    amountStr: String,
    onAmountChange: (String) -> Unit,
    categoryName: String,
    categoryId: String,
    onCategoryChange: (id: String, name: String) -> Unit,
    activeCategories: List<CashCategoryEntity>,
    activePigmySchemes: List<PigmySchemeEntity>,
    onOpenManageCategories: () -> Unit,
    onOpenNewCategory: () -> Unit,
    paymentMode: String,
    onPaymentModeChange: (String) -> Unit,
    entryTimestamp: Long,
    onTimestampChange: (Long) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    noteSuggestions: List<String>,
    note: String,
    onNoteChange: (String) -> Unit,
    attachedPhotoUri: Uri?,
    onTakeCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val parsedPaise = CostCalculator.parseRupeesToPaise(amountStr)
    val isAmountValid = parsedPaise != null && parsedPaise > 0L
    val isAmountTouched = amountStr.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AliDimens.spacingMd)
    ) {
        // 1. Amount First
        OutlinedTextField(
            value = amountStr,
            onValueChange = onAmountChange,
            label = { Text("Amount (₹) *") },
            placeholder = { Text("0.00") },
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            isError = isAmountTouched && !isAmountValid,
            supportingText = {
                if (isAmountValid) {
                    Text(
                        CostCalculator.formatPaiseToRupees(parsedPaise!!),
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                } else if (isAmountTouched) {
                    Text("Enter a valid positive amount", color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics { contentDescription = "Amount in rupees" }
        )

        // 2. Categories as FlowRow chips
        Column(verticalArrangement = Arrangement.spacedBy(AliDimens.spacingXs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onOpenManageCategories,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("⚙ Manage", fontSize = 12.sp)
                }
            }

            val selectedPigmyScheme = remember(categoryId, categoryName, activePigmySchemes) {
                activePigmySchemes.firstOrNull { it.id == categoryId || it.name.equals(categoryName, ignoreCase = true) }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeCategories.filter { it.name != "Pigmy" }.forEach { cat ->
                    FilterChip(
                        selected = selectedPigmyScheme == null && (categoryId == cat.id || categoryName == cat.name),
                        onClick = { onCategoryChange(cat.id, cat.name) },
                        label = { Text(cat.name, fontSize = AliDimens.minChipTextSize) }
                    )
                }

                activePigmySchemes.forEach { scheme ->
                    FilterChip(
                        selected = selectedPigmyScheme?.id == scheme.id || selectedPigmyScheme?.name == scheme.name,
                        onClick = { onCategoryChange(scheme.id, scheme.name) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_pigmy_coin),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        label = { Text(scheme.name, fontSize = AliDimens.minChipTextSize) }
                    )
                }

                FilterChip(
                    selected = false,
                    onClick = onOpenNewCategory,
                    leadingIcon = {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    label = { Text("New", fontSize = AliDimens.minChipTextSize) }
                )
            }

            if (selectedPigmyScheme != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(AliDimens.radiusSm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(AliDimens.spacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pigmy_coin),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(AliDimens.spacingSm))
                        Text(
                            text = if (isIncome) {
                                "Pigmy Withdrawal: Will withdraw from ${selectedPigmyScheme.name} into shop cash."
                            } else {
                                "Pigmy Deposit: Will deposit into ${selectedPigmyScheme.name} from shop cash."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Payment Mode Segmented Control
        Column(verticalArrangement = Arrangement.spacedBy(AliDimens.spacingXs)) {
            Text(
                text = "Payment Mode",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val paymentOptions = listOf("CASH" to "Cash", "GPAY" to "UPI", "BANK" to "Bank")
                paymentOptions.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = paymentMode.equals(key, ignoreCase = true),
                        onClick = { onPaymentModeChange(key) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = paymentOptions.size)
                    ) {
                        Text(label, maxLines = 1, softWrap = false, fontSize = AliDimens.minChipTextSize)
                    }
                }
            }
        }

        // 4. Compact Date and Time Row
        val context = LocalContext.current
        val cal = remember(entryTimestamp) { Calendar.getInstance().apply { timeInMillis = entryTimestamp } }
        val entryDateStr = remember(entryTimestamp) { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(entryTimestamp)) }
        val entryTimeStr = remember(entryTimestamp) { SimpleDateFormat("hh:mm a", Locale.US).format(Date(entryTimestamp)) }
        val isFuture = entryTimestamp > System.currentTimeMillis() + 60_000L

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AliDimens.radiusSm))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = AliDimens.spacingMd, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "$entryDateStr • $entryTimeStr",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
                TextButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val c = Calendar.getInstance().apply {
                                    timeInMillis = entryTimestamp
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        c.set(Calendar.HOUR_OF_DAY, hour)
                                        c.set(Calendar.MINUTE, minute)
                                        onTimestampChange(c.timeInMillis)
                                    },
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Change", fontSize = 12.sp)
                }
            }

            if (isFuture) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(AliDimens.radiusSm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ Notice: This entry has a future date/time. It will be recorded with this timestamp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(AliDimens.spacingSm)
                    )
                }
            }
        }

        // 5. Description / Particulars
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (Optional)") },
                placeholder = { Text(if (isIncome) "e.g. Counter Sales, Customer Payment" else "e.g. Office Rent, Electricity, Tea") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (noteSuggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    noteSuggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onDescriptionChange(suggestion) },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 6. Collapsible More Details (Extra Note, Receipt Photo)
        var expandedDetails by remember { mutableStateOf(attachedPhotoUri != null || note.isNotBlank()) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AliDimens.radiusSm))
                .clickable { expandedDetails = !expandedDetails }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (expandedDetails) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "More details (extra note, receipt photo)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (expandedDetails) {
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text("Extra Note (Optional)") },
                placeholder = { Text("Optional details / voucher #") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Receipt / Evidence Photo (Optional)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (attachedPhotoUri != null) {
                Card(
                    shape = RoundedCornerShape(AliDimens.radiusSm),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AliDimens.spacingSm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Photo Attached", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Ready to save", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onTakeCamera) {
                                Text("Replace", fontSize = 11.sp)
                            }
                            IconButton(onClick = onRemovePhoto) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove photo", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                ) {
                    OutlinedButton(
                        onClick = onTakeCamera,
                        shape = RoundedCornerShape(AliDimens.radiusSm),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = onPickGallery,
                        shape = RoundedCornerShape(AliDimens.radiusSm),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose Photo", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
