package com.flexplayer.innertube.pages

import com.flexplayer.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
