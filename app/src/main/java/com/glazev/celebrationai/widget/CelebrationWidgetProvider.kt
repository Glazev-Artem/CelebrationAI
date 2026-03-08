package com.glazev.celebrationai.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.glazev.celebrationai.MainActivity
import com.glazev.celebrationai.R

class CelebrationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.celebration_widget)
            
            // Клик по заголовку открывает приложение
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            // Настройка сервиса для списка
            val serviceIntent = Intent(context, CelebrationRemoteViewsService::class.java)
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            
            // Настройка шаблона клика для элементов списка
            val clickIntent = Intent(context, MainActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
