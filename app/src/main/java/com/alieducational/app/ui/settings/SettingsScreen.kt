package com.alieducational.app.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.service.SyncConnectionState
import com.alieducational.app.data.service.TwoPhoneSyncService
import com.alieducational.app.ui.AliViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isLockEnabled by viewModel.isLockEnabled.collectAsState()
    val lockTimeoutSeconds by viewModel.lockTimeoutSeconds.collectAsState()
    val deviceRole by viewModel.deviceRole.collectAsState()
    val pairedOwnerHost by viewModel.pairedOwnerHost.collectAsState()
    val syncState by viewModel.syncConnectionState.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    var showSwitchToBillingWarning by remember { mutableStateOf(false) }
    var hostAddressInput by remember(pairedOwnerHost) { mutableStateOf(pairedOwnerHost) }
    var syncResultMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold) },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Two-Phone Sync & Role Card
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
                                Text("Share database over Wi-Fi / Hotspot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        viewModel.setDeviceRole("OWNER")
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
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Keep this phone connected to the shop Wi-Fi or start its personal hotspot. On the secondary phone, select 'Billing Device' and enter this IP address.",
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

                // 2. Theme Card
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

                // 3. App Lock Card
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
                                onCheckedChange = { viewModel.setLockEnabled(it) },
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
                        viewModel.setDeviceRole("BILLING")
                        showSwitchToBillingWarning = false
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
}
