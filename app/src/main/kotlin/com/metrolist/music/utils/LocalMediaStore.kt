/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.flexplayer.music.db.entities.LocalMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

object LocalMediaStore {
    private const val FLEX_PLAYER_DIRECTORY = "flex-player/Music"
    private const val MAX_FILENAME_LENGTH = 200
    private const val LEGACY_MUSIC_DIR = "flex-player/Music"

    suspend fun saveDownloadedFile(
        context: Context,
        sourceFile: File,
        songId: String,
        displayName: String,
        mimeType: String,
        metadata: LocalMediaMetadata,
    ): LocalMediaEntity? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            Timber.tag(TAG).w("Source file missing or empty for $songId")
            return@withContext null
        }

        try {
            val extension = sanitizeExtension(mimeType)
            val safeBaseName = sanitizeFileName(displayName, songId)
            val fileName = "$safeBaseName.$extension"

            val mediaStoreUri: Uri
            val finalFile: File?
            val contentResolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaStoreUri = saveToMediaStoreQ(
                    contentResolver = contentResolver,
                    fileName = fileName,
                    mimeType = mimeType,
                    metadata = metadata,
                    sourceFile = sourceFile,
                ) ?: return@withContext null
                finalFile = null
            } else {
                finalFile = saveToLegacyFile(
                    fileName = fileName,
                    sourceFile = sourceFile,
                )
                mediaStoreUri = registerLegacyFile(
                    contentResolver = contentResolver,
                    file = finalFile,
                    mimeType = mimeType,
                    metadata = metadata,
                ) ?: return@withContext null
            }

            val now = LocalDateTime.now()
            val durationMs = metadata.duration?.toLong() ?: 0L
            val resolvedDuration = if (durationMs > 0L) {
                durationMs.toInt()
            } else {
                readDuration(sourceFile)
            }

            LocalMediaEntity(
                id = songId,
                songId = songId,
                mediaStoreUri = mediaStoreUri.toString(),
                displayName = fileName,
                mimeType = mimeType,
                size = sourceFile.length(),
                duration = resolvedDuration,
                artist = metadata.artist,
                album = metadata.album,
                title = metadata.title ?: displayName,
                thumbnailPath = metadata.thumbnailPath,
                dateAdded = now,
                dateModified = now,
                isDownloaded = true,
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save downloaded file to MediaStore for $songId")
            null
        }
    }

    private fun saveToMediaStoreQ(
        contentResolver: ContentResolver,
        fileName: String,
        mimeType: String,
        metadata: LocalMediaMetadata,
        sourceFile: File,
    ): Uri? {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relativePath = Environment.DIRECTORY_MUSIC + "/$FLEX_PLAYER_DIRECTORY"

        // Android 12 MediaStore rejects audio/webm, audio/ogg and audio/opus
        // outright. Lie about the MIME type to audio/mp4 (which IS accepted)
        // so the file gets indexed, and rely on the player's
        // DefaultExtractorsFactory to read the actual codec.
        val storedMime = if (mimeType.equals("audio/webm", true) ||
            mimeType.equals("audio/ogg", true) ||
            mimeType.equals("audio/opus", true)
        ) {
            "audio/mp4"
        } else {
            mimeType
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, storedMime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
            put(MediaStore.Audio.Media.TITLE, metadata.title ?: fileName)
            put(MediaStore.Audio.Media.ARTIST, metadata.artist)
            put(MediaStore.Audio.Media.ALBUM, metadata.album)
            put(MediaStore.Audio.Media.DURATION, metadata.duration?.toLong() ?: 0L)
        }

        val uri = try {
            contentResolver.insert(collection, contentValues)
        } catch (e: IllegalArgumentException) {
            Timber.tag(TAG).w(e, "MediaStore rejected $storedMime for $fileName")
            return null
        } ?: run {
            Timber.tag(TAG).w("MediaStore.insert returned null for $fileName")
            return null
        }

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                contentResolver.delete(uri, null, null)
                Timber.tag(TAG).w("openOutputStream returned null for $uri")
                return null
            }
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            Timber.tag(TAG).e(e, "Failed to write bytes for $uri")
            return null
        }

        contentValues.clear()
        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
        contentResolver.update(uri, contentValues, null, null)
        return uri
    }

    private fun saveToLegacyFile(
        fileName: String,
        sourceFile: File,
    ): File? {
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            LEGACY_MUSIC_DIR,
        )
        if (!musicDir.exists() && !musicDir.mkdirs()) {
            Timber.tag(TAG).w("Could not create legacy music dir: ${musicDir.absolutePath}")
            return null
        }

        val targetFile = File(musicDir, fileName)
        if (targetFile.exists()) {
            val base = fileName.substringBeforeLast('.')
            val ext = fileName.substringAfterLast('.', "")
            targetFile.delete()
            val altFile = File(musicDir, "${base}_${System.currentTimeMillis()}.${ext}")
            return copyFile(sourceFile, altFile)
        }
        return copyFile(sourceFile, targetFile)
    }

    private fun copyFile(
        src: File,
        dst: File,
    ): File? = try {
        src.inputStream().use { input ->
            FileOutputStream(dst).use { output ->
                input.copyTo(output)
            }
        }
        dst
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Failed to copy to legacy file ${dst.absolutePath}")
        null
    }

    private fun registerLegacyFile(
        contentResolver: ContentResolver,
        file: File?,
        mimeType: String,
        metadata: LocalMediaMetadata,
    ): Uri? {
        if (file == null) return null
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DATA, file.absolutePath)
            put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.TITLE, metadata.title ?: file.name)
            put(MediaStore.Audio.Media.ARTIST, metadata.artist)
            put(MediaStore.Audio.Media.ALBUM, metadata.album)
            put(MediaStore.Audio.Media.DURATION, metadata.duration?.toLong() ?: 0L)
        }
        return contentResolver.insert(collection, contentValues)
    }

    suspend fun deleteFromMediaStore(
        context: Context,
        mediaStoreUri: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(mediaStoreUri)
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to delete from MediaStore: $mediaStoreUri")
            false
        }
    }

    fun getLocalMediaUris(context: Context): List<Uri> {
        val uris = mutableListOf<Uri>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$FLEX_PLAYER_DIRECTORY%")

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                uris.add(Uri.withAppendedPath(collection, id.toString()))
            }
        }

        return uris
    }

    private fun sanitizeFileName(
        displayName: String,
        songId: String,
    ): String {
        val raw = displayName.ifBlank { "flex-player_${songId.take(12)}" }
        val cleaned = raw
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', ' ')
        return when {
            cleaned.isEmpty() -> "flex-player_${songId.take(12)}"
            cleaned.length > MAX_FILENAME_LENGTH ->
                cleaned.substring(0, MAX_FILENAME_LENGTH).trimEnd('.', ' ')
            else -> cleaned
        }
    }

    private fun sanitizeExtension(mimeType: String): String {
        val ext = mimeType.substringAfterLast("/").substringAfterLast(";").trim()
        return when (ext.lowercase()) {
            "mpeg", "mp3" -> "mp3"
            "mp4", "m4a", "mp4a-latm" -> "m4a"
            "ogg", "vorbis" -> "ogg"
            "opus" -> "opus"
            "flac", "x-flac" -> "flac"
            "wav", "x-wav" -> "wav"
            "aac", "x-aac" -> "aac"
            else -> ext.ifBlank { "mp3" }
        }
    }

    private fun readDuration(file: File): Int? = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        ms?.toLongOrNull()?.toInt()
    } catch (e: Exception) {
        null
    }

    private const val TAG = "LocalMediaStore"
}

data class LocalMediaMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val duration: Int?,
    val thumbnailPath: String?,
)
