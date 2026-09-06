/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.flexplayer.music.playback.MusicService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class flexPlayerWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile var title: String = ""
        private set
    @Volatile var artist: String = ""
        private set
    @Volatile var artworkUri: String? = null
        private set
    @Volatile var isPlaying: Boolean = false
        private set
    @Volatile var isLiked: Boolean = false
        private set
    @Volatile var isRepeatOne: Boolean = false
        private set
    @Volatile var currentPositionMs: Long = 0L
        private set
    @Volatile var durationMs: Long = 0L
        private set

    fun updateWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long,
        isRepeatOne: Boolean = false,
    ) {
        this.title = title
        this.artist = artist
        this.artworkUri = artworkUri
        this.isPlaying = isPlaying
        this.isLiked = isLiked
        this.durationMs = duration
        this.currentPositionMs = currentPosition
        this.isRepeatOne = isRepeatOne
        broadcastUpdate()
    }

    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        broadcastUpdate()
    }

    fun setLiked(liked: Boolean) {
        isLiked = liked
        broadcastUpdate()
    }

    fun setRepeatOne(enabled: Boolean) {
        isRepeatOne = enabled
        broadcastUpdate()
    }

    fun broadcastUpdate() {
        // Sync with the legacy singleton holder used by BaseNeoWidget.onUpdate
        WidgetStateHolder.get(context).updateFromManager(this)

        val mgr = AppWidgetManager.getInstance(context)
        val fourByOne = ComponentName(context, Widget4x1Receiver::class.java)
        val threeByOne = ComponentName(context, Widget3x1Receiver::class.java)
        val ids4 = mgr.getAppWidgetIds(fourByOne)
        val ids3 = mgr.getAppWidgetIds(threeByOne)
        if (ids4.isNotEmpty()) {
            val update = Intent(context, Widget4x1Receiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids4)
            }
            context.sendBroadcast(update)
        }
        if (ids3.isNotEmpty()) {
            val update = Intent(context, Widget3x1Receiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids3)
            }
            context.sendBroadcast(update)
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.flexplayer.music.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.flexplayer.music.widget.NEXT"
        const val ACTION_PREV = "com.flexplayer.music.widget.PREV"
        const val ACTION_LIKE = "com.flexplayer.music.widget.LIKE"
        const val ACTION_REPEAT = "com.flexplayer.music.widget.REPEAT"
        const val ACTION_STOP = "com.flexplayer.music.widget.STOP"
        const val ACTION_OPEN_APP = "com.flexplayer.music.widget.OPEN_APP"
    }
}
