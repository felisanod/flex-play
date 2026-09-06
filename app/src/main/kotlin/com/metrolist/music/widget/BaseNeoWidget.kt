/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.flexplayer.music.MainActivity
import com.flexplayer.music.R
import com.flexplayer.music.playback.MusicService

abstract class BaseNeoWidget : AppWidgetProvider() {

    abstract fun layoutId(): Int
    abstract fun configureRemoteViews(
        context: Context,
        views: RemoteViews,
        state: WidgetStateSnapshot,
        widgetWidthPx: Int,
        widgetHeightPx: Int,
    )

    data class WidgetStateSnapshot(
        val title: String,
        val artist: String,
        val artworkUri: String?,
        val isPlaying: Boolean,
        val isLiked: Boolean,
        val isRepeatOne: Boolean,
        val positionMs: Long,
        val durationMs: Long,
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val manager = WidgetStateHolder.get(context)
        val state = WidgetStateSnapshot(
            title = manager.title,
            artist = manager.artist,
            artworkUri = manager.artworkUri,
            isPlaying = manager.isPlaying,
            isLiked = manager.isLiked,
            isRepeatOne = manager.isRepeatOne,
            positionMs = manager.currentPositionMs,
            durationMs = manager.durationMs,
        )
        for (id in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(id)
            val widthPx = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                .coerceAtLeast(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0))
            val heightPx = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                .coerceAtLeast(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0))

            val views = RemoteViews(context.packageName, layoutId())
            configureRemoteViews(context, views, state, widthPx, heightPx)
            appWidgetManager.updateAppWidget(id, views)

            WidgetStateHolder.requestArtworkRefresh(context, id, this::class.java, state)
        }
    }

    protected fun pendingServiceIntent(
        context: Context,
        action: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }

    protected fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
