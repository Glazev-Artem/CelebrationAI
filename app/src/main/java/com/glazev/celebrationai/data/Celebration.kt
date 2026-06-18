package com.glazev.celebrationai.data

import androidx.room.Entity
import androidx.annotation.StringRes
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.glazev.celebrationai.R

enum class CelebrationTone(@StringRes val displayNameRes: Int) {
    ROMANTIC(R.string.tone_romantic),
    SOLEMN(R.string.tone_solemn),
    OFFICIAL(R.string.tone_official),
    HUMOROUS(R.string.tone_humorous),
    DARK_HUMOR(R.string.tone_dark_humor)
}

enum class CelebrationType(@StringRes val displayNameRes: Int) {
    BIRTHDAY(R.string.type_birthday),
    WEDDING(R.string.type_wedding),
    ANNIVERSARY(R.string.type_anniversary),
    OTHER(R.string.type_other)
}

enum class CelebrationGroup(@StringRes val displayNameRes: Int) {
    NONE(R.string.group_none),
    FAMILY(R.string.group_family),
    FRIENDS(R.string.group_friends),
    WORK(R.string.group_work),
    IMPORTANT(R.string.group_important)
}

@Entity(tableName = "celebrations")
data class Celebration(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val type: CelebrationType = CelebrationType.BIRTHDAY,
    val customType: String = "",
    val date: Long = 0,
    val hobby: String = "",
    val profession: String = "",
    val tone: CelebrationTone = CelebrationTone.SOLEMN,
    val group: CelebrationGroup = CelebrationGroup.NONE,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderDaysBefore: Int = 0,
    val savedGreeting: String? = null,
    val giftIdeas: String? = null,
    val giftSearchQuery: String? = null,
    val funFacts: String? = null,
    val greetingHistory: String = "",
    val wishlist: String = "", // Новое поле для хранения ссылок на подарки
    val estimatedBudget: Int = 0,
    val hasYear: Boolean = true
) {
    // Пустой конструктор для Firebase
    constructor() : this(0, "", CelebrationType.BIRTHDAY, "", 0)

    fun calculateAge(): Int? {
        if (!hasYear) return null
        val eventDate = Calendar.getInstance().apply { timeInMillis = date }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - eventDate.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < eventDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    fun daysUntil(): Long {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val eventDate = Calendar.getInstance().apply { timeInMillis = date }
        val nextOccurrence = Calendar.getInstance().apply {
            set(Calendar.MONTH, eventDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, eventDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(today)) {
                add(Calendar.YEAR, 1)
            }
        }
        val diff = nextOccurrence.timeInMillis - today.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    fun getEffectiveTypeDisplay(context: android.content.Context): String {
        return if (type == CelebrationType.OTHER && customType.isNotBlank()) customType else context.getString(type.displayNameRes)
    }
}
