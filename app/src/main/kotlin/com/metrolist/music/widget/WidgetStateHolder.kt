/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.flexplayer.music.MainActivity
import com.flexplayer.music.R
import com.flexplayer.music.playback.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetStateHolder @Inject constructor() {

    private val refreshJobs = ConcurrentHashMap<Int, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile var title: String = ""
    @Volatile var artist: String = ""
    @Volatile var artworkUri: String? = null
    @Volatile var isPlaying: Boolean = false
    @Volatile var isLiked: Boolean = false
    @Volatile var isRepeatOne: Boolean = false
    @Volatile var currentPositionMs: Long = 0L
    @Volatile var durationMs: Long = 0L

    fun updateFromManager(manager: flexPlayerWidgetManager) {
        title = manager.title
        artist = manager.artist
        artworkUri = manager.artworkUri
        isPlaying = manager.isPlaying
        isLiked = manager.isLiked
        isRepeatOne = manager.isRepeatOne
        currentPositionMs = manager.currentPositionMs
        durationMs = manager.durationMs
    }

    companion object {
        @Volatile private var instance: WidgetStateHolder? = null
        private val fallbackManager = Any()

        fun get(context: Context): WidgetStateHolder {
            instance?.let { return it }
            synchronized(fallbackManager) {
                instance?.let { return it }
                val created = WidgetStateHolder()
                instance = created
                return created
            }
        }

        fun requestArtworkRefresh(
            context: Context,
            widgetId: Int,
            providerClass: Class<*>,
            snapshot: BaseNeoWidget.WidgetStateSnapshot,
        ) {
            val holder = get(context)
            val existing = holder.refreshJobs.remove(widgetId)
            existing?.cancel()

            val uri = snapshot.artworkUri ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
            val density = context.resources.displayMetrics.density
            val widthPx = (widthDp * density).toInt().coerceAtLeast(64)
            val heightPx = (heightDp * density).toInt().coerceAtLeast(64)

            val job = holder.scope.launch {
                val bitmap = runCatching {
                    AlbumArtRenderer.loadFaded(context, uri, widthPx, heightPx)
                }.getOrNull()
                if (bitmap == null) return@launch

                val layoutId = when (providerClass) {
                    Widget3x1Receiver::class.java -> R.layout.neo_widget_3x1
                    else -> R.layout.neo_widget_4x1
                }
                
                val views = RemoteViews(context.packageName, layoutId)
                views.setImageViewBitmap(R.id.widget_artwork, bitmap)
                
                // We use partiallyUpdateAppWidget so we don't clear the text/controls
                // that were already set in onUpdate.
                appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
            }
            holder.refreshJobs[widgetId] = job
        }
    }
}
