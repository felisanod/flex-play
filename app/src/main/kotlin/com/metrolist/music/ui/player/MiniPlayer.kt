/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Performance optimized MiniPlayer - prevents unnecessary recomposition
 */

package com.flexplayer.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.flexplayer.music.LocalDatabase
import com.flexplayer.music.LocalListenTogetherManager
import com.flexplayer.music.LocalPlayerConnection
import com.flexplayer.music.R
import com.flexplayer.music.constants.CropAlbumArtKey
import com.flexplayer.music.constants.DarkModeKey
import com.flexplayer.music.constants.MiniPlayerHeight
import com.flexplayer.music.constants.PureBlackMiniPlayerKey
import com.flexplayer.music.constants.SwipeSensitivityKey
import com.flexplayer.music.constants.SwipeThumbnailKey
import com.flexplayer.music.constants.ThumbnailCornerRadius
import com.flexplayer.music.constants.UseNewMiniPlayerDesignKey
import com.flexplayer.music.db.entities.ArtistEntity
import com.flexplayer.music.listentogether.ListenTogetherManager
import com.flexplayer.music.models.MediaMetadata
import com.flexplayer.music.playback.CastConnectionHandler
import com.flexplayer.music.playback.PlayerConnection
import com.flexplayer.music.ui.screens.settings.DarkMode
import com.flexplayer.music.ui.utils.resize
import com.flexplayer.music.utils.joinToArtistString
import com.flexplayer.music.utils.makeTimeString
import com.flexplayer.music.utils.rememberEnumPreference
import com.flexplayer.music.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.flexplayer.music.ui.component.Icon as MIcon
import androidx.compose.ui.draw.blur
import com.flexplayer.music.constants.MiniPlayerBackgroundStyle
import com.flexplayer.music.constants.MiniPlayerBackgroundStyleKey
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.flexplayer.music.ui.theme.PlayerColorExtractor
import com.flexplayer.music.ui.theme.neumorphic
import com.flexplayer.music.ui.theme.neumorphicCircle

/**
 * Stable wrapper for progress state - reads values only during draw phase
 * This prevents recomposition when position/duration change
 */
@Stable
class ProgressState(
    val positionState: MutableLongState,
    val durationState: MutableLongState,
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
    val position: Long get() = positionState.longValue
    val duration: Long get() = durationState.longValue
}

@Composable
fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val useNewMiniPlayerDesign by rememberPreference(UseNewMiniPlayerDesignKey, true)

    // Create stable progress state - doesn't cause recomposition on position changes
    val progressState = remember { ProgressState(positionState, durationState) }

    if (useNewMiniPlayerDesign) {
        NewMiniPlayer(
            progressState = progressState,
            modifier = modifier,
            onClick = onClick,
        )
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            LegacyMiniPlayer(
                progressState = progressState,
                modifier = Modifier.align(Alignment.Center),
                onClick = onClick,
            )
        }
    }
}

// ============================================================================
// NEW MINI PLAYER DESIGN
// ============================================================================

@Composable
private fun NewMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    
    // Stitch Design Tokens
    val backgroundColor = Color(0xFFE8EDF5)
    val surfaceColor = Color(0xFFF6F9FF)
    val accentBlue = Color(0xFF3B82F6)
    val textMain = Color(0xFF1E293B)
    val textMuted = Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            // Left: Album Thumbnail (44dp, RoundedCornerShape(12.dp))
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl?.resize(120, 120) ?: R.drawable.default_cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Info & Progress
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mediaMetadata?.title ?: "",
                        color = textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "${makeTimeString(progressState.position)} / ${makeTimeString(progressState.duration)}",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = mediaMetadata?.artists?.joinToArtistString(", ") { it.name } ?: "",
                    color = textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar: Deep inset neomorphic track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressState.progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(accentBlue, Color(0xFF60A5FA))),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Right: Control row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = textMain,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { playerConnection.seekToPrevious() }
                )

                // Play/Pause (Primary Blue)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .neumorphicCircle(
                            backgroundColor = accentBlue,
                            shadowRadius = 8.dp
                        )
                        .background(accentBlue, CircleShape)
                        .clickable { playerConnection.togglePlayPause() }
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = textMain,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { playerConnection.player.seekToNext() }
                )
            }
        }
    }
}

/**
 * Play button with circular progress indicator
 * Uses drawWithContent to update progress without recomposition
 */
