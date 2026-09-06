/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.flexplayer.music.models.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioScanner(private val context: Context) {

    /**
     * Scans device storage for audio files using MediaStore.
     * Runs off the main thread and returns a clean list of AudioFile.
     */
    suspend fun scanAudioFiles(
        minDurationMs: Long = 0L,
    ): List<AudioFile> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioFile>()

        val collection: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                readCursor(cursor, collection, audioList)
            }
        } catch (e: SecurityException) {
            throw AudioScanException("Missing permission to read audio media", e)
        } catch (e: Exception) {
            throw AudioScanException("Failed to scan audio files", e)
        }

        audioList
    }

    private fun readCursor(
        cursor: Cursor,
        collection: Uri,
        out: MutableList<AudioFile>,
    ) {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val contentUri = Uri.withAppendedPath(collection, id.toString())

            out.add(
                AudioFile(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    duration = cursor.getLong(durationCol),
                    size = cursor.getLong(sizeCol),
                    path = cursor.getString(pathCol) ?: "",
                    uri = contentUri,
                    mimeType = cursor.getString(mimeCol) ?: "audio/*",
                    dateAdded = cursor.getLong(dateCol),
                ),
            )
        }
    }
}

class AudioScanException(message: String, cause: Throwable? = null) : Exception(message, cause)
