package com.glazev.celebrationai.data

import android.content.Context
import androidx.room.*

class Converters {
    @TypeConverter
    fun fromTone(tone: CelebrationTone): String = tone.name

    @TypeConverter
    fun toTone(value: String): CelebrationTone = CelebrationTone.valueOf(value)

    @TypeConverter
    fun fromType(type: CelebrationType): String = type.name

    @TypeConverter
    fun toType(value: String): CelebrationType = CelebrationType.valueOf(value)

    @TypeConverter
    fun fromGroup(group: CelebrationGroup): String = group.name

    @TypeConverter
    fun toGroup(value: String): CelebrationGroup = CelebrationGroup.valueOf(value)
}

@Database(entities = [Celebration::class], version = 10, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun celebrationDao(): CelebrationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "celebration_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
