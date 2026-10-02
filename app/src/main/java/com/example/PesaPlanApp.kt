package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.PesaPlanRepository
import com.example.receiver.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PesaPlanApp : Application() {

    lateinit var repository: PesaPlanRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        val database = AppDatabase.getInstance(this)
        repository = PesaPlanRepository(
            transactionDao = database.transactionDao(),
            budgetLimitDao = database.budgetLimitDao()
        )

        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeDefaultsIfEmpty()
        }
    }
}
