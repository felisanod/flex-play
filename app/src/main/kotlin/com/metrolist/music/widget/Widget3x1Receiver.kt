/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.widget

import android.content.Context
import android.widget.RemoteViews
import com.flexplayer.music.R

/**
 * 3x1 Neomorphic Home Screen Music Widget (Compact)
 *
 * Design per Engineering Specification:
 * - Soft cool off-white surface (#f6f9ff)
 * - Album artwork full-bleed left -> dissolves into surface by 52% width
 * - Top row: Title + Artist (stacked) | Like (right)
 * - Middle row: Centered transport hub (Prev | Hero Play | Next)
 * - Bottom row: Elapsed | Recessed scrubber | Total
 */
class Widget3x1Receiver : BaseNeoWidget() {

    override fun layoutId(): Int = R.layout.neo_widget_3x1

    override fun configureRemoteViews(
        context: Context,
        views: RemoteViews,
        state: BaseNeoWidget.WidgetStateSnapshot,
        widgetWidthPx: Int,
        widgetHeightPx: Int,
    ) {
        // --- Row 1: Title & Artist ---
        views.setTextViewText(
            R.id.widget_title,
            state.title.ifBlank { context.getString(R.string.no_song_playing) }
        )
        views.setTextViewText(
            R.id.widget_artist,
            state.artist.ifBlank { "" }
        )

        // --- Row 1: Like Button ---
        views.setImageViewResource(
            R.id.widget_btn_like,
            if (state.isLiked) R.drawable.neo_ic_heart else R.drawable.neo_ic_heart_outline,
        )
        val likeTint = if (state.isLiked) 0xFFEF4444.toInt() else 0xFF64748B.toInt()
        views.setInt(R.id.widget_btn_like, "setColorFilter", likeTint)

        // --- Row 2: Transport Controls ---
        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (state.isPlaying) R.drawable.neo_ic_pause else R.drawable.neo_ic_play,
        )

        // --- Row 3: Progress & Timestamps ---
        val safeDuration = state.durationMs.coerceAtLeast(1L)
        val progress = ((state.positionMs.toDouble() / safeDuration.toDouble()) * 100).toInt()
            .coerceIn(0, 100)

        // Hidden progress bar for calculation (layout uses custom track)
        views.setProgressBar(R.id.widget_progress, 100, progress, false)
        
        // Timestamps
        views.setTextViewText(R.id.widget_time_elapsed, formatTime(state.positionMs))
        views.setTextViewText(R.id.widget_time_total, formatTime(state.durationMs))
        // --- Click Intents ---
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

        // Open app when tapping title, artist, or artwork
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
