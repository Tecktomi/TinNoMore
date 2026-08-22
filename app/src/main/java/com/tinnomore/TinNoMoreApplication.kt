package com.tinnomore

import android.app.Application
import com.tinnomore.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TinNoMoreApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // Seed demo data on first launch
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.seedIfEmpty(database)
        }
    }
}
