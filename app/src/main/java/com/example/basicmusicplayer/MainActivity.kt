package com.example.basicmusicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var player: MusicPlayer
    private lateinit var nowPlaying: TextView
    private lateinit var adapter: SongAdapter
    private val songs = mutableListOf<Song>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadSongs()
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        player = MusicPlayer(this)
        player.onCompletion = { nowPlaying.text = getString(R.string.app_name) }

        nowPlaying = findViewById(R.id.nowPlayingText)
        val songList: RecyclerView = findViewById(R.id.songList)
        adapter = SongAdapter(songs) { song -> playSong(song) }
        songList.layoutManager = LinearLayoutManager(this)
        songList.adapter = adapter

        findViewById<Button>(R.id.playButton).setOnClickListener {
            if (player.isPlaying) return@setOnClickListener
            val song = songs.firstOrNull() ?: return@setOnClickListener
            playSong(song)
        }
        findViewById<Button>(R.id.pauseButton).setOnClickListener { player.pause() }
        findViewById<Button>(R.id.stopButton).setOnClickListener {
            player.stop()
            nowPlaying.text = getString(R.string.app_name)
        }

        requestPermissionIfNeeded()
    }

    private fun requestPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            loadSongs()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun loadSongs() {
        songs.clear()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown"
                val artist = cursor.getString(artistCol) ?: "Unknown"
                val uri = android.content.ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                songs.add(Song(id, title, artist, uri))
            }
        }
        adapter.notifyDataSetChanged()
        if (songs.isEmpty()) {
            Toast.makeText(this, R.string.no_songs, Toast.LENGTH_LONG).show()
        }
    }

    private fun playSong(song: Song) {
        player.play(song)
        nowPlaying.text = "${song.title} — ${song.artist}"
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
