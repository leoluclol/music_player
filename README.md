# Basic Music Player

A small Android music player built with Kotlin, Jetpack Media3 (ExoPlayer), and
a `MediaSessionService` for background playback. It scans the device's music
library, plays tracks, lets you slice out clips of long tracks (handy for DJ
sets), queue songs, and gives every track a generated cover image.

## Features

### Music library
- Scans the device via `MediaStore.Audio.Media` for audio files flagged as music.
- Lists each track with its title and artist, sorted by title.
- Requests the right runtime permission automatically: `READ_MEDIA_AUDIO` on
  Android 13+, `READ_EXTERNAL_STORAGE` on older versions.

### Playback
- Tap a song to play it.
- **Play / Pause / Stop** buttons in the toolbar.
- Playback runs in a foreground `MediaSessionService`, so it continues in the
  background and is controllable from the system media notification, the lock
  screen, and Bluetooth/headset buttons.
- The now-playing line shows "Title — Artist" and resets to the app name when
  playback ends.

### Song clips (slicing tracks)
- **Long-press any song** to open the clip editor.
- Choose a start and end point either by dragging the two sliders or by hitting
  **Set start here** / **Set end here** while the track plays — useful for
  grabbing the best part of a DJ set.
- Give the clip a name, **Preview** it, and **Save** it.
- Saved clips appear in the library as their own entries (marked with a ✂) but
  point at the same underlying audio file. Under the hood they use ExoPlayer's
  `ClippingConfiguration`, so only the selected range plays.
- Long-press an existing clip to edit or **Delete** it.
- Clips are persisted as JSON in the app's private storage (`clips.json`).

### Queue
- The **+Q** button on each row appends that song to the playback queue.
- The **Queue** button in the toolbar opens a screen listing the queued items.
- Tap an item to remove it, or use **Clear queue** to empty the queue.

### Generated cover art
- Every song gets a deterministic cover image generated on the fly — no image
  files or network access involved.
- The cover is a diagonal gradient whose hue is derived from a hash of the
  song's title and artist, with the track's initials drawn on top.
- Clips share their parent track's base hue but are shifted slightly and get a
  corner marker, so a clip and its original look related but distinct.
- Covers are cached in memory (`LruCache`) so scrolling stays smooth.

## Project structure

| File | Purpose |
| --- | --- |
| `MainActivity.kt` | Library list, playback controls, permissions, clip/queue entry points |
| `PlaybackService.kt` | `MediaSessionService` owning the ExoPlayer instance; builds media items (including clip ranges) |
| `SongAdapter.kt` | RecyclerView adapter for the song list |
| `Song.kt` | Data model for a track or clip |
| `ClipStore.kt` | Persists clips to `clips.json` |
| `ClipActivity.kt` | Clip editor (sliders + capture buttons) |
| `QueueActivity.kt` | Queue viewer/editor |
| `CoverArt.kt` | Deterministic cover image generator |
| `MusicPlayer.kt` | Legacy `MediaPlayer` wrapper (unused) |

## Requirements

- Android Studio (or the Gradle wrapper) with JDK 17.
- Android SDK 34 (`compileSdk 34`), `minSdk 24`.
- A device or emulator running Android 7.0 (API 24) or newer.

## Building and running

Build and install the debug build on a connected device or emulator:

```bash
./gradlew installDebug
```

Or build the APK only:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Usage

1. Launch the app and grant the audio/media permission (and notification
   permission on Android 13+) when prompted.
2. Tap a song to play it, or use the toolbar buttons to play/pause/stop.
3. Long-press a song to create a clip; drag the sliders or use the capture
   buttons, name it, and save.
4. Tap **+Q** on a row to add it to the queue, and **Queue** to manage the queue.

## Notes and limitations

- There is no seek bar, shuffle, or repeat control in the UI yet.
- Cover art is generated, not read from the files' embedded album art.
- `MusicPlayer.kt` is dead code left over from an earlier `MediaPlayer`-based
  implementation; playback now goes through `PlaybackService`/ExoPlayer.
- Clips reference the original file by URI, so deleting the source audio file
  will break its clips.
