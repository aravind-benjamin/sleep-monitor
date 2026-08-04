package com.example.sleepmonitor.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SoundType {
    SNORE,
    TALK,
    NOISE, // unclassified loud sound (cough, movement, etc.)
}

@Entity(
    tableName = "sound_events",
    foreignKeys = [
        ForeignKey(
            entity = SleepSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SoundEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,       // epoch millis when the event started
    val durationMs: Long,
    val type: SoundType,
    val confidence: Float,     // 0..1, how confident the heuristic classifier is
    val peakAmplitude: Float,  // 0..1 normalized
    val filePath: String       // path to the recorded .wav clip
)
