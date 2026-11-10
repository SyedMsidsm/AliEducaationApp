package com.alieducational.app.ui.stock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.alieducational.app.data.service.*
import com.alieducational.app.data.service.`import`.AutoMapper
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import com.alieducational.app.ui.theme.AliDimens
import com.alieducational.app.ui.theme.LocalAliStatusColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCatalogScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val statusColors = LocalAliStatusColors.current
    val barcodeService = remember { BarcodeGeneratorService() }

    // Wizard step: 1 (Choose File), 2 (Check Columns), 3 (Review), 4 (Options), 5 (Result)
    var currentStep by remember { mutableIntStateOf(1) }

    // File state
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableLongStateOf(0L) }
    var selectedSheetIndex by remember { mutableIntStateOf(0) }
    var allLoadedRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var headers by remember { mutableStateOf<List<String>>(emptyList()) }
    var headerRowIndex by remember { mutableIntStateOf(0) }

    // Mapping state
    var nameColIndex by remember { mutableIntStateOf(-1) }
    var categoryColIndex by remember { mutableIntStateOf(-1) }
    var brandColIndex by remember { mutableIntStateOf(-1) }
    var skuColIndex by remember { mutableIntStateOf(-1) }
    var barcodeColIndex by remember { mutableIntStateOf(-1) }
    var openingQtyColIndex by remember { mutableIntStateOf(-1) }
    var boxesColIndex by remember { mutableIntStateOf(-1) }
    var purchaseCostColIndex by remember { mutableIntStateOf(-1) }
    var sellingPieceColIndex by remember { mutableIntStateOf(-1) }
    var sellingPackColIndex by remember { mutableIntStateOf(-1) }
    var wholesaleColIndex by remember { mutableIntStateOf(-1) }
    var lowStockColIndex by remember { mutableIntStateOf(-1) }

    var confidenceMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isRestoredMapping by remember { mutableStateOf(false) }
    var saveMappingForNextTime by remember { mutableStateOf(true) }

    // Options state
    var selectedMode by remember { mutableStateOf(ImportMode.ADD_STOCK) }
    var validRowsOnly by remember { mutableStateOf(false) }
    var applyDefaultMargin by remember { mutableStateOf(true) }
    var defaultMarginPercent by remember { mutableStateOf("20") }
    var defaultMarginType by remember { mutableStateOf("MARKUP") } // MARKUP or GROSS_MARGIN
    var generateMissingBarcodes by remember { mutableStateOf(true) }
    var showPinFallbackDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val importBatches by viewModel.allImportBatches.collectAsState()

    // Review state
    var reviewStatusFilter by remember { mutableStateOf("ALL") }
    var isPlanning by remember { mutableStateOf(false) }

    val previewData by viewModel.previewData.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val plannedRows by viewModel.plannedRows.collectAsState()
    var isExporting by remember { mutableStateOf(false) }

    val createTemplateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            viewModel.saveImportTemplateToUri(
                uri = uri,
                onSuccess = {
                    isExporting = false
                    Toast.makeText(context, "Template saved successfully", Toast.LENGTH_SHORT).show()
                },
                onError = { msg ->
                    isExporting = false
                    Toast.makeText(context, "Failed to save template: $msg", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            isExporting = false
        }
    }

    val createStockExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            viewModel.saveCurrentStockToUri(
                uri = uri,
                onSuccess = {
                    isExporting = false
                    Toast.makeText(context, "Current stock saved successfully", Toast.LENGTH_SHORT).show()
                },
                onError = { msg ->
                    isExporting = false
                    Toast.makeText(context, "Failed to export stock: $msg", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            isExporting = false
        }
    }

    val createErrorReportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            viewModel.saveErrorReportToUri(
                uri = uri,
                headers = headers,
                allRows = allLoadedRows,
                plannedRows = plannedRows,
                onSuccess = {
                    isExporting = false
                    Toast.makeText(context, "Error report saved successfully", Toast.LENGTH_SHORT).show()
                },
                onError = { msg ->
                    isExporting = false
                    Toast.makeText(context, "Failed to save error report: $msg", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            isExporting = false
        }
    }

    // File picker launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            var name = "spreadsheet"
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) name = cursor.getString(nameIdx)
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }
            selectedFileName = name
            selectedFileSize = size
            selectedSheetIndex = 0

            viewModel.previewSpreadsheet(uri, name, 0)
            coroutineScope.launch {
                val pair = viewModel.loadSpreadsheetRows(uri, name, 0)
                headers = pair.first
                allLoadedRows = pair.second
            }
        }
    }

    // Auto-map when headers change
    LaunchedEffect(headers) {
        if (headers.isNotEmpty()) {
            val hash = AutoMapper.hashHeaders(headers)
            val savedJson = viewModel.getSavedImportMapping(hash)
            if (savedJson != null) {
                try {
                    val obj = JSONObject(savedJson)
                    nameColIndex = obj.optInt("name", -1)
                    categoryColIndex = obj.optInt("category", -1)
                    brandColIndex = obj.optInt("brand", -1)
                    skuColIndex = obj.optInt("sku", -1)
                    barcodeColIndex = obj.optInt("barcode", -1)
                    openingQtyColIndex = obj.optInt("openingQty", -1)
                    boxesColIndex = obj.optInt("boxes", -1)
                    purchaseCostColIndex = obj.optInt("purchaseCost", -1)
                    sellingPieceColIndex = obj.optInt("sellingPiece", -1)
                    sellingPackColIndex = obj.optInt("sellingPack", -1)
                    wholesaleColIndex = obj.optInt("wholesale", -1)
                    lowStockColIndex = obj.optInt("lowStock", -1)
                    isRestoredMapping = true
                    confidenceMap = emptyMap()
                    return@LaunchedEffect
                } catch (_: Exception) {}
            }

            // Fallback to AutoMapper suggestions
            isRestoredMapping = false
            val suggestions = AutoMapper.suggest(headers)
            val conf = mutableMapOf<String, Int>()
            suggestions["name"]?.let { nameColIndex = it.columnIndex; conf["name"] = it.confidence }
            suggestions["category"]?.let { categoryColIndex = it.columnIndex; conf["category"] = it.confidence }
            suggestions["brand"]?.let { brandColIndex = it.columnIndex; conf["brand"] = it.confidence }
            suggestions["sku"]?.let { skuColIndex = it.columnIndex; conf["sku"] = it.confidence }
            suggestions["barcode"]?.let { barcodeColIndex = it.columnIndex; conf["barcode"] = it.confidence }
            suggestions["openingQty"]?.let { openingQtyColIndex = it.columnIndex; conf["openingQty"] = it.confidence }
            suggestions["boxes"]?.let { boxesColIndex = it.columnIndex; conf["boxes"] = it.confidence }
            suggestions["purchaseCost"]?.let { purchaseCostColIndex = it.columnIndex; conf["purchaseCost"] = it.confidence }
            suggestions["sellingPrice"]?.let { sellingPieceColIndex = it.columnIndex; conf["sellingPrice"] = it.confidence }
            suggestions["wholesalePrice"]?.let { wholesaleColIndex = it.columnIndex; conf["wholesalePrice"] = it.confidence }
            suggestions["lowStock"]?.let { lowStockColIndex = it.columnIndex; conf["lowStock"] = it.confidence }
            confidenceMap = conf
        }
    }

    fun buildCurrentMapping(): ColumnMapping {
        return ColumnMapping(
            nameIndex = nameColIndex,
            categoryIndex = categoryColIndex,
            brandIndex = brandColIndex,
            skuIndex = skuColIndex,
            barcodeIndex = barcodeColIndex,
            openingQtyIndex = openingQtyColIndex,
            purchaseCostIndex = purchaseCostColIndex,
            sellingPricePieceIndex = sellingPieceColIndex,
            sellingPricePackIndex = sellingPackColIndex,
            wholesalePriceIndex = wholesaleColIndex,
            lowStockIndex = lowStockColIndex,
            headerRowIndex = previewData?.headerRowIndex ?: 0,
            boxesBroughtIndex = boxesColIndex
        )
    }

    fun saveCurrentMappingIfEnabled() {
        if (saveMappingForNextTime && headers.isNotEmpty()) {
            coroutineScope.launch {
                val hash = AutoMapper.hashHeaders(headers)
                val obj = JSONObject()
                obj.put("name", nameColIndex)
                obj.put("category", categoryColIndex)
                obj.put("brand", brandColIndex)
                obj.put("sku", skuColIndex)
                obj.put("barcode", barcodeColIndex)
                obj.put("openingQty", openingQtyColIndex)
                obj.put("boxes", boxesColIndex)
                obj.put("purchaseCost", purchaseCostColIndex)
                obj.put("sellingPiece", sellingPieceColIndex)
                obj.put("sellingPack", sellingPackColIndex)
                obj.put("wholesale", wholesaleColIndex)
                obj.put("lowStock", lowStockColIndex)
                viewModel.saveImportMapping(hash, obj.toString())
            }
        }
    }

    fun runPlan() {
        isPlanning = true
        coroutineScope.launch {
            try {
                val mapping = buildCurrentMapping()
                val marginVal = if (applyDefaultMargin) defaultMarginPercent.toDoubleOrNull() else null
                viewModel.planImport(
                    rows = allLoadedRows,
                    mapping = mapping,
                    mode = selectedMode,
                    defaultMarginPercent = marginVal,
                    defaultMarginType = defaultMarginType,
                    generateBarcodes = generateMissingBarcodes
                )
            } finally {
                isPlanning = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentStep) {
                                1 -> "1. Select Spreadsheet"
                                2 -> "2. Match Columns"
                                3 -> "3. Review & Validate"
                                4 -> "4. Import Options"
                                else -> "5. Import Result"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (selectedFileName.isNotBlank()) selectedFileName else "Product Catalog Import",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1 && currentStep < 5) {
                            currentStep--
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Filled.History, contentDescription = "Import History")
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Step Progress Indicator
                WizardStepHeader(currentStep = currentStep)

                // Step Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentStep) {
                        1 -> Step1ChooseFile(
                            selectedFileName = selectedFileName,
                            selectedFileSize = selectedFileSize,
                            previewData = previewData,
                            selectedSheetIndex = selectedSheetIndex,
                            onPickFile = {
                                filePicker.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.ms-excel",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "*/*"
                                    )
                                )
                            },
                            onSelectSheet = { newIdx ->
                                selectedSheetIndex = newIdx
                                selectedUri?.let { uri ->
                                    viewModel.previewSpreadsheet(uri, selectedFileName, newIdx)
                                    coroutineScope.launch {
                                        val pair = viewModel.loadSpreadsheetRows(uri, selectedFileName, newIdx)
                                        headers = pair.first
                                        allLoadedRows = pair.second
                                    }
                                }
                            },
                            onDownloadTemplate = {
                                if (!isExporting) {
                                    createTemplateLauncher.launch("product_catalog_template.xlsx")
                                }
                            },
                            onExportCurrentStock = {
                                if (!isExporting) {
                                    createStockExportLauncher.launch("current_stock_${System.currentTimeMillis()}.xlsx")
                                }
                            },
                            onNext = {
                                currentStep = 2
                            }
                        )

                        2 -> Step2CheckColumns(
                            headers = headers,
                            isRestoredMapping = isRestoredMapping,
                            confidenceMap = confidenceMap,
                            nameCol = nameColIndex, onNameChange = { nameColIndex = it },
                            categoryCol = categoryColIndex, onCategoryChange = { categoryColIndex = it },
                            brandCol = brandColIndex, onBrandChange = { brandColIndex = it },
                            skuCol = skuColIndex, onSkuChange = { skuColIndex = it },
                            barcodeCol = barcodeColIndex, onBarcodeChange = { barcodeColIndex = it },
                            openingQtyCol = openingQtyColIndex, onOpeningQtyChange = { openingQtyColIndex = it },
                            boxesCol = boxesColIndex, onBoxesChange = { boxesColIndex = it },
                            purchaseCostCol = purchaseCostColIndex, onPurchaseCostChange = { purchaseCostColIndex = it },
                            sellingPieceCol = sellingPieceColIndex, onSellingPieceChange = { sellingPieceColIndex = it },
                            sellingPackCol = sellingPackColIndex, onSellingPackChange = { sellingPackColIndex = it },
                            wholesaleCol = wholesaleColIndex, onWholesaleChange = { wholesaleColIndex = it },
                            lowStockCol = lowStockColIndex, onLowStockChange = { lowStockColIndex = it },
                            saveMapping = saveMappingForNextTime, onSaveMappingChange = { saveMappingForNextTime = it },
                            onBack = { currentStep = 1 },
                            onNext = {
                                saveCurrentMappingIfEnabled()
                                currentStep = 3
                                runPlan()
                            }
                        )

                        3 -> Step3Review(
                            isPlanning = isPlanning,
                            plannedRows = plannedRows,
                            filter = reviewStatusFilter,
                            onFilterChange = { reviewStatusFilter = it },
                            onExportErrorReport = {
                                if (!isExporting) {
                                    createErrorReportLauncher.launch("import_errors_${System.currentTimeMillis()}.xlsx")
                                }
                            },
                            onBack = { currentStep = 2 },
                            onNext = { currentStep = 4 }
                        )

                        4 -> Step4Options(
                            selectedMode = selectedMode,
                            onModeSelect = { selectedMode = it },
                            validRowsOnly = validRowsOnly,
                            onValidRowsChange = { validRowsOnly = it },
                            applyDefaultMargin = applyDefaultMargin,
                            onApplyMarginChange = { applyDefaultMargin = it },
                            marginPercent = defaultMarginPercent,
                            onMarginPercentChange = { defaultMarginPercent = it },
                            marginType = defaultMarginType,
                            onMarginTypeChange = { defaultMarginType = it },
                            generateBarcodes = generateMissingBarcodes,
                            onGenerateBarcodesChange = { generateMissingBarcodes = it },
                            plannedRows = plannedRows,
                            isImporting = isImporting,
                            onCommit = {
                                val activity = context as? FragmentActivity
                                val doCommit = {
                                    viewModel.commitImport(
                                        plan = plannedRows,
                                        mode = selectedMode,
                                        validRowsOnly = validRowsOnly,
                                        fileName = selectedFileName,
                                        mapping = buildCurrentMapping(),
                                        onDone = { currentStep = 5 }
                                    )
                                }

                                if (activity != null) {
                                    viewModel.stepUp.require(
                                        activity = activity,
                                        title = "Authorize Stock Import",
                                        onOk = { doCommit() },
                                        onFail = { reason ->
                                            if (reason.contains("screen lock", ignoreCase = true)) {
                                                showPinFallbackDialog = true
                                            } else {
                                                Toast.makeText(context, "Auth cancelled: $reason", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                } else {
                                    doCommit()
                                }
                            },
                            onBack = { currentStep = 3 }
                        )

                        5 -> Step5Result(
                            result = importResult,
                            plannedRows = plannedRows,
                            onPrintLabels = {
                                val newItemsWithBarcodes = plannedRows.filter {
                                    it.status == RowStatus.NEW && !it.parsedData?.barcode.isNullOrBlank()
                                }
                                if (newItemsWithBarcodes.isNotEmpty()) {
                                    val labelItems = newItemsWithBarcodes.map { r ->
                                        val d = r.parsedData!!
                                        LabelItem(
                                            productName = d.name,
                                            barcode = d.barcode!!,
                                            unitLabel = d.baseUnit.uppercase(Locale.ROOT),
                                            pricePaise = if (d.sellingPiecePaise > 0L) d.sellingPiecePaise else null,
                                            copies = 1
                                        )
                                    }
                                    val pdfFile = barcodeService.generateMultiLabelsPdf(context, labelItems, LabelPreset.A4_3X8)
                                    if (pdfFile != null) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Print Product Labels"))
                                    } else {
                                        Toast.makeText(context, "Could not create label sheet", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "No new items with barcodes to print", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDone = onNavigateBack
                        )
                    }
                }
            }

            if (showPinFallbackDialog) {
                AlertDialog(
                    onDismissRequest = { showPinFallbackDialog = false },
                    title = { Text("Confirm Stock Import") },
                    text = {
                        Text("No biometric screen lock is registered. Do you wish to proceed with committing ${plannedRows.size} items into stock?")
                    },
                    confirmButton = {
                        Button(onClick = {
                            showPinFallbackDialog = false
                            viewModel.commitImport(
                                plan = plannedRows,
                                mode = selectedMode,
                                validRowsOnly = validRowsOnly,
                                fileName = selectedFileName,
                                mapping = buildCurrentMapping(),
                                onDone = { currentStep = 5 }
                            )
                        }) {
                            Text("Confirm Import")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPinFallbackDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showHistoryDialog) {
                ImportHistoryDialog(
                    batches = importBatches,
                    onDismiss = { showHistoryDialog = false }
                )
            }
        }
    }
}

@Composable
fun WizardStepHeader(currentStep: Int) {
    val steps = listOf("File", "Columns", "Review", "Options", "Result")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val stepNum = index + 1
            val isDone = stepNum < currentStep
            val isCurrent = stepNum == currentStep

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNum",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1: Choose File
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step1ChooseFile(
    selectedFileName: String,
    selectedFileSize: Long,
    previewData: SpreadsheetPreview?,
    selectedSheetIndex: Int,
    onPickFile: () -> Unit,
    onSelectSheet: (Int) -> Unit,
    onDownloadTemplate: () -> Unit,
    onExportCurrentStock: () -> Unit,
    onNext: () -> Unit
) {
    val isCash = previewData?.isCashLedger == true
    val hasError = previewData?.errorMessage != null
    val canProceed = selectedFileName.isNotBlank() && previewData != null && !hasError && !isCash

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickFile() },
                shape = RoundedCornerShape(AliDimens.radiusLg),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedFileName.isBlank()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.5.dp, if (selectedFileName.isBlank()) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (selectedFileName.isNotBlank()) Icons.Filled.CheckCircle else Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (selectedFileName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedFileName.isNotBlank()) selectedFileName else "Select Spreadsheet (.xlsx, .xls, .csv)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    if (selectedFileSize > 0L) {
                        Text(
                            text = "Size: ${selectedFileSize / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onPickFile,
                        shape = RoundedCornerShape(AliDimens.radiusMd)
                    ) {
                        Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFileName.isBlank()) "Browse File" else "Change File")
                    }
                }
            }
        }

        // Quick Tools: Template & Stock Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AliDimens.radiusMd)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Tools & Stock Export",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download a clean Excel template with sample rows and instructions, or export current stock to edit and re-import.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDownloadTemplate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(AliDimens.radiusSm)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Template (.xlsx)", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onExportCurrentStock,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(AliDimens.radiusSm)
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Stock", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Error banner
        if (hasError) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCash) Color(0xFFFFF3E0) else Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isCash) Icons.Filled.Warning else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = if (isCash) Color(0xFFE65100) else Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isCash) "Cash In/Out Ledger Detected" else "File Error",
                                fontWeight = FontWeight.Bold,
                                color = if (isCash) Color(0xFFE65100) else Color(0xFFC62828)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = previewData!!.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Multi-sheet selector
        if (previewData != null && previewData.sheetNames.size > 1 && !isCash) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Multiple Sheets Found", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ColumnDropdownSelector(
                            label = "Select Active Sheet",
                            options = previewData.sheetNames,
                            selectedIndex = selectedSheetIndex,
                            onSelect = onSelectSheet
                        )
                    }
                }
            }
        }

        // Sample rows preview
        if (previewData != null && previewData.previewRows.isNotEmpty() && !isCash) {
            item {
                Text(
                    text = "First Few Rows (~${previewData.totalEstimatedRows} total rows):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            itemsIndexed(previewData.previewRows.take(5)) { idx, row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(AliDimens.radiusSm)
                ) {
                    Text(
                        text = "Row #${idx + 1}: ${row.take(5).joinToString(" | ")}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AliDimens.minTouchTarget),
                shape = RoundedCornerShape(AliDimens.radiusMd)
            ) {
                Text("Next: Check Columns", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2: Check Columns
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step2CheckColumns(
    headers: List<String>,
    isRestoredMapping: Boolean,
    confidenceMap: Map<String, Int>,
    nameCol: Int, onNameChange: (Int) -> Unit,
    categoryCol: Int, onCategoryChange: (Int) -> Unit,
    brandCol: Int, onBrandChange: (Int) -> Unit,
    skuCol: Int, onSkuChange: (Int) -> Unit,
    barcodeCol: Int, onBarcodeChange: (Int) -> Unit,
    openingQtyCol: Int, onOpeningQtyChange: (Int) -> Unit,
    boxesCol: Int, onBoxesChange: (Int) -> Unit,
    purchaseCostCol: Int, onPurchaseCostChange: (Int) -> Unit,
    sellingPieceCol: Int, onSellingPieceChange: (Int) -> Unit,
    sellingPackCol: Int, onSellingPackChange: (Int) -> Unit,
    wholesaleCol: Int, onWholesaleChange: (Int) -> Unit,
    lowStockCol: Int, onLowStockChange: (Int) -> Unit,
    saveMapping: Boolean, onSaveMappingChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val headersWithNone = listOf("— Not mapped —") + headers

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRestoredMapping) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(AliDimens.radiusMd)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isRestoredMapping) Icons.Filled.Bookmark else Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        tint = if (isRestoredMapping) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isRestoredMapping) "Saved layout restored for this file" else "Columns automatically suggested",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Please verify the dropdowns below. Name is required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Required field
        item {
            ColumnMappingItem(
                label = "Product Name * (Required)",
                options = headersWithNone,
                selectedIndex = nameCol + 1,
                confidence = confidenceMap["name"],
                onSelect = { onNameChange(it - 1) },
                isRequired = true
            )
        }

        // Identification fields
        item {
            ColumnMappingItem(
                label = "Category",
                options = headersWithNone,
                selectedIndex = categoryCol + 1,
                confidence = confidenceMap["category"],
                onSelect = { onCategoryChange(it - 1) }
            )
        }
        item {
            ColumnMappingItem(
                label = "Brand",
                options = headersWithNone,
                selectedIndex = brandCol + 1,
                confidence = confidenceMap["brand"],
                onSelect = { onBrandChange(it - 1) }
            )
        }
        item {
            ColumnMappingItem(
                label = "SKU / Item Code",
                options = headersWithNone,
                selectedIndex = skuCol + 1,
                confidence = confidenceMap["sku"],
                onSelect = { onSkuChange(it - 1) }
            )
        }
        item {
            ColumnMappingItem(
                label = "Barcode",
                options = headersWithNone,
                selectedIndex = barcodeCol + 1,
                confidence = confidenceMap["barcode"],
                onSelect = { onBarcodeChange(it - 1) }
            )
        }

        // Stock quantity & boxes
        item {
            ColumnMappingItem(
                label = "Opening Stock (Pieces)",
                options = headersWithNone,
                selectedIndex = openingQtyCol + 1,
                confidence = confidenceMap["openingQty"],
                onSelect = { onOpeningQtyChange(it - 1) }
            )
        }
        item {
            ColumnMappingItem(
                label = "Boxes / Cartons (multiplies by pack size)",
                options = headersWithNone,
                selectedIndex = boxesCol + 1,
                confidence = confidenceMap["boxes"],
                onSelect = { onBoxesChange(it - 1) }
            )
        }

        // Pricing fields
        item {
            ColumnMappingItem(
                label = "Purchase Cost Price (₹)",
                options = headersWithNone,
                selectedIndex = purchaseCostCol + 1,
                confidence = confidenceMap["purchaseCost"],
                onSelect = { onPurchaseCostChange(it - 1) }
            )
        }
        item {
            ColumnMappingItem(
                label = "Selling Price Piece (₹)",
                options = headersWithNone,
                selectedIndex = sellingPieceCol + 1,
                confidence = confidenceMap["sellingPrice"],
                onSelect = { onSellingPieceChange(it - 1) }
            )
        }
        item {
            ColumnMappingItem(
                label = "Selling Price Pack (₹)",
                options = headersWithNone,
                selectedIndex = sellingPackCol + 1,
                confidence = null,
                onSelect = { onSellingPackChange(it - 1) }
            )
        }
        item {
            ColumnMappingItem(
                label = "Wholesale Price (₹)",
                options = headersWithNone,
                selectedIndex = wholesaleCol + 1,
                confidence = confidenceMap["wholesalePrice"],
                onSelect = { onWholesaleChange(it - 1) }
            )
        }
        item {
            ColumnMappingItem(
                label = "Low Stock Alert Threshold",
                options = headersWithNone,
                selectedIndex = lowStockCol + 1,
                confidence = confidenceMap["lowStock"],
                onSelect = { onLowStockChange(it - 1) }
            )
        }

        // Save mapping checkbox
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSaveMappingChange(!saveMapping) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = saveMapping, onCheckedChange = onSaveMappingChange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save this column mapping for next time",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(AliDimens.minTouchTarget),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = onNext,
                    enabled = nameCol >= 0,
                    modifier = Modifier
                        .weight(2f)
                        .height(AliDimens.minTouchTarget),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Text("Next: Review & Validate", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ColumnMappingItem(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    confidence: Int?,
    onSelect: (Int) -> Unit,
    isRequired: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(AliDimens.radiusMd)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isRequired && selectedIndex <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (confidence != null && selectedIndex > 0) {
                    ConfidenceBadge(confidence = confidence)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            ColumnDropdownSelector(
                label = "",
                options = options,
                selectedIndex = selectedIndex,
                onSelect = onSelect
            )
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Int) {
    val (bg, fg, text) = when {
        confidence >= 100 -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "100% Exact")
        confidence >= 90 -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "90% Synonym")
        confidence >= 80 -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "80% Match")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Manual")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AliDimens.radiusPill))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3: Review
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step3Review(
    isPlanning: Boolean,
    plannedRows: List<PlannedRow>,
    filter: String,
    onFilterChange: (String) -> Unit,
    onExportErrorReport: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    if (isPlanning) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Analysing catalog rows and checking conflicts...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    // Exact Long paise calculations
    val totalQty = plannedRows.filter { it.status != RowStatus.ERROR && it.status != RowStatus.SKIP_DUPLICATE }
        .sumOf { (it.parsedData?.quantity ?: 0).toLong() }
    val totalCostPaise = plannedRows.filter { it.status != RowStatus.ERROR && it.status != RowStatus.SKIP_DUPLICATE }
        .sumOf { ((it.parsedData?.quantity ?: 0).toLong()) * (it.parsedData?.costPaise ?: 0L) }

    val newCount = plannedRows.count { it.status == RowStatus.NEW }
    val updateCount = plannedRows.count { it.status == RowStatus.UPDATE }
    val skipCount = plannedRows.count { it.status == RowStatus.SKIP_DUPLICATE }
    val errorCount = plannedRows.count { it.status == RowStatus.ERROR }
    val warningCount = plannedRows.count { row -> row.issues.any { it.severity == Severity.WARNING } }

    val filteredRows = when (filter) {
        "NEW" -> plannedRows.filter { it.status == RowStatus.NEW }
        "UPDATE" -> plannedRows.filter { it.status == RowStatus.UPDATE }
        "SKIP" -> plannedRows.filter { it.status == RowStatus.SKIP_DUPLICATE }
        "ERROR" -> plannedRows.filter { it.status == RowStatus.ERROR }
        "WARNING" -> plannedRows.filter { row -> row.issues.any { it.severity == Severity.WARNING } }
        else -> plannedRows
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(AliDimens.radiusLg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "This will add $totalQty pcs worth ₹${CostCalculator.formatPaiseToRupees(totalCostPaise)} at cost",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$newCount new products • $updateCount updates • $skipCount skipped • $errorCount errors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Export error report button if any errors or warnings
        if (errorCount > 0 || warningCount > 0) {
            item {
                OutlinedButton(
                    onClick = onExportErrorReport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Error Report (.xlsx)")
                }
            }
        }

        // Filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val chips = listOf(
                    "ALL" to "All (${plannedRows.size})",
                    "NEW" to "New ($newCount)",
                    "UPDATE" to "Updates ($updateCount)",
                    "ERROR" to "Errors ($errorCount)",
                    "WARNING" to "Warnings ($warningCount)",
                    "SKIP" to "Skipped ($skipCount)"
                )
                items(chips) { (fKey, label) ->
                    FilterChip(
                        selected = filter == fKey,
                        onClick = { onFilterChange(fKey) },
                        label = { Text(label, fontSize = AliDimens.minChipTextSize) }
                    )
                }
            }
        }

        // Row cards
        itemsIndexed(filteredRows.take(50)) { _, row ->
            PlannedRowCard(row = row)
        }

        if (filteredRows.size > 50) {
            item {
                Text(
                    text = "Showing 50 of ${filteredRows.size} matching rows",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
        }

        // Navigation
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(AliDimens.minTouchTarget),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(2f)
                        .height(AliDimens.minTouchTarget),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Text("Next: Options", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun PlannedRowCard(row: PlannedRow) {
    val statusColors = LocalAliStatusColors.current
    val (statusBg, statusFg, statusText) = when (row.status) {
        RowStatus.NEW -> Triple(statusColors.paidBg, statusColors.paidFg, "NEW")
        RowStatus.UPDATE -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "UPDATE")
        RowStatus.SKIP_DUPLICATE -> Triple(statusColors.voidBg, statusColors.voidFg, "SKIPPED")
        RowStatus.ERROR -> Triple(statusColors.dueBg, statusColors.dueFg, "ERROR")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(AliDimens.radiusMd),
        border = if (row.status == RowStatus.ERROR) BorderStroke(1.dp, statusColors.dueFg) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Row #${row.rowNumber}: ${row.parsedData?.name ?: "(Unnamed)"}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AliDimens.radiusPill))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = statusText, color = statusFg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Match details
            if (row.matchedBy != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Matched existing catalog by ${row.matchedBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Price review
            row.parsedData?.let { d ->
                Spacer(modifier = Modifier.height(4.dp))
                val priceInfo = buildString {
                    append("Stock: ${d.quantity} pcs")
                    if (d.costPaise > 0L) append(" • Cost: ₹${CostCalculator.formatPaiseToRupees(d.costPaise)}")
                    if (d.sellingPiecePaise > 0L) {
                        if (row.oldSellingPiecePaise != null && row.oldSellingPiecePaise != d.sellingPiecePaise) {
                            append(" • Price: ₹${CostCalculator.formatPaiseToRupees(row.oldSellingPiecePaise)} -> ₹${CostCalculator.formatPaiseToRupees(d.sellingPiecePaise)}")
                        } else {
                            append(" • Price: ₹${CostCalculator.formatPaiseToRupees(d.sellingPiecePaise)}")
                        }
                    }
                }
                Text(text = priceInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Issues list
            if (row.issues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                row.issues.forEach { issue ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Icon(
                            if (issue.severity == Severity.ERROR) Icons.Filled.ErrorOutline else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (issue.severity == Severity.ERROR) MaterialTheme.colorScheme.error else Color(0xFFF57F17),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = issue.message,
                            fontSize = 11.sp,
                            color = if (issue.severity == Severity.ERROR) MaterialTheme.colorScheme.error else Color(0xFFF57F17)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4: Options
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step4Options(
    selectedMode: ImportMode,
    onModeSelect: (ImportMode) -> Unit,
    validRowsOnly: Boolean,
    onValidRowsChange: (Boolean) -> Unit,
    applyDefaultMargin: Boolean,
    onApplyMarginChange: (Boolean) -> Unit,
    marginPercent: String,
    onMarginPercentChange: (String) -> Unit,
    marginType: String,
    onMarginTypeChange: (String) -> Unit,
    generateBarcodes: Boolean,
    onGenerateBarcodesChange: (Boolean) -> Unit,
    plannedRows: List<PlannedRow>,
    isImporting: Boolean,
    onCommit: () -> Unit,
    onBack: () -> Unit
) {
    val errorRows = plannedRows.filter { it.status == RowStatus.ERROR }
    val willRollback = errorRows.isNotEmpty() && !validRowsOnly

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector
        item {
            Text(
                text = "Select Import Mode",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))

            val modes = listOf(
                ImportMode.ADD_STOCK to Pair("Add to stock (Default)", "Adds quantity to existing stock; updates prices"),
                ImportMode.NEW_ONLY to Pair("Add new items only", "Skips any product already present in catalog"),
                ImportMode.SET_STOCK to Pair("Set stock (Stock-take)", "Replaces current stock with spreadsheet counts"),
                ImportMode.PRICES_ONLY to Pair("Update prices only", "Updates costs and selling prices; stock stays unchanged"),
                ImportMode.DETAILS_ONLY to Pair("Update details only", "Updates name, category, brand, barcodes; no stock change")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                modes.forEach { (mode, pair) ->
                    val isSelected = selectedMode == mode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelect(mode) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(AliDimens.radiusMd),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { onModeSelect(mode) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(pair.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(pair.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Safety Switch: Valid Rows Only
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AliDimens.radiusMd)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Import valid rows only", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (validRowsOnly) "Skips invalid rows and imports good ones" else "Strict: Any error safely cancels the entire import",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = validRowsOnly, onCheckedChange = onValidRowsChange)
                }
            }
        }

        // Default Margin for missing prices
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AliDimens.radiusMd)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyDefaultMargin, onCheckedChange = onApplyMarginChange)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Calculate selling price if missing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (applyDefaultMargin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = marginPercent,
                                onValueChange = onMarginPercentChange,
                                label = { Text("Margin %") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(AliDimens.radiusSm)
                            )
                            ColumnDropdownSelector(
                                label = "Method",
                                options = listOf("Markup on cost", "Gross margin"),
                                selectedIndex = if (marginType == "MARKUP") 0 else 1,
                                onSelect = { idx -> onMarginTypeChange(if (idx == 0) "MARKUP" else "GROSS_MARGIN") },
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }
                }
            }
        }

        // Barcode Generation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AliDimens.radiusMd)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = generateBarcodes, onCheckedChange = onGenerateBarcodesChange)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Auto-generate shop barcodes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Assigns short 12-char barcodes (AE-P...) to items without one", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Warning banner if will rollback
        if (willRollback) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFC62828))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This file contains ${errorRows.size} errors. In all-or-nothing mode, the entire import will fail. Enable 'Import valid rows only' above to skip error rows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !isImporting,
                    modifier = Modifier
                        .weight(1f)
                        .height(AliDimens.minTouchTarget),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = onCommit,
                    enabled = !isImporting && !(willRollback),
                    modifier = Modifier
                        .weight(2f)
                        .height(AliDimens.minTouchTarget),
                    shape = RoundedCornerShape(AliDimens.radiusMd),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Committing...")
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Commit Import", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 5: Result
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step5Result(
    result: ImportResult?,
    plannedRows: List<PlannedRow>,
    onPrintLabels: () -> Unit,
    onDone: () -> Unit
) {
    val statusColors = LocalAliStatusColors.current
    val isSuccess = result?.isSuccess == true

    val newItemsWithBarcodes = plannedRows.filter {
        it.status == RowStatus.NEW && !it.parsedData?.barcode.isNullOrBlank()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = if (isSuccess) statusColors.paidFg else statusColors.dueFg,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isSuccess) "Import Completed Successfully!" else "Import Failed",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            if (result?.failureReason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.failureReason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColors.dueFg,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Metric breakdown
        if (result != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(title = "New", count = result.importedCount, color = statusColors.paidFg, modifier = Modifier.weight(1f))
                    MetricBox(title = "Updated", count = result.updatedCount, color = Color(0xFF1565C0), modifier = Modifier.weight(1f))
                    MetricBox(title = "Skipped", count = result.skippedDuplicatesCount, color = Color(0xFFE65100), modifier = Modifier.weight(1f))
                    MetricBox(title = "Errors", count = result.errors.size, color = statusColors.dueFg, modifier = Modifier.weight(1f))
                }
            }
        }

        // Print Labels Button
        if (isSuccess && newItemsWithBarcodes.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Print Labels for New Stock", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${newItemsWithBarcodes.size} new items have barcodes ready to print on A4 or thermal sticker sheets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onPrintLabels,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AliDimens.radiusSm)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate & Share Label Sheet")
                        }
                    }
                }
            }
        }

        // Updated Details List
        if (result != null && result.updatedDetails.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(AliDimens.radiusMd)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Updated Items:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        result.updatedDetails.take(10).forEach { detail ->
                            Text("• $detail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Done button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AliDimens.minTouchTarget),
                shape = RoundedCornerShape(AliDimens.radiusMd)
            ) {
                Text("Done (Back to Stock)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricBox(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(AliDimens.radiusMd)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnDropdownSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = if (selectedIndex in options.indices) options[selectedIndex] else "— Not mapped —"

    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(AliDimens.radiusSm),
                textStyle = MaterialTheme.typography.bodySmall
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { idx, opt ->
                    DropdownMenuItem(
                        text = { Text(opt, fontSize = 13.sp) },
                        onClick = {
                            onSelect(idx)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// TODO(next run): undo last import, purchase mode, multi-sheet

@Composable
fun ImportHistoryDialog(
    batches: List<com.alieducational.app.data.local.entity.ImportBatchEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import History", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (batches.isEmpty()) {
                Text(
                    text = "No import batches recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(batches) { batch ->
                        val dateStr = remember(batch.timestamp) {
                            java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                .format(java.util.Date(batch.timestamp))
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(AliDimens.radiusSm)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = batch.fileName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Imported: ${batch.importedCount} • Skipped: ${batch.skippedCount} • Total rows: ${batch.totalRows}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
