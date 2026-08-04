package com.example.sleepmonitor.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.sleepmonitor.data.SleepRepository
import com.example.sleepmonitor.data.SoundType

data class DashboardStats(
    val totalSessions: Int,
    val totalTrackedMinutes: Long,
    val snoreEvents: Int,
    val talkEvents: Int,
    val noiseEvents: Int,
    val avgSnorePerNight: Float
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SleepRepository(application)

    val stats: LiveData<DashboardStats> = MediatorLiveData<DashboardStats>().apply {
        val sessions = repository.observeSessions()
        val events = repository.observeAllEvents()

        fun recompute() {
            val sessionList = sessions.value ?: emptyList()
            val eventList = events.value ?: emptyList()

            val totalMinutes = sessionList.sumOf { it.durationMinutes }
            val snore = eventList.count { it.type == SoundType.SNORE }
            val talk = eventList.count { it.type == SoundType.TALK }
            val noise = eventList.count { it.type == SoundType.NOISE }
            val nights = sessionList.size.coerceAtLeast(1)

            value = DashboardStats(
                totalSessions = sessionList.size,
                totalTrackedMinutes = totalMinutes,
                snoreEvents = snore,
                talkEvents = talk,
                noiseEvents = noise,
                avgSnorePerNight = snore.toFloat() / nights
            )
        }

        addSource(sessions) { recompute() }
        addSource(events) { recompute() }
    }
}
