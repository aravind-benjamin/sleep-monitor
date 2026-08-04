package com.example.sleepmonitor.data

import android.content.Context

class SleepRepository(context: Context) {
    private val dao = SleepDatabase.getInstance(context).sleepDao()

    fun observeSessions() = dao.observeSessions()
    fun observeAllEvents() = dao.observeAllEvents()
    fun observeEventsForSession(sessionId: Long) = dao.observeEventsForSession(sessionId)

    suspend fun startSession(startTime: Long): Long =
        dao.insertSession(SleepSession(startTime = startTime, endTime = null))

    suspend fun endSession(session: SleepSession, endTime: Long) =
        dao.updateSession(session.copy(endTime = endTime))

    suspend fun getActiveSession(): SleepSession? = dao.getActiveSession()

    suspend fun addManualSession(session: SleepSession) = dao.insertSession(session)

    suspend fun updateSession(session: SleepSession) = dao.updateSession(session)

    suspend fun deleteSession(session: SleepSession) = dao.deleteSession(session)

    suspend fun addEvent(event: SoundEvent): Long = dao.insertEvent(event)

    suspend fun deleteEvent(event: SoundEvent) = dao.deleteEvent(event)

    suspend fun snoreCountSince(since: Long) = dao.countByTypeSince(SoundType.SNORE, since)
    suspend fun talkCountSince(since: Long) = dao.countByTypeSince(SoundType.TALK, since)
}
