/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.models

import com.flexplayer.innertube.models.YTItem
import com.flexplayer.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
