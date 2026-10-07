package com.example.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
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
import com.google.common.util.concurrent.MoreExecutors

class MainActivity : AppCompatActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private val songs = mutableListOf<Song>()
    private lateinit var adapter: MusicAdapter

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { loadSongs() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        adapter = MusicAdapter(songs) { index ->
            controller?.let {
                it.seekTo(index, 0L)
                it.play()
            }
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            controller?.let { c ->
                if (c.isPlaying) c.pause() else c.play()
            }
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener { controller?.seekToNext() }
        findViewById<Button>(R.id.btnPrev).setOnClickListener { controller?.seekToPrevious() }

        connectController()
        askPermission()
    }

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val idx = controller?.currentMediaItemIndex ?: 0
                    findViewById<TextView>(R.id.nowPlaying).text =
                        songs.getOrNull(idx)?.let { "${it.title} — ${it.artist}" } ?: ""
                }
            })
            pushToPlayer()
        }, MoreExecutors.directExecutor())
    }

    private fun askPermission() {
        val perm = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, perm)
            == PackageManager.PERMISSION_GRANTED
        ) loadSongs()
        else permLauncher.launch(perm)
    }

    private fun loadSongs() {
        songs.clear()
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(0) ?: "Unknown"
                val artist = it.getString(1) ?: "Unknown"
                val path = it.getString(2) ?: continue
                songs.add(Song(title, artist, Uri.fromFile(java.io.File(path)).toString()))
            }
        }
        adapter.notifyDataSetChanged()
        pushToPlayer()
    }

    private fun pushToPlayer() {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { MediaItem.fromUri(it.uri) }
        c.setMediaItems(items)
        c.prepare()
    }

    override fun onDestroy() {
        controller?.release()
        MediaController.releaseFuture(controllerFuture)
        super.onDestroy()
    }
}