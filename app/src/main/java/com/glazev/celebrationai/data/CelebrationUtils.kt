package com.glazev.celebrationai.data

import android.content.Context
import com.glazev.celebrationai.R
import java.util.*

object CelebrationUtils {

    fun getZodiacSignRes(dateMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1 // 1-based

        return when (month) {
            1 -> if (day < 20) R.string.zodiac_capricorn else R.string.zodiac_aquarius
            2 -> if (day < 19) R.string.zodiac_aquarius else R.string.zodiac_pisces
            3 -> if (day < 21) R.string.zodiac_pisces else R.string.zodiac_aries
            4 -> if (day < 20) R.string.zodiac_aries else R.string.zodiac_taurus
            5 -> if (day < 21) R.string.zodiac_taurus else R.string.zodiac_gemini
            6 -> if (day < 21) R.string.zodiac_gemini else R.string.zodiac_cancer
            7 -> if (day < 23) R.string.zodiac_cancer else R.string.zodiac_leo
            8 -> if (day < 23) R.string.zodiac_leo else R.string.zodiac_virgo
            9 -> if (day < 23) R.string.zodiac_virgo else R.string.zodiac_libra
            10 -> if (day < 23) R.string.zodiac_libra else R.string.zodiac_scorpio
            11 -> if (day < 22) R.string.zodiac_scorpio else R.string.zodiac_sagittarius
            12 -> if (day < 22) R.string.zodiac_sagittarius else R.string.zodiac_capricorn
            else -> R.string.error_ai
        }
    }

    fun getChineseZodiacRes(dateMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val year = calendar.get(Calendar.YEAR)
        val resIds = listOf(
            R.string.cz_rat, R.string.cz_ox, R.string.cz_tiger, R.string.cz_rabbit,
            R.string.cz_dragon, R.string.cz_snake, R.string.cz_horse, R.string.cz_goat,
            R.string.cz_monkey, R.string.cz_rooster, R.string.cz_dog, R.string.cz_pig
        )
        return resIds[(year - 4) % 12]
    }
    
    fun getLocalizedLifeStats(context: Context, dateMillis: Long): String {
        val bday = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val now = Calendar.getInstance()
        val diff = now.timeInMillis - bday.timeInMillis
        if (diff < 0) return context.getString(R.string.label_not_born)
        
        val days = diff / (1000 * 60 * 60 * 24)
        return context.getString(R.string.label_days_lived, days)
    }
}
