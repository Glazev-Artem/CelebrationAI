package com.glazev.celebrationai.data

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var defaultHour: Int
        get() = prefs.getInt("default_hour", 9)
        set(value) = prefs.edit().putInt("default_hour", value).apply()

    var defaultMinute: Int
        get() = prefs.getInt("default_minute", 0)
        set(value) = prefs.edit().putInt("default_minute", value).apply()

    var defaultReminderDaysBefore: Int
        get() = prefs.getInt("default_reminder_days", 0)
        set(value) = prefs.edit().putInt("default_reminder_days", value).apply()

    var selectedTheme: AppTheme
        get() = AppTheme.valueOf(prefs.getString("selected_theme", AppTheme.CELEBRATION.name) ?: AppTheme.CELEBRATION.name)
        set(value) = prefs.edit().putString("selected_theme", value.name).apply()

    var selectedLanguage: AppLanguage
        get() = AppLanguage.valueOf(prefs.getString("selected_language", AppLanguage.RU.name) ?: AppLanguage.RU.name)
        set(value) = prefs.edit().putString("selected_language", value.name).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("is_biometric_enabled", false)
        set(value) = prefs.edit().putBoolean("is_biometric_enabled", value).apply()

    var isSubscribed: Boolean
        get() = prefs.getBoolean("is_subscribed", false)
        set(value) = prefs.edit().putBoolean("is_subscribed", value).apply()

    var invoiceId: String?
        get() = prefs.getString("invoice_id", null)
        set(value) = prefs.edit().putString("invoice_id", value).apply()

    var appUserId: String
        get() {
            var id = prefs.getString("app_user_id", null)
            if (id == null) {
                id = java.util.UUID.randomUUID().toString()
                prefs.edit().putString("app_user_id", id).apply()
            }
            return id
        }
        private set(value) {}
}

enum class AppTheme {
    LIGHT, DARK, CELEBRATION
}

enum class AppLanguage(val displayName: String) {
    RU("Русский"), 
    EN("English"),
    KK("Қазақша"),
    TT("Татарча"),
    KA("ქართული"),
    UZ("O'zbekcha"),
    KY("Кыргызча"),
    TG("Тоҷикӣ"),
    HY("Հაიերեն"),
    AV("Мааруул")
}
