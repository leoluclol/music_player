package com.example.basicmusicplayer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.LruCache

/**
 * Generates a deterministic cover image for a song so each track is
 * recognisable at a glance. The same title/artist always yields the same
 * colours and initials.
 *
 * Clips seed from their *source* track, so they share the parent's base hue,
 * but get a small hue shift plus a corner marker to tell them apart.
 */
object CoverArt {

    private const val SIZE = 128

    private val cache = object : LruCache<String, Bitmap>(64) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun forSong(song: Song): Bitmap {
        val key = "${song.sourceTitle}|${song.artist}|${song.startMs}|${song.endMs}"
        cache.get(key)?.let { return it }
        val bitmap = generate(song)
        cache.put(key, bitmap)
        return bitmap
    }

    private fun generate(song: Song): Bitmap {
        // Seed from the *source* track so a clip shares its parent's base hue.
        val seed = "${song.sourceTitle}|${song.artist}".hashCode()
        val baseHue = ((seed % 360) + 360) % 360
        // Clips get a small hue shift so they look related but distinct.
        val hue = if (song.isClip) (baseHue + 25) % 360 else baseHue
        val startColor = Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.65f, 0.85f))
        val endColor = Color.HSVToColor(floatArrayOf(((hue + 40) % 360).toFloat(), 0.75f, 0.45f))

        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, SIZE.toFloat(), SIZE.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), bg)

        val initials = initialsFor(song.title)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = SIZE * 0.42f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val y = SIZE / 2f - (text.descent() + text.ascent()) / 2f
        canvas.drawText(initials, SIZE / 2f, y, text)

        if (song.isClip) {
            drawClipMarker(canvas)
        }

        return bitmap
    }

    /** Draws a small corner notch so clips are recognisable at a glance. */
    private fun drawClipMarker(canvas: Canvas) {
        val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 0, 0, 0)
        }
        val size = SIZE * 0.32f
        val path = Path().apply {
            moveTo(SIZE - size, 0f)
            lineTo(SIZE.toFloat(), 0f)
            lineTo(SIZE.toFloat(), size)
            close()
        }
        canvas.drawPath(path, marker)

        val scissors = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = SIZE * 0.18f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("✂", SIZE - 4f, SIZE * 0.22f, scissors)
    }

    private fun initialsFor(title: String): String {
        val words = title.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }
}