@Composable
private fun NewMiniPlayerPlayButton(
    progressState: ProgressState,
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata?,
    primaryColor: Color,
    outlineColor: Color,
    listenTogetherManager: ListenTogetherManager?,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val trackColor = outlineColor.copy(alpha = 0.2f)
    val strokeWidth = 3.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(48.dp)
                .drawWithContent {
                    drawContent()
                    // Draw progress arc - this reads progressState.progress during draw phase only
                    val progress = progressState.progress
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    val startAngle = -90f
                    val sweepAngle = 360f * progress
                    val diameter = size.minDimension
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

                    // Draw track
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke,
                    )
                    // Draw progress
                    drawArc(
                        color = primaryColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke,
                    )
                },
    ) {
        // Thumbnail with play/pause overlay
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .neumorphicCircle(backgroundColor = MaterialTheme.colorScheme.surfaceContainer, shadowRadius = 3.dp)
                    .clickable {
                        if (isListenTogetherGuest) {
                            playerConnection.toggleMute()
                            return@clickable
                        }
                        if (isCasting) {
                            if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                        } else if (playbackState == Player.STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.togglePlayPause()
                        }
                    },
        ) {
            mediaMetadata?.let { metadata ->
                val thumbnailUrl =
                    remember(metadata.thumbnailUrl) {
                        metadata.thumbnailUrl?.resize(120, 120)
                    }
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            }

            // Overlay for paused state or muted (guest)
            if (isListenTogetherGuest && isMuted ||
                (!isListenTogetherGuest && (!effectiveIsPlaying || playbackState == Player.STATE_ENDED))
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                )
                Icon(
                    painter =
                        painterResource(
                            if (isListenTogetherGuest) {
                                if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                            } else if (playbackState == Player.STATE_ENDED) {
                                R.drawable.replay
                            } else {
                                R.drawable.play
                            },
                        ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Song info display - title and artist
 */
@Composable
private fun NewMiniPlayerSongInfo(
    mediaMetadata: MediaMetadata?,
    onSurfaceColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        mediaMetadata?.let { metadata ->
            Text(
                text = metadata.title,
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (metadata.explicit) MIcon.Explicit()
                 if (metadata.artists.any { it.name.isNotBlank() }) {
                     Text(
                         text = metadata.artists.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name },
                         color = onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                    )
                }
            }

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.error_playing),
                    color = errorColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// LEGACY MINI PLAYER DESIGN
// ============================================================================

@Composable
private fun LegacyMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val pureBlack by rememberPreference(PureBlackMiniPlayerKey, defaultValue = false)

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()

    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)

    // Disable swipe for Listen Together guests
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isTabletLandscape =
        remember(windowInfo.containerSize.width, configuration.orientation) {
            (windowInfo.containerSize.width / density.density) >= 600f && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec =
        remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        }

    val autoSwipeThreshold =
        remember(swipeSensitivity) {
            (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
        }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .then(if (isTabletLandscape) Modifier.width(500.dp) else Modifier.fillMaxWidth())
                .height(MiniPlayerHeight)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    if (pureBlack && isSystemInDarkTheme()) {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ).clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                ).let { baseModifier ->
                    if (swipeThumbnail) {
                        baseModifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragStartTime = System.currentTimeMillis()
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val adjustedDragAmount =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val tryingToSwipeRight = adjustedDragAmount > 0
                                    val tryingToSwipeLeft = adjustedDragAmount < 0
                                    val allowLeft = tryingToSwipeLeft && canSkipNext
                                    val allowRight = tryingToSwipeRight && canSkipPrevious

                                    val canReturnToCenter =
                                        (tryingToSwipeRight && !canSkipPrevious && offsetXAnimatable.value < 0) ||
                                            (tryingToSwipeLeft && !canSkipNext && offsetXAnimatable.value > 0)

                                    if (allowLeft || allowRight || canReturnToCenter) {
                                        totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val dragDuration = System.currentTimeMillis() - dragStartTime
                                    val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                    val currentOffset = offsetXAnimatable.value
                                    val minDistanceThreshold = 50f
                                    val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                    val shouldChangeSong =
                                        (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                            (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                    if (shouldChangeSong) {
                                        if (currentOffset > 0 && canSkipPrevious) {
                                            playerConnection.seekToPrevious()
                                        } else if (currentOffset <= 0 && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                    coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                                },
                            )
                        }
                    } else {
                        baseModifier
                    }
                },
    ) {
        // Progress bar - uses drawWithContent to avoid recomposition
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .drawWithContent {
                        val progress = progressState.progress
                        drawRect(trackColor)
                        drawRect(primaryColor, size = Size(size.width * progress, size.height))
                    },
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .padding(end = 12.dp),
        ) {
            Box(Modifier.weight(1f)) {
                mediaMetadata?.let {
                    LegacyMiniMediaInfo(
                        mediaMetadata = it,
                        pureBlack = pureBlack,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }

            LegacyPlayPauseButton(
                playbackState = playbackState,
                isCasting = isCasting,
                castHandler = castHandler,
                playerConnection = playerConnection,
                listenTogetherManager = listenTogetherManager,
            )

            IconButton(
                enabled = canSkipNext && !isListenTogetherGuest,
                onClick = if (isListenTogetherGuest) ({}) else ({ playerConnection.seekToNext() }),
            ) {
                Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null)
            }
        }

        // Swipe indicator
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier =
                    Modifier
                        .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 16.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next,
                        ),
                    contentDescription = null,
                    tint =
                        primaryColor.copy(
                            alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f),
                        ),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun LegacyPlayPauseButton(
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    listenTogetherManager: ListenTogetherManager?,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    IconButton(
        onClick = {
            if (isListenTogetherGuest) {
                playerConnection.toggleMute()
                return@IconButton
            }
            if (isCasting) {
                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
            } else if (playbackState == Player.STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                playerConnection.togglePlayPause()
            }
        },
    ) {
        Icon(
            painter =
                painterResource(
                    when {
                        isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        playbackState == Player.STATE_ENDED -> R.drawable.replay
                        effectiveIsPlaying -> R.drawable.pause
                        else -> R.drawable.play
                    },
                ),
            contentDescription = null,
        )
    }
}

@Composable
private fun LegacyMiniMediaInfo(
    mediaMetadata: MediaMetadata,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(6.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            val thumbnailUrl =
                remember(mediaMetadata.thumbnailUrl) {
                    mediaMetadata.thumbnailUrl?.resize(144, 144)
                }
            AsyncImage(
                model = thumbnailUrl ?: R.drawable.default_cover,
                contentDescription = null,
                contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            androidx.compose.animation.AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = if (pureBlack) Color.Black else Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
        ) {
            Text(
                text = mediaMetadata.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )

             if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                 Text(
                     text = mediaMetadata.artists.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name },
                     color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// ISOLATED BUTTON COMPOSABLES - Prevent parent recomposition
// ============================================================================

@Composable
private fun SubscribeButton(
    artistId: String,
    metadata: MediaMetadata,
    primaryColor: Color,
    outlineColor: Color,
    onSurfaceColor: Color,
) {
    val database = LocalDatabase.current
    val libraryArtist by database.artist(artistId).collectAsStateWithLifecycle(initialValue = null)
    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null


    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isSubscribed) primaryColor.copy(alpha = 0.5f) else outlineColor.copy(alpha = 0.3f),
                    shape = CircleShape,
                ).background(
                    color = if (isSubscribed) primaryColor.copy(alpha = 0.1f) else Color.Transparent,
                    shape = CircleShape,
                ).clickable {
                    database.transaction {
                        val artist = libraryArtist?.artist
                        if (artist != null) {
                            update(artist.toggleLike())
                        } else {
                            metadata.artists.firstOrNull()?.let { artistInfo ->
                                insert(
                                    ArtistEntity(
                                        id = artistInfo.id ?: "",
                                        name = artistInfo.name,
                                        channelId = null,
                                        thumbnailUrl = null,
                                    ).toggleLike(),
                                )
                            }
                        }
                    }
                },
    ) {
        Icon(
            painter = painterResource(if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe),
            contentDescription = null,
            tint = if (isSubscribed) primaryColor else onSurfaceColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FavoriteButton(
    songId: String,
    errorColor: Color,
    outlineColor: Color,
    onSurfaceColor: Color,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(songId).collectAsStateWithLifecycle(initialValue = null)
    // For episodes, show saved state (inLibrary); for songs, show liked state
    val isEpisode = librarySong?.song?.isEpisode == true
    val isLiked = if (isEpisode) librarySong?.song?.inLibrary != null else librarySong?.song?.liked == true

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isLiked) errorColor.copy(alpha = 0.5f) else outlineColor.copy(alpha = 0.3f),
                    shape = CircleShape,
                ).background(
                    color = if (isLiked) errorColor.copy(alpha = 0.1f) else Color.Transparent,
                    shape = CircleShape,
                ).clickable { playerConnection.service.toggleLike() },
    ) {
        Icon(
            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
            contentDescription = null,
            tint = if (isLiked) errorColor else onSurfaceColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}
