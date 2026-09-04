package com.flexplayer.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

fun Modifier.neumorphic(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color(0xFFE0E0E0),
    lightShadowColor: Color = Color.White,
    darkShadowColor: Color = Color(0xFFA0A0A0),
    shadowRadius: Dp = 8.dp,
    isPressed: Boolean = false,
): Modifier = this.then(
    Modifier.drawBehind {
        val radiusPx = shadowRadius.toPx()

        if (!isPressed) {
            drawRoundRect(
                color = lightShadowColor,
                topLeft = Offset(radiusPx / 2, radiusPx / 2),
                size = size.copy(width = size.width - radiusPx, height = size.height - radiusPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx),
                alpha = 0.8f,
            )
            drawRoundRect(
                color = darkShadowColor,
                topLeft = Offset(-radiusPx / 2, -radiusPx / 2),
                size = size.copy(width = size.width + radiusPx, height = size.height + radiusPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx),
                alpha = 0.8f,
            )
        } else {
            drawRoundRect(
                color = darkShadowColor,
                topLeft = Offset(radiusPx / 2, radiusPx / 2),
                size = size.copy(width = size.width - radiusPx, height = size.height - radiusPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx),
                alpha = 0.5f,
            )
            drawRoundRect(
                color = lightShadowColor,
                topLeft = Offset(-radiusPx / 2, -radiusPx / 2),
                size = size.copy(width = size.width + radiusPx, height = size.height + radiusPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx),
                alpha = 0.5f,
            )
        }
    }
)

fun Modifier.neumorphicCircle(
    backgroundColor: Color = Color(0xFFE0E0E0),
    lightShadowColor: Color = Color.White,
    darkShadowColor: Color = Color(0xFFA0A0A0),
    shadowRadius: Dp = 8.dp,
    isPressed: Boolean = false,
): Modifier = this.then(
    Modifier.drawBehind {
        val radiusPx = shadowRadius.toPx()
        val centerX = size.width / 2
        val centerY = size.height / 2
        val circleRadius = min(size.width, size.height) / 2 - radiusPx / 2

        if (!isPressed) {
            drawCircle(
                color = lightShadowColor,
                radius = circleRadius + radiusPx / 2,
                center = Offset(centerX + radiusPx / 3, centerY + radiusPx / 3),
                alpha = 0.8f,
            )
            drawCircle(
                color = darkShadowColor,
                radius = circleRadius + radiusPx / 2,
                center = Offset(centerX - radiusPx / 3, centerY - radiusPx / 3),
                alpha = 0.8f,
            )
        } else {
            drawCircle(
                color = darkShadowColor,
                radius = circleRadius + radiusPx / 2,
                center = Offset(centerX + radiusPx / 3, centerY + radiusPx / 3),
                alpha = 0.5f,
            )
            drawCircle(
                color = lightShadowColor,
                radius = circleRadius + radiusPx / 2,
                center = Offset(centerX - radiusPx / 3, centerY - radiusPx / 3),
                alpha = 0.5f,
            )
        }
    }
)
