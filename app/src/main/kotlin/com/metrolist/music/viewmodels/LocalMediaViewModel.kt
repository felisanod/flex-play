/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.viewmodels

import android.content.Context
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexplayer.music.db.MusicDatabase
import com.flexplayer.music.db.entities.LocalMediaEntity
import com.flexplayer.music.utils.LocalMediaScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalMediaViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    private val _localMedia = MutableStateFlow<List<LocalMediaEntity>>(emptyList())
    val localMedia: StateFlow<List<LocalMediaEntity>> = _localMedia.asStateFlow()

    fun scanLocalMedia() {
        viewModelScope.launch {
            val scanned = LocalMediaScanner.scanLocalMedia(context, database)
            _localMedia.value = scanned
        }
    }
}
