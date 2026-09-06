/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.widget

import android.content.Context
import android.widget.RemoteViews
import com.flexplayer.music.R

class Widget3x1Receiver : BaseNeoWidget() {

    override fun layoutId(): Int = R.layout.neo_widget_3x1

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

        views.setOnClickPendingIntent(
            R.id.widget_btn_play_pause,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_PLAY_PAUSE, 201),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_NEXT, 202),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_PREV, 203),
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_like,
            pendingServiceIntent(context, flexPlayerWidgetManager.ACTION_LIKE, 204),
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
}
