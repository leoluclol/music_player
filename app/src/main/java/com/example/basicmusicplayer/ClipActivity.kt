package com.example.basicmusicplayer

import android.content.ComponentName
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/**
 * Lets the user slice a song into a clip by choosing a start and end point,
 * either with the sliders or by capturing the current playback position.
 */
class ClipActivity : AppCompatActivity() {

    private lateinit var song: Song
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private lateinit var startSeek: SeekBar
    private lateinit var endSeek: SeekBar
    private lateinit var startLabel: TextView
    private lateinit var endLabel: TextView
    private lateinit var nameInput: EditText

    private var durationMs: Long = 0L
    private var startMs: Long = 0L
    private var endMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clip)

        song = intent.getParcelableExtra(EXTRA_SONG) ?: run {
            finish()
            return
        }

        startSeek = findViewById(R.id.clipStartSeek)
        endSeek = findViewById(R.id.clipEndSeek)
        startLabel = findViewById(R.id.clipStartLabel)
        endLabel = findViewById(R.id.clipEndLabel)
        nameInput = findViewById(R.id.clipName)

        findViewById<TextView>(R.id.clipSongTitle).text = song.displayTitle
        nameInput.setText(song.title)

        startMs = song.startMs ?: 0L
        endMs = song.endMs ?: 0L

        startSeek.setOnSeekBarChangeListener(simpleListener { progress ->
            startMs = progress.toLong()
            if (endMs in 1..startMs) endMs = startMs + 1
            updateLabels()
        })
        endSeek.setOnSeekBarChangeListener(simpleListener { progress ->
            endMs = progress.toLong()
            updateLabels()
        })

        findViewById<Button>(R.id.clipSetStart).setOnClickListener {
            val c = controller ?: return@setOnClickListener toast(R.string.clip_need_controller)
            startMs = c.currentPosition.coerceAtLeast(0L)
            if (endMs in 1..startMs) endMs = startMs + 1
            syncSeekBars()
        }
        findViewById<Button>(R.id.clipSetEnd).setOnClickListener {
            val c = controller ?: return@setOnClickListener toast(R.string.clip_need_controller)
            endMs = c.currentPosition.coerceAtLeast(startMs + 1)
            syncSeekBars()
        }
        findViewById<Button>(R.id.clipPreview).setOnClickListener { preview() }
        findViewById<Button>(R.id.clipSave).setOnClickListener { save() }
        findViewById<Button>(R.id.clipDelete).setOnClickListener { delete() }

        connectToPlaybackService()
    }

    private fun connectToPlaybackService() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && durationMs == 0L) {
                        durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
                        if (endMs == 0L) endMs = durationMs
                        configureSeekBars()
                    }
                }
            })
            // Load the full track so we know its duration and can preview it.
            controller?.setMediaItem(
                PlaybackService.mediaItemFor(song.copy(startMs = null, endMs = null))
            )
            controller?.prepare()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun configureSeekBars() {
        val max = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        startSeek.max = max
        endSeek.max = max
        syncSeekBars()
    }

    private fun syncSeekBars() {
        startSeek.progress = startMs.coerceIn(0L, startSeek.max.toLong()).toInt()
        endSeek.progress = endMs.coerceIn(0L, endSeek.max.toLong()).toInt()
        updateLabels()
    }

    private fun updateLabels() {
        startLabel.text = getString(R.string.clip_start) + ": " + format(startMs)
        endLabel.text = getString(R.string.clip_end) + ": " + format(endMs)
    }

    private fun preview() {
        val c = controller ?: return toast(R.string.clip_need_controller)
        if (durationMs == 0L) return toast(R.string.clip_need_duration)
        val item: MediaItem = PlaybackService.mediaItemFor(
            song.copy(startMs = startMs, endMs = endMs)
        )
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    private fun save() {
        if (endMs <= startMs) return toast(R.string.clip_invalid_range)
        val name = nameInput.text.toString().trim().ifEmpty { song.title }
        val clip = song.copy(
            id = System.currentTimeMillis(),
            title = name,
            startMs = startMs,
            endMs = endMs,
            sourceTitle = song.sourceTitle
        )
        ClipStore.add(this, clip)
        toast(R.string.clip_saved)
        finish()
    }

    private fun delete() {
        if (!song.isClip) {
            finish()
            return
        }
        ClipStore.remove(this, song)
        toast(R.string.clip_deleted)
        finish()
    }

    private fun simpleListener(onChange: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onChange(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    private fun format(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SONG = "extra_song"
    }
}
