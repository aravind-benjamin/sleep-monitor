package com.example.sleepmonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One night's sleep session.
 * startTime / endTime can be filled automatically (when tracking is started/stopped
 * from the app) or edited manually by the user afterward.
 */
@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,      // epoch millis
    val endTime: Long?,       // null while a session is still in progress
    val isManualEntry: Boolean = false,
    val notes: String? = null
) {
    val durationMinutes: Long
        get() = if (endTime != null) (endTime - startTime) / 60000 else 0
}
