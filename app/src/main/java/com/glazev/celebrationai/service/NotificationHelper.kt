package com.glazev.celebrationai.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.glazev.celebrationai.data.Celebration
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAllNotifications(celebration: Celebration) {
        // Основное уведомление (в день события)
        scheduleNotification(celebration, 0, "EVENT")
        
        // Предварительное уведомление (за X дней)
        if (celebration.reminderDaysBefore > 0) {
            scheduleNotification(celebration, -celebration.reminderDaysBefore, "PRE_EVENT")
        }
        
        // Уведомление об идеях подарков (за 7 дней по умолчанию, если не совпадает с пред. уведомлением)
        if (celebration.reminderDaysBefore != 7) {
            scheduleNotification(celebration, -7, "GIFT_IDEAS")
        }
    }

    fun cancelAllNotifications(celebration: Celebration) {
        cancelNotification(celebration, "EVENT")
        cancelNotification(celebration, "PRE_EVENT")
        cancelNotification(celebration, "GIFT_IDEAS")
    }

    private fun scheduleNotification(celebration: Celebration, daysOffset: Int, type: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                setInexactNotification(celebration, daysOffset, type)
                return
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("celebration_id", celebration.id)
            putExtra("notification_type", type)
        }
        
        val requestCode = when (type) {
            "EVENT" -> celebration.id
            "PRE_EVENT" -> celebration.id + 10000
            else -> celebration.id + 5000
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = calculateTriggerTime(celebration, daysOffset)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, 
                calendar.timeInMillis, 
                pendingIntent
            )
        } catch (e: SecurityException) {
            setInexactNotification(celebration, daysOffset, type)
        }
    }

    private fun setInexactNotification(celebration: Celebration, daysOffset: Int, type: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("celebration_id", celebration.id)
            putExtra("notification_type", type)
        }
        val requestCode = when (type) {
            "EVENT" -> celebration.id
            "PRE_EVENT" -> celebration.id + 10000
            else -> celebration.id + 5000
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = calculateTriggerTime(celebration, daysOffset)
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun cancelNotification(celebration: Celebration, type: String) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val requestCode = when (type) {
            "EVENT" -> celebration.id
            "PRE_EVENT" -> celebration.id + 10000
            else -> celebration.id + 5000
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun calculateTriggerTime(celebration: Celebration, daysOffset: Int): Calendar {
        val eventDate = Calendar.getInstance().apply { timeInMillis = celebration.date }
        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.MONTH, eventDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, eventDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, celebration.reminderHour)
            set(Calendar.MINUTE, celebration.reminderMinute)
            set(Calendar.SECOND, 0)
            
            add(Calendar.DAY_OF_YEAR, daysOffset)
            
            // Если дата в этом году уже прошла, планируем на следующий год
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.YEAR, 1)
            }
        }
        return triggerTime
    }
}
