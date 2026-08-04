# Sleep Monitor (Android, Kotlin)

A native Android app that records through the night, detects snoring / sleep-talk /
other sounds, and lets you review them the next day.

## How to open it

1. Install **Android Studio** (Koala or newer).
2. `File > Open` and select this `SleepMonitor` folder.
3. Let Android Studio sync Gradle (it will regenerate `gradlew`/the wrapper jar
   automatically on first sync — you don't need internet access baked into this repo).
4. Run on a device or emulator with **API 26+**. A real device is strongly
   recommended for testing the microphone/snore detection — emulator mics are unreliable.

## What's included

- **Foreground service** (`SleepTrackingService`) that keeps the mic open overnight,
  segments audio into "events" whenever volume rises above a noise floor, and closes
  an event after ~600ms of silence.
- **Heuristic classifier** (`AudioClassifier`) that labels each event as Snore, Talk,
  or Noise using loudness, zero-crossing rate, amplitude variance, and rhythm
  (snoring recurs on a steady cycle; talking doesn't). This is NOT a trained ML model —
  see "Improving accuracy" below.
- **Room database** storing sleep sessions and sound events.
- **Dashboard tab** — snore/talk counts, total time tracked, and a 7-night bar chart.
- **Sessions tab** — list of nights; tap the **+** to add or edit a sleep session
  manually (pick start/end date & time) — this is separate from live tracking, so you
  can log past nights or fix an inaccurate stop time.
- **Playback tab** — every captured clip with type, time, duration, confidence %,
  and a play button (writes real `.wav` files, played back with `MediaPlayer`).

## Getting an APK without installing Android Studio

This project includes `.github/workflows/build-apk.yml`, which builds a debug
APK automatically using GitHub Actions (free for public repos, and for most
personal use on private ones too):

1. Push this folder to a new GitHub repository.
2. Go to the repo's **Actions** tab — the workflow runs automatically on push.
   If it doesn't, click **Build APK > Run workflow**.
3. Once it finishes (a few minutes), open the completed run and download the
   **SleepMonitor-debug-apk** artifact from the bottom of the page — that's a
   zip containing `app-debug.apk`.
4. Transfer that APK to your phone and install it (you'll need to allow
   "install from unknown sources" for whatever app you transfer it with).

Note: this produces a **debug** build, which is unsigned for release but installs
and runs fine for personal use. If you eventually want to publish it or sign it
properly, you'd add a signing config and change the workflow to run
`gradle assembleRelease` instead.

## Starting/stopping tracking

Tap the mic FAB on any tab to start the foreground service (grants RECORD_AUDIO +
notification permission on first use). A persistent notification with a **Stop**
button appears — tap it (or the FAB again, if you wire that up) when you wake up to
close out the session.

## Tuning detection

In `SleepTrackingService.kt`:
- `SILENCE_RMS_THRESHOLD` — raise this if the app is picking up too much ambient
  room noise; lower it if quiet snoring is being missed.
- `MIN_EVENT_MS` / `MAX_EVENT_MS` / `END_SILENCE_MS` — control how bursts are
  grouped into a single clip.

In `AudioClassifier.kt`, the scoring weights for SNORE vs TALK vs NOISE are plain
constants — easy to adjust once you see real clips misclassified.

## Improving accuracy (optional next step)

The heuristic classifier will get maybe 65-80% of clear cases right, but it's not a
substitute for a trained model. If you want materially better accuracy:
- Fine-tune a small audio model (e.g. YAMNet or a custom CNN) on labeled
  snore/speech/noise clips, convert to TensorFlow Lite, and call it from inside
  `AudioClassifier.classify()` instead of (or alongside) the heuristics.
- Or send clips to a cloud audio-classification API — trades battery/data for
  accuracy, and only works with connectivity.

## Known platform constraints to be aware of

- **Battery optimization**: Android will kill background mic access aggressively on
  some OEMs (Samsung, Xiaomi, etc.) unless the user disables battery optimization for
  this app. Consider prompting for that in Settings.
- **Android 14+ mic restrictions**: a foreground service with `microphone` type is
  required (already configured) and the notification must stay visible while recording.
- **Storage growth**: a full night of snoring can generate many small `.wav` clips.
  Consider adding a "delete clips older than N days" cleanup job if you'll use this
  long-term.
- No cloud sync/backup is implemented — everything is stored locally in the app's
  private storage (`filesDir/clips` + the Room DB), so uninstalling the app deletes
  all recordings.
