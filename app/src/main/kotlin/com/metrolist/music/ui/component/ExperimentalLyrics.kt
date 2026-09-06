/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import android.text.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.flexplayer.music.LocalDatabase
import com.flexplayer.music.LocalListenTogetherManager
import com.flexplayer.music.LocalPlayerConnection
import com.flexplayer.music.R
import com.flexplayer.music.constants.AiProviderKey
import com.flexplayer.music.constants.AiSystemPromptKey
import com.flexplayer.music.constants.DeeplApiKey
import com.flexplayer.music.constants.DeeplFormalityKey
import com.flexplayer.music.constants.LyricsClickKey
import com.flexplayer.music.constants.LyricsRomanizeAsMainKey
import com.flexplayer.music.constants.LyricsRomanizeCyrillicByLineKey
import com.flexplayer.music.constants.LyricsRomanizeList
import com.flexplayer.music.constants.LyricsTextPositionKey
import com.flexplayer.music.constants.OpenRouterApiKey
import com.flexplayer.music.constants.OpenRouterBaseUrlKey
import com.flexplayer.music.constants.OpenRouterDefaultBaseUrl
import com.flexplayer.music.constants.OpenRouterDefaultModel
import com.flexplayer.music.constants.OpenRouterModelKey
import com.flexplayer.music.constants.PlayerBackgroundStyle
import com.flexplayer.music.constants.PlayerBackgroundStyleKey
import com.flexplayer.music.constants.RespectAgentPositioningKey
import com.flexplayer.music.constants.ShowIntervalIndicatorKey
import com.flexplayer.music.constants.TranslateLanguageKey
import com.flexplayer.music.constants.TranslateModeKey
import com.flexplayer.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.flexplayer.music.lyrics.LyricsTranslationHelper
import com.flexplayer.music.lyrics.LyricsUtils.findActiveLineIndices
import com.flexplayer.music.lyrics.lyricsTextLooksSynced
import com.flexplayer.music.ui.component.shimmer.ShimmerHost
import com.flexplayer.music.ui.component.shimmer.TextPlaceholder
import com.flexplayer.music.ui.screens.settings.LyricsPosition
import com.flexplayer.music.ui.screens.settings.defaultList
import com.flexplayer.music.ui.utils.fadingEdge
import com.flexplayer.music.utils.ComposeToImage
import com.flexplayer.music.utils.rememberEnumPreference
import com.flexplayer.music.utils.rememberPreference
import com.flexplayer.music.viewmodels.LyricsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LYRICS_ANCHOR_RATIO = 0.35f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 68.dp
private val LYRICS_ITEM_GAP_DP = 16.dp
private val LYRICS_FADE_TOP_DP = 130.dp
private val LYRICS_FADE_BOTTOM_DP = 160.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 20
private const val LYRICS_STAGGER_DELAY_MAX_MS = 200
private const val LYRICS_PREVIEW_TIME = 8000L

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun ExperimentalLyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    lyricsViewModel: LyricsViewModel = hiltViewModel()
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val romanizeLyricsList = rememberPreference(LyricsRomanizeList, "")
    val romanizeAsMain by rememberPreference(LyricsRomanizeAsMainKey, false)
    val romanizeCyrillicByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    val respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
    val showIntervalIndicator by rememberPreference(ShowIntervalIndicatorKey, true)
    
    // AI Translation Preferences
    val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val deeplApiKey by rememberPreference(DeeplApiKey, "")
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, OpenRouterDefaultBaseUrl)
    val openRouterModel by rememberPreference(OpenRouterModelKey, OpenRouterDefaultModel)
    val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(TranslateModeKey, "Literal")
    val deeplFormality by rememberPreference(DeeplFormalityKey, "default")
    val aiSystemPrompt by rememberPreference(AiSystemPromptKey, "")
    
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val translationStatus by LyricsTranslationHelper.status.collectAsStateWithLifecycle()
    val currentLyricsEntity by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    var lastValidLyricsEntity by remember { mutableStateOf<com.flexplayer.music.db.entities.LyricsEntity?>(null) }
    
    LaunchedEffect(currentLyricsEntity) {
        if (currentLyricsEntity != null) {
            lastValidLyricsEntity = currentLyricsEntity
        }
    }
    
    val lyricsEntity = remember(currentLyricsEntity, translationStatus) {
        if (currentLyricsEntity != null) {
            currentLyricsEntity
        } else if (translationStatus is LyricsTranslationHelper.TranslationStatus.Translating || translationStatus is LyricsTranslationHelper.TranslationStatus.Success) {
            lastValidLyricsEntity
        } else {
            null
        }
    }
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val enabledLanguages = remember(romanizeLyricsList.value) {
        if (romanizeLyricsList.value.isEmpty()) {
            defaultList
        } else {
            romanizeLyricsList.value.split(",").map { entry ->
                val (lang, checked) = entry.split(":")
                Pair(lang, checked.toBoolean())
            }
        }.filter { it.second }.map { it.first }
    }

    val lines by lyricsViewModel.lines.collectAsStateWithLifecycle()
    val mergedLyricsList by lyricsViewModel.mergedLyricsList.collectAsStateWithLifecycle()

    LaunchedEffect(lyrics, enabledLanguages, romanizeCyrillicByLine, showIntervalIndicator) {
        lyricsViewModel.processLyrics(lyrics, enabledLanguages, romanizeCyrillicByLine, showIntervalIndicator)
    }

    val isSynced = remember(lyrics) { lyricsTextLooksSynced(lyrics) }
    val hasWordTimings = remember(lines) { lines.any { it.words?.isNotEmpty() == true } }

    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose {
            LyricsTranslationHelper.setCompositionActive(false)
            LyricsTranslationHelper.cancelTranslation()
        }
    }
    
    LaunchedEffect(lines, lyricsEntity, translateLanguage, translateMode) {
        if (lines.isNotEmpty() && lyricsEntity != null) {
            LyricsTranslationHelper.loadTranslationsFromDatabase(
                lyrics = lines,
                lyricsEntity = lyricsEntity,
                targetLanguage = translateLanguage,
                mode = translateMode
            )
        }
    }
    
    LaunchedEffect(
        showLyrics, 
        lines, 
        aiProvider, 
        openRouterApiKey, 
        deeplApiKey, 
        openRouterBaseUrl, 
        openRouterModel, 
        translateLanguage, 
        translateMode,
        deeplFormality,
        aiSystemPrompt,
        currentSong,
        database
    ) {
        LyricsTranslationHelper.manualTrigger.collectLatest {
            val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
            if (showLyrics && lines.isNotEmpty() && effectiveApiKey.isNotBlank()) {
                LyricsTranslationHelper.translateLyrics(
                    lyrics = lines,
                    targetLanguage = translateLanguage,
                    apiKey = openRouterApiKey,
                    baseUrl = openRouterBaseUrl,
                    model = openRouterModel,
                    mode = translateMode,
                    scope = scope,
                    context = context,
                    provider = aiProvider,
                    deeplApiKey = deeplApiKey,
                    deeplFormality = deeplFormality,
                    useStreaming = true,
                    songId = currentSong?.id ?: "",
                    database = database,
                    systemPrompt = aiSystemPrompt,
                )
            } else if (effectiveApiKey.isBlank()) {
                Toast.makeText(context, context.getString(R.string.ai_api_key_required), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    LaunchedEffect(lines) {
        LyricsTranslationHelper.clearTranslationsTrigger.collectLatest {
            lines.forEach { it.translatedTextFlow.value = null }
        }
    }

    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> Color.White
    }

    var activeLineIndices by remember { mutableStateOf(emptySet<Int>()) }
    var scrollTargetIndex by rememberSaveable { mutableIntStateOf(-1) }
    var previousScrollActiveIndices by remember { mutableStateOf(emptySet<Int>()) }
    
    var currentPositionState by remember { mutableLongStateOf(0L) }
    var deferredCurrentLineIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastPreviewTime by rememberSaveable { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    val isLyricsProviderShown = lyricsEntity != null && lyricsEntity.provider != "Unknown" && lyricsEntity.provider != "Manual" && !isSelectionModeActive
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    val maxSelectionLimit = 5
    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(context, context.getString(R.string.max_selection_limit, maxSelectionLimit), Toast.LENGTH_SHORT).show()
            showMaxSelectionToast = false
        }
    }

    var lastMainMaxSeen by remember(lyrics, lines) { mutableIntStateOf(-1) }
    var smoothPositionForSync by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lyrics, lines) {
        if (lyrics.isNullOrEmpty() || lines.isEmpty()) {
            activeLineIndices = emptySet()
            return@LaunchedEffect
        }
        
        var lastPlayerPos = playerConnection.player.currentPosition
        var lastUpdateTime = System.currentTimeMillis()
        
        while (isActive) {
            withFrameNanos { _ -> }
            val now = System.currentTimeMillis()
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            
            val position = if (isSeeking) {
                sliderPosition!!
            } else {
                val playerPos = playerConnection.player.currentPosition
                if (playerPos != lastPlayerPos) {
                    lastPlayerPos = playerPos
                    lastUpdateTime = now
                }
                val elapsed = now - lastUpdateTime
                lastPlayerPos + (if (playerConnection.player.isPlaying) elapsed else 0)
            }
            
            currentPositionState = position
            smoothPositionForSync = position
            
            val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
            val effectivePosition = position + lyricsOffset
            
            val initialActiveIndices = findActiveLineIndices(lines, effectivePosition)
            val scrollActiveIndicesRaw = findActiveLineIndices(lines, effectivePosition + (if (hasWordTimings) 0L else 250L))
            
            val scrollActiveIndices = scrollActiveIndicesRaw.toMutableSet()
            for (i in scrollActiveIndicesRaw) {
                if (lines.getOrNull(i)?.isBackground == true) {
                    for (j in i - 1 downTo 0) {
                        if (lines.getOrNull(j)?.isBackground == false) {
                            scrollActiveIndices.add(j)
                            break
                        }
                    }
                }
            }
            
            val newActiveIndices = initialActiveIndices.toMutableSet()
            for (i in initialActiveIndices) {
                if (lines.getOrNull(i)?.isBackground == true) {
                    for (j in i - 1 downTo 0) {
                        if (lines.getOrNull(j)?.isBackground == false) {
                            newActiveIndices.add(j)
                            break
                        }
                    }
                }
            }

            val scrollMax = scrollActiveIndices
                .filter { lines.getOrNull(it)?.isBackground == false }
                .maxOrNull() ?: (scrollActiveIndices.maxOrNull() ?: -1)

            val isCurrentTargetStillActive = scrollTargetIndex in scrollActiveIndices
            val anyStillActive = scrollActiveIndices.isNotEmpty()

            val shouldScroll = when {
                isSeeking -> true
                // If current target just ended and there are other active lines, scroll to the new max
                !isCurrentTargetStillActive && anyStillActive && scrollMax > scrollTargetIndex -> true
                
                // If current target just ended and no other active lines
                !isCurrentTargetStillActive && !anyStillActive && previousScrollActiveIndices.isNotEmpty() -> true
                
                // If we don't have a target yet and lines became active
                scrollTargetIndex == -1 && anyStillActive -> true
                
                // New line started while nothing was active before
                previousScrollActiveIndices.isEmpty() && anyStillActive && scrollMax > scrollTargetIndex -> true

                else -> false
            }

            if (shouldScroll) {
                val targetToScroll = when {
                    isSeeking -> scrollMax
                    !isCurrentTargetStillActive && anyStillActive -> scrollMax
                    !isCurrentTargetStillActive && !anyStillActive -> {
                        (lastMainMaxSeen + 1 until lines.size).firstOrNull {
                            lines.getOrNull(it)?.isBackground == false
                        } ?: scrollTargetIndex
                    }
                    else -> scrollMax
                }
                if (targetToScroll != -1 && (isSeeking || targetToScroll > scrollTargetIndex)) {
                    scrollTargetIndex = targetToScroll
                }
            }
            
            if (scrollMax > lastMainMaxSeen && scrollMax != -1) {
                lastMainMaxSeen = scrollMax
            }
            
            previousScrollActiveIndices = scrollActiveIndices
            activeLineIndices = newActiveIndices
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LYRICS_PREVIEW_TIME)
            lastPreviewTime = 0L
        }
    }

    LaunchedEffect(scrollTargetIndex, isAutoScrollEnabled) {
        if (scrollTargetIndex != -1 && isAutoScrollEnabled) {
            deferredCurrentLineIndex = scrollTargetIndex
        }
    }

    var userManualOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(lyrics, lines) {
        isAutoScrollEnabled = true
        userManualOffset = 0f
        scrollTargetIndex = -1
        deferredCurrentLineIndex = 0
        isSelectionModeActive = false
        selectedIndices.clear()
        previousScrollActiveIndices = emptySet()
    }
    
    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val itemHeights = remember(lyrics, mergedLyricsList) { mutableStateMapOf<Int, Int>() }
    var isInitialLayout by remember(lyrics, mergedLyricsList) { mutableStateOf(true) }

    val activeListIndex by remember(mergedLyricsList, deferredCurrentLineIndex) {
        derivedStateOf {
            mergedLyricsList.indexOfFirst { 
                (it is LyricsListItem.Line && it.index == deferredCurrentLineIndex) ||
                (it is LyricsListItem.Indicator && it.afterLineIndex == deferredCurrentLineIndex)
            }.coerceAtLeast(0)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(showLyrics) {
        val activity = context as? Activity
        if (showLyrics) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize().padding(bottom = 12.dp)
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val anchorY = maxHeightPx * LYRICS_ANCHOR_RATIO
        val lineHeightPx = with(density) { LYRICS_ITEM_FALLBACK_HEIGHT_DP.toPx() }
        val indicatorHeightPx = with(density) { 72.dp.toPx() }
        
        // Use a more permissive fallback for constraints to prevent "locks" if items are not measured yet
        val constraintLineHeightPx = with(density) { 120.dp.toPx() }

        val positions = remember(itemHeights.toMap(), activeListIndex, mergedLyricsList) {
            val map = mutableMapOf<Int, Float>()
            if (activeListIndex == -1 || mergedLyricsList.isEmpty()) return@remember map
            
            map[activeListIndex] = 0f
            var currentY = 0f
            for (i in activeListIndex - 1 downTo 0) {
                val item = mergedLyricsList[i]
                val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItem.Indicator) indicatorHeightPx else lineHeightPx)
                val noGap = (item as? LyricsListItem.Line)?.entry?.isBackground == true || item is LyricsListItem.Indicator
                currentY -= (height + if (noGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                map[i] = currentY
            }
            currentY = 0f
            for (i in activeListIndex until mergedLyricsList.size - 1) {
                val currentItem = mergedLyricsList[i]
                val nextItem = mergedLyricsList[i + 1]
                val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItem.Indicator) indicatorHeightPx else lineHeightPx)
                val nextNoGap = (nextItem as? LyricsListItem.Line)?.entry?.isBackground == true || nextItem is LyricsListItem.Indicator
                currentY += (height + if (nextNoGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                map[i + 1] = currentY
            }
            map
        }

        val minOffset = remember(itemHeights.toMap(), mergedLyricsList, activeListIndex, anchorY) {
            if (mergedLyricsList.isEmpty() || activeListIndex == -1) return@remember 0f
            val totalBelow = (activeListIndex until mergedLyricsList.size - 1).sumOf { i ->
                val currentItem = mergedLyricsList[i]
                val nextItem = mergedLyricsList[i + 1]
                val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                val nextNoGap = (nextItem as? LyricsListItem.Line)?.entry?.isBackground == true || nextItem is LyricsListItem.Indicator
                (height + if (nextNoGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
            }.toFloat()
            val lastItem = mergedLyricsList.last()
            val lastHeight = itemHeights[mergedLyricsList.size - 1]?.toFloat() ?: (if (lastItem is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
            with(density) { 100.dp.toPx() } - anchorY - totalBelow - lastHeight
        }

        val maxOffset = remember(itemHeights.toMap(), mergedLyricsList, activeListIndex, maxHeightPx, anchorY) {
            if (mergedLyricsList.isEmpty() || activeListIndex == -1) return@remember 0f
            val totalAbove = (0 until activeListIndex).sumOf { i ->
                val item = mergedLyricsList[i]
                val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                val noGap = (item as? LyricsListItem.Line)?.entry?.isBackground == true || item is LyricsListItem.Indicator
                (height + if (noGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
            }.toFloat()
            maxHeightPx - with(density) { 150.dp.toPx() } - anchorY + totalAbove
        }

        // Clamp to real content bounds only. minOffset/maxOffset already use conservative height
        // fallbacks (constraintLineHeightPx) for items not yet measured; a large buffer here caused
        // effectively unbounded overscroll away from the lyrics.
        val scrollClampMin = minOf(minOffset, maxOffset)
        val scrollClampMax = maxOf(minOffset, maxOffset)

        LaunchedEffect(scrollClampMin, scrollClampMax) {
            if (userManualOffset < scrollClampMin || userManualOffset > scrollClampMax) {
                userManualOffset = userManualOffset.coerceIn(scrollClampMin, scrollClampMax)
            }
        }

        LaunchedEffect(isAutoScrollEnabled, lines) {
            if (isAutoScrollEnabled) {
                val start = userManualOffset
                if (abs(start) < 1f) {
                    userManualOffset = 0f
                    return@LaunchedEffect
                }
                val anim = Animatable(start)
                var lastValue = start
                anim.animateTo(0f, tween((abs(start) / 4f).toInt().coerceIn(200, 600), easing = FastOutSlowInEasing)) {
                    userManualOffset += (value - lastValue)
                    lastValue = value
                }
                userManualOffset = 0f
            }
        }

        LaunchedEffect(showLyrics, lyrics, mergedLyricsList.size) {
            if (showLyrics && mergedLyricsList.isNotEmpty()) {
                isInitialLayout = true
                snapshotFlow { 
                    val h = itemHeights.toMap()
                    val windowStart = (activeListIndex - 8).coerceAtLeast(0)
                    val windowEnd = (activeListIndex + 12).coerceAtMost(mergedLyricsList.size - 1)
                    (windowStart..windowEnd).all { h.containsKey(it) } 
                }.first { it }
                isInitialLayout = false
            }
        }

        // When jumping significantly, we might want to reset isInitialLayout to prevent jumps
        LaunchedEffect(activeListIndex) {
            if (mergedLyricsList.isNotEmpty()) {
                val h = itemHeights.toMap()
                val windowStart = (activeListIndex - 3).coerceAtLeast(0)
                val windowEnd = (activeListIndex + 3).coerceAtMost(mergedLyricsList.size - 1)
                val needsMeasurement = (windowStart..windowEnd).any { !h.containsKey(it) }
                if (needsMeasurement) {
                    isInitialLayout = true
                    snapshotFlow { 
                        val hh = itemHeights.toMap()
                        (windowStart..windowEnd).all { hh.containsKey(it) } 
                    }.first { it }
                    isInitialLayout = false
                }
            }
        }

        val latestResyncLyrics by rememberUpdatedState(
            newValue = {
                flingJob?.cancel()
                var target = scrollTargetIndex
                if (target == -1) {
                    target =
                        findActiveLineIndices(
                            lines,
                            currentPositionState + (currentSong?.song?.lyricsOffset ?: 0),
                        ).maxOrNull() ?: -1
                }
                if (target != -1) {
                    val listIdx =
                        mergedLyricsList.indexOfFirst {
                            it is LyricsListItem.Line && it.index == target
                        }.coerceAtLeast(0)
                    userManualOffset += positions[listIdx] ?: 0f
                    deferredCurrentLineIndex = target
                    scrollTargetIndex = target
                }
                isAutoScrollEnabled = true
            },
        )

        LyricsTranslationHeader(
            status = translationStatus,
            modifier = Modifier.zIndex(1f).padding(top = 56.dp)
        )

        if (lyrics == LYRICS_NOT_FOUND) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.lyrics_not_found), fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.alpha(0.5f))
            }
        } else if (lyrics == null && (translationStatus is LyricsTranslationHelper.TranslationStatus.Idle || translationStatus is LyricsTranslationHelper.TranslationStatus.Error)) {
             Column(modifier = Modifier.padding(top = 100.dp)) {
                 ShimmerHost { repeat(10) { Box(contentAlignment = when (lyricsTextPosition) {
                     LyricsPosition.LEFT -> Alignment.CenterStart; LyricsPosition.CENTER -> Alignment.Center; else -> Alignment.CenterEnd
                 }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) { TextPlaceholder() } } }
             }
        } else {
            if (isSynced) {
                // DOCUMENT_25: 3-line focused lyrics display with blur + 4-way feathering
                val currentActiveIndex = activeLineIndices.firstOrNull() ?: -1
                val priorEntry = if (currentActiveIndex > 0) lines.getOrNull(currentActiveIndex - 1) else null
                val activeEntry = if (currentActiveIndex >= 0 && currentActiveIndex < lines.size) lines[currentActiveIndex] else null
                val upcomingEntry = if (currentActiveIndex + 1 < lines.size) lines.getOrNull(currentActiveIndex + 1) else null

            // Blurred background layer (backdrop blur)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
                    .clip(RoundedCornerShape(16.dp))
                    .fadingEdge(top = 24.dp, bottom = 24.dp, left = 16.dp, right = 16.dp)
                    .graphicsLayer {
                        renderEffect = BlurEffect(24f, 24f, TileMode.Clamp)
                    }
                    .background(Color(0xFFF6F9FF).copy(alpha = 0.22f)),
            )

            // Foreground lyrics text (not blurred)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .zIndex(6f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                    // Prior Line (Past Lyric)
                    AnimatedVisibility(visible = priorEntry?.text?.isNotBlank() == true) {
                        Text(
                            text = priorEntry?.text ?: "",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B).copy(alpha = 0.38f),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            minLines = 1,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 24.dp),
                        )
                    }

                    // Active Line (Current Singing Lyric)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val pulseAlpha by animateFloatAsState(
                            targetValue = if (activeEntry != null) 1f else 0f,
                            animationSpec = tween(durationMillis = 1500),
                        )
                        val pulseScale by animateFloatAsState(
                            targetValue = if (activeEntry != null) 1.5f else 1f,
                            animationSpec = tween(durationMillis = 3000),
                        )

                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                                .graphicsLayer {
                                    alpha = pulseAlpha
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                },
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                         Text(
                             text = activeEntry?.text ?: "",
                             fontSize = 26.sp,
                             fontWeight = FontWeight.ExtraBold,
                             color = if (activeEntry != null) Color(0xFF2563EB) else expressiveAccent,
                             textAlign = TextAlign.Center,
                             lineHeight = 30.sp,
                             minLines = 1,
                             maxLines = 2,
                             overflow = TextOverflow.Ellipsis,
                             modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }

                    // Upcoming Line (Next Lyric)
                    AnimatedVisibility(visible = upcomingEntry?.text?.isNotBlank() == true) {
                        Text(
                            text = upcomingEntry?.text ?: "",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B).copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            minLines = 1,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }

            }
        }

        // Lyrics provider header
        if (isLyricsProviderShown) {
            Text(
                text = stringResource(R.string.lyrics_from_provider, lyricsEntity.provider),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            )
        }

        LyricsActionOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            isAutoScrollEnabled = isAutoScrollEnabled, isSynced = isSynced,
            isSelectionModeActive = isSelectionModeActive, anySelected = selectedIndices.isNotEmpty(),
            onSyncClick = latestResyncLyrics,
            onCancelSelection = { isSelectionModeActive = false; selectedIndices.clear() },
            onShareSelection = {
                val text = selectedIndices.sorted().mapNotNull { lines.getOrNull(it)?.text }.joinToString("\n")
                if (text.isNotBlank()) {
                    shareDialogData = Triple(text, mediaMetadata?.title ?: "", mediaMetadata?.artists?.joinToString { it.name } ?: "")
                    showShareDialog = true
                }
                isSelectionModeActive = false; selectedIndices.clear()
            }
        )
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = {}) {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Box(Modifier.padding(32.dp)) { Text(stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait)) }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (txt, title, arts) = shareDialogData!!
        LyricsShareDialog(
            txt = txt, title = title, arts = arts, songId = mediaMetadata?.id ?: "",
            onDismiss = { showShareDialog = false },
            onShareAsImage = {
                showShareDialog = false
                showColorPickerDialog = true
            }
        )
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (txt, title, arts) = shareDialogData!!
        LyricsColorPickerDialog(
            txt = txt, title = title, arts = arts, thumbnailUrl = mediaMetadata?.thumbnailUrl,
            lyricsTextPosition = lyricsTextPosition,
            onDismiss = { showColorPickerDialog = false },
            onShare = { bgColor, textColor, secTextColor, style ->
                showColorPickerDialog = false
                showProgressDialog = true
                scope.launch {
                    try {
                        val image = ComposeToImage.createLyricsImage(
                            context, mediaMetadata?.thumbnailUrl, title, arts, txt,
                            (configuration.screenWidthDp * density.density).toInt(),
                            (configuration.screenHeightDp * density.density).toInt(),
                            bgColor.toArgb(),
                            when(style) {
                                LyricsBackgroundStyle.SOLID -> LyricsBackgroundStyle.SOLID
                                LyricsBackgroundStyle.BLUR -> LyricsBackgroundStyle.BLUR
                                LyricsBackgroundStyle.GRADIENT -> LyricsBackgroundStyle.GRADIENT
                            },
                            textColor.toArgb(), secTextColor.toArgb(),
                            when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL
                                LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER
                                else -> Layout.Alignment.ALIGN_OPPOSITE
                            }
                        )
                        val uri = ComposeToImage.saveBitmapAsFile(context, image, "lyrics_${System.currentTimeMillis()}")
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, context.getString(R.string.share_lyrics)))
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.failed_to_create_image, e.message), Toast.LENGTH_SHORT).show()
                    } finally {
                        showProgressDialog = false
                    }
                }
            }
        )
            }

            // Fallback for non-synced lyrics
            if (!isSynced) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(5f)
                        .clip(RoundedCornerShape(16.dp))
                        .fadingEdge(top = 24.dp, bottom = 24.dp, left = 16.dp, right = 16.dp)
                        .graphicsLayer {
                            renderEffect = BlurEffect(24f, 24f, TileMode.Clamp)
                        }
                        .background(Color(0xFFF6F9FF).copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (lines.isNotEmpty()) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            lines.forEach { line ->
                                Text(
                                    text = line.text,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 26.sp,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.lyrics_not_found),
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
