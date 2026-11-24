/*
 * Zynx Launcher - simple launcher music player
 * Based on Zalith Launcher 2 (GPL-3.0)
 */

package com.movtery.zalithlauncher.zynx

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.setting.launcherMMKV
import org.json.JSONArray

data class MusicTrack(val uri: Uri, val title: String)

/**
 * User-picked audio files (kept as persistable SAF URIs). Playback pauses when the game starts.
 */
object MusicPlayerManager {
    private const val TAG = "MusicPlayerManager"
    private const val KEY_TRACKS = "zynxMusicTracks"

    val tracks = mutableStateListOf<MusicTrack>()

    var currentIndex by mutableIntStateOf(-1)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    private var player: MediaPlayer? = null
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val raw = launcherMMKV().decodeString(KEY_TRACKS, null) ?: return
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val uri = Uri.parse(array.getString(i))
                tracks.add(MusicTrack(uri, resolveTitle(context, uri)))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load playlist", e)
        }
    }

    fun addTracks(context: Context, uris: List<Uri>) {
        for (uri in uris) {
            if (tracks.none { it.uri == uri }) tracks.add(MusicTrack(uri, resolveTitle(context, uri)))
        }
        save()
    }

    fun removeTrack(index: Int) {
        if (index !in tracks.indices) return
        if (index == currentIndex) stop()
        else if (index < currentIndex) currentIndex--
        tracks.removeAt(index)
        save()
    }

    fun play(context: Context, index: Int) {
        if (index !in tracks.indices) return
        releasePlayer()
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setDataSource(context.applicationContext, tracks[index].uri)
            mp.setOnCompletionListener { next(context) }
            mp.setOnErrorListener { _, _, _ ->
                isPlaying = false
                true
            }
            mp.prepare()
            mp.start()
            player = mp
            currentIndex = index
            isPlaying = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play track", e)
            releasePlayer()
            isPlaying = false
        }
    }

    fun toggle(context: Context) {
        val mp = player
        if (mp == null) {
            if (tracks.isNotEmpty()) play(context, if (currentIndex in tracks.indices) currentIndex else 0)
            return
        }
        if (mp.isPlaying) {
            mp.pause()
            isPlaying = false
        } else {
            mp.start()
            isPlaying = true
        }
    }

    fun next(context: Context) {
        if (tracks.isEmpty()) return
        play(context, (currentIndex + 1).mod(tracks.size))
    }

    fun previous(context: Context) {
        if (tracks.isEmpty()) return
        play(context, (currentIndex - 1).mod(tracks.size))
    }

    /** Pauses without losing the position, e.g. when the game starts. */
    fun pause() {
        try {
            player?.takeIf { it.isPlaying }?.pause()
        } catch (_: IllegalStateException) {
        }
        isPlaying = false
    }

    fun stop() {
        releasePlayer()
        currentIndex = -1
        isPlaying = false
    }

    private fun releasePlayer() {
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }

    private fun save() {
        val array = JSONArray()
        tracks.forEach { array.put(it.uri.toString()) }
        launcherMMKV().encode(KEY_TRACKS, array.toString())
    }

    private fun resolveTitle(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0).substringBeforeLast('.')
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: "Track"
    }
}
