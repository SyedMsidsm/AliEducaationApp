package com.alieducational.app.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.alieducational.app.data.local.AppDatabase

object TestDatabaseProvider {
    fun createInMemory(context: Context = ApplicationProvider.getApplicationContext()): AppDatabase {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
}
