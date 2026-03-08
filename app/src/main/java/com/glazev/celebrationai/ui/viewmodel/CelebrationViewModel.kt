package com.glazev.celebrationai.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glazev.celebrationai.R
import com.glazev.celebrationai.data.Celebration
import com.glazev.celebrationai.data.CelebrationDao
import com.glazev.celebrationai.data.CelebrationType
import com.glazev.celebrationai.service.GeminiService
import com.glazev.celebrationai.service.NotificationHelper
import com.glazev.celebrationai.service.SyncManager
import com.glazev.celebrationai.widget.CelebrationWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CelebrationViewModel(
    application: Application,
    private val dao: CelebrationDao,
    private val geminiService: GeminiService,
    private val notificationHelper: NotificationHelper,
    private val syncManager: SyncManager
) : AndroidViewModel(application) {

    val allCelebrations: StateFlow<List<Celebration>> = dao.getAllCelebrations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun updateWidgets() {
        try {
            val intent = Intent(getApplication(), CelebrationWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(ComponentName(getApplication(), CelebrationWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            getApplication<Application>().sendBroadcast(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun sync() {
        viewModelScope.launch {
            try {
                syncManager.syncToCloud()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun loadFromCloud() {
        viewModelScope.launch {
            try {
                syncManager.syncFromCloud()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addCelebration(celebration: Celebration, onIdGenerated: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val id = dao.insertCelebration(celebration).toInt()
            notificationHelper.scheduleAllNotifications(celebration.copy(id = id))
            updateWidgets()
            sync()
            onIdGenerated(id)
        }
    }

    fun updateCelebration(celebration: Celebration) {
        viewModelScope.launch {
            dao.insertCelebration(celebration)
            notificationHelper.cancelAllNotifications(celebration)
            notificationHelper.scheduleAllNotifications(celebration)
            updateWidgets()
            sync()
        }
    }

    fun saveGreeting(celebration: Celebration, greeting: String) {
        viewModelScope.launch {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val entry = "$year:$greeting"
            val historyList = if (celebration.greetingHistory.isEmpty()) mutableListOf() 
                             else celebration.greetingHistory.split("|").toMutableList()
            
            // Проверка, чтобы не дублировать один и тот же год
            val yearPrefix = "$year:"
            historyList.removeAll { it.startsWith(yearPrefix) }
            historyList.add(entry)
            
            val newHistory = historyList.joinToString("|")
            dao.insertCelebration(celebration.copy(savedGreeting = greeting, greetingHistory = newHistory))
            updateWidgets()
            sync()
        }
    }

    fun updateWishlist(celebration: Celebration, wishlist: String) {
        viewModelScope.launch {
            dao.insertCelebration(celebration.copy(wishlist = wishlist))
            sync()
        }
    }

    fun deleteHistoryEntry(celebration: Celebration, historyIndex: Int) {
        viewModelScope.launch {
            val historyItems = celebration.greetingHistory.split("|").toMutableList()
            val reversedIndex = historyItems.size - 1 - historyIndex
            if (reversedIndex in historyItems.indices) {
                historyItems.removeAt(reversedIndex)
                val newHistory = historyItems.joinToString("|")
                dao.insertCelebration(celebration.copy(greetingHistory = newHistory))
                sync()
            }
        }
    }

    fun deleteCelebration(celebration: Celebration) {
        viewModelScope.launch {
            dao.deleteCelebration(celebration)
            notificationHelper.cancelAllNotifications(celebration)
            updateWidgets()
            syncManager.deleteFromCloud(celebration.id)
        }
    }

    @SuppressLint("Range")
    fun importFromContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = getApplication<Application>().contentResolver
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Event.START_DATE, ContactsContract.Contacts.DISPLAY_NAME)
            val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
            val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString())
            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
            var count = 0
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    val bdayString = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE))
                    parseBirthday(bdayString)?.let { date ->
                        if (!allCelebrations.value.any { c -> c.name == name }) {
                            dao.insertCelebration(Celebration(name = name, date = date.time, type = CelebrationType.BIRTHDAY))
                            count++
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                if (count > 0) { updateWidgets(); sync(); Toast.makeText(getApplication(), "Импортировано: $count", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun parseBirthday(dateStr: String): Date? {
        val formats = listOf(SimpleDateFormat("yyyy-MM-dd", Locale.US), SimpleDateFormat("--MM-dd", Locale.US), SimpleDateFormat("dd.MM.yyyy", Locale.US))
        for (format in formats) { try { return format.parse(dateStr) } catch (e: Exception) {} }
        return null
    }

    suspend fun generateAiGreeting(celebration: Celebration, isApology: Boolean = false): String? {
        return geminiService.generateGreeting(celebration, isApology)
    }

    suspend fun generateGiftIdeas(celebration: Celebration): String? {
        return geminiService.generateGiftIdeas(celebration)
    }

    suspend fun generateFunFacts(celebration: Celebration): String? {
        return geminiService.generateFunFacts(celebration.date)
    }
}
