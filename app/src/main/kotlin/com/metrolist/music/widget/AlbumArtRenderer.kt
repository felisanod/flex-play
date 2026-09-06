/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlbumArtRenderer {

    /**
     * Loads [artworkUri] and returns a bitmap sized to [width]x[height]
     * with a horizontal linear-gradient mask applied so the album art fades
     * seamlessly into the widget's surface color.
     */
    suspend fun loadFaded(
        context: Context,
        artworkUri: String?,
        width: Int,
        height: Int,
        surfaceColorArgb: Int = 0xFFE8EDF5.toInt(),
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (width <= 0 || height <= 0) return@withContext null

        val baseBitmap = loadBitmap(context, artworkUri ?: com.flexplayer.music.R.drawable.default_cover, width, height) ?: return@withContext null

        val output = createBitmap(width, height)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(baseBitmap, 0f, 0f, paint)

        val maskShader = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(
                0xFFFFFFFF.toInt(),
                0xFFFFFFFF.toInt(),
                0x66FFFFFF,
                0x00FFFFFF,
            ),
            floatArrayOf(0f, 0.32f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )
        val maskPaint = Paint().apply {
            shader = maskShader
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)

        val surfacePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                intArrayOf(0x00FFFFFF, surfaceColorArgb),
                floatArrayOf(0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), surfacePaint)

        // if (output !== baseBitmap) baseBitmap.recycle()
        output
    }

    private suspend fun loadBitmap(
        context: Context,
        data: Any?,
        width: Int,
        height: Int,
    ): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(data)
            .size(width, height)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        if (result !is SuccessResult) return null
        val image = result.image
        val original = image.toBitmap()
        return if (original.width == width && original.height == height) {
            original
        } else {
            val scaled = Bitmap.createScaledBitmap(original, width, height, true)
            // if (scaled !== original) original.recycle()
            scaled
        }
    }
}
