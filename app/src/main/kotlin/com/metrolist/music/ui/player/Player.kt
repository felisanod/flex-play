/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.ui.player

import androidx.activity.compose.BackHandler
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.flexplayer.music.LocalDatabase
import com.flexplayer.music.LocalDownloadUtil
import com.flexplayer.music.LocalListenTogetherManager
import com.flexplayer.music.LocalPlayerConnection
import com.flexplayer.music.R
import com.flexplayer.music.constants.CropAlbumArtKey
import com.flexplayer.music.constants.DarkModeKey
import com.flexplayer.music.constants.HidePlayerThumbnailKey
import com.flexplayer.music.constants.HideStatusBarOnFullscreenKey
import com.flexplayer.music.constants.KeepScreenOn
import com.flexplayer.music.constants.PlayerBackgroundStyle
import com.flexplayer.music.constants.PlayerBackgroundStyleKey
import com.flexplayer.music.constants.PlayerButtonsStyle
import com.flexplayer.music.constants.PlayerButtonsStyleKey
import com.flexplayer.music.constants.QueuePeekHeight
import com.flexplayer.music.constants.SleepTimerDefaultKey
import com.flexplayer.music.constants.SleepTimerFadeOutKey
import com.flexplayer.music.constants.SleepTimerStopAfterCurrentSongKey
import com.flexplayer.music.db.entities.LyricsEntity
import com.flexplayer.music.extensions.metadata
import com.flexplayer.music.extensions.toggleRepeatMode
import com.flexplayer.music.listentogether.RoomRole
import com.flexplayer.music.models.MediaMetadata
import com.flexplayer.music.ui.component.ActionPromptDialog
import com.flexplayer.music.ui.component.BottomSheet
import com.flexplayer.music.ui.component.BottomSheetState
import com.flexplayer.music.ui.component.LocalBottomSheetPageState
import com.flexplayer.music.ui.component.LocalMenuState
import com.flexplayer.music.ui.component.Lyrics
import com.flexplayer.music.ui.component.NeumorphCard
import com.flexplayer.music.ui.component.NeumorphIconButton
import com.flexplayer.music.ui.component.NeumorphVolumePill
import com.flexplayer.music.ui.menu.PlayerMenu
import com.flexplayer.music.ui.screens.settings.DarkMode
import com.flexplayer.music.ui.theme.PlayerColorExtractor
import com.flexplayer.music.ui.theme.neumorphic
import com.flexplayer.music.ui.theme.neumorphicCircle
import com.flexplayer.music.ui.utils.ShowMediaInfo
import com.flexplayer.music.utils.dataStore
import com.flexplayer.music.utils.makeTimeString
import com.flexplayer.music.utils.rememberEnumPreference
import com.flexplayer.music.utils.rememberPreference
import com.flexplayer.music.utils.safeDataStoreEdit
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.shrinkVertically
import com.flexplayer.music.ui.component.rememberBottomSheetState
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Stitch Design Tokens
 */
