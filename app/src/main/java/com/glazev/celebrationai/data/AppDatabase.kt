package com.glazev.celebrationai.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

}

@Database(entities = [Celebration::class], version = 11, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun celebrationDao(): CelebrationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE celebrations ADD COLUMN estimatedBudget INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE celebrations ADD COLUMN hasYear INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "celebration_database"
                )
                .addMigrations(MIGRATION_10_11)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
