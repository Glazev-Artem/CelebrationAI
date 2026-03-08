# --- Общие правила ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# --- Kotlin & Coroutines ---
-keep class kotlin.coroutines.jvm.internal.SuspendLambda { *; }
-dontwarn kotlin.coroutines.jvm.internal.SuspendLambda

# --- Retrofit & OkHttp ---
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Kotlinx Serialization ---
-keepclassmembers class ** {
    *** Companion;
}
-keepnames @kotlinx.serialization.Serializable class **
-keep class kotlinx.serialization.** { *; }


# --- КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ ДЛЯ AI-ГЕНЕРАЦИИ (НЕ УДАЛЯТЬ!) ---
# Запрещаем R8 трогать вообще всё в пакете service, чтобы Retrofit и Serialization работали в APK.
-keep class com.glazev.celebrationai.service.** { *; }
-keep interface com.glazev.celebrationai.service.** { *; }
# --- КОНЕЦ КРИТИЧЕСКОГО ИСПРАВЛЕНИЯ ---


# --- Room ---
-keep class androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
   public *;
}

# --- Модели данных Room ---
-keep class com.glazev.celebrationai.data.** { *; }

# --- Firebase & Google Auth ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
