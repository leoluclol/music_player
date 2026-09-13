package com.example.basicmusicplayer

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingConfiguration
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service that owns the [ExoPlayer] instance and exposes a
 * [MediaSession] so playback continues in the background and can be
 * controlled from the system media notification, lock screen, and
 * Bluetooth/headset buttons.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        /**
         * Builds a media item for the given [song] so it can be handed to the
         * session's player.
         */
        fun mediaItemFor(song: Song): MediaItem {
            val builder = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.displayTitle)
                        .setArtist(song.artist)
                        .build()
                )
            if (song.isClip) {
                val clipping = ClippingConfiguration.Builder()
                song.startMs?.let { clipping.setStartPositionMs(it) }
                song.endMs?.let { clipping.setEndPositionMs(it) }
                builder.setClippingConfiguration(clipping.build())
            }
            return builder.build()
        }
    }
}
