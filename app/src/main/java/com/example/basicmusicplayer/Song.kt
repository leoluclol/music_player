package com.example.basicmusicplayer

import android.net.Uri

/**
 * Simple data holder for a single audio track on the device.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri
)
