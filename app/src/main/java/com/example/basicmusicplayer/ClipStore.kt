package com.example.basicmusicplayer

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists user-created song clips as a small JSON file in app storage.
 *
 * MediaStore has no concept of a "clip", so clips live here and are merged
 * into the library at load time. Each clip points at the same [Uri] as its
 * source track but carries a start/end range.
 */
object ClipStore {

    private const val FILE_NAME = "clips.json"

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun load(context: Context): List<Song> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(f.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Song(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    artist = o.getString("artist"),
                    uri = Uri.parse(o.getString("uri")),
                    startMs = o.optLong("startMs").takeIf { o.has("startMs") },
                    endMs = o.optLong("endMs").takeIf { o.has("endMs") },
                    sourceTitle = o.optString("sourceTitle", o.getString("title"))
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, clips: List<Song>) {
        val array = JSONArray()
        clips.forEach { song ->
            val o = JSONObject()
                .put("id", song.id)
                .put("title", song.title)
                .put("artist", song.artist)
                .put("uri", song.uri.toString())
                .put("sourceTitle", song.sourceTitle)
            song.startMs?.let { o.put("startMs", it) }
            song.endMs?.let { o.put("endMs", it) }
            array.put(o)
        }
        file(context).writeText(array.toString())
    }

    fun add(context: Context, clip: Song) {
        val clips = load(context).toMutableList()
        clips.add(clip)
        save(context, clips)
    }

    fun remove(context: Context, clip: Song) {
        val clips = load(context).toMutableList()
        clips.removeAll { it.id == clip.id && it.startMs == clip.startMs && it.endMs == clip.endMs }
        save(context, clips)
    }
}
