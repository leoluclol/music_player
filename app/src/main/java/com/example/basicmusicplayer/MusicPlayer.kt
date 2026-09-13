package com.example.basicmusicplayer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

/**
 * Thin wrapper around [MediaPlayer] that handles a single track at a time.
 */
class MusicPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null

    var onCompletion: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    fun play(song: Song) {
        if (currentSong?.id == song.id && mediaPlayer != null) {
            mediaPlayer?.start()
            return
        }
        release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, song.uri)
            setOnPreparedListener { it.start() }
            setOnCompletionListener {
                onCompletion?.invoke()
            }
            prepareAsync()
        }
        currentSong = song
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
        }
        release()
        currentSong = null
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
