/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.widget

import android.content.Context
import android.widget.RemoteViews
import com.flexplayer.music.R

class Widget4x1Receiver : BaseNeoWidget() {

    override fun layoutId(): Int = R.layout.neo_widget_4x1

    override fun configureRemoteViews(
        context: Context,
        views: RemoteViews,
        state: BaseNeoWidget.WidgetStateSnapshot,
        widgetWidthPx: Int,
        widgetHeightPx: Int,
    ) {
        views.setTextViewText(R.id.widget_title, state.title.ifBlank { context.getString(R.string.no_song_playing) })
        views.setTextViewText(R.id.widget_artist, state.artist.ifBlank { context.getString(R.string.tap_to_open) })

        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (state.isPlaying) R.drawable.neo_ic_pause else R.drawable.neo_ic_play,
        )

        views.setImageViewResource(
            R.id.widget_btn_like,
            if (state.isLiked) R.drawable.neo_ic_heart else R.drawable.neo_ic_heart_outline,
        )
        val likeTint = if (state.isLiked) 0xFFEF4444.toInt() else 0xFF64748B.toInt()
        views.setInt(R.id.widget_btn_like, "setColorFilter", likeTint)

        views.setImageViewResource(
            R.id.widget_btn_repeat,
            if (state.isRepeatOne) R.drawable.neo_ic_repeat_active else R.drawable.neo_ic_repeat,
        )
        val repeatTint = if (state.isRepeatOne) 0xFF2563EB.toInt() else 0xFF64748B.toInt()
        views.setInt(R.id.widget_btn_repeat, "setColorFilter", repeatTint)

        val safeDuration = state.durationMs.coerceAtLeast(1L)
        val progress = ((state.positionMs.toDouble() / safeDuration.toDouble()) * 100).toInt()
            .coerceIn(0, 100)
        
        views.setProgressBar(
            R.id.widget_progress,
            100,
            progress,
            false
        )
        views.setTextViewText(R.id.widget_time, formatTime(state.positionMs))

        val playPauseAction = if (state.isPlaying) "pause" else "play"
        views.setOnClickPendingIntent(
            R.id.widget_btn_play_pause,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_PLAY_PAUSE, 101),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_NEXT, 102),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_PREV, 103),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_like,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_LIKE, 104),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_repeat,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_REPEAT, 105),
        )

        val clickableTargets = intArrayOf(
            R.id.widget_title,
            R.id.widget_artist,
            R.id.widget_artwork,
        )
        for (id in clickableTargets) {
            views.setOnClickPendingIntent(id, openAppIntent(context))
        }
    }

    private fun formatTime(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0L)
        val m = total / 60
        val s = total % 60
        return String.format("%d:%02d", m, s)
    }
}
