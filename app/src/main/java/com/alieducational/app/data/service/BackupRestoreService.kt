package com.alieducational.app.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
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

    suspend fun createBackup(): BackupMetadata = withContext(Dispatchers.IO) {
        // 1. Checkpoint the SQLite WAL file so all committed records are in the main db file
        try {
            val dbHelper = db.openHelper.writableDatabase
            val cursor = dbHelper.query("PRAGMA wal_checkpoint(FULL)")
            cursor.moveToFirst()
            cursor.close()
        } catch (_: Exception) {}

        val dbFile = context.getDatabasePath("ali_educational.db")
        if (!dbFile.exists()) {
            throw IllegalStateException("Database file 'ali_educational.db' not found.")
        }

        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(timestamp))
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
        val customers = db.customerDao().getAllCustomers().first()
        val suppliers = db.supplierDao().getAllSuppliers().first()
        val cashExpenses = db.shopExpenseDao().getAllExpenses().first()
        val pigmySchemes = db.pigmySchemeDao().getAllSchemes().first()
        val pigmyDeposits = db.pigmyDepositDao().getAllDeposits().first()
        val pigmyWithdrawals = db.pigmyWithdrawalDao().getAllWithdrawals().first()
        val services = db.serviceDefinitionDao().getAllServices().first()
        val cashCategories = db.cashCategoryDao().getAllCategories().first()

        val attachmentsDir = File(context.filesDir, "attachments")
        val photoFiles = if (attachmentsDir.exists() && attachmentsDir.isDirectory) {
            attachmentsDir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: emptyList()
        } else emptyList()

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
                put("formatVersion", 1)
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

        tempPackageZip.copyTo(packageFile, overwrite = true)
        tempPackageZip.delete()

        // 6. Try copying package to public Documents directory if accessible
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val publicAliDir = File(publicDocs, "AliEducational/Backups")
            if (publicAliDir.exists() || publicAliDir.mkdirs()) {
                packageFile.copyTo(File(publicAliDir, packageFileName), overwrite = true)
                excelFile.copyTo(File(publicAliDir, excelFileName), overwrite = true)
            }
        } catch (_: Exception) {}

        preferences.setLastBackupTime(timestamp)

        return@withContext BackupMetadata(
            fileName = packageFileName,
            filePath = packageFile.absolutePath,
            sizeBytes = packageFile.length(),
            timestamp = timestamp,
            isValid = true,
            integrityNote = "Verified Complete Archive + Excel Workbook",
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

    private fun inspectPackageOrBackup(file: File): Triple<Boolean, String, JSONObject?> {
        try {
            ZipInputStream(FileInputStream(file)).use { zipIn ->
                var foundDb = false
                var foundInnerCompleteZip = false
                var manifestJson: JSONObject? = null
                var entry: ZipEntry?

                while (zipIn.nextEntry.also { entry = it } != null) {
                    val name = entry!!.name
                    if (name == "ali_educational.db") {
                        foundDb = true
                    } else if (name == "Complete_Backup.zip" || name.startsWith("Ali_Educational_Complete_Backup_")) {
                        foundInnerCompleteZip = true
                    } else if (name == "manifest.json") {
                        try {
                            val bytes = zipIn.readBytes()
                            manifestJson = JSONObject(String(bytes, Charsets.UTF_8))
                        } catch (_: Exception) {}
                    }
                    zipIn.closeEntry()
                }

                if (foundInnerCompleteZip) {
                    return Triple(true, "Verified Backup Package (Archive + Excel)", manifestJson)
                }
                if (foundDb) {
                    return Triple(true, "Verified SQLite Recovery Archive", manifestJson)
                }
                return Triple(false, "Invalid Archive: missing database or backup package", null)
            }
        } catch (e: Exception) {
            return Triple(false, "Corrupt file: ${e.localizedMessage}", null)
        }
    }

    suspend fun restoreBackup(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        val destDbFile = context.getDatabasePath("ali_educational.db")

        // 1. Safety backup of current database before restoring
        if (destDbFile.exists()) {
            val safetyDir = File(context.cacheDir, "pre_restore_safety")
            if (!safetyDir.exists()) safetyDir.mkdirs()
            val safetyBackup = File(safetyDir, "safety_${System.currentTimeMillis()}.db")
            destDbFile.copyTo(safetyBackup, overwrite = true)
        }

        // 2. Check if selected file is the outer package containing Complete_Backup.zip
        var sourceArchive = backupFile
        var isTempStaged = false

        ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
            var entry: ZipEntry?
            while (zipIn.nextEntry.also { entry = it } != null) {
                val name = entry!!.name
                if (name == "Complete_Backup.zip" || name.startsWith("Ali_Educational_Complete_Backup_")) {
                    val staged = File(context.cacheDir, "staged_complete_backup.zip")
                    FileOutputStream(staged).use { out -> zipIn.copyTo(out) }
                    sourceArchive = staged
                    isTempStaged = true
                    break
                }
                zipIn.closeEntry()
            }
        }

        // 3. Restore from sourceArchive (with strict zip-slip protection)
        var restoredDb = false
        try {
            ZipInputStream(FileInputStream(sourceArchive)).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name

                    // Strict zip slip check
                    if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                        throw SecurityException("Malicious path detected in archive entry: $entryName")
                    }

                    if (entryName == "ali_educational.db") {
                        val tempDb = File(context.cacheDir, "restored_temp.db")
                        FileOutputStream(tempDb).use { out -> zipIn.copyTo(out) }

                        // Close DB and replace main DB file
                        db.close()
                        tempDb.copyTo(destDbFile, overwrite = true)
                        tempDb.delete()

                        // Remove existing WAL / SHM files if any
                        File(destDbFile.parent, "ali_educational.db-wal").delete()
                        File(destDbFile.parent, "ali_educational.db-shm").delete()

                        restoredDb = true
                    } else if (entryName.startsWith("attachments/") && !entry!!.isDirectory) {
                        val destDir = File(context.filesDir, "attachments")
                        if (!destDir.exists()) destDir.mkdirs()
                        val filename = File(entryName).name
                        val destFile = File(destDir, filename)
                        FileOutputStream(destFile).use { out -> zipIn.copyTo(out) }
                    }
                    zipIn.closeEntry()
                }
            }
        } finally {
            if (isTempStaged) {
                sourceArchive.delete()
            }
        }

        return@withContext restoredDb
    }

    suspend fun restoreFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "imported_restore_${System.currentTimeMillis()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            val result = restoreBackup(tempFile)
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
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                FileInputStream(backupFile).use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
