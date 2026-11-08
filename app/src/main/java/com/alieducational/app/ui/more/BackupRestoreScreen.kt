package com.alieducational.app.ui.more

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.service.BackupMetadata
import com.alieducational.app.ui.AliViewModel
import com.alieducational.app.ui.theme.*
import androidx.compose.foundation.BorderStroke
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backupStatus by viewModel.backupStatus.collectAsState()
    val availableBackups by viewModel.availableBackups.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val isCreatingBackup by viewModel.isCreatingBackup.collectAsState()
    val lastExternalSaveTime by viewModel.lastExternalSaveTime.collectAsState()
    val lastExternalSaveDestination by viewModel.lastExternalSaveDestination.collectAsState()

    var selectedBackupForRestore by remember { mutableStateOf<BackupMetadata?>(null) }
    var viewingDetailsBackup by remember { mutableStateOf<BackupMetadata?>(null) }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val positiveColor = if (isDarkTheme) PositiveGreenDark else PositiveGreenLight
    val negativeColor = if (isDarkTheme) NegativeRedDark else NegativeRedLight
    val warningColor = if (isDarkTheme) WarningOrangeDark else WarningOrangeLight

    LaunchedEffect(Unit) {
        viewModel.loadBackups()
    }

    var pendingFileToSave by remember { mutableStateOf<File?>(null) }
    val saveToDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null && pendingFileToSave != null) {
            viewModel.exportBackupToUri(pendingFileToSave!!, uri)
        }
        pendingFileToSave = null
    }

    val driveFolderUri by viewModel.googleDriveFolderUri.collectAsState()
    val driveFolderName by viewModel.googleDriveFolderName.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not grantable
            }
            val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
            val folderName = docFile?.name ?: "Google Drive / External Folder"
            viewModel.setGoogleDriveFolder(uri.toString(), folderName)
        }
    }

    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreFromUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
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
                // Daily Closing Routine Reminder Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Routine Reminder",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Save an external copy to Google Drive / Files after shop closing each evening, and always before app updates.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Backup Status Card matching Section 4 recommendation
                item {
                    val latestBackup = availableBackups.firstOrNull()
                    val hasBackup = latestBackup != null
                    val dateFormatted = if (hasBackup) {
                        SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault()).format(Date(latestBackup!!.timestamp))
                    } else if (lastBackupTime > 0) {
                        SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault()).format(Date(lastBackupTime))
                    } else "Never"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.3f else 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (hasBackup) "Backup ready" else "Local Phone Backup",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (hasBackup) positiveColor.copy(alpha = 0.15f) else warningColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (hasBackup) "Verified Ready" else "Pending",
                                        color = if (hasBackup) positiveColor else warningColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Three status rows from Section 4
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Complete recovery file", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (hasBackup) "Ready" else "Not created", fontWeight = FontWeight.SemiBold, color = if (hasBackup) positiveColor else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Readable Excel records", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (hasBackup) "Ready" else "Not created", fontWeight = FontWeight.SemiBold, color = if (hasBackup) positiveColor else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Photos and files", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (hasBackup) "${latestBackup!!.photoCount} verified" else "0 files",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── CONNECTED GOOGLE DRIVE FOLDER (Section 10) ────────
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (driveFolderUri.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            if (driveFolderUri.isNotBlank()) Icons.Filled.CheckCircle else Icons.Filled.Folder,
                                            contentDescription = null,
                                            tint = if (driveFolderUri.isNotBlank()) positiveColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (driveFolderUri.isNotBlank()) "Drive: $driveFolderName" else "External backup destination",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (driveFolderUri.isNotBlank()) "Connected folder; external copy status is checked on each backup" else "Connect folder once for one-tap backup",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { folderPickerLauncher.launch(null) }
                                    ) {
                                        Text(if (driveFolderUri.isNotBlank()) "Change" else "Connect", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Primary One-Tap Backup & Save Button (Section 10)
                            Button(
                                onClick = {
                                    viewModel.createBackup()
                                },
                                enabled = !isCreatingBackup,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (driveFolderUri.isNotBlank()) "Back up locally + copy to connected folder" else "Create Backup Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Secondary actions: Share Package + View Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val target = latestBackup ?: return@OutlinedButton
                                        val path = target.packageFilePath ?: target.filePath
                                        val shareIntent = viewModel.getBackupShareIntent(path)
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup Package"))
                                    },
                                    enabled = hasBackup,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share package", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewingDetailsBackup = latestBackup
                                    },
                                    enabled = hasBackup,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View details", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: Create Backup Now & Restore from File
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.createBackup() },
                                    enabled = !isCreatingBackup,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    if (isCreatingBackup) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Creating...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Backup Now", fontSize = 12.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        restoreFilePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore Backup", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                // Status Message
                if (backupStatus != null) {
                    item {
                        val isFailed = backupStatus!!.contains("fail", ignoreCase = true)
                        val statusColor = if (isFailed) Color(0xFFE57373) else Color(0xFF75B890)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = backupStatus!!,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isFailed) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.createBackup() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }

                // Storage Location & File Access Transparency Card (F14 requirement)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Storage Location on Phone",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.backupFolderPath,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Backups are saved directly in your phone's Documents/AliEducational/Backups folder, easily visible in Samsung My Files. You can also tap 'Export / Share' to send a backup via WhatsApp, Google Drive, or email.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Contents Breakdown Card (F14 requirement)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFE4B765),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "What is Included in Backups",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Complete inventory, piece/pack prices & moving-average costs\n" +
                                       "• All sales history, cash memos & counter bills\n" +
                                       "• Customer credit balances & khata ledgers\n" +
                                       "• Supplier purchases, dues & payment logs\n" +
                                       "• Shop expenses, daily cash balances & profit reports",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Available Backups List
                item {
                    Text(
                        text = "Dated Backups Stored on Phone (${availableBackups.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (availableBackups.isEmpty()) {
                    item {
                        Text(
                            text = "No previous backups found on this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(availableBackups) { backup ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = backup.fileName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(backup.timestamp))
                                        val sizeKb = backup.sizeBytes / 1024
                                        Text(
                                            text = "$dateStr • $sizeKb KB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Verification status badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (backup.isValid) Color(0xFF75B890).copy(alpha = 0.2f) else Color(0xFFE57373).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (backup.isValid) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = if (backup.isValid) Color(0xFF75B890) else Color(0xFFE57373),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (backup.isValid) "Verified" else "Invalid",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (backup.isValid) Color(0xFF75B890) else Color(0xFFE57373)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons: Share / Export, Save to Drive, and Restore
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            pendingFileToSave = File(backup.filePath)
                                            saveToDriveLauncher.launch(backup.fileName)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = ButtonDefaults.TextButtonContentPadding
                                    ) {
                                        Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("To Drive", fontSize = 11.sp, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = viewModel.getBackupShareIntent(backup.filePath)
                                            context.startActivity(Intent.createChooser(shareIntent, "Export / Share Backup"))
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = ButtonDefaults.TextButtonContentPadding
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Share", fontSize = 11.sp, maxLines = 1)
                                    }

                                    Button(
                                        onClick = { selectedBackupForRestore = backup },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = ButtonDefaults.TextButtonContentPadding
                                    ) {
                                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Restore", fontSize = 11.sp, maxLines = 1)
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

    // Restore Warning Confirmation Dialog
    if (selectedBackupForRestore != null) {
        val backup = selectedBackupForRestore!!
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(backup.timestamp))
        val sizeKb = backup.sizeBytes / 1024

        AlertDialog(
            onDismissRequest = { selectedBackupForRestore = null },
            icon = {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFE4B765), modifier = Modifier.size(36.dp))
            },
            title = { Text("Restore This Backup?") },
            text = {
                Column {
                    Text("File: ${backup.fileName}", fontWeight = FontWeight.Bold)
                    Text("Created: $dateStr ($sizeKb KB)", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "WARNING: Restoring will overwrite all current local stock, customer khata, and sales records with this backup's snapshot.\n\nA safety backup of your existing records will be created automatically before restoring.",
                        color = Color(0xFFE4B765),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = File(backup.filePath)
                        selectedBackupForRestore = null
                        viewModel.restoreBackup(file)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedBackupForRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

// View Details Dialog
    if (viewingDetailsBackup != null) {
        val b = viewingDetailsBackup!!
        val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(b.timestamp))

        AlertDialog(
            onDismissRequest = { viewingDetailsBackup = null },
            icon = {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            },
            title = { Text("Backup Package Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Snapshot ID: ${b.snapshotId.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Date: $dateStr", style = MaterialTheme.typography.bodySmall)
                    Text("Package Size: ${b.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Verified Record Counts:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Text("• Products in Catalog: ${b.productCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Total Bills / Invoices: ${b.billCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Customer Credit Accounts: ${b.customerCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Supplier Accounts: ${b.supplierCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Cash Ledger Transactions: ${b.cashInOutCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Pigmy Savings Records: ${b.pigmyCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• Photos & Attachments: ${b.photoCount}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: ${b.integrityNote}", style = MaterialTheme.typography.labelSmall, color = positiveColor)
                }
            },
            confirmButton = {
                Button(onClick = { viewingDetailsBackup = null }) {
                    Text("Close")
                }
            }
        )
    }
}
