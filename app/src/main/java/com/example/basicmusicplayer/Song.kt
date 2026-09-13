package com.example.basicmusicplayer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Simple data holder for a single audio track on the device.
 *
 * A [Song] can also represent a *clip*: a slice of an existing track. In that
 * case [startMs]/[endMs] are set and [sourceTitle] keeps the original title so
 * the generated cover stays visually related to the parent track.
 */
@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    /** Start of the clip in milliseconds, or null for the whole track. */
    val startMs: Long? = null,
    /** End of the clip in milliseconds, or null for the whole track. */
    val endMs: Long? = null,
    /** Title of the original track this entry was sliced from. */
    val sourceTitle: String = title
) : Parcelable {

    /** True when this entry only plays a slice of the underlying file. */
    val isClip: Boolean
        get() = startMs != null || endMs != null

    /** Title shown in the library, with a marker for clips. */
    val displayTitle: String
        get() = if (isClip) "$title ✂" else title
}