private val SoftBg = Color(0xFFF6F9FF)
private val BlueAccent = Color(0xFF2563EB)
private val TextMain = Color(0xFF1E293B)
private val TextMuted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val (hidePlayerThumbnail) = rememberPreference(HidePlayerThumbnailKey, false)
    val (hideStatusBarOnFullscreen) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

    var showInlineLyrics by rememberSaveable { mutableStateOf(false) }
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    val playerBackground by rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(state.isExpanded, keepScreenOn, isFullScreen, hideStatusBarOnFullscreen) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            
            if (isFullScreen && hideStatusBarOnFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }

            if (keepScreenOn) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = state.isExpanded) { state.collapseSoft() }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "pulsingAlpha"
    )

    val castHandler = remember(playerConnection) { runCatching { playerConnection.service.castConnectionHandler }.getOrNull() }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val positionState = remember { mutableLongStateOf(runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)) }
    val durationState = remember {
        mutableLongStateOf((mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
            ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L))
    }

    var position by positionState
    var duration by durationState
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    val effectivePosition by remember { derivedStateOf { if (isCasting) castPosition else position } }

    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100)
                if (sliderPosition == null) {
                    position = playerConnection.player.currentPosition
                    playerConnection.player.duration.takeIf { it > 0 }?.let { duration = it }
                }
            }
        }
    }

    val scope = rememberCoroutineScope()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)

    if (showSleepTimerDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.sleep_timer),
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = {
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer?.start(
                    minute = sleepTimerValue.roundToInt(),
                    stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                    fadeOut = sleepTimerFadeOut
                )
            },
            onCancel = { showSleepTimerDialog = false }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pluralStringResource(R.plurals.minute, sleepTimerValue.roundToInt(), sleepTimerValue.roundToInt()),
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = sleepTimerValue,
                    onValueChange = { sleepTimerValue = it },
                    valueRange = 5f..120f,
                    steps = (120 - 5) / 5 - 1
                )
            }
        }
    }

    val dismissedBound = 0.dp
    val queueSheetState = rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = state.expandedBound,
        collapsedBound = 0.dp,
        initialAnchor = 1
    )

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(modifier = Modifier.fillMaxSize().background(SoftBg).graphicsLayer { if (showInlineLyrics) renderEffect = BlurEffect(16f, 16f, TileMode.Clamp) }) {
                val artworkUrl = mediaMetadata?.thumbnailUrl
                val artworkData = if (!hidePlayerThumbnail && artworkUrl != null) artworkUrl else R.drawable.default_cover
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkData)
                        .crossfade(true)
                        .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 0.85f
                            },
                    )

                // Layer 1: Top Frosted Vignette (0dp to ~200dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to SoftBg.copy(alpha = 0.94f),
                                0.45f to SoftBg.copy(alpha = 0.78f),
                                0.75f to SoftBg.copy(alpha = 0.35f),
                                1.0f to Color.Transparent,
                            ),
                        )
                            .graphicsLayer {
                                if (!hidePlayerThumbnail || artworkData == R.drawable.default_cover) alpha = 1f
                            },
                )

                // Layer 2: Bottom Frosted Dissolve (from ~360dp to bottom)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 360.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.25f to SoftBg.copy(alpha = 0.40f),
                                0.50f to SoftBg.copy(alpha = 0.82f),
                                0.72f to SoftBg.copy(alpha = 0.98f),
                                0.85f to SoftBg,
                                1.00f to SoftBg,
                            ),
                        ),
                )
            }
        },
        onDismiss = if (!isListenTogetherGuest) ({
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        }) else null,
        collapsedContent = {
            MiniPlayer(positionState = positionState, durationState = durationState, onClick = { state.expandSoft() })
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { metadata ->
            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextMain,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = metadata.artists.firstOrNull()?.name ?: "Unknown Artist",
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                
                val isLiked = currentSong?.song?.liked == true
                NeumorphIconButton(
                    onClick = playerConnection::toggleLike,
                    size = 44.dp,
                    background = Color.White
                ) {
                    Icon(
                        painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = null,
                        tint = if (isLiked) BlueAccent else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                val progress = if (duration > 0) (sliderPosition ?: effectivePosition).toFloat() / duration.toFloat() else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .neumorphic(shape = RoundedCornerShape(50), isPressed = true, shadowRadius = 2.dp)
                        .background(Color(0xFFE8EDF5), RoundedCornerShape(50))
                        .pointerInput(duration) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    sliderPosition = (offset.x / size.width.toFloat() * duration.coerceAtLeast(1L).toFloat()).toLong().coerceIn(0L, duration)
                                },
                                onDrag = { change, _ ->
                                    sliderPosition = (change.position.x / size.width.toFloat() * duration.coerceAtLeast(1L).toFloat()).toLong().coerceIn(0L, duration)
                                },
                                onDragEnd = {
                                    sliderPosition?.let { pos ->
                                        val offset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L
                                        playerConnection.player.seekTo(pos - offset)
                                    }
                                    sliderPosition = null
                                },
                                onDragCancel = {
                                    sliderPosition = null
                                },
                            )
                        }
                ) {
                    Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(BlueAccent, RoundedCornerShape(50)))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = makeTimeString(sliderPosition ?: effectivePosition), fontSize = 12.sp, color = TextMuted)
                    Text(text = if (duration > 0) makeTimeString(duration) else "0:00", fontSize = 12.sp, color = TextMuted)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Primary Playback / Transport Hub (Exact Sizes)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                // Previous Button: 56dp circle, 20dp icon
                NeumorphIconButton(
                    onClick = { playerConnection.player.seekToPrevious() },
                    enabled = canSkipPrevious && !isListenTogetherGuest,
                    size = 56.dp,
                    background = Color.White
                ) {
                    Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, tint = TextMain, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(28.dp))

                // Hero Play/Pause Button: 84dp outer ring, 72dp inner disc, 24dp icon
                NeumorphIconButton(
                    onClick = {
                        if (isListenTogetherGuest) playerConnection.toggleMute()
                        else if (isCasting) (if (castIsPlaying) castHandler?.pause() else castHandler?.play())
                        else if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.play() }
                        else playerConnection.togglePlayPause()
                    },
                    size = 84.dp,
                    background = Color.White,
                    modifier = Modifier.focusRequester(focusRequester),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(if (isListenTogetherGuest) (if (isMuted) R.drawable.volume_off else R.drawable.volume_up) else (if (effectiveIsPlaying) R.drawable.pause else R.drawable.play)),
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(28.dp))

                // Next Button: 56dp circle, 20dp icon
                NeumorphIconButton(
                    onClick = { playerConnection.player.seekToNext() },
                    enabled = canSkipNext && !isListenTogetherGuest,
                    size = 56.dp,
                    background = Color.White
                ) {
                    Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, tint = TextMain, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(40.dp))

            // Bottom Utility Row (Exact Sizes & space-between)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Queue Button (≡): 44dp circle, 18dp icon
                NeumorphIconButton(onClick = { queueSheetState.expandSoft() }, size = 44.dp, background = Color.White) {
                    Icon(painter = painterResource(R.drawable.queue_music), contentDescription = null, tint = TextMain, modifier = Modifier.size(18.dp))
                }

                // Shuffle Button (🔀): 44dp circle, 18dp icon
                val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
                NeumorphIconButton(
                    onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                    size = 44.dp,
                    background = Color.White
                ) {
                    Icon(painter = painterResource(R.drawable.shuffle), contentDescription = null, tint = if (shuffleModeEnabled) BlueAccent else TextMain, modifier = Modifier.size(18.dp))
                }

                // Dedicated Repeat Button (🔁): 44dp circle, 18dp icon + pip
                NeumorphIconButton(onClick = { playerConnection.player.toggleRepeatMode() }, size = 44.dp, background = Color.White) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                            contentDescription = null,
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) BlueAccent else TextMain,
                            modifier = Modifier.size(18.dp)
                        )
                        if (repeatMode != Player.REPEAT_MODE_OFF) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 2.dp)
                                    .size(4.dp)
                                    .background(BlueAccent, CircleShape)
                            )
                        }
                    }
                }

                // Tactile Lyrics Pill (💬 Lyrics): 42dp height, 108dp width
                NeumorphCard(
                    onClick = { showInlineLyrics = !showInlineLyrics },
                    shape = RoundedCornerShape(9999.dp),
                    background = Color.White,
                    elevation = 8.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(42.dp).width(108.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .graphicsLayer {
                                    alpha = pulsingAlpha
                                }
                                .clip(CircleShape)
                                .background(if (showInlineLyrics) BlueAccent else Color.Gray)
                        )
                        Icon(
                            painter = painterResource(R.drawable.lyrics),
                            contentDescription = null,
                            tint = if (showInlineLyrics) BlueAccent else TextMain,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Lyrics",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showInlineLyrics) BlueAccent else TextMain
                        )
                    }
                }

                // Sleep Timer (🌙): 44dp circle, 18dp icon
                NeumorphIconButton(onClick = { showSleepTimerDialog = true }, size = 44.dp, background = Color.White) {
                    Icon(painter = painterResource(R.drawable.bedtime), contentDescription = null, tint = TextMain, modifier = Modifier.size(18.dp))
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .nestedScroll(queueSheetState.preUpPostDownNestedScrollConnection)
        ) {
            // [z-10] Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                NeumorphIconButton(onClick = { state.collapseSoft() }, size = 42.dp, background = Color.White) {
                    Icon(painter = painterResource(R.drawable.expand_more), contentDescription = null, modifier = Modifier.size(18.dp), tint = TextMain)
                }
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.5f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))) {
                    Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = Color.White, shadowElevation = 2.dp) {
                            Text("flex-player", color = BlueAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                        Text("Local & Cloud", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
                NeumorphIconButton(onClick = {
                    menuState.show {
                        PlayerMenu(mediaMetadata = mediaMetadata!!, playerBottomSheetState = state, onShowDetailsDialog = { bottomSheetPageState.show { ShowMediaInfo(mediaMetadata!!.id) } }, onDismiss = menuState::dismiss)
                    }
                }, size = 42.dp, background = Color.White) {
                    Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null, modifier = Modifier.size(18.dp), tint = TextMain)
                }
            }

            Text("NOW PLAYING", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)

            // Focal Area (weight(1f) to occupy middle space where background is crisp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showInlineLyrics,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    InlineLyricsView(
                        mediaMetadata = mediaMetadata,
                        showLyrics = true,
                        positionProvider = { effectivePosition }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            mediaMetadata?.let { controlsContent(it) }

            Spacer(Modifier.height(48.dp))
        }

        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                background = MaterialTheme.colorScheme.surfaceContainer,
                onBackgroundColor = MaterialTheme.colorScheme.secondary,
                TextBackgroundColor = MaterialTheme.colorScheme.onBackground,
                textButtonColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                iconButtonColor = MaterialTheme.colorScheme.onSurface,
                pureBlack = pureBlack,
                showInlineLyrics = showInlineLyrics,
                playerBackground = playerBackground,
                onToggleLyrics = { showInlineLyrics = !showInlineLyrics },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.flexplayer.music.di.LyricsHelperEntryPoint::class.java)
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetched = lyricsHelper.getLyrics(mediaMetadata)
                    database.query { upsert(LyricsEntity(mediaMetadata.id, fetched.lyrics, fetched.provider)) }
                } catch (e: Exception) {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        if (lyrics == null) ContainedLoadingIndicator()
        else if (lyrics == LyricsEntity.LYRICS_NOT_FOUND) Text(text = stringResource(R.string.lyrics_not_found), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        else {
            ProvideTextStyle(value = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, textAlign = TextAlign.Center)) {
                Lyrics(sliderPositionProvider = positionProvider, modifier = Modifier.padding(horizontal = 24.dp), showLyrics = showLyrics)
            }
        }
    }
}
