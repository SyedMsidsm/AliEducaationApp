package com.alieducational.app.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.alieducational.app.data.service.SyncConnectionState
import com.alieducational.app.ui.AliViewModel
import com.alieducational.app.widget.AliSlimBarWidgetProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isLockEnabled by viewModel.isLockEnabled.collectAsState()
    val lockTimeoutSeconds by viewModel.lockTimeoutSeconds.collectAsState()
    val deviceRole by viewModel.deviceRole.collectAsState()
    val pairingToken by viewModel.syncToken.collectAsState()
    val ownerCertificatePin by viewModel.ownerCertificatePin.collectAsState()
    var newOwnerPin by remember { mutableStateOf("") }
    var pinSetupMessage by remember { mutableStateOf<String?>(null) }
    var tokenInput by remember(pairingToken) { mutableStateOf(pairingToken) }
    var certificateInput by remember(ownerCertificatePin) { mutableStateOf(ownerCertificatePin) }
    val pairedOwnerHost by viewModel.pairedOwnerHost.collectAsState()
    val syncState by viewModel.syncConnectionState.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val appTextSize by viewModel.appTextSize.collectAsState()
    val connectedFolderUri by viewModel.googleDriveFolderUri.collectAsState()
    val connectedFolderName by viewModel.googleDriveFolderName.collectAsState()

    var showSwitchToBillingWarning by remember { mutableStateOf(false) }
    var showOwnerPinDialog by remember { mutableStateOf(false) }
    var enteredOwnerPin by remember { mutableStateOf("") }
    var ownerPinError by remember { mutableStateOf<String?>(null) }

    var hostAddressInput by remember(pairedOwnerHost) { mutableStateOf(pairedOwnerHost) }
    var syncResultMsg by remember { mutableStateOf<String?>(null) }

    val driveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val doc = DocumentFile.fromTreeUri(context, uri)
                val name = doc?.name ?: "Selected Folder"
                viewModel.setGoogleDriveFolder(uri.toString(), name)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Two-Phone Sync & Role Card (A01 & A02)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Two-Phone Billing & Sync", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Secure authenticated sync over Wi-Fi / Hotspot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Current Phone Role", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = deviceRole == "OWNER",
                                onClick = {
                                    if (deviceRole != "OWNER") {
                                        // Enforce real role protection (A02): require Owner PIN unlock!
                                        showOwnerPinDialog = true
                                        enteredOwnerPin = ""
                                        ownerPinError = null
                                    }
                                },
                                label = { Text("Owner (Main)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = deviceRole == "BILLING",
                                onClick = {
                                    if (deviceRole != "BILLING") {
                                        showSwitchToBillingWarning = true
                                    }
                                },
                                label = { Text("Billing Device") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (deviceRole == "BILLING") {
                            OutlinedTextField(value = tokenInput, onValueChange = { tokenInput = it }, label = { Text("Owner pairing token") })
                            OutlinedTextField(value = certificateInput, onValueChange = { certificateInput = it }, label = { Text("Owner certificate SHA-256") })
                            TextButton(onClick = { viewModel.configurePairing(tokenInput, certificateInput) }) { Text("Save verified pairing") }
                        }
                        if (deviceRole == "OWNER") {
                            // OWNER HOST CONTROLS
                            val localIp = remember { viewModel.twoPhoneSyncService.getLocalIpAddress() }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Main Phone Host Info", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("IP Address: $localIp (Port: 8080)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    Text("Pairing token: $pairingToken", style = MaterialTheme.typography.bodySmall)
                                    Text("Certificate SHA-256: ${com.alieducational.app.data.service.SyncTls.fingerprint()}", style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { viewModel.rotateSyncToken() }) { Text("Create / revoke pairing token") }
                                    OutlinedTextField(value = newOwnerPin, onValueChange = { newOwnerPin = it }, label = { Text("Set Owner PIN (6–12 digits)") }, visualTransformation = PasswordVisualTransformation())
                                    TextButton(onClick = { viewModel.saveOwnerPin(newOwnerPin) { pinSetupMessage = it ?: "Owner PIN saved"; newOwnerPin = "" } }) { Text("Save Owner PIN") }
                                    pinSetupMessage?.let { Text(it) }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Keep this phone connected to shop Wi-Fi or its personal hotspot. On the billing phone, enter this IP address.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (syncState is SyncConnectionState.Hosting) "Server Active" else "Server Inactive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (syncState is SyncConnectionState.Hosting) Color(0xFF75B890) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        if (syncState is SyncConnectionState.Hosting) {
                                            viewModel.stopSyncServer()
                                        } else {
                                            viewModel.startSyncServer(8080)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (syncState is SyncConnectionState.Hosting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (syncState is SyncConnectionState.Hosting) "Stop Server" else "Start Server")
                                }
                            }
                        } else {
                            // BILLING DEVICE CONTROLS
                            OutlinedTextField(
                                value = hostAddressInput,
                                onValueChange = { hostAddressInput = it },
                                label = { Text("Owner Phone IP Address") },
                                placeholder = { Text("e.g. 192.168.1.5") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.setPairedOwnerHost(hostAddressInput.trim())
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Text("Save IP")
                                }

                                Button(
                                    onClick = {
                                        val host = hostAddressInput.trim()
                                        if (host.isNotBlank()) {
                                            viewModel.setPairedOwnerHost(host)
                                            viewModel.syncWithMainPhone(host) { success ->
                                                syncResultMsg = if (success) "Sync successful!" else "Sync failed. Check IP & Wi-Fi."
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Now")
                                }
                            }

                            if (syncResultMsg != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = syncResultMsg!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (syncResultMsg!!.contains("successful")) Color(0xFF75B890) else MaterialTheme.colorScheme.error
                                )
                            }

                            if (pendingSyncCount > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$pendingSyncCount bills waiting to sync to Main Phone",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE4B765)
                                )
                            }
                        }
                    }
                }

                // 2. App Text Size Setting (Section 5)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("App Text Size", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Adjust text size throughout the app. Printed receipts remain independent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val sizeOptions = listOf("Small", "Default", "Large", "Extra Large")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            sizeOptions.forEach { opt ->
                                FilterChip(
                                    selected = appTextSize == opt,
                                    onClick = { viewModel.setAppTextSize(opt) },
                                    label = { Text(opt, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Live Preview Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Preview ($appTextSize)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Ali Educational", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Stationery & Educational Supplies", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("₹12,235.00", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    Button(
                                        onClick = {},
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Sample Button", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. One-Tap Google Drive / External Destination (Section 10)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Cloud & Drive Destination", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Connect Google Drive or SD folder once", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (connectedFolderUri.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = Color(0xFF75B890), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Connected Destination:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(connectedFolderName.ifBlank { "Connected Folder" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { driveFolderLauncher.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Change Folder")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.clearGoogleDriveFolder() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        } else {
                            Text(
                                "No destination connected. Select a Google Drive folder or local folder once to enable 1-tap backup uploads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { driveFolderLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect Google Drive Folder")
                            }
                        }
                    }
                }

                // 4. Appearance Theme Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Appearance Theme", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(
                                selected = isDarkTheme,
                                onClick = { viewModel.setTheme("dark") },
                                label = { Text("Dark Theme (Default)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !isDarkTheme,
                                onClick = { viewModel.setTheme("light") },
                                label = { Text("Light Theme") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 5. App Lock Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Device Lock Protection", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Fingerprint or device PIN/pattern", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isLockEnabled,
                                onCheckedChange = { enable ->
                                    if (!enable) {
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        if (activity != null) {
                                            viewModel.stepUp.require(
                                                activity = activity,
                                                title = "Authenticate to disable device lock",
                                                onOk = { viewModel.setLockEnabled(false) },
                                                onFail = { /* stay enabled */ }
                                            )
                                        } else {
                                            viewModel.setLockEnabled(false)
                                        }
                                    } else {
                                        viewModel.setLockEnabled(true)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        if (isLockEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Inactivity Relock Timeout", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))

                            val timeoutOptions = listOf(
                                30 to "30 Seconds",
                                60 to "1 Minute",
                                120 to "2 Minutes (Default)",
                                300 to "5 Minutes",
                                600 to "10 Minutes"
                            )

                            timeoutOptions.forEach { (seconds, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setLockTimeout(seconds) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = lockTimeoutSeconds == seconds,
                                        onClick = { viewModel.setLockTimeout(seconds) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // 6. Home Screen Widget Card
                val canPinWidget = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                    (context.getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true)

                if (canPinWidget) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Home Screen Widget", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Slim bar with Cash In, Cash Out, New Bill & Add Stock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                                    val myProvider = ComponentName(context, AliSlimBarWidgetProvider::class.java)
                                    appWidgetManager?.requestPinAppWidget(myProvider, null, null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Widget to Home Screen")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSwitchToBillingWarning) {
        AlertDialog(
            onDismissRequest = { showSwitchToBillingWarning = false },
            title = { Text("Switch to Billing Device Role?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This phone will be set to Staff Billing mode. It will pull stock and products from the Owner phone and record bills with the 'AE-B1-' prefix. Financial and profit reports will be hidden on this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (activity != null) {
                            viewModel.stepUp.require(
                                activity = activity,
                                title = "Authenticate to change device role",
                                onOk = {
                                    viewModel.setDeviceRole("BILLING")
                                    showSwitchToBillingWarning = false
                                },
                                onFail = { /* cancel role switch */ }
                            )
                        } else {
                            viewModel.setDeviceRole("BILLING")
                            showSwitchToBillingWarning = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchToBillingWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showOwnerPinDialog) {
        AlertDialog(
            onDismissRequest = { showOwnerPinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Owner PIN Required", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Enter the Owner PIN previously provisioned on this device. A paired Billing phone cannot create its own Owner authorization.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (activity == null) ownerPinError = "Cannot open device authentication"
                        else viewModel.recoverOwner(activity) { error ->
                            if (error == null) { showOwnerPinDialog = false; pinSetupMessage = "Owner access recovered. Set and save a new Owner PIN below." }
                            else ownerPinError = error
                        }
                    }) { Text("Forgot Owner PIN?") }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = enteredOwnerPin,
                        onValueChange = {
                            enteredOwnerPin = it
                            ownerPinError = null
                        },
                        label = { Text("Owner PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        isError = ownerPinError != null,
                        supportingText = ownerPinError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.authorizeOwner(enteredOwnerPin) { valid ->
                            if (valid) showOwnerPinDialog = false
                            else ownerPinError = "Owner authorization failed or temporarily locked."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Unlock & Promote")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOwnerPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
