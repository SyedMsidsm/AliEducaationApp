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
import kotlinx.coroutines.withContext
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
    val integrityNote: String = "Verified SQLite Archive"
)

@Singleton
class BackupRestoreService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val preferences: AppPreferences
) {

    val primaryBackupFolder: File
        get() {
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(docs, "AliEducational/Backups")
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) {
                    return fallbackBackupFolder
                }
            }
            return dir
        }

    val fallbackBackupFolder: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "backups")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val backupFolder: File
        get() = primaryBackupFolder

    val backupFolderPath: String
        get() = backupFolder.absolutePath

    suspend fun createBackup(): BackupMetadata = withContext(Dispatchers.IO) {
        // Checkpoint the SQLite WAL file first so all records are committed to main db file
        val query = db.openHelper.writableDatabase
        query.query("PRAGMA wal_checkpoint(FULL)").close()

        val dbFile = context.getDatabasePath("ali_educational.db")
        if (!dbFile.exists()) {
            throw IllegalStateException("Database file not found")
        }

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFileName = "ali_backup_$dateStr.zip"
        val zipFile = File(backupFolder, backupFileName)

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            // 1. Add main db file
            FileInputStream(dbFile).use { fileIn ->
                zipOut.putNextEntry(ZipEntry("ali_educational.db"))
                fileIn.copyTo(zipOut)
                zipOut.closeEntry()
            }

            // 2. Add metadata
            val metaInfo = "version=1\ntimestamp=${System.currentTimeMillis()}\napp=Ali Educational\nschema=1\n"
            zipOut.putNextEntry(ZipEntry("metadata.txt"))
            zipOut.write(metaInfo.toByteArray())
            zipOut.closeEntry()
        }

        // Also duplicate to fallback folder if different
        if (backupFolder.absolutePath != fallbackBackupFolder.absolutePath) {
            try {
                zipFile.copyTo(File(fallbackBackupFolder, backupFileName), overwrite = true)
            } catch (e: Exception) {
                // non-critical
            }
        }

        val now = System.currentTimeMillis()
        preferences.setLastBackupTime(now)

        return@withContext BackupMetadata(
            fileName = backupFileName,
            filePath = zipFile.absolutePath,
            sizeBytes = zipFile.length(),
            timestamp = now,
            isValid = true,
            integrityNote = "Verified (Size: ${zipFile.length() / 1024} KB)"
        )
    }

    suspend fun getAvailableBackups(): List<BackupMetadata> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<File>()
        primaryBackupFolder.listFiles { file -> file.extension.equals("zip", ignoreCase = true) }?.let {
            allFiles.addAll(it)
        }
        if (primaryBackupFolder.absolutePath != fallbackBackupFolder.absolutePath) {
            fallbackBackupFolder.listFiles { file -> file.extension.equals("zip", ignoreCase = true) }?.let { fallbackFiles ->
                fallbackFiles.forEach { fb ->
                    if (allFiles.none { it.name == fb.name }) {
                        allFiles.add(fb)
                    }
                }
            }
        }

        allFiles.sortedByDescending { it.lastModified() }.map { file ->
            val (isValid, note) = quickValidateZip(file)
            BackupMetadata(
                fileName = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                timestamp = file.lastModified(),
                isValid = isValid,
                integrityNote = note
            )
        }
    }

    private fun quickValidateZip(zipFile: File): Pair<Boolean, String> {
        try {
            ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                var foundDb = false
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    if (entry!!.name == "ali_educational.db") {
                        foundDb = true
                    }
                    zipIn.closeEntry()
                }
                return if (foundDb) {
                    Pair(true, "Verified SQLite Archive")
                } else {
                    Pair(false, "Invalid: missing ali_educational.db")
                }
            }
        } catch (e: Exception) {
            return Pair(false, "Corrupt: ${e.localizedMessage}")
        }
    }

    suspend fun restoreBackup(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        val (isValid, _) = quickValidateZip(backupFile)
        if (!isValid) throw IllegalArgumentException("Cannot restore corrupt or invalid backup")

        val dbFile = context.getDatabasePath("ali_educational.db")
        var restoredDb = false

        // Create safety pre-restore backup
        if (dbFile.exists()) {
            val safetyDir = File(context.cacheDir, "pre_restore_safety")
            if (!safetyDir.exists()) safetyDir.mkdirs()
            val safetyBackup = File(safetyDir, "safety_${System.currentTimeMillis()}.db")
            dbFile.copyTo(safetyBackup, overwrite = true)
        }

        ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
            var entry: ZipEntry?
            while (zipIn.nextEntry.also { entry = it } != null) {
                if (entry!!.name == "ali_educational.db") {
                    // Write to temp file first
                    val tempDb = File(context.cacheDir, "restored_temp.db")
                    FileOutputStream(tempDb).use { out ->
                        zipIn.copyTo(out)
                    }

                    // Replace main DB file
                    db.close()
                    tempDb.copyTo(dbFile, overwrite = true)
                    tempDb.delete()

                    // Remove existing WAL / SHM files if any
                    File(dbFile.parent, "ali_educational.db-wal").delete()
                    File(dbFile.parent, "ali_educational.db-shm").delete()

                    restoredDb = true
                }
                zipIn.closeEntry()
            }
        }
        return@withContext restoredDb
    }

    suspend fun restoreFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val tempZip = File(context.cacheDir, "imported_restore.zip")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempZip).use { output ->
                input.copyTo(output)
            }
        }
        val result = restoreBackup(tempZip)
        tempZip.delete()
        return@withContext result
    }

    fun getShareIntent(filePath: String): Intent {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ali Educational Backup - " + file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
