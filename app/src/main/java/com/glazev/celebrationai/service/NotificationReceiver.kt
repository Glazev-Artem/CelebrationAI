package com.glazev.celebrationai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.glazev.celebrationai.MainActivity
import com.glazev.celebrationai.R
import com.glazev.celebrationai.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val celebrationId = intent.getIntExtra("celebration_id", -1)
        val type = intent.getStringExtra("notification_type") ?: "EVENT"

        if (celebrationId == -1) return

        val dao = AppDatabase.getDatabase(context).celebrationDao()
        
        CoroutineScope(Dispatchers.IO).launch {
            val celebration = dao.getCelebrationById(celebrationId)
            if (celebration != null) {
                showNotification(context, celebration.name, type)
            }
        }
    }

    private fun showNotification(context: Context, name: String, type: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "celebration_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Напоминания о праздниках",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (type == "EVENT") "Сегодня праздник!" else "Скоро праздник!"
        val text = if (type == "EVENT") "Не забудьте поздравить: $name" else "Через неделю: $name. Пора подумать о подарке!"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Убедитесь, что этот ресурс существует
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(name.hashCode(), builder.build())
    }
}
