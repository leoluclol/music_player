package com.example.basicmusicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {

    private lateinit var nowPlaying: TextView
    private lateinit var adapter: SongAdapter
    private val songs = mutableListOf<Song>()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadSongs()
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nowPlaying = findViewById(R.id.nowPlayingText)
        val songList: RecyclerView = findViewById(R.id.songList)
        adapter = SongAdapter(
            songs,
            onClick = { song -> playSong(song) },
            onLongClick = { song -> openClipEditor(song) },
            onQueue = { song -> enqueue(song) }
        )
        songList.layoutManager = LinearLayoutManager(this)
        songList.adapter = adapter

        findViewById<Button>(R.id.playButton).setOnClickListener {
            val c = controller ?: return@setOnClickListener
            if (c.isPlaying) return@setOnClickListener
            if (c.mediaItemCount == 0) {
                val song = songs.firstOrNull() ?: return@setOnClickListener
                playSong(song)
            } else {
                c.play()
            }
        }
        findViewById<Button>(R.id.pauseButton).setOnClickListener { controller?.pause() }
        findViewById<Button>(R.id.stopButton).setOnClickListener {
            controller?.stop()
            controller?.clearMediaItems()
            nowPlaying.text = getString(R.string.app_name)
        }
        findViewById<Button>(R.id.queueButton).setOnClickListener {
            startActivity(Intent(this, QueueActivity::class.java))
        }

        requestPermissionIfNeeded()
        requestNotificationPermissionIfNeeded()
        connectToPlaybackService()
    }

    private fun connectToPlaybackService() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
        }, ContextCompat.getMainExecutor(this))
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
            val title = mediaMetadata.title?.toString() ?: return
            val artist = mediaMetadata.artist?.toString().orEmpty()
            nowPlaying.text = if (artist.isEmpty()) title else "$title — $artist"
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                nowPlaying.text = getString(R.string.app_name)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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

    private fun openClipEditor(song: Song) {
        val intent = Intent(this, ClipActivity::class.java)
        intent.putExtra(ClipActivity.EXTRA_SONG, song)
        startActivity(intent)
    }

    private fun enqueue(song: Song) {
        val c = controller ?: return
        c.addMediaItem(PlaybackService.mediaItemFor(song))
        Toast.makeText(this, R.string.queue_add, Toast.LENGTH_SHORT).show()
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
        songs.addAll(ClipStore.load(this))
        adapter.notifyDataSetChanged()
        if (songs.isEmpty()) {
            Toast.makeText(this, R.string.no_songs, Toast.LENGTH_LONG).show()
        }
    }

    private fun playSong(song: Song) {
        val c = controller ?: return
        val item: MediaItem = PlaybackService.mediaItemFor(song)
        c.setMediaItem(item)
        c.prepare()
        c.play()
        nowPlaying.text = "${song.displayTitle} — ${song.artist}"
    }

    override fun onResume() {
        super.onResume()
        // Clips may have been added/removed while the clip editor was open.
        if (::adapter.isInitialized) {
            songs.clear()
            loadSongs()
        }
    }

    override fun onDestroy() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }
}
