package com.flexplayer.innertube.pages

import com.flexplayer.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
