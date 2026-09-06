package com.flexplayer.music.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.min

enum class NeumorphicStyle {
    Raised, RaisedLG, Inset
}

fun Modifier.neumorphic(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color(0xFFE8EDF5),
    lightShadowColor: Color = Color.White,
    darkShadowColor: Color = Color(0xFFA6B4C8).copy(alpha = 0.65f),
    shadowRadius: Dp = 12.dp,
    style: NeumorphicStyle? = null,
    isPressed: Boolean = false,
): Modifier = this.then(
    Modifier.drawBehind {
        val actualStyle = style ?: when {
            isPressed -> NeumorphicStyle.Inset
            shadowRadius >= 20.dp -> NeumorphicStyle.RaisedLG
            else -> NeumorphicStyle.Raised
        }
        
        val (offsetPx, blurPx) = when (actualStyle) {
            NeumorphicStyle.Raised -> 5.dp.toPx() to 13.dp.toPx()
            NeumorphicStyle.RaisedLG -> 8.dp.toPx() to 21.dp.toPx()
            NeumorphicStyle.Inset -> 3.dp.toPx() to 6.dp.toPx()
        }

        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                color = darkShadowColor.toArgb()
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }

            if (actualStyle != NeumorphicStyle.Inset) {
                // Light shadow (top-left)
                val lightPaint = Paint().asFrameworkPaint().apply {
                    color = lightShadowColor.toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                
                // Using a slightly larger area to avoid clipping of the blur
                val cornerRadius = if (shape is RoundedCornerShape) {
                    shape.topStart.toPx(size, this)
                } else blurPx

                canvas.nativeCanvas.drawRoundRect(
                    -offsetPx, -offsetPx, size.width - offsetPx, size.height - offsetPx,
                    cornerRadius, cornerRadius, lightPaint
                )
                // Dark shadow (bottom-right)
                canvas.nativeCanvas.drawRoundRect(
                    offsetPx, offsetPx, size.width + offsetPx, size.height + offsetPx,
                    cornerRadius, cornerRadius, paint
                )
            } else {
                // Inset dark shadow (bottom-right of well)
                val cornerRadius = if (shape is RoundedCornerShape) {
                    shape.topStart.toPx(size, this)
                } else blurPx
                
                canvas.nativeCanvas.drawRoundRect(
                    offsetPx, offsetPx, size.width - offsetPx, size.height - offsetPx,
                    cornerRadius, cornerRadius, paint
                )

                // Inset light shadow (top-left of well)
                val lightPaint = Paint().asFrameworkPaint().apply {
                    color = lightShadowColor.toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawRoundRect(
                    -offsetPx, -offsetPx, size.width - offsetPx, size.height - offsetPx,
                    cornerRadius, cornerRadius, lightPaint
                )
            }
        }
    }
)

fun Modifier.neumorphicCircle(
    backgroundColor: Color = Color(0xFFE8EDF5),
    lightShadowColor: Color = Color.White,
    darkShadowColor: Color = Color(0xFFA6B4C8).copy(alpha = 0.65f),
    shadowRadius: Dp = 12.dp,
    style: NeumorphicStyle? = null,
    isPressed: Boolean = false,
): Modifier = this.then(
    Modifier.drawBehind {
        val actualStyle = style ?: when {
            isPressed -> NeumorphicStyle.Inset
            shadowRadius >= 20.dp -> NeumorphicStyle.RaisedLG
            else -> NeumorphicStyle.Raised
        }
        
        val (offsetPx, blurPx) = when (actualStyle) {
            NeumorphicStyle.Raised -> 5.dp.toPx() to 13.dp.toPx()
            NeumorphicStyle.RaisedLG -> 8.dp.toPx() to 21.dp.toPx()
            NeumorphicStyle.Inset -> 3.dp.toPx() to 6.dp.toPx()
        }

        val centerX = size.width / 2
        val centerY = size.height / 2
        val circleRadius = min(size.width, size.height) / 2

        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                color = darkShadowColor.toArgb()
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }

            if (actualStyle != NeumorphicStyle.Inset) {
                // Light shadow (top-left)
                val lightPaint = Paint().asFrameworkPaint().apply {
                    color = lightShadowColor.toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawCircle(
                    centerX - offsetPx, centerY - offsetPx, circleRadius, lightPaint
                )
                // Dark shadow (bottom-right)
                canvas.nativeCanvas.drawCircle(
                    centerX + offsetPx, centerY + offsetPx, circleRadius, paint
                )
            } else {
                // Inset dark shadow (bottom-right of well)
                canvas.nativeCanvas.drawCircle(
                    centerX + offsetPx / 2, centerY + offsetPx / 2, circleRadius - offsetPx / 2, paint
                )
                // Inset light shadow (top-left of well)
                val lightPaint = Paint().asFrameworkPaint().apply {
                    color = lightShadowColor.toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawCircle(
                    centerX - offsetPx / 2, centerY - offsetPx / 2, circleRadius - offsetPx / 2, lightPaint
                )
            }
        }
    }
)

