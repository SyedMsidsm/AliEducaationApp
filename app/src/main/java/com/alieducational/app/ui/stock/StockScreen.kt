package com.alieducational.app.ui.stock

import android.Manifest
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.material3.Checkbox
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.alieducational.app.data.service.BarcodeGeneratorService
import androidx.compose.ui.text.input.KeyboardCapitalization
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.SupplierEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import com.alieducational.app.ui.billing.BarcodeAnalyzer
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun StockScreen(
    viewModel: AliViewModel,
    onNavigateToReceiveStock: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToRemoveStock: () -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }
    val allProducts by viewModel.allProducts.collectAsState()
    val allSuppliers by viewModel.allSuppliers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Low Stock"
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showBarcodeLabelExport by remember { mutableStateOf<ProductEntity?>(null) }
    val barcodeService = remember { BarcodeGeneratorService() }

    val widgetAction by viewModel.widgetActionTrigger.collectAsState()
    androidx.compose.runtime.LaunchedEffect(widgetAction) {
        if (widgetAction == com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_ADD_STOCK) {
            showAddDialog = true
            viewModel.consumeWidgetAction()
        }
    }

    val filteredProducts = allProducts.filter { product ->
        val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                (product.barcode?.contains(searchQuery, ignoreCase = true) == true) ||
                (product.sku?.contains(searchQuery, ignoreCase = true) == true) ||
                (product.brand?.contains(searchQuery, ignoreCase = true) == true) ||
                product.category.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Low Stock" -> product.currentSellableStock <= product.lowStockThreshold
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            androidx.compose.material3.OutlinedButton(onClick = onNavigateToReceiveStock, modifier = Modifier.fillMaxWidth()) {
                Text("Receive supplier invoice · multiple items")
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Integrated Top Action Buttons: Add Stock & Product + Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Add Stock & Product", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToImport,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Import Excel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search product name, barcode, brand...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text("All (${allProducts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    val lowStockCount = allProducts.count { it.currentSellableStock <= it.lowStockThreshold }
                    FilterChip(
                        selected = selectedFilter == "Low Stock",
                        onClick = { selectedFilter = "Low Stock" },
                        label = { Text("Low Stock ($lowStockCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isBlank()) "No products in inventory" else "No matching products found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Add Stock & Product' above to create one",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductItemCard(
                            product = product,
                            onClick = { selectedProductForEdit = product }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Integrated Add / Edit Product & Stock Dialog
    if (showAddDialog || selectedProductForEdit != null) {
        IntegratedAddStockProductDialog(
            existingProduct = selectedProductForEdit,
            suppliers = allSuppliers,
            onResolveBarcode = { code, done -> viewModel.lookupBarcode(code) { product, unit, count -> done(product, unit ?: "PIECE", count ?: 1) } },
            onAddSupplier = { newSup, onCreated ->
                viewModel.addSupplier(newSup) {
                    onCreated(newSup)
                }
            },
            onDismiss = {
                showAddDialog = false
                selectedProductForEdit = null
            },
            onGenerateBarcode = { prodId, onDone ->
                viewModel.generateShopBarcode(prodId, "PIECE", 1) { entity ->
                    onDone(entity.barcode)
                }
            },
            onExportLabels = { prod ->
                showBarcodeLabelExport = prod
            },
            onSave = { existingId, name, barcode, category, brand, packSize, boxesBrought, loosePieces, costPaise, sellingPaise, wholesalePaise, lowStock, supplierName, billNo, transportPaise, isPaidNow, billPaymentMethod ->
                viewModel.addOrUpdateProductWithStock(
                    existingId = existingId,
                    name = name,
                    barcode = barcode,
                    category = category,
                    brand = brand,
                    packSize = packSize,
                    boxesBrought = boxesBrought,
                    loosePiecesBrought = loosePieces,
                    costPerPiecePaise = costPaise,
                    sellingPricePiecePaise = sellingPaise,
                    wholesalePricePiecePaise = wholesalePaise,
                    lowStockThreshold = lowStock,
                    supplierName = supplierName,
                    billNo = billNo,
                    transportChargesPaise = transportPaise,
                    isPaidNow = isPaidNow,
                    paymentMethod = billPaymentMethod
                )
                showAddDialog = false
                selectedProductForEdit = null
            }
        )
    }

    // ── BARCODE LABEL EXPORT DIALOG (Section 16) ───────────────────
    showBarcodeLabelExport?.let { exportProd ->
        var exportBarcode by remember(exportProd.id) { mutableStateOf(exportProd.barcode ?: "") }
        var includePriceOnLabel by remember { mutableStateOf(true) }
        var copiesCountStr by remember { mutableStateOf("24") }
        var selectedUnitLabel by remember { mutableStateOf("Piece") }
        var labelPrice by remember { mutableStateOf(exportProd.sellingPricePiecePaise) }
        val exportContext = LocalContext.current

        val barcodeBitmap = remember(exportBarcode) {
            try {
                if (exportBarcode.isNotBlank()) barcodeService.generateBarcodeBitmap(exportBarcode, 360, 100) else null
            } catch (e: Exception) {
                android.util.Log.w("StockScreen", "Failed to generate barcode preview bitmap: ${e.message}", e)
                null
            }
        }

        AlertDialog(
            onDismissRequest = { showBarcodeLabelExport = null },
            title = { Text("Print / Export Barcode Labels", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(exportProd.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (exportProd.packSize > 1 && exportProd.barcode?.startsWith("AE-P") == true) {
                        Row {
                            TextButton(onClick = { exportBarcode = exportProd.barcode ?: ""; selectedUnitLabel = "Piece"; labelPrice = exportProd.sellingPricePiecePaise }) { Text("Piece") }
                            TextButton(onClick = { viewModel.generateShopBarcode(exportProd.id, "PACK", exportProd.packSize) { b -> exportBarcode = b.barcode; selectedUnitLabel = "Pack of ${b.packQuantity}"; labelPrice = exportProd.sellingPricePackPaise } }) { Text("Pack of ${exportProd.packSize}") }
                        }
                    }
                    Text("Barcode: $exportBarcode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                    if (barcodeBitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = barcodeBitmap.asImageBitmap(),
                                    contentDescription = "Barcode Preview",
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includePriceOnLabel,
                            onCheckedChange = { includePriceOnLabel = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Include Price: ${CostCalculator.formatPaiseToRupees(labelPrice)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    OutlinedTextField(
                        value = copiesCountStr,
                        onValueChange = { copiesCountStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Number of Copies (1 - 100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val copies = (copiesCountStr.toIntOrNull() ?: 24).coerceIn(1, 100)
                        val price = if (includePriceOnLabel) labelPrice else null
                        try {
                            val pdfFile = barcodeService.generateLabelsPdf(
                                context = exportContext,
                                productName = exportProd.name,
                                barcode = exportBarcode,
                                unitLabel = selectedUnitLabel,
                                pricePaise = price,
                                copies = copies
                            )
                            val uri = FileProvider.getUriForFile(
                                exportContext,
                                "${exportContext.packageName}.fileprovider",
                                pdfFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            exportContext.startActivity(Intent.createChooser(shareIntent, "Share Barcode Labels PDF"))
                            showBarcodeLabelExport = null
                        } catch (e: Exception) {
                            android.util.Log.w("StockScreen", "Error creating barcode label PDF", e)
                            android.widget.Toast.makeText(exportContext, "Failed to create barcode label PDF. Please retry.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Export / Share PDF")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBarcodeLabelExport = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    val isLowStock = product.currentSellableStock <= product.lowStockThreshold
    val marginPct = if (product.sellingPricePiecePaise > 0L && product.sellingPricePiecePaise > product.movingAvgCostPaise) {
        ((product.sellingPricePiecePaise - product.movingAvgCostPaise).toDouble() / product.sellingPricePiecePaise.toDouble()) * 100.0
    } else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val subtitle = buildList {
                        if (!product.brand.isNullOrBlank()) add(product.brand)
                        add(product.category)
                        if (!product.barcode.isNullOrBlank()) add("Barcode: ${product.barcode}")
                        else if (!product.sku.isNullOrBlank()) add("SKU: ${product.sku}")
                    }.joinToString(" • ")
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${product.currentSellableStock} pcs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    if (isLowStock) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFE4B765)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Low stock",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE4B765)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing, Wholesale and Margin details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selling: ${CostCalculator.formatPaiseToRupees(product.sellingPricePiecePaise)}/pc",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (product.wholesalePricePiecePaise > 0L) {
                        Text(
                            text = "Wholesale: ${CostCalculator.formatPaiseToRupees(product.wholesalePricePiecePaise)}/pc",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (marginPct > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF75B890).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f%% Margin", marginPct),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF75B890)
                            )
                        }
                    }
                    Text(
                        text = "Cost: ${CostCalculator.formatPaiseToRupees(product.movingAvgCostPaise)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegratedAddStockProductDialog(
    existingProduct: ProductEntity?,
    suppliers: List<SupplierEntity>,
    onResolveBarcode: (String, (ProductEntity?, String, Int) -> Unit) -> Unit = { _, done -> done(null, "PIECE", 1) },
    onAddSupplier: (SupplierEntity, (SupplierEntity) -> Unit) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onGenerateBarcode: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onExportLabels: (ProductEntity) -> Unit = {},
    onSave: (
        existingId: String?,
        name: String,
        barcode: String?,
        category: String,
        brand: String?,
        packSize: Int,
        boxesBrought: Int,
        loosePieces: Int,
        costPaise: Long,
        sellingPaise: Long,
        wholesalePaise: Long,
        lowStock: Int,
        supplierName: String?,
        billNo: String?,
        transportPaise: Long,
        isPaidNow: Boolean,
        paymentMethod: String
    ) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var scannedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var scannedQuantity by remember { mutableStateOf("1") }
    var scannedUnit by remember { mutableStateOf("PIECE") }
    var scannedPackSize by remember { mutableStateOf(1) }
    var confirmScan by remember { mutableStateOf(false) }
    var supplierName by remember { mutableStateOf("") }
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierMenuExpanded by remember { mutableStateOf(false) }
    var showAddSupplierInlineDialog by remember { mutableStateOf(false) }
    var billNo by remember { mutableStateOf("") }
    var transportChargesStr by remember { mutableStateOf("0") }

    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var barcode by remember { mutableStateOf(existingProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(existingProduct?.category ?: "Stationery") }
    var brand by remember { mutableStateOf(existingProduct?.brand ?: "") }

    var piecesStr by remember {
        mutableStateOf("0")
    }

    var costPerPieceStr by remember {
        mutableStateOf(if (existingProduct != null && existingProduct.movingAvgCostPaise > 0L) (existingProduct.movingAvgCostPaise / 100.0).toString() else "0")
    }

    var sellingPieceStr by remember {
        mutableStateOf(if (existingProduct != null && existingProduct.sellingPricePiecePaise > 0L) (existingProduct.sellingPricePiecePaise / 100.0).toString() else "0")
    }

    var wholesalePieceStr by remember {
        mutableStateOf(if (existingProduct != null && existingProduct.wholesalePricePiecePaise > 0L) (existingProduct.wholesalePricePiecePaise / 100.0).toString() else "")
    }

    var pricingMode by remember { mutableStateOf(CostCalculator.PricingMode.GROSS_MARGIN) }
    var marginPercentStr by remember {
        val cost = (existingProduct?.movingAvgCostPaise ?: 0L) / 100.0
        val selling = (existingProduct?.sellingPricePiecePaise ?: 0L) / 100.0
        val initialMargin = if (selling > 0 && selling > cost) ((selling - cost) / selling) * 100.0 else 0.0
        mutableStateOf(if (initialMargin > 0) String.format(Locale.US, "%.1f", initialMargin) else "0")
    }

    var lowStockStr by remember { mutableStateOf(existingProduct?.lowStockThreshold?.toString() ?: "5") }

    var isPaidNow by remember { mutableStateOf(false) }
    var billPaymentMethod by remember { mutableStateOf("CASH") }

    // Camera Barcode Scanner State
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            showBarcodeScanner = true
        }
    }

    // Computations
    val packSize = scannedProduct?.packSize ?: existingProduct?.packSize ?: 1
    val totalPiecesAdded = piecesStr.replace(",", "").trim().toIntOrNull() ?: 0
    val boxesBrought = 0
    val loosePieces = totalPiecesAdded

    val baseCost = costPerPieceStr.replace(",", "").trim().toDoubleOrNull() ?: 0.0
    val transportTotal = transportChargesStr.replace(",", "").trim().toDoubleOrNull() ?: 0.0
    val transportPerPiece = if (totalPiecesAdded > 0 && transportTotal > 0.0) {
        transportTotal / totalPiecesAdded
    } else 0.0
    val landedCost = baseCost + transportPerPiece

    val selling = sellingPieceStr.replace(",", "").trim().toDoubleOrNull() ?: 0.0
    androidx.compose.runtime.LaunchedEffect(landedCost, pricingMode) {
        val percent = CostCalculator.calculatePercentageFromPrice((landedCost * 100).toLong(), (selling * 100).toLong(), pricingMode)
        marginPercentStr = percent?.let { String.format(Locale.US, "%.1f", it) } ?: ""
    }
    val profitBeforeTransport = selling - baseCost
    val profitAfterTransport = selling - landedCost

    val calculatedMarginPct = if (selling > 0.0) {
        (profitAfterTransport / selling) * 100.0
    } else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(if (existingProduct != null) "Edit Product & Stock" else "Add Stock & Product", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Product Name *
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    placeholder = { Text("Product Name") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Barcode (Optional) with working Camera Scanner button
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode (Optional)") },
                    placeholder = { Text("Scan or enter barcode") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    showBarcodeScanner = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingProduct != null) {
                    if (barcode.isBlank()) {
                        Button(
                            onClick = {
                                onGenerateBarcode(existingProduct.id) { generated ->
                                    if (generated.isNotBlank()) barcode = generated
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Shop Barcode (Code 128)")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onExportLabels(existingProduct.copy(barcode = barcode))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print / Export Barcode Labels")
                        }
                    }
                }

                // 3. Category & Brand
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Pcs & Low Stock Alert
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = piecesStr,
                        onValueChange = { piecesStr = it },
                        label = { Text("Pcs") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lowStockStr,
                        onValueChange = { lowStockStr = it },
                        label = { Text("Low Stock Alert") },
                        placeholder = { Text("5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 5. Cost & Selling Price
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPerPieceStr,
                        onValueChange = {
                            costPerPieceStr = it
                            val c = it.toDoubleOrNull() ?: 0.0
                            val effCost = c + transportPerPiece
                            val s = sellingPieceStr.toDoubleOrNull() ?: 0.0
                            if (s > 0 && s > effCost) {
                                marginPercentStr = String.format(Locale.US, "%.1f", ((s - effCost) / s) * 100.0)
                            }
                        },
                        label = { Text("Base Cost (₹/pc) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sellingPieceStr,
                        onValueChange = {
                            sellingPieceStr = it
                            val s = it.toDoubleOrNull() ?: 0.0
                            marginPercentStr = CostCalculator.calculatePercentageFromPrice((landedCost * 100).toLong(), (s * 100).toLong(), pricingMode)?.let { value -> String.format(Locale.US, "%.1f", value) } ?: ""
                        },
                        label = { Text("Selling (₹/pc) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = pricingMode == CostCalculator.PricingMode.GROSS_MARGIN,
                        onClick = { pricingMode = CostCalculator.PricingMode.GROSS_MARGIN }, label = { Text("Gross margin") })
                    FilterChip(selected = pricingMode == CostCalculator.PricingMode.MARKUP,
                        onClick = { pricingMode = CostCalculator.PricingMode.MARKUP }, label = { Text("Markup on cost") })
                }
                // 6. Selected pricing percentage and wholesale price
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = marginPercentStr,
                        onValueChange = {
                            marginPercentStr = it
                            val m = it.toDoubleOrNull() ?: 0.0
                            CostCalculator.calculatePriceFromPercentage((landedCost * 100).toLong(), m, pricingMode)?.let { price ->
                                sellingPieceStr = String.format(Locale.US, "%.2f", price / 100.0)
                            }
                        },
                        label = { Text(if (pricingMode == CostCalculator.PricingMode.GROSS_MARGIN) "Margin (%)" else "Markup (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = wholesalePieceStr,
                        onValueChange = { wholesalePieceStr = it },
                        label = { Text("Wholesale", maxLines = 1) },
                        placeholder = { Text("Optional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 7. Supplier Selector (Full Width)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Supplier",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = supplierMenuExpanded,
                        onExpandedChange = { supplierMenuExpanded = !supplierMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = supplierName,
                            onValueChange = {
                                supplierName = it
                                supplierMenuExpanded = true
                            },
                            label = { Text("Search or select supplier") },
                            placeholder = { Text("Select supplier from directory") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierMenuExpanded) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        val matchingSuppliers = remember(suppliers, supplierName) {
                            if (supplierName.isBlank()) suppliers
                            else suppliers.filter { it.name.contains(supplierName, ignoreCase = true) }
                        }
                        ExposedDropdownMenu(
                            expanded = supplierMenuExpanded,
                            onDismissRequest = { supplierMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Add new supplier", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    supplierMenuExpanded = false
                                    showAddSupplierInlineDialog = true
                                }
                            )
                            HorizontalDivider()
                            matchingSuppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(sup.name, fontWeight = FontWeight.SemiBold)
                                            if (!sup.phone.isNullOrBlank()) {
                                                Text(sup.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedSupplier = sup
                                        supplierName = sup.name
                                        supplierMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Bill No. (Dedicated Row)
                OutlinedTextField(
                    value = billNo,
                    onValueChange = { billNo = it },
                    label = { Text("Bill / Invoice No.") },
                    placeholder = { Text("Optional invoice number") },
                    leadingIcon = { Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 8. Transport Charges
                OutlinedTextField(
                    value = transportChargesStr,
                    onValueChange = { transportChargesStr = it },
                    label = { Text("Transport Charges (₹)") },
                    placeholder = { Text("0") },
                    leadingIcon = { Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Supplier Bill Payment Status
                if (supplierName.isNotBlank() || billNo.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Supplier Bill Payment",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isPaidNow,
                                onClick = { isPaidNow = false },
                                label = { Text("Unpaid (Add to Dues)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isPaidNow,
                                onClick = { isPaidNow = true },
                                label = { Text("Paid Now") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (isPaidNow) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = billPaymentMethod == "CASH",
                                    onClick = { billPaymentMethod = "CASH" },
                                    label = { Text("Cash") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = billPaymentMethod == "UPI",
                                    onClick = { billPaymentMethod = "UPI" },
                                    label = { Text("UPI / GPay") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = billPaymentMethod == "BANK",
                                    onClick = { billPaymentMethod = "BANK" },
                                    label = { Text("Bank") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 9. Profit After Transport Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Profit after transport",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (selling > 0) {
                            if (transportTotal > 0.0 && totalPiecesAdded > 0) {
                                Text(
                                    text = "Profit after transport: ${CostCalculator.formatPaiseToRupees((profitAfterTransport * 100).toLong())}/pc (${String.format(Locale.US, "%.1f", calculatedMarginPct)}% margin)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (profitAfterTransport >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                                )
                                Text(
                                    text = "Before transport was: ${CostCalculator.formatPaiseToRupees((profitBeforeTransport * 100).toLong())}/pc • Landed Cost: ${CostCalculator.formatPaiseToRupees((landedCost * 100).toLong())}/pc",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Profit per piece: ${CostCalculator.formatPaiseToRupees((profitAfterTransport * 100).toLong())} (${String.format(Locale.US, "%.1f", calculatedMarginPct)}% margin)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (profitAfterTransport >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val costPaise = CostCalculator.parseRupeesToPaise(costPerPieceStr)
                    val transportPaise = CostCalculator.parseRupeesToPaise(transportChargesStr.ifBlank { "0" })
                    val sellingPaise = CostCalculator.parseRupeesToPaise(sellingPieceStr)
                    val wholesalePaise = CostCalculator.parseRupeesToPaise(wholesalePieceStr.ifBlank { "0" })
                    val lowStock = lowStockStr.replace(",", "").trim().toIntOrNull()
                    val quantity = piecesStr.replace(",", "").trim().toIntOrNull()
                    if (costPaise == null || transportPaise == null || sellingPaise == null || wholesalePaise == null || lowStock == null || lowStock < 0 || quantity == null || quantity < 0) {
                        android.widget.Toast.makeText(context, "Enter valid nonnegative amounts (up to 2 decimals) and whole quantities", android.widget.Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    onSave(
                        scannedProduct?.id ?: existingProduct?.id,
                        name.trim(),
                        barcode.trim().ifBlank { null },
                        category.trim().ifBlank { "Stationery" },
                        brand.trim().ifBlank { null },
                        packSize,
                        boxesBrought,
                        loosePieces,
                        costPaise,
                        sellingPaise,
                        wholesalePaise,
                        lowStock,
                        supplierName.trim().ifBlank { null },
                        billNo.trim().ifBlank { null },
                        transportPaise,
                        isPaidNow,
                        billPaymentMethod
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (confirmScan) AlertDialog(onDismissRequest = { confirmScan = false }, title = { Text("Quantity received") },
        text = { Column { Text(if (scannedUnit == "PACK") "Packs of $scannedPackSize" else "Pieces"); OutlinedTextField(value = scannedQuantity, onValueChange = { scannedQuantity = it.filter(Char::isDigit) }, label = { Text("Quantity") }) } },
        confirmButton = { TextButton(onClick = { val qty = scannedQuantity.toIntOrNull() ?: 0; if (qty > 0 && qty.toLong() * scannedPackSize <= Int.MAX_VALUE) { piecesStr = (qty * if (scannedUnit == "PACK") scannedPackSize else 1).toString(); confirmScan = false } }) { Text("Use quantity") } },
        dismissButton = { TextButton(onClick = { confirmScan = false }) { Text("Cancel") } })
    // Camera Barcode Scanner Dialog
    if (showBarcodeScanner) {
        Dialog(onDismissRequest = { showBarcodeScanner = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Scan Barcode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                val cameraExecutor = Executors.newSingleThreadExecutor()

                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val imageAnalyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { analysis ->
                                            analysis.setAnalyzer(
                                                cameraExecutor,
                                                BarcodeAnalyzer(
                                                    onBarcodeDetected = { scannedCode, _ ->
                                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                            if (showBarcodeScanner) {
                                                                showBarcodeScanner = false
                                                                onResolveBarcode(scannedCode) { product, unit, count ->
                                                                    scannedProduct = product; scannedUnit = unit; scannedPackSize = count
                                                                    barcode = product?.barcode ?: scannedCode
                                                                    if (product != null) { name = product.name; category = product.category; brand = product.brand ?: ""; costPerPieceStr = (product.movingAvgCostPaise / 100.0).toString(); sellingPieceStr = (product.sellingPricePiecePaise / 100.0).toString() }
                                                                    confirmScan = true
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalyzer
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.w("StockScreen", "Failed to bind camera preview: ${e.message}", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 200.dp, height = 110.dp)
                                .align(Alignment.Center)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        )
                    }
                    Button(
                        onClick = { showBarcodeScanner = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
