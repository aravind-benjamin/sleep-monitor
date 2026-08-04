package com.example.sleepmonitor.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SleepDao {

    // ---- Sessions ----

    @Insert
    suspend fun insertSession(session: SleepSession): Long

    @Update
    suspend fun updateSession(session: SleepSession)

    @Delete
    suspend fun deleteSession(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    fun observeSessions(): LiveData<List<SleepSession>>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getSession(id: Long): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActiveSession(): LiveData<SleepSession?>

    // ---- Sound events ----

    @Insert
    suspend fun insertEvent(event: SoundEvent): Long

    @Delete
    suspend fun deleteEvent(event: SoundEvent)

    @Query("SELECT * FROM sound_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeEventsForSession(sessionId: Long): LiveData<List<SoundEvent>>

    @Query("SELECT * FROM sound_events ORDER BY timestamp DESC")
    fun observeAllEvents(): LiveData<List<SoundEvent>>

    @Query("SELECT COUNT(*) FROM sound_events WHERE sessionId = :sessionId AND type = :type")
    suspend fun countByType(sessionId: Long, type: SoundType): Int

    @Query("""
        SELECT COUNT(*) FROM sound_events e
        JOIN sleep_sessions s ON e.sessionId = s.id
        WHERE e.type = :type AND s.startTime >= :since
    """)
    suspend fun countByTypeSince(type: SoundType, since: Long): Int
}
