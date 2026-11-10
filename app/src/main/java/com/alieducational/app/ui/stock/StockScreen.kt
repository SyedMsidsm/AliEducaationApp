package com.alieducational.app.ui.stock

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.text.input.ImeAction
import com.alieducational.app.ui.theme.AliDimens
import com.alieducational.app.ui.theme.LocalAliStatusColors



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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.DropdownMenu
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val allSuppliers by viewModel.allSuppliers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Low Stock", "Archived"
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showMultiBarcodeExportDialog by remember { mutableStateOf(false) }
    var preSelectedBarcodeProducts by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
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
            "Low Stock" -> !product.isArchived && product.currentSellableStock <= product.lowStockThreshold
            "Archived" -> product.isArchived
            else -> !product.isArchived
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

            // Integrated Top Action Buttons: Add Stock & Product + Import + Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Stock", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onNavigateToImport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Import", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        preSelectedBarcodeProducts = emptyList()
                        showMultiBarcodeExportDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Labels", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
                    val activeCount = allProducts.count { !it.isArchived }
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text("All ($activeCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    val lowStockCount = allProducts.count { !it.isArchived && it.currentSellableStock <= it.lowStockThreshold }
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
                item {
                    val archivedCount = allProducts.count { it.isArchived }
                    FilterChip(
                        selected = selectedFilter == "Archived",
                        onClick = { selectedFilter = "Archived" },
                        label = { Text("Archived ($archivedCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            onClick = { selectedProductForEdit = product },
                            onPrintLabels = {
                                preSelectedBarcodeProducts = listOf(product)
                                showMultiBarcodeExportDialog = true
                            },
                            onArchiveOrDelete = { productToDelete = product },
                            onRestore = {
                                viewModel.unarchiveProduct(product.id) {
                                    android.widget.Toast.makeText(context, "Product '${product.name}' restored", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Product Archive/Delete Confirmation Dialog
    productToDelete?.let { prod ->
        val dialogContext = LocalContext.current
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Archive or Delete Product?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Product: ${prod.name}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (prod.currentSellableStock > 0 || prod.currentDamagedStock > 0) {
                        Text(
                            text = "• This product has physical stock (${prod.currentSellableStock} sellable, ${prod.currentDamagedStock} damaged).\n• It will be safely archived (preserving stock value, past bills and reports). To remove physical units, use Stock Adjustment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "• If this product has historical transactions (sales or purchases), it will be safely archived.\n• If it is completely unused with no history, it will be permanently deleted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeProduct(prod.id) { res ->
                            android.widget.Toast.makeText(dialogContext, res.message, android.widget.Toast.LENGTH_LONG).show()
                        }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Integrated Add / Edit Product & Stock Dialog
    if (showAddDialog || selectedProductForEdit != null) {
        IntegratedAddStockProductDialog(
            existingProduct = selectedProductForEdit,
            suppliers = allSuppliers,
            categories = remember(allProducts) { allProducts.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() },
            allProducts = allProducts,
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
                if (prodId.isNotBlank()) {
                    viewModel.generateShopBarcode(prodId, "PIECE", 1) { entity ->
                        onDone(entity.barcode)
                    }
                } else {
                    viewModel.suggestShopBarcode("PIECE", 1) { code ->
                        onDone(code)
                    }
                }
            },
            onExportLabels = { prod ->
                preSelectedBarcodeProducts = listOf(prod)
                showMultiBarcodeExportDialog = true
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

    // ── MULTI-PRODUCT BARCODE LABEL EXPORT DIALOG (Item 13) ─────────
    if (showMultiBarcodeExportDialog) {
        MultiBarcodeLabelExportDialog(
            allProducts = allProducts,
            preSelectedProducts = preSelectedBarcodeProducts,
            barcodeService = barcodeService,
            onDismiss = {
                showMultiBarcodeExportDialog = false
                preSelectedBarcodeProducts = emptyList()
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onPrintLabels: () -> Unit,
    onArchiveOrDelete: () -> Unit,
    onRestore: () -> Unit
) {
    val isLowStock = !product.isArchived && product.currentSellableStock <= product.lowStockThreshold
    val marginPct = if (product.sellingPricePiecePaise > 0L && product.sellingPricePiecePaise > product.movingAvgCostPaise) {
        ((product.sellingPricePiecePaise - product.movingAvgCostPaise).toDouble() / product.sellingPricePiecePaise.toDouble()) * 100.0
    } else 0.0
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isArchived)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (product.isArchived) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(AliDimens.radiusPill),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "Archived",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
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

                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Product options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Stock & Details") },
                                onClick = {
                                    menuExpanded = false
                                    onClick()
                                }
                            )
                            if (!product.barcode.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Print Barcode Labels") },
                                    onClick = {
                                        menuExpanded = false
                                        onPrintLabels()
                                    }
                                )
                            }
                            HorizontalDivider()
                            if (!product.isArchived) {
                                DropdownMenuItem(
                                    text = { Text("Archive / Delete Product", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        onArchiveOrDelete()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Restore Product", color = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        menuExpanded = false
                                        onRestore()
                                    }
                                )
                            }
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
    categories: List<String> = emptyList(),
    allProducts: List<ProductEntity> = emptyList(),
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
    val statusColors = LocalAliStatusColors.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isCrowded = configuration.screenWidthDp < 400 || density.fontScale > 1.1f

    // Initial state tracking for discard protection
    val initialName = existingProduct?.name ?: ""
    val initialBarcode = existingProduct?.barcode ?: ""
    val initialCategory = existingProduct?.category ?: "Stationery"
    val initialBrand = existingProduct?.brand ?: ""
    val initialCostStr = if (existingProduct != null && existingProduct.movingAvgCostPaise > 0L) {
        if (existingProduct.movingAvgCostPaise % 100L == 0L) (existingProduct.movingAvgCostPaise / 100L).toString()
        else String.format(Locale.US, "%.2f", existingProduct.movingAvgCostPaise / 100.0)
    } else ""
    val initialSellingStr = if (existingProduct != null && existingProduct.sellingPricePiecePaise > 0L) {
        if (existingProduct.sellingPricePiecePaise % 100L == 0L) (existingProduct.sellingPricePiecePaise / 100L).toString()
        else String.format(Locale.US, "%.2f", existingProduct.sellingPricePiecePaise / 100.0)
    } else ""
    val initialWholesaleStr = if (existingProduct != null && existingProduct.wholesalePricePiecePaise > 0L) {
        if (existingProduct.wholesalePricePiecePaise % 100L == 0L) (existingProduct.wholesalePricePiecePaise / 100L).toString()
        else String.format(Locale.US, "%.2f", existingProduct.wholesalePricePiecePaise / 100.0)
    } else ""
    val initialLowStockStr = existingProduct?.lowStockThreshold?.toString() ?: "5"

    var name by remember { mutableStateOf(initialName) }
    var barcode by remember { mutableStateOf(initialBarcode) }
    var printLabelsAfterSave by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(initialCategory) }
    var brand by remember { mutableStateOf(initialBrand) }

    var piecesStr by remember { mutableStateOf("") }
    var costPerPieceStr by remember { mutableStateOf(initialCostStr) }
    var sellingPieceStr by remember { mutableStateOf(initialSellingStr) }
    var wholesalePieceStr by remember { mutableStateOf(initialWholesaleStr) }
    var lowStockStr by remember { mutableStateOf(initialLowStockStr) }

    var pricingMode by remember { mutableStateOf(CostCalculator.PricingMode.GROSS_MARGIN) }
    var marginPercentStr by remember {
        val costP = CostCalculator.parseRupeesToPaise(initialCostStr) ?: 0L
        val sellP = CostCalculator.parseRupeesToPaise(initialSellingStr) ?: 0L
        val pct = CostCalculator.calculatePercentageFromPrice(costP, sellP, CostCalculator.PricingMode.GROSS_MARGIN)
        mutableStateOf(pct?.let { String.format(Locale.US, "%.1f", it) } ?: "")
    }

    var supplierName by remember { mutableStateOf("") }
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierMenuExpanded by remember { mutableStateOf(false) }
    var showAddSupplierInlineDialog by remember { mutableStateOf(false) }
    var billNo by remember { mutableStateOf("") }
    var transportChargesStr by remember { mutableStateOf("") }
    var isPaidNow by remember { mutableStateOf(false) }
    var billPaymentMethod by remember { mutableStateOf("CASH") }

    // Category dropdown & new category state
    var categoryExpanded by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    val availableCategories = remember(categories) {
        (categories + listOf("Stationery", "Books", "Uniform", "General", "Art Supplies", "Electronics"))
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // Scanned product helper state
    var scannedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var scannedQuantity by remember { mutableStateOf("1") }
    var scannedUnit by remember { mutableStateOf("PIECE") }
    var scannedPackSize by remember { mutableStateOf(1) }
    var confirmScan by remember { mutableStateOf(false) }

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

    // Auto-expand supplier bill section when quantity > 0
    val totalPiecesAdded = piecesStr.replace(",", "").trim().toIntOrNull() ?: 0
    var supplierSectionExpanded by remember {
        mutableStateOf(existingProduct == null || totalPiecesAdded > 0 || supplierName.isNotBlank() || billNo.isNotBlank())
    }
    LaunchedEffect(totalPiecesAdded) {
        if (totalPiecesAdded > 0) {
            supplierSectionExpanded = true
        }
    }

    // Exact Paise Math
    val packSize = scannedProduct?.packSize ?: existingProduct?.packSize ?: 1
    val boxesBrought = 0
    val loosePieces = totalPiecesAdded

    val baseCostPaise = CostCalculator.parseRupeesToPaise(costPerPieceStr.ifBlank { "0" }) ?: 0L
    val transportTotalPaise = CostCalculator.parseRupeesToPaise(transportChargesStr.ifBlank { "0" }) ?: 0L
    val transportPerPiecePaise = if (totalPiecesAdded > 0 && transportTotalPaise > 0L) {
        transportTotalPaise / totalPiecesAdded
    } else 0L
    val landedCostPaise = baseCostPaise + transportPerPiecePaise

    val sellingPaise = CostCalculator.parseRupeesToPaise(sellingPieceStr.ifBlank { "0" }) ?: 0L
    val wholesalePaise = CostCalculator.parseRupeesToPaise(wholesalePieceStr.ifBlank { "0" }) ?: 0L
    val lowStock = lowStockStr.replace(",", "").trim().toIntOrNull() ?: 5

    // Margin & Profit
    val profitPaise = sellingPaise - landedCostPaise
    val calculatedMarginPct = if (sellingPaise > 0L) {
        (profitPaise.toDouble() / sellingPaise.toDouble()) * 100.0
    } else 0.0

    val stockValuePaise = totalPiecesAdded.toLong() * landedCostPaise

    // Duplicate barcode check
    val conflictingProduct = remember(barcode, allProducts, existingProduct) {
        if (barcode.isNotBlank()) {
            allProducts.firstOrNull {
                it.barcode.equals(barcode.trim(), ignoreCase = true) &&
                        it.id != (existingProduct?.id ?: "")
            }
        } else null
    }

    // Discard Protection
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    fun hasChanges(): Boolean {
        return name != initialName ||
                barcode != initialBarcode ||
                category != initialCategory ||
                brand != initialBrand ||
                piecesStr.isNotBlank() ||
                costPerPieceStr != initialCostStr ||
                sellingPieceStr != initialSellingStr ||
                wholesalePieceStr != initialWholesaleStr ||
                lowStockStr != initialLowStockStr ||
                supplierName.isNotBlank() ||
                billNo.isNotBlank() ||
                transportChargesStr.isNotBlank()
    }

    fun onAttemptDismiss() {
        if (hasChanges()) {
            showDiscardConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler {
        onAttemptDismiss()
    }

    fun doSave() {
        if (name.isBlank()) return
        val costP = CostCalculator.parseRupeesToPaise(costPerPieceStr.ifBlank { "0" })
        val transP = CostCalculator.parseRupeesToPaise(transportChargesStr.ifBlank { "0" })
        val sellP = CostCalculator.parseRupeesToPaise(sellingPieceStr.ifBlank { "0" })
        val wsP = CostCalculator.parseRupeesToPaise(wholesalePieceStr.ifBlank { "0" })
        val ls = lowStockStr.replace(",", "").trim().toIntOrNull()
        val qty = piecesStr.ifBlank { "0" }.replace(",", "").trim().toIntOrNull()

        if (costP == null || transP == null || sellP == null || wsP == null || ls == null || ls < 0 || qty == null || qty < 0) {
            android.widget.Toast.makeText(context, "Enter valid nonnegative amounts and whole quantities", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        onSave(
            scannedProduct?.id ?: existingProduct?.id,
            name.trim(),
            barcode.trim().ifBlank { null },
            category.trim().ifBlank { "Stationery" },
            brand.trim().ifBlank { null },
            packSize,
            boxesBrought,
            qty,
            costP,
            sellP,
            wsP,
            ls,
            supplierName.trim().ifBlank { null },
            billNo.trim().ifBlank { null },
            transP,
            isPaidNow,
            billPaymentMethod
        )

        if (printLabelsAfterSave && barcode.isNotBlank()) {
            val tempProd = ProductEntity(
                id = scannedProduct?.id ?: existingProduct?.id ?: java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                category = category.trim().ifBlank { "Stationery" },
                brand = brand.trim().ifBlank { null },
                barcode = barcode.trim().ifBlank { null },
                packSize = packSize,
                sellingPricePiecePaise = sellP,
                movingAvgCostPaise = costP
            )
            onExportLabels(tempProd)
        }
    }

    // Full-screen Dialog
    Dialog(
        onDismissRequest = { onAttemptDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Pinned Top Bar
                Surface(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AliDimens.spacingLg, vertical = AliDimens.spacingSm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onAttemptDismiss() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                            Spacer(modifier = Modifier.width(AliDimens.spacingXs))
                            Text(
                                text = if (existingProduct != null) "Edit Stock & Product" else "Add Stock & Product",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { doSave() },
                            enabled = name.isNotBlank() && baseCostPaise >= 0L && sellingPaise >= 0L,
                            shape = RoundedCornerShape(AliDimens.radiusSm)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider()

                // Scrollable Form Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = AliDimens.spacingLg),
                    contentPadding = PaddingValues(top = AliDimens.spacingMd, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(AliDimens.spacingLg)
                ) {
                    // ── SECTION 1: PRODUCT ─────────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(AliDimens.radiusMd)
                        ) {
                            Column(
                                modifier = Modifier.padding(AliDimens.spacingMd),
                                verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                            ) {
                                Text(
                                    "PRODUCT",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Product Name *") },
                                    placeholder = { Text("Product name") },
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = barcode,
                                    onValueChange = { barcode = it },
                                    label = { Text("Barcode (Optional)") },
                                    placeholder = { Text("Scan or enter barcode") },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (barcode.isBlank()) {
                                                IconButton(
                                                    onClick = {
                                                        onGenerateBarcode(existingProduct?.id ?: "") { generated ->
                                                            if (generated.isNotBlank()) barcode = generated
                                                        }
                                                    }
                                                ) {
                                                    Text("✨", fontSize = 16.sp)
                                                }
                                            }
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
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (conflictingProduct != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(AliDimens.radiusSm),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ Barcode already assigned to '${conflictingProduct.name}'",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(AliDimens.spacingSm)
                                        )
                                    }
                                }

                                if (barcode.isNotBlank() && barcode.startsWith("AE-P")) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(AliDimens.radiusSm)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val previewBitmap = remember(barcode) {
                                                try {
                                                    BarcodeGeneratorService().generateBarcodeBitmap(barcode, 360, 90)
                                                } catch (_: Exception) { null }
                                            }
                                            if (previewBitmap != null) {
                                                Image(
                                                    bitmap = previewBitmap.asImageBitmap(),
                                                    contentDescription = "Barcode Preview",
                                                    modifier = Modifier.height(48.dp).fillMaxWidth()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(barcode, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                if (barcode.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { printLabelsAfterSave = !printLabelsAfterSave }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Checkbox(
                                            checked = printLabelsAfterSave,
                                            onCheckedChange = { printLabelsAfterSave = it }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Print labels after saving", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                val categoryMenuContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                    ExposedDropdownMenuBox(
                                        expanded = categoryExpanded,
                                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                                        modifier = fieldMod
                                    ) {
                                        OutlinedTextField(
                                            value = category,
                                            onValueChange = { category = it },
                                            label = { Text("Category *") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = categoryExpanded,
                                            onDismissRequest = { categoryExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("New category...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                },
                                                onClick = {
                                                    categoryExpanded = false
                                                    showNewCategoryDialog = true
                                                }
                                            )
                                            HorizontalDivider()
                                            availableCategories.forEach { catName ->
                                                DropdownMenuItem(
                                                    text = { Text(catName) },
                                                    onClick = {
                                                        category = catName
                                                        categoryExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                val brandFieldContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                    OutlinedTextField(
                                        value = brand,
                                        onValueChange = { brand = it },
                                        label = { Text("Brand") },
                                        placeholder = { Text("Optional") },
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        modifier = fieldMod
                                    )
                                }

                                if (isCrowded) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                    ) {
                                        categoryMenuContent(Modifier.fillMaxWidth())
                                        brandFieldContent(Modifier.fillMaxWidth())
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                    ) {
                                        categoryMenuContent(Modifier.weight(1f))
                                        brandFieldContent(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 2: STOCK ───────────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(AliDimens.radiusMd)
                        ) {
                            Column(
                                modifier = Modifier.padding(AliDimens.spacingMd),
                                verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "STOCK",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (existingProduct != null) {
                                        Surface(
                                            shape = RoundedCornerShape(AliDimens.radiusPill),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "In stock now: ${existingProduct.currentSellableStock} pcs",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                val stockQtyContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                    OutlinedTextField(
                                        value = piecesStr,
                                        onValueChange = { piecesStr = it },
                                        label = { Text(if (existingProduct != null) "Add to stock (pcs)" else "Quantity received (pcs)") },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        modifier = fieldMod
                                    )
                                }
                                val lowStockContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                    OutlinedTextField(
                                        value = lowStockStr,
                                        onValueChange = { lowStockStr = it },
                                        label = { Text("Low-stock alert") },
                                        placeholder = { Text("5") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        modifier = fieldMod
                                    )
                                }
                                if (isCrowded) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                    ) {
                                        stockQtyContent(Modifier.fillMaxWidth())
                                        lowStockContent(Modifier.fillMaxWidth())
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                    ) {
                                        stockQtyContent(Modifier.weight(1f))
                                        lowStockContent(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 3: PRICING ─────────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(AliDimens.radiusMd)
                        ) {
                            Column(
                                modifier = Modifier.padding(AliDimens.spacingMd),
                                verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                            ) {
                                Text(
                                    "PRICING",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val costFieldContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                    OutlinedTextField(
                                        value = costPerPieceStr,
                                        onValueChange = {
                                            costPerPieceStr = it
                                            val c = CostCalculator.parseRupeesToPaise(it.ifBlank { "0" }) ?: 0L
                                            val effLanded = c + transportPerPiecePaise
                                            val s = CostCalculator.parseRupeesToPaise(sellingPieceStr.ifBlank { "0" }) ?: 0L
                                            if (s > 0L && s > effLanded) {
                                                val pct = CostCalculator.calculatePercentageFromPrice(effLanded, s, pricingMode)
                                                marginPercentStr = pct?.let { String.format(Locale.US, "%.1f", it) } ?: ""
                                            }
                                        },
                                        label = { Text("Cost price (₹/pc) *") },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        modifier = fieldMod
                                    )
                                }

                                val sellingFieldContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                    OutlinedTextField(
                                        value = sellingPieceStr,
                                        onValueChange = {
                                            sellingPieceStr = it
                                            val s = CostCalculator.parseRupeesToPaise(it.ifBlank { "0" }) ?: 0L
                                            if (s > 0L && landedCostPaise > 0L) {
                                                val pct = CostCalculator.calculatePercentageFromPrice(landedCostPaise, s, pricingMode)
                                                marginPercentStr = pct?.let { String.format(Locale.US, "%.1f", it) } ?: ""
                                            }
                                        },
                                        label = { Text("Selling price (₹/pc) *") },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        modifier = fieldMod
                                    )
                                }

                                if (isCrowded) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                    ) {
                                        costFieldContent(Modifier.fillMaxWidth())
                                        sellingFieldContent(Modifier.fillMaxWidth())
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                    ) {
                                        costFieldContent(Modifier.weight(1f))
                                        sellingFieldContent(Modifier.weight(1f))
                                    }
                                }

                                val wholesaleFieldContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                    OutlinedTextField(
                                        value = wholesalePieceStr,
                                        onValueChange = { wholesalePieceStr = it },
                                        label = { Text("Wholesale (₹/pc)") },
                                        placeholder = { Text("Optional") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        modifier = fieldMod
                                    )
                                }

                                val marginFieldContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                    OutlinedTextField(
                                        value = marginPercentStr,
                                        onValueChange = {
                                            marginPercentStr = it
                                            val m = it.toDoubleOrNull() ?: 0.0
                                            if (landedCostPaise > 0L) {
                                                CostCalculator.calculatePriceFromPercentage(landedCostPaise, m, pricingMode)?.let { targetSelling ->
                                                    sellingPieceStr = String.format(Locale.US, "%.2f", targetSelling / 100.0)
                                                }
                                            }
                                        },
                                        label = { Text(if (pricingMode == CostCalculator.PricingMode.GROSS_MARGIN) "Margin (%)" else "Markup (%)") },
                                        placeholder = { Text("0.0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        modifier = fieldMod
                                    )
                                }

                                if (isCrowded) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                    ) {
                                        wholesaleFieldContent(Modifier.fillMaxWidth())
                                        marginFieldContent(Modifier.fillMaxWidth())
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                    ) {
                                        wholesaleFieldContent(Modifier.weight(1f))
                                        marginFieldContent(Modifier.weight(1f))
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = pricingMode == CostCalculator.PricingMode.GROSS_MARGIN,
                                        onClick = { pricingMode = CostCalculator.PricingMode.GROSS_MARGIN },
                                        label = { Text("Gross margin", fontSize = AliDimens.minChipTextSize) }
                                    )
                                    FilterChip(
                                        selected = pricingMode == CostCalculator.PricingMode.MARKUP,
                                        onClick = { pricingMode = CostCalculator.PricingMode.MARKUP },
                                        label = { Text("Markup on cost", fontSize = AliDimens.minChipTextSize) }
                                    )
                                }

                                // Warnings
                                if (sellingPaise > 0L && sellingPaise < landedCostPaise) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(AliDimens.radiusSm),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ Selling price is below landed cost (${CostCalculator.formatPaiseToRupees(landedCostPaise)})!",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(AliDimens.spacingSm)
                                        )
                                    }
                                }

                                if (sellingPieceStr.isNotBlank() && sellingPaise == 0L) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(AliDimens.radiusSm),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ Selling price is ₹0.00",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(AliDimens.spacingSm)
                                        )
                                    }
                                }

                                if (totalPiecesAdded > 0 && baseCostPaise == 0L) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(AliDimens.radiusSm),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ Cost price is ₹0.00 with incoming stock",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(AliDimens.spacingSm)
                                        )
                                    }
                                }

                                // Pricing summary chip
                                if (sellingPaise > 0L) {
                                    Surface(
                                        shape = RoundedCornerShape(AliDimens.radiusSm),
                                        color = if (profitPaise >= 0L) statusColors.paidBg else statusColors.dueBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Profit ${CostCalculator.formatPaiseToRupees(profitPaise)}/pc • ${String.format(Locale.US, "%.1f", calculatedMarginPct)}% margin",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (profitPaise >= 0L) statusColors.paidFg else statusColors.dueFg,
                                            modifier = Modifier.padding(AliDimens.spacingSm)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 4: SUPPLIER BILL ───────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(AliDimens.radiusMd)
                        ) {
                            Column(
                                modifier = Modifier.padding(AliDimens.spacingMd),
                                verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(AliDimens.radiusSm))
                                        .clickable { supplierSectionExpanded = !supplierSectionExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (supplierSectionExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "SUPPLIER BILL (optional)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (!supplierSectionExpanded && supplierName.isNotBlank()) {
                                        Text(
                                            supplierName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (supplierSectionExpanded) {
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
                                            label = { Text("Supplier") },
                                            placeholder = { Text("Search or select supplier") },
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
                                                        Text("Add supplier", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

                                    val billNoFieldContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                        OutlinedTextField(
                                            value = billNo,
                                            onValueChange = { billNo = it },
                                            label = { Text("Bill / Invoice No.") },
                                            placeholder = { Text("Optional #") },
                                            leadingIcon = { Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            singleLine = true,
                                            modifier = fieldMod
                                        )
                                    }

                                    val transportFieldContent: @Composable (Modifier) -> Unit = { fieldMod ->
                                        OutlinedTextField(
                                            value = transportChargesStr,
                                            onValueChange = { transportChargesStr = it },
                                            label = { Text("Transport (₹)") },
                                            placeholder = { Text("0") },
                                            leadingIcon = { Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = fieldMod
                                        )
                                    }

                                    if (isCrowded) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                        ) {
                                            billNoFieldContent(Modifier.fillMaxWidth())
                                            transportFieldContent(Modifier.fillMaxWidth())
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                                        ) {
                                            billNoFieldContent(Modifier.weight(1f))
                                            transportFieldContent(Modifier.weight(1f))
                                        }
                                    }

                                    // Payment Status
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(AliDimens.radiusSm))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                            .padding(AliDimens.spacingSm),
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
                                                label = { Text("Unpaid (Add to Dues)", fontSize = AliDimens.minChipTextSize) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = isPaidNow,
                                                onClick = { isPaidNow = true },
                                                label = { Text("Paid Now", fontSize = AliDimens.minChipTextSize) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (isPaidNow) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FilterChip(
                                                    selected = billPaymentMethod == "CASH",
                                                    onClick = { billPaymentMethod = "CASH" },
                                                    label = { Text("Cash", fontSize = AliDimens.minChipTextSize) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FilterChip(
                                                    selected = billPaymentMethod == "UPI",
                                                    onClick = { billPaymentMethod = "UPI" },
                                                    label = { Text("UPI", fontSize = AliDimens.minChipTextSize) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FilterChip(
                                                    selected = billPaymentMethod == "BANK",
                                                    onClick = { billPaymentMethod = "BANK" },
                                                    label = { Text("Bank", fontSize = AliDimens.minChipTextSize) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Pinned Bottom Bar (Summary + Actions)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AliDimens.spacingLg, vertical = AliDimens.spacingSm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stock value: ${CostCalculator.formatPaiseToRupees(stockValuePaise)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (sellingPaise > 0L) {
                                Text(
                                    text = "Profit: ${CostCalculator.formatPaiseToRupees(profitPaise)}/pc (${String.format(Locale.US, "%.1f", calculatedMarginPct)}%)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (profitPaise >= 0L) statusColors.paidFg else statusColors.dueFg
                                )
                            }
                        }

                        if (name.isBlank()) {
                            Text(
                                text = "⚠️ Product name is required to save",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(AliDimens.spacingXs))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AliDimens.spacingSm)
                        ) {
                            OutlinedButton(
                                onClick = { onAttemptDismiss() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(AliDimens.radiusSm)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { doSave() },
                                enabled = name.isNotBlank() && baseCostPaise >= 0L && sellingPaise >= 0L,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(AliDimens.radiusSm)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Sub-dialog: New Category
    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryInput,
                    onValueChange = { newCategoryInput = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Uniform, Textbooks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryInput.trim()
                        if (trimmed.isNotBlank()) {
                            category = trimmed
                            showNewCategoryDialog = false
                            newCategoryInput = ""
                        }
                    }
                ) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sub-dialog: Discard Changes Confirmation
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    // Confirm scan dialog
    if (confirmScan) {
        AlertDialog(
            onDismissRequest = { confirmScan = false },
            title = { Text("Quantity received") },
            text = {
                Column {
                    Text(if (scannedUnit == "PACK") "Packs of $scannedPackSize" else "Pieces")
                    OutlinedTextField(
                        value = scannedQuantity,
                        onValueChange = { scannedQuantity = it.filter(Char::isDigit) },
                        label = { Text("Quantity") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qty = scannedQuantity.toIntOrNull() ?: 0
                    if (qty > 0 && qty.toLong() * scannedPackSize <= Int.MAX_VALUE) {
                        piecesStr = (qty * if (scannedUnit == "PACK") scannedPackSize else 1).toString()
                        confirmScan = false
                    }
                }) {
                    Text("Use quantity")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmScan = false }) { Text("Cancel") }
            }
        )
    }

    // Inline Add Supplier Dialog
    if (showAddSupplierInlineDialog) {
        var inlineSupplierName by remember { mutableStateOf("") }
        var inlineSupplierPhone by remember { mutableStateOf("") }
        var inlineSupplierNotes by remember { mutableStateOf("") }
        var inlineSupplierError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAddSupplierInlineDialog = false
                inlineSupplierError = null
            },
            title = { Text("Add Supplier", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inlineSupplierName,
                        onValueChange = {
                            inlineSupplierName = it
                            inlineSupplierError = null
                        },
                        label = { Text("Supplier / Company Name *") },
                        isError = inlineSupplierError != null,
                        supportingText = inlineSupplierError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inlineSupplierPhone,
                        onValueChange = { inlineSupplierPhone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inlineSupplierNotes,
                        onValueChange = { inlineSupplierNotes = it },
                        label = { Text("Notes / Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inlineSupplierName.trim()
                        if (trimmed.isBlank()) {
                            inlineSupplierError = "Supplier name is required"
                        } else if (suppliers.any { it.name.equals(trimmed, ignoreCase = true) }) {
                            inlineSupplierError = "Supplier '$trimmed' already exists"
                        } else {
                            val newSup = SupplierEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                name = trimmed,
                                phone = inlineSupplierPhone.trim().ifBlank { null },
                                notes = inlineSupplierNotes.trim().ifBlank { null }
                            )
                            onAddSupplier(newSup) { created ->
                                selectedSupplier = created
                                supplierName = created.name
                                showAddSupplierInlineDialog = false
                                inlineSupplierError = null
                            }
                        }
                    }
                ) {
                    Text("Save Supplier")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddSupplierInlineDialog = false
                    inlineSupplierError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                                                                    scannedProduct = product
                                                                    scannedUnit = unit
                                                                    scannedPackSize = count
                                                                    barcode = product?.barcode ?: scannedCode
                                                                    if (product != null) {
                                                                        name = product.name
                                                                        category = product.category
                                                                        brand = product.brand ?: ""
                                                                        costPerPieceStr = (product.movingAvgCostPaise / 100.0).toString()
                                                                        sellingPieceStr = (product.sellingPricePiecePaise / 100.0).toString()
                                                                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiBarcodeLabelExportDialog(
    allProducts: List<ProductEntity>,
    preSelectedProducts: List<ProductEntity>,
    barcodeService: BarcodeGeneratorService,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPieceCopies by remember {
        mutableStateOf(preSelectedProducts.filter { !it.barcode.isNullOrBlank() }.associate { it.id to 24 }.toMutableMap())
    }
    var selectedPackCopies by remember {
        mutableStateOf(mutableMapOf<String, Int>())
    }
    var includePriceOnLabel by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf(com.alieducational.app.data.service.LabelPreset.A4_3X8) }
    var isExporting by remember { mutableStateOf(false) }

    val eligibleProducts = remember(allProducts) {
        allProducts.filter { !it.isArchived && !it.barcode.isNullOrBlank() }
    }
    val filteredProducts = remember(eligibleProducts, searchQuery) {
        if (searchQuery.isBlank()) eligibleProducts
        else eligibleProducts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.barcode?.contains(searchQuery, ignoreCase = true) == true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalSelectedLabels = selectedPieceCopies.values.sum() + selectedPackCopies.values.sum()

    Dialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (!isExporting) onDismiss() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Print / Export Barcodes",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search products to print...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedPieceCopies.size + selectedPackCopies.size} variant(s) selected",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        val newMap = selectedPieceCopies.toMutableMap()
                                        filteredProducts.forEach { p ->
                                            if (!newMap.containsKey(p.id)) newMap[p.id] = 24
                                        }
                                        selectedPieceCopies = newMap
                                    }
                                ) {
                                    Text("Select All (${filteredProducts.size})")
                                }
                                TextButton(
                                    onClick = {
                                        selectedPieceCopies = mutableMapOf()
                                        selectedPackCopies = mutableMapOf()
                                    }
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }

                    items(filteredProducts) { prod ->
                        val isPieceSelected = selectedPieceCopies.containsKey(prod.id)
                        val pieceCount = selectedPieceCopies[prod.id] ?: 24
                        val isPackSelected = selectedPackCopies.containsKey(prod.id)
                        val packCount = selectedPackCopies[prod.id] ?: 24

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPieceSelected || isPackSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isPieceSelected,
                                        onCheckedChange = { checked ->
                                            val newMap = selectedPieceCopies.toMutableMap()
                                            if (checked) newMap[prod.id] = 24 else newMap.remove(prod.id)
                                            selectedPieceCopies = newMap
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Barcode: ${prod.barcode} • Selling: ${CostCalculator.formatPaiseToRupees(prod.sellingPricePiecePaise)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (isPieceSelected) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Piece label copies:", style = MaterialTheme.typography.bodySmall)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val newMap = selectedPieceCopies.toMutableMap()
                                                    val cur = newMap[prod.id] ?: 24
                                                    if (cur > 1) newMap[prod.id] = cur - 1
                                                    selectedPieceCopies = newMap
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                            Text(
                                                text = "$pieceCount",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.width(36.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            IconButton(
                                                onClick = {
                                                    val newMap = selectedPieceCopies.toMutableMap()
                                                    val cur = newMap[prod.id] ?: 24
                                                    if (cur < 100) newMap[prod.id] = cur + 1
                                                    selectedPieceCopies = newMap
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                        }
                                    }
                                }

                                if (prod.packSize > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isPackSelected,
                                            onCheckedChange = { checked ->
                                                val newMap = selectedPackCopies.toMutableMap()
                                                if (checked) newMap[prod.id] = 24 else newMap.remove(prod.id)
                                                selectedPackCopies = newMap
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Pack of ${prod.packSize}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                            Text("Selling: ${CostCalculator.formatPaiseToRupees(prod.sellingPricePackPaise)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    if (isPackSelected) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(start = 48.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Pack label copies:", style = MaterialTheme.typography.bodySmall)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        val newMap = selectedPackCopies.toMutableMap()
                                                        val cur = newMap[prod.id] ?: 24
                                                        if (cur > 1) newMap[prod.id] = cur - 1
                                                        selectedPackCopies = newMap
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                }
                                                Text(
                                                    text = "$packCount",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.width(36.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val newMap = selectedPackCopies.toMutableMap()
                                                        val cur = newMap[prod.id] ?: 24
                                                        if (cur < 100) newMap[prod.id] = cur + 1
                                                        selectedPackCopies = newMap
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = includePriceOnLabel,
                                        onCheckedChange = { includePriceOnLabel = it }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Include Price on Labels", fontWeight = FontWeight.SemiBold)
                                }

                                Text("Paper Sheet Layout", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    com.alieducational.app.data.service.LabelPreset.values().forEach { preset ->
                                        FilterChip(
                                            selected = selectedPreset == preset,
                                            onClick = { selectedPreset = preset },
                                            label = { Text(preset.title, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Labels: $totalSelectedLabels",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val perPage = selectedPreset.cols * selectedPreset.rows
                            val pages = if (totalSelectedLabels > 0) (totalSelectedLabels + perPage - 1) / perPage else 0
                            Text(
                                text = if (pages > 0) "$pages page(s) on ${selectedPreset.title}" else "Select at least 1 label",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                if (totalSelectedLabels <= 0) return@Button
                                isExporting = true
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        val labelItems = mutableListOf<com.alieducational.app.data.service.LabelItem>()
                                        selectedPieceCopies.forEach { (prodId, count) ->
                                            val p = allProducts.firstOrNull { it.id == prodId }
                                            if (p != null && count > 0) {
                                                labelItems.add(
                                                    com.alieducational.app.data.service.LabelItem(
                                                        productName = p.name,
                                                        barcode = p.barcode ?: "",
                                                        unitLabel = "PIECE",
                                                        pricePaise = if (includePriceOnLabel) p.sellingPricePiecePaise else null,
                                                        copies = count
                                                    )
                                                )
                                            }
                                        }
                                        selectedPackCopies.forEach { (prodId, count) ->
                                            val p = allProducts.firstOrNull { it.id == prodId }
                                            if (p != null && count > 0) {
                                                labelItems.add(
                                                    com.alieducational.app.data.service.LabelItem(
                                                        productName = p.name,
                                                        barcode = p.barcode ?: "",
                                                        unitLabel = "PACK OF ${p.packSize}",
                                                        pricePaise = if (includePriceOnLabel) p.sellingPricePackPaise else null,
                                                        copies = count
                                                    )
                                                )
                                            }
                                        }

                                        val pdfFile = barcodeService.generateMultiLabelsPdf(
                                            context = context,
                                            items = labelItems,
                                            preset = selectedPreset
                                        )

                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isExporting = false
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                pdfFile
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share / Print Barcode Labels"))
                                            onDismiss()
                                        }
                                    } catch (e: Exception) {
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isExporting = false
                                            android.widget.Toast.makeText(context, "Failed to generate PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            enabled = totalSelectedLabels > 0 && !isExporting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isExporting) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Export / Share PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
