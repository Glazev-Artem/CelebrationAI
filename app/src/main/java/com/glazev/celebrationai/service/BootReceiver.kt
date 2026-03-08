package com.glazev.celebrationai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.glazev.celebrationai.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val helper = NotificationHelper(context)
            val dao = AppDatabase.getDatabase(context).celebrationDao()
            
            CoroutineScope(Dispatchers.IO).launch {
                val celebrations = dao.getAllCelebrationsSync()
                celebrations.forEach {
                    helper.scheduleAllNotifications(it)
                }
            }
        }
    }
}
