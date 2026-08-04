package com.example.sleepmonitor.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.sleepmonitor.MainActivity
import com.example.sleepmonitor.R
import com.example.sleepmonitor.data.SleepRepository
import com.example.sleepmonitor.data.SoundEvent
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class SleepTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "sleep_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.example.sleepmonitor.action.STOP"

        // Tune these to your phone's mic sensitivity / room noise floor.
        const val SAMPLE_RATE = 16000
        const val SILENCE_RMS_THRESHOLD = 0.02f   // below this = "quiet room"
        const val MIN_EVENT_MS = 300
        const val MAX_EVENT_MS = 8000
        const val END_SILENCE_MS = 600            // silence needed to close out an event
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: SleepRepository
    private var sessionId: Long = -1
    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val classifier = AudioClassifier()

    override fun onCreate() {
        super.onCreate()
        repository = SleepRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        serviceScope.launch {
            sessionId = repository.startSession(System.currentTimeMillis())
            startRecording()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        recordJob?.cancel()
        audioRecord?.release()
        audioRecord = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopTracking() {
        serviceScope.launch {
            val active = repository.getActiveSession()
            if (active != null) {
                repository.endSession(active, System.currentTimeMillis())
            }
            recordJob?.cancel()
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    @Suppress("MissingPermission")
    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuf, SAMPLE_RATE) // >= ~0.5s of audio per read chunk safety

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()

        val clipsDir = File(filesDir, "clips").apply { mkdirs() }

        recordJob = serviceScope.launch {
            val chunkSamples = SAMPLE_RATE / 10 // 100ms chunks
            val chunk = ShortArray(chunkSamples)

            var capturing = false
            var eventBuffer = mutableListOf<Short>()
            var eventStartMs = 0L
            var silenceStreakMs = 0

            while (isActive) {
                val read = audioRecord?.read(chunk, 0, chunk.size) ?: -1
                if (read <= 0) continue

                val samples = chunk.copyOf(read)
                val level = rms(samples)
                val now = System.currentTimeMillis()

                if (level >= SILENCE_RMS_THRESHOLD) {
                    if (!capturing) {
                        capturing = true
                        eventBuffer = mutableListOf()
                        eventStartMs = now
                    }
                    eventBuffer.addAll(samples.toList())
                    silenceStreakMs = 0
                } else if (capturing) {
                    eventBuffer.addAll(samples.toList()) // keep a little trailing audio
                    silenceStreakMs += 100
                    val elapsed = now - eventStartMs
                    if (silenceStreakMs >= END_SILENCE_MS || elapsed >= MAX_EVENT_MS) {
                        finalizeEvent(eventBuffer, eventStartMs, clipsDir)
                        capturing = false
                        eventBuffer = mutableListOf()
                        silenceStreakMs = 0
                    }
                }
            }
        }
    }

    private suspend fun finalizeEvent(buffer: List<Short>, startMs: Long, clipsDir: File) {
        val durationMs = (buffer.size.toLong() * 1000) / SAMPLE_RATE
        if (durationMs < MIN_EVENT_MS) return

        val samples = buffer.toShortArray()
        val result = classifier.classify(samples, startMs, durationMs)
        val peak = samples.maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toFloat()

        val fileName = "event_${startMs}.wav"
        val file = File(clipsDir, fileName)
        withContext(Dispatchers.IO) {
            WavWriter.write(file, samples, SAMPLE_RATE)
        }

        repository.addEvent(
            SoundEvent(
                sessionId = sessionId,
                timestamp = startMs,
                durationMs = durationMs,
                type = result.type,
                confidence = result.confidence,
                peakAmplitude = peak,
                filePath = file.absolutePath
            )
        )
    }

    private fun rms(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (s in samples) sum += s.toDouble() * s.toDouble()
        return (sqrt(sum / samples.size) / Short.MAX_VALUE).toFloat()
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, SleepTrackingService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(openPending)
            .addAction(0, getString(R.string.stop_tracking), stopPending)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
