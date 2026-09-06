/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flexplayer.music.db.entities.Song
import com.flexplayer.music.db.entities.SongEntity
import java.time.LocalDateTime

@Entity(
    tableName = "local_media",
    indices = [
        Index(value = ["songId"], unique = true),
        Index(value = ["mediaStoreUri"], unique = true),
        Index(value = ["dateAdded"]),
    ],
)
data class LocalMediaEntity(
    @PrimaryKey
    val id: String,
    val songId: String,
    val mediaStoreUri: String,
    val displayName: String,
    val mimeType: String?,
    val size: Long,
    val duration: Int?,
    val artist: String?,
    val album: String?,
    val title: String?,
    val thumbnailPath: String?,
    val dateAdded: LocalDateTime,
    val dateModified: LocalDateTime,
    val isDownloaded: Boolean = false,
) {
    fun toSong(): Song {
        val artistName = artist?.takeIf { it.isNotBlank() } ?: "Unknown"
        val artistEntity = ArtistEntity(
            id = "local_${songId}_artist",
            name = artistName,
            isLocal = true,
        )
        val albumEntity = album?.takeIf { it.isNotBlank() }?.let { albumName ->
            AlbumEntity(
                id = "local_${songId}_album",
                title = albumName,
                songCount = 0,
                duration = 0,
                isLocal = true,
            )
        }

        val songEntity = SongEntity(
            id = songId,
            title = title ?: displayName,
            duration = duration ?: 0,
            thumbnailUrl = thumbnailPath,
            albumId = albumEntity?.id,
            albumName = album,
            isLocal = true,
            isDownloaded = isDownloaded,
        )
        return Song(
            song = songEntity,
            artists = listOf(artistEntity),
            artistMaps = emptyList(),
            album = albumEntity,
            format = null,
        )
    }
}
