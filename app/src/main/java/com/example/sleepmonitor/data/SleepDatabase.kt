package com.example.sleepmonitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromSoundType(type: SoundType): String = type.name

    @TypeConverter
    fun toSoundType(value: String): SoundType = SoundType.valueOf(value)
}

@Database(entities = [SleepSession::class, SoundEvent::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile private var INSTANCE: SleepDatabase? = null

        fun getInstance(context: Context): SleepDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    "sleep_monitor.db"
                ).build().also { INSTANCE = it }
            }
    }
}
