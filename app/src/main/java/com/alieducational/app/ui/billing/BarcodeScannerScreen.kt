package com.alieducational.app.ui.billing

import com.alieducational.app.ui.components.AliLogo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alieducational.app.R
import com.alieducational.app.data.local.entity.ProductEntity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val billingState by viewModel.billingState.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    // Barcode confirmation state (Section 6)
    var scannedProductToConfirm by remember { mutableStateOf<Pair<ProductEntity, String>?>(null) }
    var scannedPackQty by remember { mutableStateOf(1) }
    var scannedQuantityStr by remember { mutableStateOf("1") }
    var isScannerPaused by remember { mutableStateOf(false) }

    // Unknown Barcode Link Dialog State
    var unknownBarcodeToLink by remember { mutableStateOf<String?>(null) }
    var selectedProductForLink by remember { mutableStateOf("") }
    var selectedUnitForLink by remember { mutableStateOf("PIECE") }
    var linkDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                AliLogo(
                    isDark = isDarkTheme,
                    modifier = Modifier
                        .height(38.dp)
                        .width(72.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Ali Educational",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.billing_header_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Title & Frame Guidance
            Text(
                text = stringResource(R.string.scanner_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.scanner_guidance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Camera Viewfinder Box with Barcode Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                if (hasCameraPermission) {
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
                                                onBarcodeDetected = { barcode, _ ->
                                                    if (!isScannerPaused && scannedProductToConfirm == null && unknownBarcodeToLink == null) {
                                                        isScannerPaused = true
                                                        viewModel.lookupBarcode(barcode) { prod, unitType, packQty ->
                                                            if (prod != null) {
                                                                scannedProductToConfirm = Pair(prod, unitType ?: "PIECE")
                                                                scannedPackQty = packQty ?: 1
                                                                scannedQuantityStr = "1"
                                                            } else {
                                                                unknownBarcodeToLink = barcode
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
                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalyzer
                                    )
                                } catch (e: Exception) {
                                    // Camera bind failure fallback
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Reticle overlay
                    Box(
                        modifier = Modifier
                            .size(width = 240.dp, height = 130.dp)
                            .align(Alignment.Center)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    )
                } else {
                    // Permission not granted fallback
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.scanner_permission_needed),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.scanner_grant_permission))
                        }
                    }
                }

                // Torch Toggle Control inside Viewfinder
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x99000000))
                        .clickable {
                            isTorchOn = !isTorchOn
                            camera?.cameraControl?.enableTorch(isTorchOn)
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = null,
                        tint = if (isTorchOn) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.scanner_torch),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Last Scanned Item Banner with Undo
            val lastItem = billingState.lastAddedItem
            if (lastItem != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${stringResource(R.string.scanner_added_label)}: ${lastItem.productName}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val unitStr = if (lastItem.unitType == "PACK") "pack" else "piece"
                            val priceStr = CostCalculator.formatPaiseToRupees(lastItem.unitPricePaise)
                            Text(
                                text = "${lastItem.quantity} $unitStr · $priceStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = { viewModel.undoLastAddition() }) {
                            Text(
                                text = stringResource(R.string.scanner_undo),
                                color = Color(0xFFE57373),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Anti-duplicate Guidance Note
            Text(
                text = stringResource(R.string.scanner_debounce_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bottom Action Row: [Add again] and [View bill · ₹165]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.addAgainLastItem() },
                    enabled = lastItem != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.scanner_add_again),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                val totalStr = CostCalculator.formatPaiseToRupees(billingState.grossTotalPaise)
                Button(
                    onClick = onNavigateToReview,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "${stringResource(R.string.scanner_view_bill)} · $totalStr",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Fallback: No barcode? Search or browse items
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.scanner_no_barcode_fallback),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
            }
        }
    }

    // Quantity Confirmation Dialog (Section 6)
    if (scannedProductToConfirm != null) {
        val (prod, unitType) = scannedProductToConfirm!!
        val unitPricePaise = if (unitType == "PACK") prod.sellingPricePackPaise else prod.sellingPricePiecePaise
        val unitLabel = if (unitType == "PACK") "Pack of ${prod.packSize}" else "Piece"

        AlertDialog(
            onDismissRequest = {
                scannedProductToConfirm = null
                isScannerPaused = false
            },
            title = {
                Text("Confirm Scanned Product", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = prod.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unit: $unitLabel", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Price: ${CostCalculator.formatPaiseToRupees(unitPricePaise)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "Current Stock: ${prod.currentSellableStock} pieces available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Enter Quantity:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val current = scannedQuantityStr.toIntOrNull() ?: 1
                                if (current > 1) scannedQuantityStr = (current - 1).toString()
                            },
                            modifier = Modifier.size(44.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = scannedQuantityStr,
                            onValueChange = { scannedQuantityStr = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedButton(
                            onClick = {
                                val current = scannedQuantityStr.toIntOrNull() ?: 1
                                scannedQuantityStr = (current + 1).toString()
                            },
                            modifier = Modifier.size(44.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = scannedQuantityStr.toIntOrNull() ?: 1
                        if (qty > 0) {
                            viewModel.addItemToBill(prod, unitType, qty, if (unitType == "PACK") scannedPackQty else null)
                        }
                        scannedProductToConfirm = null
                        isScannerPaused = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add to Bill")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        scannedProductToConfirm = null
                        isScannerPaused = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for Unknown Barcode: Link to existing product or Cancel
    if (unknownBarcodeToLink != null) {
        val code = unknownBarcodeToLink!!
        AlertDialog(
            onDismissRequest = {
                unknownBarcodeToLink = null
                isScannerPaused = false
                viewModel.clearUnknownBarcode()
                    isScannerPaused = false
            },
            title = { Text(stringResource(R.string.scanner_unknown_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${stringResource(R.string.scanner_scanned_code)}: $code",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.scanner_unknown_prompt))

                    ExposedDropdownMenuBox(
                        expanded = linkDropdownExpanded,
                        onExpandedChange = { linkDropdownExpanded = !linkDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = allProducts.find { it.id == selectedProductForLink }?.name ?: stringResource(R.string.scanner_select_product_to_link),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = linkDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = linkDropdownExpanded,
                            onDismissRequest = { linkDropdownExpanded = false }
                        ) {
                            allProducts.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        selectedProductForLink = p.id
                                        linkDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedProductForLink.isNotBlank()) {
                            viewModel.linkAndAddUnknownBarcode(
                                barcode = code,
                                productId = selectedProductForLink,
                                unitType = selectedUnitForLink,
                                packQty = 1,
                                onDone = {
                                    unknownBarcodeToLink = null
                                    allProducts.firstOrNull { it.id == selectedProductForLink }?.let { product ->
                                        scannedProductToConfirm = product to selectedUnitForLink
                                        scannedPackQty = 1
                                        scannedQuantityStr = "1"
                                    }
                                }
                            )
                        }
                    },
                    enabled = selectedProductForLink.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.scanner_link_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    unknownBarcodeToLink = null
                    viewModel.clearUnknownBarcode()
                    isScannerPaused = false
                }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
