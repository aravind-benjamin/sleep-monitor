package com.example.sleepmonitor.service

import com.example.sleepmonitor.data.SoundType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lightweight, on-device heuristic classifier.
 *
 * This is NOT a trained ML model — it's a rule-based approximation using signal
 * features that are cheap to compute in real time:
 *   - RMS amplitude of the burst (loudness)
 *   - Zero-crossing rate (proxy for "tonal drone" vs "broadband speech")
 *   - Duration of the burst
 *   - Periodicity: whether similar bursts have been recurring at a steady interval
 *     (snoring is rhythmic; sleep talk is not)
 *
 * To get materially better accuracy, replace classify() with a call into a
 * TensorFlow Lite model (e.g. YAMNet fine-tuned on snore/speech clips) — the
 * feature extraction below (samples -> event window) is where you'd feed it in.
 */
class AudioClassifier {

    private data class RecentBurst(val startMs: Long, val durationMs: Long)

    private val recentBursts = ArrayDeque<RecentBurst>()
    private val maxHistory = 8

    data class Result(val type: SoundType, val confidence: Float)

    fun classify(samples: ShortArray, startMs: Long, durationMs: Long): Result {
        val zcr = zeroCrossingRate(samples)
        val rms = rms(samples)
        val amplitudeVariance = amplitudeVariance(samples)

        val isRhythmic = isPeriodic(startMs, durationMs)

        // Snoring: longer, low-to-mid ZCR (low tonal "buzz"), fairly steady amplitude,
        // and recurs at a roughly steady interval.
        val snoreScore =
            score(durationMs in 400..4000, 0.25f) +
            score(zcr < 0.12, 0.30f) +
            score(amplitudeVariance < 0.35f, 0.15f) +
            score(isRhythmic, 0.30f)

        // Sleep talk: shorter bursts, higher ZCR (speech has more high-frequency
        // content), more amplitude variance (syllables), and NOT rhythmic.
        val talkScore =
            score(durationMs in 150..2500, 0.25f) +
            score(zcr > 0.12, 0.30f) +
            score(amplitudeVariance > 0.35f, 0.25f) +
            score(!isRhythmic, 0.20f)

        recentBursts.addLast(RecentBurst(startMs, durationMs))
        if (recentBursts.size > maxHistory) recentBursts.removeFirst()

        return when {
            snoreScore >= talkScore && snoreScore > 0.5f -> Result(SoundType.SNORE, snoreScore.coerceAtMost(0.97f))
            talkScore > snoreScore && talkScore > 0.5f -> Result(SoundType.TALK, talkScore.coerceAtMost(0.97f))
            else -> Result(SoundType.NOISE, maxOf(0.3f, rms))
        }
    }

    private fun score(condition: Boolean, weight: Float) = if (condition) weight else 0f

    private fun isPeriodic(startMs: Long, durationMs: Long): Boolean {
        if (recentBursts.size < 2) return false
        val last = recentBursts.last()
        val gap = startMs - (last.startMs + last.durationMs)
        // Snoring typically recurs every ~2-6 seconds of the breathing cycle.
        return gap in 500..7000
    }

    private fun zeroCrossingRate(samples: ShortArray): Float {
        if (samples.size < 2) return 0f
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i - 1] >= 0) != (samples[i] >= 0)) crossings++
        }
        return crossings.toFloat() / samples.size
    }

    private fun rms(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (s in samples) sum += (s.toDouble() * s.toDouble())
        val rms = sqrt(sum / samples.size)
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    private fun amplitudeVariance(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        val mean = samples.map { abs(it.toInt()) }.average()
        val variance = samples.map { (abs(it.toInt()) - mean) * (abs(it.toInt()) - mean) }.average()
        val normalized = sqrt(variance) / Short.MAX_VALUE
        return normalized.toFloat().coerceIn(0f, 1f)
    }
}
