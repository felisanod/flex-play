/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import android.graphics.Bitmap
import com.flexplayer.innertube.YouTube
import com.flexplayer.innertube.models.SongItem
import com.metrolist.innertubex.extraction.ContentHints
import com.flexplayer.music.constants.AudioQuality
import com.flexplayer.music.constants.AudioQualityKey
import com.flexplayer.music.db.MusicDatabase
import com.flexplayer.music.db.entities.AlbumEntity
import com.flexplayer.music.db.entities.FormatEntity
import com.flexplayer.music.db.entities.Song
import com.flexplayer.music.db.entities.SongEntity
import com.flexplayer.music.di.DownloadCache
import com.flexplayer.music.di.PlayerCache
import com.flexplayer.music.models.MediaMetadata
import com.flexplayer.music.models.toMediaMetadata
import com.flexplayer.music.utils.InnerTubeXPlayer
import com.flexplayer.music.utils.enumPreference
import com.flexplayer.music.utils.LocalMediaMetadata
import com.flexplayer.music.utils.LocalMediaStore
import com.flexplayer.music.utils.LocalMediaScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: Cache,
    @PlayerCache val playerCache: Cache,
) {
    private val TAG = "DownloadUtil"
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val songUrlCache = StreamUrlCache()
    private val streamHttpClient =
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .proxyAuthenticator { _, response ->
                YouTube.proxyAuth?.let { auth ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", auth)
                        .build()
                } ?: response.request
            }
            .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadPreparations = Semaphore(3)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setCacheWriteDataSinkFactory(null)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(streamHttpClient),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.let { cachedStream ->
                return@Factory dataSpec.withResolvedStream(cachedStream)
            }
            val cacheGeneration = songUrlCache.generation(mediaId)

            val playbackData = runBlocking(Dispatchers.IO) {
                val song = database.songEntity(mediaId)
                InnerTubeXPlayer.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    contentHints = ContentHints(
                        isExplicit = song?.explicit,
                        isUploaded = song?.isUploaded,
                    ),
                    allowBoundedRange = false,
                )
            }.getOrThrow()
            val format = playbackData.format

            val actualContentLength =
                format.contentLength?.takeIf { it > 0L } ?: run {
                    val request = okhttp3.Request.Builder()
                        .get()
                        .url(playbackData.streamUrl)
                        .apply {
                            playbackData.streamHeaders.forEach { (name, value) ->
                                header(name, value)
                            }
                        }
                        .header("Range", "bytes=0-0")
                        .build()
                    try {
                        streamHttpClient.newCall(request).execute().use { response ->
                            downloadContentLength(
                                statusCode = response.code,
                                contentRange = response.header("Content-Range"),
                                contentLength = response.header("Content-Length"),
                            )
                        }
                    } catch (_: IOException) {
                        null
                    }
                }

            database.query {
                if (actualContentLength != null) {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.substringBefore(";"),
                            codecs =
                                format.mimeType
                                    .substringAfter("codecs=", missingDelimiterValue = "")
                                    .substringBefore(";")
                                    .trim()
                                    .removeSurrounding("\""),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = actualContentLength,
                            loudnessDb = playbackData.audioConfig?.loudnessDb,
                            perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                            playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                        ),
                    )
                } else {
                    deleteFormat(mediaId)
                }

                // Metadata registration only — dateDownload is intentionally NOT set here.
                // It belongs solely to onDownloadChanged()'s STATE_COMPLETED branch below,
                // which only fires once the download has actually finished. Setting it here
                // (at URL-resolve time, i.e. the moment the download merely *starts*) would
                // mark the song as "cached" before a single byte is written.
                val existing = getSongByIdBlocking(mediaId)?.song
                val updatedSong = existing ?: SongEntity(
                    id = mediaId,
                    title = playbackData.videoDetails?.title ?: "Unknown",
                    duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                    thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                    dateDownload = null,
                    isDownloaded = false
                )

                upsert(updatedSong)
            }

            val streamUrl = playbackData.streamUrl

            songUrlCache.put(
                mediaId = mediaId,
                url = streamUrl,
                requestHeaders = playbackData.streamHeaders,
                clientName = playbackData.streamClient,
                expiresInSeconds = playbackData.streamExpiresInSeconds,
                requireBoundedRange = playbackData.requireBoundedRange,
                rangeChunkSizeBytes = playbackData.rangeChunkSizeBytes,
                useRangeChunks = playbackData.useRangeChunks,
                expectedGeneration = cacheGeneration,
            )
            dataSpec.withResolvedStream(
                CachedStreamUrl(
                    url = streamUrl,
                    requestHeaders = playbackData.streamHeaders,
                    clientName = playbackData.streamClient,
                    requireBoundedRange = playbackData.requireBoundedRange,
                    rangeChunkSizeBytes = playbackData.rangeChunkSizeBytes,
                    useRangeChunks = playbackData.useRangeChunks,
                ),
            )
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        if (download.state == Download.STATE_FAILED && finalException.isExpiredStreamError()) {
                            songUrlCache.invalidate(download.request.id)
                        }

                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        scope.launch {
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    removeFromPlayerCache(download.request.id)
                                    scope.launch {
                                        saveToMediaStore(download.request.id)
                                        kotlinx.coroutines.delay(500)
                                        LocalMediaScanner.removeMissingFiles(context, database)
                                        LocalMediaScanner.scanLocalMedia(context, database)
                                    }
                                }
                                Download.STATE_FAILED,
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)
                                }
                                else -> {
                                }
                            }
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download,
                    ) {
                        val downloadId = download.request.id
                        songUrlCache.invalidate(downloadId)

                        scope.launch {
                            runCatching {
                                database.localMedia(downloadId)?.let { localMedia ->
                                    LocalMediaStore.deleteFromMediaStore(context, localMedia.mediaStoreUri)
                                    database.deleteLocalMedia(downloadId)
                                }
                                database.updateDownloadedInfo(downloadId, false, null)
                            }.onSuccess {
                                downloads.update { map ->
                                    map.toMutableMap().apply {
                                        remove(downloadId)
                                    }
                                }
                                Timber.tag(TAG).d("Successfully removed download $downloadId from in-memory map")
                            }.onFailure { error ->
                                Timber.tag(TAG).e(error, "Failed to update database for removed download $downloadId, keeping in-memory entry")
                            }
                        }
                    }
                }
            )
        }

    init {
        val result = mutableMapOf<String, Download>()
        downloadManager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.download.request.id] = cursor.download
            }
        }
        downloads.value = result
        scope.launch {
            result.values
                .filter { it.state == Download.STATE_COMPLETED }
                .forEach { removeFromPlayerCache(it.request.id) }
        }
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun download(song: Song) = download(song.toMediaMetadata())

    fun download(song: SongItem) = download(song.toMediaMetadata())

    fun download(mediaMetadata: MediaMetadata) {
        scope.launch {
            downloadPreparations.withPermit {
                if (!shouldPrepareDownload(downloads.value[mediaMetadata.id]?.state)) return@withPermit

                mediaMetadata.album?.let { album ->
                    if (database.albumEntity(album.id) == null) {
                        database.insert(
                            AlbumEntity(
                                id = album.id,
                                title = album.title,
                                thumbnailUrl = mediaMetadata.thumbnailUrl,
                                songCount = 0,
                                duration = 0,
                            ),
                        )
                    }
                }

                val existing = database.getSongByIdBlocking(mediaMetadata.id)
                if (existing == null) {
                    database.insert(mediaMetadata)
                } else {
                    database.update(
                        existing,
                        mediaMetadata,
                        overwriteTitle = false,
                        overwriteArtists = false,
                    )
                }

                if (!shouldPrepareDownload(downloadManager.downloadIndex.getDownload(mediaMetadata.id)?.state)) {
                    return@withPermit
                }

                val request =
                    DownloadRequest
                        .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                        .setCustomCacheKey(mediaMetadata.id)
                        .setData(mediaMetadata.title.toByteArray())
                        .build()
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    request,
                    false,
                )

                val albumArtwork = database.getSongByIdBlocking(mediaMetadata.id)?.album?.thumbnailUrl
                downloadArtworkUrls(mediaMetadata.thumbnailUrl, albumArtwork).forEach { artworkUrl ->
                    runCatching {
                        context.imageLoader.execute(
                            ImageRequest
                                .Builder(context)
                                .data(artworkUrl)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .networkCachePolicy(CachePolicy.ENABLED)
                                .build(),
                        )
                    }
                }
            }
        }
    }

    fun download(songId: String) {
        scope.launch {
            database.getSongByIdBlocking(songId)?.let { song ->
                download(song)
            }
        }
    }

    fun release() {
        scope.cancel()
    }

    private suspend fun saveToMediaStore(songId: String) {
        val song = database.getSongByIdBlocking(songId) ?: run {
            Timber.tag(TAG).w("saveToMediaStore: song $songId not found")
            return
        }
        val formatEntity = database.format(songId).first() ?: run {
            Timber.tag(TAG).w("saveToMediaStore: format for $songId not found")
            return
        }

        // MediaStore rejects some containers (e.g. audio/webm). When that
        // happens LocalMediaStore falls back to writing the file directly to
        // the public Music directory so the bytes still end up on disk and
        // visible to other apps.
        val mimeType = formatEntity.mimeType ?: "audio/mpeg"

        // Download cover artwork for embedding
        val artworkFile = downloadArtwork(songId, song.thumbnailUrl, song.title)

        val metadata = LocalMediaMetadata(
            title = song.title,
            artist = song.artists.joinToString(", ") { it.name }.ifBlank { null },
            album = song.album?.title,
            duration = song.song.duration.takeIf { it > 0 },
            thumbnailPath = song.thumbnailUrl,
            artworkFile = artworkFile,
        )

        val cacheDataSourceFactory = CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(streamHttpClient),
            )

        val tempFile = File(context.cacheDir, "tmp_download_$songId")
        if (tempFile.exists()) tempFile.delete()

        val dataSource = cacheDataSourceFactory.createDataSource()
        val dataSpec = DataSpec(songId.toUri())
        try {
            dataSource.open(dataSpec)
            FileOutputStream(tempFile).use { fos ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val bytesRead = dataSource.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) break
                    fos.write(buffer, 0, bytesRead)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to read cached file for $songId")
            tempFile.delete()
            return
        } finally {
            runCatching { dataSource.close() }
        }

        if (!tempFile.exists() || tempFile.length() == 0L) {
            Timber.tag(TAG).w("saveToMediaStore: temp file missing/empty for $songId")
            tempFile.delete()
            return
        }

        Timber.tag(TAG).d(
            "saveToMediaStore: read ${tempFile.length()} bytes for $songId (expected ${formatEntity.contentLength}, mimeType=$mimeType)",
        )

        val localMedia = LocalMediaStore.saveDownloadedFile(
            context = context,
            sourceFile = tempFile,
            songId = songId,
            displayName = song.title,
            mimeType = mimeType,
            metadata = metadata,
        )

        tempFile.delete()

        if (localMedia != null) {
            runCatching { database.insert(localMedia) }
                .onFailure { Timber.tag(TAG).e(it, "Failed to insert LocalMediaEntity for $songId") }
            database.updateDownloadedInfo(songId, true, LocalDateTime.now())
            Timber.tag(TAG).d("saveToMediaStore: $songId saved to ${localMedia.mediaStoreUri}")
        } else {
            Timber.tag(TAG).w("saveToMediaStore: failed to persist $songId to MediaStore")
        }
    }

    private fun removeFromPlayerCache(songId: String) {
        runCatching { playerCache.removeResource(songId) }
            .onFailure { Timber.tag(TAG).w(it, "Failed to remove downloaded song $songId from player cache") }
    }

    private suspend fun downloadArtwork(songId: String, artworkUrl: String?, songTitle: String?): File? {
        if (artworkUrl == null || artworkUrl.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .size(512, 512)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result !is SuccessResult) {
                    Timber.tag(TAG).w("Failed to download artwork for $songId")
                    return@withContext null
                }
                val bitmap = result.image.toBitmap()
                val safeTitle = songTitle?.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")?.trim()?.takeIf { it.isNotBlank() } ?: "cover_$songId"
                val artworkFile = File(context.cacheDir, "artwork_${safeTitle}.jpg")
                artworkFile.outputStream().use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                artworkFile
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to download artwork for $songId")
                null
            }
        }
    }

    private fun Throwable?.isExpiredStreamError(): Boolean {
        var current = this
        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException &&
                (current.responseCode == 403 || current.responseCode == 410 || current.responseCode == 416)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}

internal fun shouldPrepareDownload(downloadState: Int?): Boolean = downloadState != Download.STATE_COMPLETED

internal fun downloadArtworkUrls(
    songArtwork: String?,
    albumArtwork: String?,
): List<String> = listOfNotNull(songArtwork, albumArtwork).filter(String::isNotBlank).distinct()

internal fun downloadContentLength(
    statusCode: Int,
    contentRange: String?,
    contentLength: String?,
): Long? {
    val rangePattern =
        when (statusCode) {
            206 -> PARTIAL_CONTENT_RANGE
            416 -> UNSATISFIED_CONTENT_RANGE
            else -> null
        }
    if (rangePattern != null) {
        return contentRange
            ?.trim()
            ?.let(rangePattern::matchEntire)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
    }
    return if (statusCode == 200) contentLength?.toLongOrNull()?.takeIf { it > 0L } else null
}

private val PARTIAL_CONTENT_RANGE = Regex("""bytes\s+0-0/(\d+)""", RegexOption.IGNORE_CASE)
private val UNSATISFIED_CONTENT_RANGE = Regex("""bytes\s+\*/(\d+)""", RegexOption.IGNORE_CASE)
