package com.alieducational.app.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import androidx.room.Room
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.documentfile.provider.DocumentFile
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupMetadata(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val isValid: Boolean = true,
    val integrityNote: String = "Verified SQLite Archive",
    val snapshotId: String = "",
    val packageFilePath: String? = null,
    val completeBackupFilePath: String? = null,
    val excelFilePath: String? = null,
    val photoCount: Int = 0,
    val productCount: Int = 0,
    val billCount: Int = 0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val cashInOutCount: Int = 0,
    val pigmyCount: Int = 0
)

@Singleton
class BackupRestoreService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val preferences: AppPreferences,
    private val excelGenerator: ShopRecordsExcelGenerator
) {

    private val operationMutex = Mutex()

    val primaryBackupFolder: File
        get() {
            val appExternal = context.getExternalFilesDir("backups")
            val dir = if (appExternal != null) {
                File(appExternal, "snapshots")
            } else {
                File(context.filesDir, "backups")
            }
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val fallbackBackupFolder: File
        get() = primaryBackupFolder

    val backupFolder: File
        get() = primaryBackupFolder

    val backupFolderPath: String
        get() = backupFolder.absolutePath

    fun isEncrypted(file: File): Boolean = runCatching {
        file.inputStream().use { BackupCrypto.isEncryptedBackup(it) }
    }.getOrDefault(false)

    fun isEncryptedUri(uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { BackupCrypto.isEncryptedBackup(it) } ?: false
    }.getOrDefault(false)

    suspend fun createBackup(passphrase: CharArray? = null): BackupMetadata = withContext(Dispatchers.IO) {
        operationMutex.withLock {
        db.withTransaction {
        val dbFile = File(context.cacheDir, "snapshot-${java.util.UUID.randomUUID()}.db")
        DatabaseSnapshot.export(db.openHelper.writableDatabase, dbFile)
        try {
        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.US).format(Date(timestamp))
        val snapshotId = "SNAP_$dateStr"
        val shopName = preferences.shopName.first()

        // 2. Fetch all entity collections from Room
        val products = db.productDao().getAllProducts().first()
        val movements = db.stockMovementDao().getAllMovements().first()
        val purchases = db.purchaseDao().getAllPurchases().first()
        val purchaseLines = db.purchaseDao().getAllPurchaseLines().first()
        val sales = db.saleDao().getAllSalesWithVoided().first()
        val saleLines = db.saleDao().getAllSaleLines().first()
        val accountPayments = db.accountPaymentDao().getAllAccountPayments().first()
        val customers = db.customerDao().getAllIncludingArchived().first()
        val suppliers = db.supplierDao().getAllIncludingArchived().first()
        val cashExpenses = db.shopExpenseDao().getAllIncludingArchived().first()
        val pigmySchemes = db.pigmySchemeDao().getAllSchemes().first()
        val pigmyDeposits = db.pigmyDepositDao().getAllDeposits().first()
        val pigmyWithdrawals = db.pigmyWithdrawalDao().getAllWithdrawals().first()
        val services = db.serviceDefinitionDao().getAllServices().first()
        val cashCategories = db.cashCategoryDao().getAllCategories().first()

        val attachmentsDir = File(context.filesDir, "attachments")
        val photoFiles = if (attachmentsDir.exists() && attachmentsDir.isDirectory) {
            attachmentsDir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: emptyList()
        } else emptyList()

        val referencedPaths = (products.mapNotNull { it.photoUri } + purchases.mapNotNull { it.photoUri } + cashExpenses.mapNotNull { it.attachmentUri }).filter { it.isNotBlank() }
        referencedPaths.forEach { path ->
            require(photoFiles.any { it.name == File(path.removePrefix("file://")).name }) { "Backup incomplete: referenced attachment is missing ($path)" }
        }

        val settingsBytes = preferences.exportSupportedSettings().toString().toByteArray(Charsets.UTF_8)
        val snapshotData = ShopSnapshotData(
            snapshotId = snapshotId,
            timestamp = timestamp,
            shopName = shopName,
            products = products,
            movements = movements,
            purchases = purchases,
            purchaseLines = purchaseLines,
            sales = sales,
            saleLines = saleLines,
            accountPayments = accountPayments,
            customers = customers,
            suppliers = suppliers,
            cashExpenses = cashExpenses,
            pigmySchemes = pigmySchemes,
            pigmyDeposits = pigmyDeposits,
            pigmyWithdrawals = pigmyWithdrawals,
            services = services,
            cashCategories = cashCategories,
            photoCount = photoFiles.size
        )

        // 3. Build Output A: Complete recovery archive
        val completeBackupFileName = "Ali_Educational_Complete_Backup_$dateStr.zip"
        val completeBackupFile = File(backupFolder, completeBackupFileName)
        val tempCompleteZip = File(context.cacheDir, "temp_$completeBackupFileName")

        ZipOutputStream(FileOutputStream(tempCompleteZip)).use { zipOut ->
            // A. Main SQLite DB file
            FileInputStream(dbFile).use { fileIn ->
                zipOut.putNextEntry(ZipEntry("ali_educational.db"))
                fileIn.copyTo(zipOut)
                zipOut.closeEntry()
            }

            // B. Manifest JSON
            val manifestJson = JSONObject().apply {
                put("formatVersion", 2)
                put("dbSha256", sha256(dbFile))
                put("settingsSha256", java.security.MessageDigest.getInstance("SHA-256").digest(settingsBytes).joinToString("") { "%02x".format(it) })
                put("attachments", JSONObject().apply { photoFiles.forEach { put(it.name, sha256(it)) } })
                put("app", "Ali Educational")
                put("packageName", context.packageName)
                put("schemaVersion", 8)
                put("snapshotId", snapshotId)
                put("timestamp", timestamp)
                put("date", dateStr)
                put("dbSizeBytes", dbFile.length())
                put("productCount", products.size)
                put("billCount", sales.size)
                put("customerCount", customers.size)
                put("supplierCount", suppliers.size)
                put("cashInOutCount", cashExpenses.size)
                put("pigmyCount", pigmyDeposits.size + pigmyWithdrawals.size)
                put("photoCount", photoFiles.size)
            }.toString(2)

            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write(manifestJson.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry("settings.json"))
            zipOut.write(settingsBytes)
            zipOut.closeEntry()

            // C. Attachment photos
            photoFiles.forEach { file ->
                zipOut.putNextEntry(ZipEntry("attachments/${file.name}"))
                FileInputStream(file).use { fIn -> fIn.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }

        tempCompleteZip.copyTo(completeBackupFile, overwrite = true)
        tempCompleteZip.delete()

        // 4. Build Output B: Readable Excel records
        val excelFileName = "Ali_Educational_Shop_Records_$dateStr.xlsx"
        val excelFile = File(backupFolder, excelFileName)
        excelGenerator.generateWorkbook(snapshotData, excelFile)

        // 5. Build Outer Save Both package
        val packageFileName = "Ali_Educational_Backup_Package_$dateStr.zip"
        val packageFile = File(backupFolder, packageFileName)
        val tempPackageZip = File(context.cacheDir, "temp_$packageFileName")

        ZipOutputStream(FileOutputStream(tempPackageZip)).use { zipOut ->
            // Add Complete_Backup.zip
            FileInputStream(completeBackupFile).use { fIn ->
                zipOut.putNextEntry(ZipEntry("Complete_Backup.zip"))
                fIn.copyTo(zipOut)
                zipOut.closeEntry()
            }

            // Add Shop_Records.xlsx
            FileInputStream(excelFile).use { fIn ->
                zipOut.putNextEntry(ZipEntry("Shop_Records.xlsx"))
                fIn.copyTo(zipOut)
                zipOut.closeEntry()
            }

            // Add Read_Me.txt
            val readMeText = """
                Ali Educational - Complete Backup Package
                Snapshot ID: $snapshotId
                Created: $dateStr

                Package Contents:
                1. Complete_Backup.zip:
                   The full offline database, settings, and photo attachments archive.
                   Use this file in the Ali Educational app under "Restore Backup" to completely restore your shop data.
                   You do NOT need to extract this zip file manually.

                2. Shop_Records.xlsx:
                   Human-readable Excel workbook containing all business records across 11 sheets:
                   - Summary
                   - Products and Stock
                   - Stock Movements
                   - Bills and Items
                   - Payments and Dues
                   - Customers and Suppliers
                   - Cash In Out
                   - Expenses and Refunds
                   - Services
                   - Pigmy Savings
                   - Categories

                Notice: 100% offline and securely packaged for Ali Educational.
            """.trimIndent()

            zipOut.putNextEntry(ZipEntry("Read_Me.txt"))
            zipOut.write(readMeText.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        if (passphrase != null && passphrase.isNotEmpty()) {
            FileInputStream(tempPackageZip).use { inStream ->
                FileOutputStream(packageFile).use { outStream ->
                    BackupCrypto.encrypt(inStream, outStream, passphrase)
                }
            }
            tempPackageZip.delete()
        } else {
            tempPackageZip.copyTo(packageFile, overwrite = true)
            tempPackageZip.delete()
        }
        // Secure storage: public Documents directory copy removed to prevent data leakage (SEC-06c).

        // 7. If connected Google Drive / external folder URI is configured, auto-export
        val connectedFolderUri = preferences.googleDriveFolderUri.first()
        if (connectedFolderUri.isNotBlank()) {
            val (saved, message) = saveBackupToConnectedFolder(packageFile, connectedFolderUri)
            check(saved) { "Local backup saved at ${packageFile.absolutePath}. External copy failed: $message. Retry external save." }
        }

        preferences.setLastBackupTime(timestamp)

        BackupMetadata(
            fileName = packageFileName,
            filePath = packageFile.absolutePath,
            sizeBytes = packageFile.length(),
            timestamp = timestamp,
            isValid = true,
            integrityNote = "Snapshot saved; integrity checks available in backup details",
            snapshotId = snapshotId,
            packageFilePath = packageFile.absolutePath,
            completeBackupFilePath = completeBackupFile.absolutePath,
            excelFilePath = excelFile.absolutePath,
            photoCount = photoFiles.size,
            productCount = products.size,
            billCount = sales.size,
            customerCount = customers.size,
            supplierCount = suppliers.size,
            cashInOutCount = cashExpenses.size,
            pigmyCount = pigmyDeposits.size + pigmyWithdrawals.size
        )
        } finally { dbFile.delete() }
        }
        }
    }

    suspend fun saveBackupToConnectedFolder(packageFile: File, treeUriStr: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            if (treeUriStr.isBlank()) {
                return@withContext Pair(false, "No Google Drive or external backup folder connected")
            }
            val treeUri = Uri.parse(treeUriStr)
            val docFolder = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Pair(false, "Cannot open connected backup folder")
            if (!docFolder.canWrite()) {
                return@withContext Pair(false, "Folder write permission expired or revoked. Please reconnect folder.")
            }

            // Create or replace package file in connected folder
            val targetDoc = docFolder.createFile("application/zip", packageFile.name)
                ?: return@withContext Pair(false, "Failed to create backup in connected folder")

            context.contentResolver.openOutputStream(targetDoc.uri)?.use { out ->
                FileInputStream(packageFile).use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext Pair(false, "Failed to open output stream to destination")

            val expectedDigest = sha256(packageFile)
            val actualDigest = (context.contentResolver.openInputStream(targetDoc.uri) ?: error("Destination cannot be reopened for verification")).use { input ->
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(65536)
                while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            check(actualDigest == expectedDigest) { "External backup checksum mismatch; retry the upload" }
            val destName = docFolder.name ?: "Connected Folder"
            preferences.setLastExternalSave(System.currentTimeMillis(), destName)
            Pair(true, "Successfully saved to $destName")
        } catch (e: Exception) {
            Pair(false, "External save error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun getAvailableBackups(): List<BackupMetadata> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<File>()
        primaryBackupFolder.listFiles { file ->
            file.extension.equals("zip", ignoreCase = true)
        }?.let { allFiles.addAll(it) }

        // Find package files and complete backup files
        val packages = allFiles.filter { it.name.startsWith("Ali_Educational_Backup_Package_") }
        val completeBackups = allFiles.filter { it.name.startsWith("Ali_Educational_Complete_Backup_") || it.name.startsWith("ali_backup_") }

        val resultList = mutableListOf<BackupMetadata>()

        // 1. Process package files
        packages.sortedByDescending { it.lastModified() }.forEach { pkgFile ->
            val (isValid, note, manifest) = inspectPackageOrBackup(pkgFile)
            val baseName = pkgFile.nameWithoutExtension.removePrefix("Ali_Educational_Backup_Package_")
            val excelFile = File(primaryBackupFolder, "Ali_Educational_Shop_Records_$baseName.xlsx")
            val completeFile = File(primaryBackupFolder, "Ali_Educational_Complete_Backup_$baseName.zip")

            resultList.add(
                BackupMetadata(
                    fileName = pkgFile.name,
                    filePath = pkgFile.absolutePath,
                    sizeBytes = pkgFile.length(),
                    timestamp = pkgFile.lastModified(),
                    isValid = isValid,
                    integrityNote = note,
                    snapshotId = manifest?.optString("snapshotId") ?: ("SNAP_$baseName"),
                    packageFilePath = pkgFile.absolutePath,
                    completeBackupFilePath = if (completeFile.exists()) completeFile.absolutePath else null,
                    excelFilePath = if (excelFile.exists()) excelFile.absolutePath else null,
                    photoCount = manifest?.optInt("photoCount") ?: 0,
                    productCount = manifest?.optInt("productCount") ?: 0,
                    billCount = manifest?.optInt("billCount") ?: 0,
                    customerCount = manifest?.optInt("customerCount") ?: 0,
                    supplierCount = manifest?.optInt("supplierCount") ?: 0,
                    cashInOutCount = manifest?.optInt("cashInOutCount") ?: 0,
                    pigmyCount = manifest?.optInt("pigmyCount") ?: 0
                )
            )
        }

        // 2. Add standalone complete backups if no corresponding package was created
        completeBackups.sortedByDescending { it.lastModified() }.forEach { cbFile ->
            val isAlreadyRepresented = resultList.any { it.completeBackupFilePath == cbFile.absolutePath }
            if (!isAlreadyRepresented) {
                val (isValid, note, manifest) = inspectPackageOrBackup(cbFile)
                resultList.add(
                    BackupMetadata(
                        fileName = cbFile.name,
                        filePath = cbFile.absolutePath,
                        sizeBytes = cbFile.length(),
                        timestamp = cbFile.lastModified(),
                        isValid = isValid,
                        integrityNote = note,
                        snapshotId = manifest?.optString("snapshotId") ?: "",
                        completeBackupFilePath = cbFile.absolutePath,
                        photoCount = manifest?.optInt("photoCount") ?: 0,
                        productCount = manifest?.optInt("productCount") ?: 0,
                        billCount = manifest?.optInt("billCount") ?: 0,
                        customerCount = manifest?.optInt("customerCount") ?: 0,
                        supplierCount = manifest?.optInt("supplierCount") ?: 0
                    )
                )
            }
        }

        resultList.sortByDescending { it.timestamp }
        return@withContext resultList
    }

    private fun sha256(file: File): String = java.security.MessageDigest.getInstance("SHA-256").let { digest ->
        file.inputStream().use { stream ->
            val buffer = ByteArray(65536)
            while (true) { val n = stream.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractArchive(file: File, stage: File, depth: Int = 0): File {
        require(depth <= 1) { "Nested archive limit" }
        var total = 0L
        var count = 0
        val seen = mutableSetOf<String>()
        java.util.zip.ZipInputStream(file.inputStream()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                require(++count <= 10000) { "Too many files" }
                val target = File(stage, e.name).canonicalFile
                require(target.path.startsWith(stage.canonicalPath + File.separator) && seen.add(target.path)) { "Unsafe or duplicate archive path" }
                if (!e.isDirectory) {
                    target.parentFile!!.mkdirs()
                    target.outputStream().use { out ->
                        val buffer = ByteArray(65536)
                        while (true) { val n = zip.read(buffer); if (n < 0) break; total += n; require(total <= 512L * 1024 * 1024) { "Archive exceeds 512 MB" }; out.write(buffer, 0, n) }
                    }
                }
                zip.closeEntry()
            }
        }
        val dbFile = File(stage, "ali_educational.db")
        if (dbFile.exists()) return stage
        val inner = stage.listFiles()!!.filter { it.name == "Complete_Backup.zip" || it.name.startsWith("Ali_Educational_Complete_Backup_") }
        require(inner.size == 1) { "Missing or ambiguous recovery archive" }
        val sub = File(stage, "validated-inner").apply { mkdirs() }
        return extractArchive(inner.single(), sub, depth + 1)
    }

    private fun validateStage(stage: File): JSONObject? {
        val file = File(stage, "ali_educational.db")
        val manifestFile = File(stage, "manifest.json")
        val manifest = if (manifestFile.exists()) JSONObject(manifestFile.readText()) else null
        if (manifest?.has("dbSha256") == true) require(manifest.getString("dbSha256") == sha256(file)) { "Database checksum mismatch" }
        manifest?.optJSONObject("attachments")?.let { hashes ->
            hashes.keys().forEach { name ->
                val attachment = File(stage, "attachments/$name")
                require(attachment.isFile && sha256(attachment) == hashes.getString(name)) { "Missing or damaged attachment" }
            }
        }
        if (manifest?.has("settingsSha256") == true) require(sha256(File(stage, "settings.json")) == manifest.getString("settingsSha256")) { "Settings checksum mismatch" }
        android.database.sqlite.SQLiteDatabase.openDatabase(file.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { raw ->
            require(raw.version in 1..9) { "Unsupported backup schema ${raw.version}" }
            raw.rawQuery("PRAGMA integrity_check", null).use { c -> require(c.moveToFirst() && c.getString(0) == "ok") { "Corrupt database" } }
        }
        // Never trust a stored identity hash as proof that actual tables still match.
        android.database.sqlite.SQLiteDatabase.openDatabase(file.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { raw ->
            raw.execSQL("DROP TABLE IF EXISTS room_master_table")
        }
        // Room validates every table when its identity marker is absent, and migrates older schemas.
        val staged = Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9)
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE).build()
        try { staged.openHelper.writableDatabase.query("SELECT COUNT(*) FROM products").use { it.moveToFirst() } }
        finally { staged.close() }
        val settingsFile = File(stage, "settings.json")
        if (settingsFile.exists()) require(JSONObject(settingsFile.readText()).optInt("version", 1) == 1) { "Unsupported settings" }
        return manifest
    }

    private fun inspectPackageOrBackup(file: File): Triple<Boolean, String, JSONObject?> {
        val stage = File(context.cacheDir, "inspect-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        return try {
            val root = extractArchive(file, stage)
            val manifest = validateStage(root)
            Triple(true, "Database schema and integrity checked", manifest)
        } catch (e: Exception) { Triple(false, "Invalid backup: ${e.message}", null) }
        finally { stage.deleteRecursively() }
    }

    suspend fun restoreBackup(backupFile: File, passphrase: CharArray? = null): Boolean = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val stage = File(context.cacheDir, "restore-${java.util.UUID.randomUUID()}").apply { mkdirs() }
            val previousSettings = preferences.exportSupportedSettings()
            val writtenFiles = mutableListOf<File>()
            try {
                val isEncrypted = backupFile.inputStream().use { BackupCrypto.isEncryptedBackup(it) }
                val targetArchive = if (isEncrypted) {
                    require(passphrase != null && passphrase.isNotEmpty()) {
                        "This backup is encrypted and requires a passphrase to restore."
                    }
                    val decrypted = File(stage, "decrypted_archive.zip")
                    val success = backupFile.inputStream().use { inStream ->
                        FileOutputStream(decrypted).use { outStream ->
                            BackupCrypto.decrypt(inStream, outStream, passphrase)
                        }
                    }
                    require(success) {
                        "Incorrect backup passphrase or corrupted archive."
                    }
                    decrypted
                } else {
                    backupFile
                }

                val root = extractArchive(targetArchive, stage)
                validateStage(root)
                val source = File(root, "ali_educational.db")
                val settings = File(root, "settings.json").takeIf { it.exists() }?.let { JSONObject(it.readText()) }
                // Copy attachments under unique names first; a failed DB transaction cannot overwrite live evidence.
                val remap = mutableMapOf<String, String>()
                File(root, "attachments").listFiles()?.filter { it.isFile }?.forEach { original ->
                    val destination = File(context.filesDir, "attachments/restored-${java.util.UUID.randomUUID()}-${original.name}")
                    destination.parentFile!!.mkdirs(); original.copyTo(destination)
                    writtenFiles.add(destination); remap[original.name] = destination.absolutePath
                }
                val safety = File(context.filesDir, "pre_restore_safety/safety-${System.currentTimeMillis()}.db")
                safety.parentFile!!.mkdirs()
                db.withTransaction {
                    DatabaseSnapshot.export(db.openHelper.writableDatabase, safety)
                    if (settings != null) preferences.importSupportedSettings(settings)
                    android.database.sqlite.SQLiteDatabase.openDatabase(source.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { raw ->
                        DatabaseSnapshot.replace(raw, db.openHelper.writableDatabase)
                    }
                    for ((table, column) in listOf("shop_expenses" to "attachmentUri", "products" to "photoUri", "purchases" to "photoUri")) {
                        val changes = mutableListOf<Pair<String, String>>()
                        db.openHelper.writableDatabase.query("SELECT id, $column FROM $table WHERE $column IS NOT NULL").use { c ->
                            while (c.moveToNext()) {
                                val old = c.getString(1); val replacement = remap[File(old.removePrefix("file://")).name]
                                require(replacement != null) { "Missing attachment for $table ${c.getString(0)}" }
                                changes.add(c.getString(0) to replacement)
                            }
                        }
                        changes.forEach { (id, path) -> db.openHelper.writableDatabase.execSQL("UPDATE $table SET $column = ? WHERE id = ?", arrayOf(path, id)) }
                    }
                }
                true
            } catch (e: Exception) {
                preferences.importSupportedSettings(previousSettings)
                writtenFiles.forEach { it.delete() }
                throw e
            } finally { stage.deleteRecursively() }
        }
    }

    suspend fun restoreFromUri(uri: Uri, passphrase: CharArray? = null): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "imported_restore_${System.currentTimeMillis()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            val result = restoreBackup(tempFile, passphrase)
            return@withContext result
        } finally {
            tempFile.delete()
        }
    }

    fun getShareIntent(filePath: String): Intent {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = when {
            file.extension.equals("xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/zip"
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ali Educational Backup - " + file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun exportBackupToUri(backupFile: File, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            (context.contentResolver.openOutputStream(targetUri) ?: error("Destination unavailable")).use { out ->
                FileInputStream(backupFile).use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.w("BackupRestoreService", "Failed to export backup to target URI: ${e.message}", e)
            false
        }
    }
}
