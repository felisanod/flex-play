/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.flexplayer.music.db.MusicDatabase
import com.flexplayer.music.db.entities.LocalMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

object LocalMediaScanner {
    private const val flex-player_DIRECTORY = "flex-player/Music"

    suspend fun scanLocalMedia(context: Context, database: MusicDatabase): List<LocalMediaEntity> = withContext(Dispatchers.IO) {
        val localMediaList = mutableListOf<LocalMediaEntity>()
        val contentResolver = context.contentResolver

        val existingUris = database.localMediaSongs().first().map { it.mediaStoreUri }.toSet()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.RELATIVE_PATH,
        )

        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(
            "%$flex-player_DIRECTORY%",
            "%flex-player%"
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val mediaStoreUri = contentUri.toString()
                val displayName = cursor.getString(displayNameColumn) ?: "Unknown"
                val mimeType = cursor.getString(mimeTypeColumn)
                val size = cursor.getLong(sizeColumn)
                val duration = cursor.getInt(durationColumn).takeIf { it > 0 }
                val artist = cursor.getString(artistColumn)
                val album = cursor.getString(albumColumn)
                val title = cursor.getString(titleColumn)
                val dateAddedSeconds = cursor.getLong(dateAddedColumn)
                val dateModifiedSeconds = cursor.getLong(dateModifiedColumn)
                val relativePath = cursor.getString(relativePathColumn) ?: ""

                val dateAdded = LocalDateTime.ofEpochSecond(dateAddedSeconds, 0, ZoneOffset.UTC)
                val dateModified = LocalDateTime.ofEpochSecond(dateModifiedSeconds, 0, ZoneOffset.UTC)

                val songId = generateSongId(mediaStoreUri)

                if (mediaStoreUri !in existingUris) {
                    val thumbnailPath = extractThumbnail(context, contentUri)

                    val localMedia = LocalMediaEntity(
                        id = songId,
                        songId = songId,
                        mediaStoreUri = mediaStoreUri,
                        displayName = displayName,
                        mimeType = mimeType,
                        size = size,
                        duration = duration,
                        artist = artist,
                        album = album,
                        title = title,
                        thumbnailPath = thumbnailPath,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        isDownloaded = relativePath.contains(flex-player_DIRECTORY),
                    )
                    localMediaList.add(localMedia)
                }
            }
        }

        if (localMediaList.isNotEmpty()) {
            database.insert(localMediaList)
            Timber.tag("LocalMediaScanner").d("Inserted ${localMediaList.size} new local media items")
        }

        localMediaList
    }

    suspend fun removeMissingFiles(context: Context, database: MusicDatabase): Int = withContext(Dispatchers.IO) {
        val deletedCount = database.localMediaSongs().first().count { localMedia ->
            try {
                val uri = Uri.parse(localMedia.mediaStoreUri)
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Audio.Media._ID),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    !cursor.moveToFirst()
                } ?: true
            } catch (e: Exception) {
                true
            }
        }

        if (deletedCount > 0) {
            database.clearLocalMedia()
            Timber.tag("LocalMediaScanner").d("Removed $deletedCount missing local media items")
        }

        deletedCount
    }

    private fun generateSongId(mediaStoreUri: String): String {
        return "local_${mediaStoreUri.hashCode().toString(16)}"
    }

    private fun extractThumbnail(context: Context, contentUri: Uri): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, contentUri)
            val rawArt = retriever.embeddedPicture
            retriever.release()

            if (rawArt != null) {
                val file = File(context.cacheDir, "local_thumb_${contentUri.hashCode()}.jpg")
                FileOutputStream(file).use { fos ->
                    fos.write(rawArt)
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag("LocalMediaScanner").e(e, "Failed to extract thumbnail")
            null
        }
    }
}
