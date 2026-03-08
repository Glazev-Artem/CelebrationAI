package com.glazev.celebrationai.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.glazev.celebrationai.R
import com.glazev.celebrationai.data.AppDatabase
import com.glazev.celebrationai.data.Celebration
import kotlinx.coroutines.runBlocking

class CelebrationRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CelebrationRemoteViewsFactory(this.applicationContext)
    }
}

class CelebrationRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var celebrations: List<Celebration> = emptyList()
    private val dao = AppDatabase.getDatabase(context).celebrationDao()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            celebrations = dao.getAllCelebrationsSync()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = celebrations.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= celebrations.size) return RemoteViews(context.packageName, R.layout.widget_item)
        
        val celebration = celebrations[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        
        views.setTextViewText(R.id.item_name, celebration.name)
        views.setTextViewText(R.id.item_date, "Через ${celebration.daysUntil()} дн.")
        
        // Создаем fill-in intent для клика по элементу
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = if (position < celebrations.size) celebrations[position].id.toLong() else position.toLong()
    override fun hasStableIds(): Boolean = true
}
