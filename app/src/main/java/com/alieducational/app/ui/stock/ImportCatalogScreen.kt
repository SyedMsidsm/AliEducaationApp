package com.alieducational.app.ui.stock

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.alieducational.app.data.service.ColumnMapping
import com.alieducational.app.ui.AliViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCatalogScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedSheetIndex by remember { mutableIntStateOf(0) }

    val previewData by viewModel.previewData.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    var nameColIndex by remember { mutableIntStateOf(0) }
    var categoryColIndex by remember { mutableIntStateOf(-1) }
    var brandColIndex by remember { mutableIntStateOf(-1) }
    var skuColIndex by remember { mutableIntStateOf(-1) }
    var barcodeColIndex by remember { mutableIntStateOf(-1) }
    var openingQtyColIndex by remember { mutableIntStateOf(-1) }
    var purchaseCostColIndex by remember { mutableIntStateOf(-1) }
    var sellingPieceColIndex by remember { mutableIntStateOf(-1) }
    var updateExisting by remember { mutableStateOf(false) }

    // Auto-detect headers whenever previewData changes
    LaunchedEffect(previewData) {
        val headers = previewData?.headers ?: return@LaunchedEffect
        headers.forEachIndexed { idx, rawHeader ->
            val h = rawHeader.lowercase().trim()
            when {
                nameColIndex <= 0 && (h.contains("name") || h.contains("item") || h.contains("product") || h.contains("title")) ->
                    nameColIndex = idx
                categoryColIndex == -1 && (h.contains("category") || h.contains("group") || h.contains("type")) ->
                    categoryColIndex = idx
                brandColIndex == -1 && (h.contains("brand") || h.contains("company") || h.contains("make")) ->
                    brandColIndex = idx
                skuColIndex == -1 && (h.contains("sku") || h.contains("code") || h.contains("item_code") || h.contains("product_code")) ->
                    skuColIndex = idx
                barcodeColIndex == -1 && (h.contains("barcode") || h.contains("upc") || h.contains("ean")) ->
                    barcodeColIndex = idx
                openingQtyColIndex == -1 && (h.contains("qty") || h.contains("quantity") || h.contains("stock") || h.contains("opening")) ->
                    openingQtyColIndex = idx
                purchaseCostColIndex == -1 && (h.contains("cost") || h.contains("purchase") || h.contains("buy")) ->
                    purchaseCostColIndex = idx
                sellingPieceColIndex == -1 && (h.contains("sell") || h.contains("mrp") || h.contains("rate") || h.contains("price")) ->
                    sellingPieceColIndex = idx
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val name = cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) it.getString(nameIndex) else "spreadsheet"
                } else "spreadsheet"
            } ?: "spreadsheet"

            selectedFileName = name
            selectedSheetIndex = 0
            viewModel.previewSpreadsheet(uri, name, 0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Product Catalog", fontWeight = FontWeight.Bold) },
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
                // File Selection Card
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Select Excel or CSV Spreadsheet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Accepts modern Excel (.xlsx), legacy Excel (.xls), and (.csv) files. Safe and crash-proof for up to 3,000 products.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
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
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose File from Phone")
                            }

                            if (selectedFileName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Selected File: $selectedFileName",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Error Message Card (if file invalid / encrypted / corrupt / cash ledger)
                if (previewData != null && previewData?.errorMessage != null) {
                    item {
                        val isCash = previewData?.isCashLedger == true
                        val cardBg = if (isCash) Color(0xFFF9A825).copy(alpha = 0.15f) else Color(0xFFE57373).copy(alpha = 0.15f)
                        val iconColor = if (isCash) Color(0xFFF9A825) else Color(0xFFE57373)
                        val titleText = if (isCash) "Cash In/Out Ledger Detected" else "Cannot Read Spreadsheet"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isCash) Icons.Filled.Warning else Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = iconColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(titleText, fontWeight = FontWeight.Bold, color = iconColor)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = previewData!!.errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isCash) "To import transactions from this file, please open the Cash In / Out Tracker from the Home screen."
                                    else "Tip: Ensure your file contains headers like 'Product Name', 'Category', 'Selling Price', 'Stock Qty', and is not password protected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Multiple Worksheets Selector
                if (previewData != null && previewData!!.sheetNames.size > 1 && !previewData!!.isCashLedger) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Workbook Has Multiple Sheets", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Select which worksheet contains your product catalog:", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))

                                ColumnSelectorDropdown(
                                    label = "Active Sheet",
                                    options = previewData!!.sheetNames,
                                    selectedIndex = selectedSheetIndex,
                                    onSelect = { newIdx ->
                                        selectedSheetIndex = newIdx
                                        val uri = selectedUri
                                        if (uri != null) {
                                            viewModel.previewSpreadsheet(uri, selectedFileName, newIdx)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Preview & Column Mapping Section (Only show if valid product stock spreadsheet)
                if (previewData != null && previewData?.headers?.isNotEmpty() == true && !previewData!!.isCashLedger) {
                    val preview = previewData!!
                    val headersWithNone = listOf("None / Do Not Import") + preview.headers

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Map Spreadsheet Columns",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "We automatically detected matching columns. Please confirm or adjust:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                ColumnSelectorDropdown(
                                    label = "Product Name *",
                                    options = headersWithNone,
                                    selectedIndex = nameColIndex + 1,
                                    onSelect = { nameColIndex = it - 1 }
                                )

                                ColumnSelectorDropdown(
                                    label = "Category",
                                    options = headersWithNone,
                                    selectedIndex = categoryColIndex + 1,
                                    onSelect = { categoryColIndex = it - 1 }
                                )

                                ColumnSelectorDropdown(
                                    label = "Brand",
                                    options = headersWithNone,
                                    selectedIndex = brandColIndex + 1,
                                    onSelect = { brandColIndex = it - 1 }
                                )

                                ColumnSelectorDropdown(
                                    label = "SKU / Item Code",
                                    options = headersWithNone,
                                    selectedIndex = skuColIndex + 1,
                                    onSelect = { skuColIndex = it - 1 }
                                )

                                ColumnSelectorDropdown(
                                    label = "Barcode",
                                    options = headersWithNone,
                                    selectedIndex = barcodeColIndex + 1,
                                    onSelect = { barcodeColIndex = it - 1 }
                                )

                                ColumnSelectorDropdown(
                                    label = "Opening Stock Quantity",
                                    options = headersWithNone,
                                    selectedIndex = openingQtyColIndex + 1,
                                    onSelect = { openingQtyColIndex = it - 1 }
                                )

                                ColumnSelectorDropdown(
                                    label = "Purchase Cost (₹)",
                                    options = headersWithNone,
                                    selectedIndex = purchaseCostColIndex + 1,
                                    onSelect = { purchaseCostColIndex = it - 1 }
                                )

                                ColumnSelectorDropdown(
                                    label = "Selling Price Piece (₹)",
                                    options = headersWithNone,
                                    selectedIndex = sellingPieceColIndex + 1,
                                    onSelect = { sellingPieceColIndex = it - 1 }
                                )
                            }
                        }
                    }

                    // Existing Products & Duplicates Policy Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "When Existing Item is Found",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Choose what happens if a spreadsheet row matches a product already in your shop (by SKU, barcode, or name):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Option 1: Skip
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { updateExisting = false },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!updateExisting) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (!updateExisting) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = !updateExisting,
                                            onClick = { updateExisting = false }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Skip existing items (Recommended)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Keep current catalog prices and stock untouched; only import newly found products.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Option 2: Update
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { updateExisting = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (updateExisting) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (updateExisting) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = updateExisting,
                                            onClick = { updateExisting = true }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Update existing items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Update prices and moving cost; add incoming quantity to current sellable stock.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preview Rows
                    item {
                        Text(
                            text = "Sample Rows (${preview.previewRows.size} of ~${preview.totalEstimatedRows} total):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    itemsIndexed(preview.previewRows) { idx, row ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Row #${idx + 1}: ${row.joinToString(" | ")}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Execute Import Button with Loading State
                    item {
                        Button(
                            onClick = {
                                val uri = selectedUri ?: return@Button
                                val mapping = ColumnMapping(
                                    nameIndex = nameColIndex,
                                    categoryIndex = categoryColIndex,
                                    brandIndex = brandColIndex,
                                    skuIndex = skuColIndex,
                                    barcodeIndex = barcodeColIndex,
                                    openingQtyIndex = openingQtyColIndex,
                                    purchaseCostIndex = purchaseCostColIndex,
                                    sellingPricePieceIndex = sellingPieceColIndex
                                )
                                viewModel.executeImport(
                                    uri = uri,
                                    fileName = selectedFileName,
                                    sheetIndex = selectedSheetIndex,
                                    mapping = mapping,
                                    updateExisting = updateExisting
                                )
                            },
                            enabled = nameColIndex >= 0 && !isImporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Importing Products...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Filled.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Catalog Import", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Import Result Notice with Detailed Breakdown
                if (importResult != null) {
                    val res = importResult!!
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (res.isSuccess) Color(0xFF75B890).copy(alpha = 0.2f) else Color(0xFFE57373).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (res.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (res.isSuccess) Color(0xFF75B890) else Color(0xFFE57373),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            if (res.isSuccess) "Import Completed!" else "Import Issue",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Total rows: ${res.totalRead}. New products imported: ${res.importedCount}.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                if (res.updatedCount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Updated ${res.updatedCount} existing products with spreadsheet prices and stock:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF43A047)
                                    )
                                    res.updatedDetails.forEach { detail ->
                                        Text("• $detail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (res.skippedDuplicatesCount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Skipped ${res.skippedDuplicatesCount} existing products (catalog kept untouched):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFE4B765)
                                    )
                                    res.skippedDetails.forEach { detail ->
                                        Text("• $detail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (res.errors.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Issues noted (${res.errors.size}):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFE57373)
                                    )
                                    res.errors.take(5).forEach { err ->
                                        Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnSelectorDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = if (selectedIndex in options.indices) options[selectedIndex] else "None / Do Not Import"

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
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
                shape = RoundedCornerShape(8.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { idx, opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
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
