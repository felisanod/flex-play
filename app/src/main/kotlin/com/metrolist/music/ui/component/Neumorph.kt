/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexplayer.music.ui.theme.neumorphic
import com.flexplayer.music.ui.theme.neumorphicCircle

object NeumorphDefaults {
    val CornerSmall = 12.dp
    val CornerMedium = 18.dp
    val CornerLarge = 24.dp
    val CornerXLarge = 32.dp

    val ElevationSmall = 6.dp
    val ElevationMedium = 12.dp
    val ElevationLarge = 20.dp
}

@Composable
fun NeumorphCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(NeumorphDefaults.CornerLarge),
    elevation: Dp = NeumorphDefaults.ElevationMedium,
    background: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "scale"
    )

    val activeElevation = if (pressed) (elevation / 2) else elevation
    val modifierWithClick = if (onClick != null) {
        modifier.clickable(
            interactionSource = src,
            indication = null,
            onClick = onClick,
        )
    } else {
        modifier
    }
    val shadowModifier = if (pressed) {
        Modifier.neumorphicInsetShadow(elevation = activeElevation, shape = shape, background = background)
    } else {
        Modifier.neumorphicRaisedShadow(elevation = activeElevation, shape = shape, background = background)
    }
    Box(
        modifier = modifierWithClick
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(shadowModifier)
            .clip(shape)
            .background(background)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun NeumorphIconButton(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = CircleShape,
    elevation: Dp = NeumorphDefaults.ElevationMedium,
    background: Color = MaterialTheme.colorScheme.surface,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "scale"
    )

    val activeElevation = if (pressed) (elevation / 2) else elevation
    val shadowModifier = if (pressed) {
        Modifier.neumorphicInsetShadow(elevation = activeElevation, shape = shape, background = background)
    } else {
        Modifier.neumorphicRaisedShadow(elevation = activeElevation, shape = shape, background = background)
    }
    val clickableModifier = if (enabled) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    val effectiveBackground = if (enabled) background else background.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(shadowModifier)
            .clip(shape)
            .background(effectiveBackground)
            .then(clickableModifier),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun NeumorphPrimaryButton(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    shape: Shape = CircleShape,
    background: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "scale"
    )

    val shadowModifier = if (pressed) {
        Modifier.neumorphicInsetShadow(elevation = NeumorphDefaults.ElevationMedium, shape = shape, background = background)
    } else {
        Modifier.neumorphicRaisedShadow(elevation = NeumorphDefaults.ElevationLarge, shape = shape, background = background)
    }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(shadowModifier)
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun NeumorphVolumePill(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color(0xFFE8EDF5),
) {
    NeumorphCard(
        shape = RoundedCornerShape(50),
        elevation = 8.dp,
        background = background,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(140.dp)
        ) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(com.flexplayer.music.R.drawable.volume_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color(0xFF3B82F6),
                    inactiveTrackColor = Color.Black.copy(alpha = 0.05f)
                ),
                track = { sliderState ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .neumorphic(
                                shape = RoundedCornerShape(50),
                                isPressed = true,
                                shadowRadius = 2.dp
                            )
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sliderState.value)
                                .fillMaxHeight()
                                .background(Color(0xFF3B82F6), RoundedCornerShape(50))
                        )
                    }
                }
            )

            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(com.flexplayer.music.R.drawable.volume_up),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NeumorphFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = Color(0xFFE8EDF5)
    val surfaceColor = Color(0xFFF6F9FF)
    val accentBlue = Color(0xFF3B82F6)
    val textMuted = Color(0xFF64748B)

    val shadowModifier = if (selected) {
        Modifier.neumorphicInsetShadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(50),
            background = backgroundColor
        )
    } else {
        Modifier.neumorphicRaisedShadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(50),
            background = backgroundColor
        )
    }

    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .then(shadowModifier)
            .clip(RoundedCornerShape(50))
            .background(if (selected) backgroundColor else surfaceColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                color = if (selected) accentBlue else textMuted,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun NeumorphListItem(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(NeumorphDefaults.CornerMedium),
    content: @Composable RowScope.() -> Unit,
) {
    NeumorphCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        elevation = NeumorphDefaults.ElevationSmall,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun Modifier.neumorphicRaisedShadow(
    elevation: Dp,
    shape: Shape,
    background: Color,
): Modifier {
    val isCircle = shape == CircleShape
    return if (isCircle) {
        this.neumorphicCircle(
            backgroundColor = background,
            shadowRadius = elevation,
        )
    } else {
        this.neumorphic(
            shape = shape,
            backgroundColor = background,
            shadowRadius = elevation,
        )
    }
}

@Composable
fun Modifier.neumorphicInsetShadow(
    elevation: Dp,
    shape: Shape,
    background: Color,
): Modifier {
    val isCircle = shape == CircleShape
    return if (isCircle) {
        this.neumorphicCircle(
            backgroundColor = background,
            shadowRadius = elevation,
            isPressed = true,
        )
    } else {
        this.neumorphic(
            shape = shape,
            backgroundColor = background,
            shadowRadius = elevation,
            isPressed = true,
        )
    }
}

@Composable
fun NeumorphFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    background: Color = MaterialTheme.colorScheme.surface,
    tint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shadowModifier = if (pressed) {
        Modifier.neumorphicInsetShadow(
            elevation = NeumorphDefaults.ElevationMedium,
            shape = CircleShape,
            background = background,
        )
    } else {
        Modifier.neumorphicRaisedShadow(
            elevation = NeumorphDefaults.ElevationLarge,
            shape = CircleShape,
            background = background,
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .then(shadowModifier)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides tint,
        ) {
            content()
        }
    }
}
