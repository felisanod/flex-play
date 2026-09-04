/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flexplayer.music.R
import com.flexplayer.music.db.entities.LocalMediaEntity
import com.flexplayer.music.ui.component.SongListItem
import com.flexplayer.music.viewmodels.LocalMediaViewModel

@Composable
fun LocalMediaScreen() {
    val viewModel: LocalMediaViewModel = hiltViewModel()
    val localMedia by viewModel.localMedia.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.scanLocalMedia()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (localMedia.isEmpty()) {
            Text(
                text = stringResource(R.string.no_downloaded_episodes),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn {
                items(localMedia, key = { it.id }) { localMediaItem ->
                    SongListItem(
                        song = localMediaItem.toSong(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
