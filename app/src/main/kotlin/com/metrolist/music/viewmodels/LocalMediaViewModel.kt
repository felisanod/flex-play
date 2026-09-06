/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexplayer.music.db.MusicDatabase
import com.flexplayer.music.db.entities.LocalMediaEntity
import com.flexplayer.music.utils.AudioScanner
import com.flexplayer.music.utils.LocalMediaScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class LocalMediaViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    val isRefreshing = MutableStateFlow(false)

    val localMedia: StateFlow<List<LocalMediaEntity>> = database.localMediaSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val audioScanner = AudioScanner(context)

    init {
        scanLocalMedia()
    }

    fun scanLocalMedia() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                LocalMediaScanner.removeMissingFiles(context, database)
                // Scan with the standalone AudioScanner; falls back to the legacy
                // scanner on permission errors or older API levels.
                val files = runCatching { audioScanner.scanAudioFiles(minDurationMs = 5_000L) }
                    .onFailure { Timber.tag(TAG).e(it, "AudioScanner failed; falling back to LocalMediaScanner") }
                    .getOrNull()

                if (files != null) {
                    val existingUris = database.localMediaSongs().first().map { it.mediaStoreUri }.toSet()
                    val entities = files
                        .filter { file -> file.uri.toString() !in existingUris }
                        .map { file ->
                            val uriString = file.uri.toString()
                            LocalMediaEntity(
                                id = "audio_${file.id}",
                                songId = "audio_${file.id}",
                                mediaStoreUri = uriString,
                                displayName = file.path.substringAfterLast('/').ifEmpty { file.title },
                                mimeType = file.mimeType,
                                size = file.size,
                                duration = file.duration.toInt().takeIf { it > 0 },
                                artist = file.artist,
                                album = file.album,
                                title = file.title,
                                thumbnailPath = null,
                                dateAdded = LocalDateTime.ofEpochSecond(file.dateAdded, 0, ZoneOffset.UTC),
                                dateModified = LocalDateTime.ofEpochSecond(file.dateAdded, 0, ZoneOffset.UTC),
                                isDownloaded = false,
                            )
                        }
                    if (entities.isNotEmpty()) {
                        database.insert(entities)
                    }
                    Timber.tag(TAG).d("AudioScanner inserted ${entities.size} new audio files (${files.size - entities.size} already known)")
                } else {
                    LocalMediaScanner.scanLocalMedia(context, database)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "scanLocalMedia failed")
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private companion object {
        const val TAG = "LocalMediaViewModel"
    }
}
