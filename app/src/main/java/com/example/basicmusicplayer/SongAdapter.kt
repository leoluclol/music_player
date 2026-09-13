package com.example.basicmusicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter that renders the list of songs.
 */
class SongAdapter(
    private val songs: List<Song>,
    private val onClick: (Song) -> Unit,
    private val onLongClick: (Song) -> Unit = {},
    private val onQueue: (Song) -> Unit = {}
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.songCover)
        val title: TextView = view.findViewById(R.id.songTitle)
        val artist: TextView = view.findViewById(R.id.songArtist)
        val queue: TextView = view.findViewById(R.id.queueButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.cover.setImageBitmap(CoverArt.forSong(song))
        holder.title.text = song.displayTitle
        holder.artist.text = song.artist
        holder.itemView.setOnClickListener { onClick(song) }
        holder.itemView.setOnLongClickListener {
            onLongClick(song)
            true
        }
        holder.queue.setOnClickListener { onQueue(song) }
    }

    override fun getItemCount(): Int = songs.size
}
