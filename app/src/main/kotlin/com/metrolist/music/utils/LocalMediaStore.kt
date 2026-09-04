/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.flexplayer.music.db.entities.LocalMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

object LocalMediaStore {
    private const val flex-player_DIRECTORY = "flex-player/Music"

    suspend fun saveDownloadedFile(
        context: Context,
        sourceFile: File,
        songId: String,
        displayName: String,
        mimeType: String,
        metadata: LocalMediaMetadata,
    ): LocalMediaEntity? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Environment.DIRECTORY_MUSIC + "/$flex-player_DIRECTORY"
            } else {
                Environment.DIRECTORY_MUSIC + "/$flex-player_DIRECTORY"
            }

            val fileName = "$displayName.${mimeType.substringAfterLast("/").substringAfterLast(";")}"

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                put(MediaStore.Audio.Media.TITLE, metadata.title ?: displayName)
                put(MediaStore.Audio.Media.ARTIST, metadata.artist)
                put(MediaStore.Audio.Media.ALBUM, metadata.album)
                put(MediaStore.Audio.Media.DURATION, metadata.duration?.toLong() ?: 0L)
            }

            val uri = contentResolver.insert(collection, contentValues)
                ?: return@withContext null

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }

            val mediaStoreUri = uri.toString()
            val now = LocalDateTime.now()

            LocalMediaEntity(
                id = songId,
                songId = songId,
                mediaStoreUri = mediaStoreUri,
                displayName = fileName,
                mimeType = mimeType,
                size = sourceFile.length(),
                duration = metadata.duration,
                artist = metadata.artist,
                album = metadata.album,
                title = metadata.title,
                thumbnailPath = metadata.thumbnailPath,
                dateAdded = now,
                dateModified = now,
                isDownloaded = true,
            )
        } catch (e: Exception) {
            Timber.tag("LocalMediaStore").e(e, "Failed to save downloaded file to MediaStore")
            null
        }
    }

    suspend fun deleteFromMediaStore(context: Context, mediaStoreUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(mediaStoreUri)
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Timber.tag("LocalMediaStore").e(e, "Failed to delete from MediaStore: $mediaStoreUri")
            false
        }
    }

    fun getLocalMediaUris(context: Context): List<Uri> {
        val uris = mutableListOf<Uri>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$flex-player_DIRECTORY%")

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                uris.add(Uri.withAppendedPath(collection, id.toString()))
            }
        }

        return uris
    }
}

data class LocalMediaMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val duration: Int?,
    val thumbnailPath: String?,
)
