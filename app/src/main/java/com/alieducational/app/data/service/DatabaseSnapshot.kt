package com.alieducational.app.data.service

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

/** Call export/replace inside the caller's Room transaction. No live database file replacement. */
object DatabaseSnapshot {
    private fun quote(name: String) = "\"" + name.replace("\"", "\"\"") + "\""
    fun export(source: SupportSQLiteDatabase, target: File) {
        check(!target.exists()) { "Snapshot already exists" }
        SQLiteDatabase.openOrCreateDatabase(target, null).use { destination ->
            destination.beginTransaction()
            try {
                val tables = mutableListOf<Pair<String, String>>()
                source.query("SELECT name, sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").use { c ->
                    while (c.moveToNext()) tables.add(c.getString(0) to c.getString(1))
                }
                for ((name, sql) in tables) {
                    if (name == "android_metadata") destination.execSQL("DROP TABLE IF EXISTS android_metadata")
                    destination.execSQL(sql)
                    source.query("SELECT * FROM ${quote(name)}").use { c ->
                        val insert = "INSERT INTO ${quote(name)} VALUES (${List(c.columnCount) { "?" }.joinToString()})"
                        while (c.moveToNext()) destination.execSQL(insert, values(c))
                    }
                }
                source.query("SELECT sql FROM sqlite_master WHERE type='index' AND sql IS NOT NULL").use { c ->
                    while (c.moveToNext()) destination.execSQL(c.getString(0))
                }
                destination.version = source.version
                destination.setTransactionSuccessful()
            } finally { destination.endTransaction() }
        }
    }
    fun replace(source: SQLiteDatabase, destination: SupportSQLiteDatabase) {
        val names = mutableListOf<String>()
        source.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table'", null).use { c ->
            while (c.moveToNext()) names.add(c.getString(0))
        }
        for (name in names) destination.execSQL("DELETE FROM ${quote(name)}")
        for (name in names) source.rawQuery("SELECT * FROM ${quote(name)}", null).use { c ->
            val columns = c.columnNames.joinToString { quote(it) }
            val sql = "INSERT INTO ${quote(name)} ($columns) VALUES (${List(c.columnCount) { "?" }.joinToString()})"
            while (c.moveToNext()) destination.execSQL(sql, values(c))
        }
    }
    private fun values(c: android.database.Cursor): Array<Any?> = Array(c.columnCount) { i ->
        when (c.getType(i)) {
            android.database.Cursor.FIELD_TYPE_NULL -> null
            android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
            android.database.Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
            android.database.Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
            else -> c.getString(i)
        }
    }
}
