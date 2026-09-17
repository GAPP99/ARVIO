@file:Suppress("UnsafeOptInUsageError")

package com.arflix.tv.ui.screens.tv.live

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.List
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.arflix.tv.R
import com.arflix.tv.data.model.IptvNowNext
import com.arflix.tv.data.model.IptvProgram
import com.arflix.tv.util.formatGenreName
import com.arflix.tv.util.DeviceType
import com.arflix.tv.util.LocalDeviceType

internal enum class LiveTvMiniPlayerLayout {
    STANDARD,
    PORTRAIT_STACKED,
    LANDSCAPE_COMPACT,
}

internal data class LandscapePhoneMiniPlayerSpec(
    val videoWidthDp: Int,
    val videoHeightDp: Int,
    val outerVerticalPaddingDp: Int,
    val showDescription: Boolean,
    val showNextProgramme: Boolean,
) {
    val totalHeightDp: Int = videoHeightDp + outerVerticalPaddingDp
}

internal fun landscapePhoneMiniPlayerSpec(): LandscapePhoneMiniPlayerSpec =
    LandscapePhoneMiniPlayerSpec(
        videoWidthDp = 180,
        videoHeightDp = 101,
        outerVerticalPaddingDp = 14,
        showDescription = false,
        showNextProgramme = false,
    )

private val VideoPanelVerticalPaddingDp = 16

internal fun liveTvMiniPlayerLayout(
    isTouchDevice: Boolean,
    smallestScreenWidthDp: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
): LiveTvMiniPlayerLayout = when {
    !isTouchDevice || smallestScreenWidthDp >= 600 -> LiveTvMiniPlayerLayout.STANDARD
    screenWidthDp > screenHeightDp -> LiveTvMiniPlayerLayout.LANDSCAPE_COMPACT
    else -> LiveTvMiniPlayerLayout.PORTRAIT_STACKED
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MiniPlayerRow(
    exoPlayer: ExoPlayer,
    channel: EnrichedChannel?,
    clockTickMillis: Long,
    nowNext: IptvNowNext?,
    favoriteSet: Set<String>,
    onFavoriteToggle: (String) -> Unit,
    onFullscreenClick: (() -> Unit)? = null,
    onProgrammeGuideClick: (() -> Unit)? = null,
    variantCount: Int = 1,
    onOpenVariants: (() -> Unit)? = null,
    compact: Boolean = false,
    landscapeCompact: Boolean = false,
    playerActive: Boolean = true,
    focusedProgramme: Pair<EnrichedChannel, IptvProgram>? = null,
    onVideoBoundsPositioned: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
    focusedProgrammeProvider: (() -> Pair<EnrichedChannel, IptvProgram>?)? = null,
    newDesign: Boolean = true,
) {
    val drawerTranslation = LocalLiveDrawerTranslation.current
    // Read rapidly changing programme focus here, not in the surrounding guide.
    val displayedProgramme = focusedProgrammeProvider?.invoke() ?: focusedProgramme
    val drawerDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else -1f
    if (landscapeCompact) {
        val spec = landscapePhoneMiniPlayerSpec()
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            VideoCard(
                exoPlayer = exoPlayer,
                channel = channel,
                landscapeCompact = true,
                playerActive = playerActive,
                onFullscreenClick = onFullscreenClick,
                onVideoBoundsPositioned = onVideoBoundsPositioned,
            )
            InfoColumn(
                focusedProgramme = displayedProgramme,
                channel = channel,
                clockTickMillis = clockTickMillis,
                nowNext = nowNext,
                isFavorite = channel?.id?.let { it in favoriteSet } == true,
                onFavoriteToggle = onFavoriteToggle,
                variantCount = variantCount,
                onOpenVariants = onOpenVariants,
                landscapeCompact = true,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = spec.videoHeightDp.dp),
            )
        }
    } else if (compact) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VideoCard(
                exoPlayer = exoPlayer,
                channel = channel,
                compact = true,
                playerActive = playerActive,
                onFullscreenClick = onFullscreenClick,
                onVideoBoundsPositioned = onVideoBoundsPositioned,
                modifier = Modifier.fillMaxWidth(),
            )
            InfoColumn(
                focusedProgramme = displayedProgramme,
                channel = channel,
                clockTickMillis = clockTickMillis,
                nowNext = nowNext,
                isFavorite = channel?.id?.let { it in favoriteSet } == true,
                onFavoriteToggle = onFavoriteToggle,
                variantCount = variantCount,
                onOpenVariants = onOpenVariants,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        if (newDesign) {
            val videoHeightDp = LiveGuideDensity.infoPanelHeightDp(topBarVisible = true) - 2 * VideoPanelVerticalPaddingDp
            val videoWidthDp = videoHeightDp * 16 / 9
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 28.dp,
                        top = VideoPanelVerticalPaddingDp.dp,
                        bottom = VideoPanelVerticalPaddingDp.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top,
            ) {
                VideoCard(
                    exoPlayer = exoPlayer,
                    channel = channel,
                    playerActive = playerActive,
                    onFullscreenClick = onFullscreenClick,
                    onVideoBoundsPositioned = onVideoBoundsPositioned,
                    videoSize = DpSize(videoWidthDp.dp, videoHeightDp.dp),
                    cornerRadius = 6.dp,
                    modifier = Modifier.graphicsLayer { translationX = drawerDirection * drawerTranslation() },
                )
                GuideProgrammeSummary(
                    channel = displayedProgramme?.first ?: channel,
                    programme = displayedProgramme?.second ?: nowNext?.now,
                    clockTickMillis = clockTickMillis,
                    isFavorite = (displayedProgramme?.first ?: channel)?.id?.let { it in favoriteSet } == true,
                    modifier = Modifier.weight(1f).height(videoHeightDp.dp),
                )
            }
        } else {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 14.dp, top = 1.dp, bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                LegacyGuideProgrammeSummary(displayedProgramme?.first ?: channel,
                    displayedProgramme?.second ?: nowNext?.now,
                    Modifier.weight(1f).height(LiveDims.MiniPlayerHeight))
                VideoCard(
                    exoPlayer = exoPlayer,
                    channel = channel,
                    playerActive = playerActive,
                    onFullscreenClick = onFullscreenClick,
                    onVideoBoundsPositioned = onVideoBoundsPositioned,
                    modifier = Modifier.graphicsLayer { translationX = drawerDirection * drawerTranslation() },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LegacyGuideProgrammeSummary(
    channel: EnrichedChannel?, programme: IptvProgram?, modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.height(30.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (channel != null) ChannelLogo(
                channel = channel,
                size = 60.dp,
                modifier = Modifier.size(60.dp, 24.dp),
                contentPadding = 0.dp,
                showPlaceholder = false,
            )
            Text(listOfNotNull(channel?.name, channel?.quality?.takeIf { it != Quality.UNKNOWN }?.label).joinToString(" · "),
                color = LiveColors.FgDim, fontSize = 11.sp, lineHeight = 13.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Text(programme?.title ?: channel?.name ?: stringResource(R.string.live_empty_no_programme),
            color = LiveColors.Fg, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(programme?.let(::formatTimeWindow),
            channel?.genre?.name?.let(::formatGenreName), remainingLabel(programme).takeIf(String::isNotBlank)).joinToString("  ·  "),
            color = LiveColors.FgDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(programme?.description.orEmpty(), color = LiveColors.Fg, fontSize = 12.sp, lineHeight = 16.sp,
            maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideProgrammeSummary(
    channel: EnrichedChannel?,
    programme: IptvProgram?,
    clockTickMillis: Long,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
) {
    val accentColor = liveAccent()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = programme?.title ?: channel?.name ?: stringResource(R.string.live_empty_no_programme),
                color = LiveColors.Fg,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                val groupName = channel?.source?.group?.trim()?.takeIf { it.isNotEmpty() }
                if (groupName != null) {
                    Text(
                        text = buildAnnotatedString {
                            append(groupName)
                            channel.quality.takeIf { it != Quality.UNKNOWN }?.let { quality ->
                                withStyle(
                                    SpanStyle(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.W600,
                                        baselineShift = BaselineShift.Superscript,
                                    ),
                                ) { append(" ${quality.label}") }
                            }
                        },
                        color = LiveColors.FgMute,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = LiveColors.FgMute,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        if (programme == null) {
            // Uden programdata stod hele feltet under titlen tomt. Kanalens egne
            // oplysninger er stadig bedre end en sort boks, og siger samtidig
            // hvorfor der ikke er mere at vise.
            Text(
                text = listOfNotNull(
                    channel?.genre?.name?.let(::formatGenreName),
                    channel?.quality?.takeIf { it != Quality.UNKNOWN }?.label,
                    stringResource(R.string.live_empty_no_programme),
                ).joinToString("  ·  "),
                color = LiveColors.FgMute,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (programme != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = formatTimeWindow(programme),
                    color = LiveColors.FgDim,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.14f)),
                ) {
                    progressOf(programme.takeIf { clockTickMillis >= 0L })?.let { progress ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(accentColor),
                        )
                    }
                }
                val remaining = remainingLabel(programme.takeIf { clockTickMillis >= 0L })
                if (remaining.isNotBlank()) {
                    Text(
                        text = remaining,
                        color = LiveColors.FgDim,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Genre/kategori og varighed hører til her i headeren — bevidst
                // ikke i programcellerne, hvor de før gentog det samme tre gange
                // pr. skærmbillede.
                val details = listOfNotNull(
                    programme.category?.trim()?.takeIf { it.isNotEmpty() },
                    channel?.genre?.name?.let(::formatGenreName)
                        ?.takeIf { programme.category.isNullOrBlank() },
                    ((programme.endUtcMillis - programme.startUtcMillis) / 60_000L)
                        .takeIf { it > 0 }
                        ?.let { stringResource(R.string.live_label_duration_min, it) },
                )
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString("  ·  "),
                        color = LiveColors.FgMute,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            text = programme?.description.orEmpty(),
            color = LiveColors.FgDim,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoCard(
    exoPlayer: ExoPlayer,
    channel: EnrichedChannel?,
    compact: Boolean = false,
    landscapeCompact: Boolean = false,
    playerActive: Boolean = true,
    onFullscreenClick: (() -> Unit)? = null,
    onVideoBoundsPositioned: ((Rect) -> Unit)? = null,
    videoSize: DpSize? = null,
    cornerRadius: Dp = LiveDims.VideoRadius,
    modifier: Modifier = Modifier,
) {
    val deviceType = LocalDeviceType.current
    val isTouchDevice = deviceType.isTouchDevice()
    val landscapeSpec = if (landscapeCompact) landscapePhoneMiniPlayerSpec() else null

    val playerAlpha by animateFloatAsState(
        targetValue = if (playerActive) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "mini-player-fade",
    )

    Box(
        modifier = modifier
            .then(
                when {
                    compact -> Modifier.aspectRatio(16f / 9f)
                    landscapeSpec != null -> Modifier.size(
                        landscapeSpec.videoWidthDp.dp,
                        landscapeSpec.videoHeightDp.dp,
                    )
                    videoSize != null -> Modifier.size(videoSize)
                    else -> Modifier.size(LiveDims.MiniPlayerWidth, LiveDims.MiniPlayerHeight)
                }
            )
            .then(
                if (onVideoBoundsPositioned != null) {
                    Modifier.onGloballyPositioned { coords ->
                        onVideoBoundsPositioned(coords.boundsInRoot())
                    }
                } else {
                    Modifier
                }
            )
            .clickable(enabled = isTouchDevice && onFullscreenClick != null) {
                onFullscreenClick?.invoke()
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(LiveColors.PanelDeep),
    ) {
        // Fallback brand gradient while video is loading or channel is null.
        if (channel == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(LiveColors.Panel, LiveColors.Bg),
                        )
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(LiveColors.Panel, LiveColors.Bg),
                        )
                    ),
            )
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        if (playerActive) {
                            this.player = exoPlayer
                        }
                        useController = false
                        setKeepContentOnPlayerReset(true)
                    }
                },
                update = { view ->
                    if (playerActive) {
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }
                    } else {
                        if (view.player != null) {
                            view.player = null
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = playerAlpha
                    },
            )
            LiveBug(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .graphicsLayer {
                        alpha = playerAlpha
                    },
            )
        }

        if (isTouchDevice && onFullscreenClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onFullscreenClick.invoke() },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable { onFullscreenClick.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.FitScreen,
                    contentDescription = stringResource(R.string.live_cd_fullscreen),
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveBug(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live-bug")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "live-alpha",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xAA000000))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(LiveColors.LiveRed, CircleShape),
        )
        Text(
            text = stringResource(R.string.live_badge_live),
            style = LiveType.Badge.copy(color = Color.White),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InfoColumn(
    focusedProgramme: Pair<EnrichedChannel, IptvProgram>? = null,
    channel: EnrichedChannel?,
    clockTickMillis: Long,
    nowNext: IptvNowNext?,
    isFavorite: Boolean,
    onFavoriteToggle: (String) -> Unit,
    variantCount: Int,
    onOpenVariants: (() -> Unit)?,
    landscapeCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val landscapeSpec = if (landscapeCompact) landscapePhoneMiniPlayerSpec() else null
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (landscapeCompact) 5.dp else 8.dp),
    ) {
        if (focusedProgramme != null) {
            val (focusedChannel, programme) = focusedProgramme
            Text(focusedChannel.source.name, style = LiveType.SectionTag.copy(color = LiveColors.FgDim))
            Text("${formatClock(programme.startUtcMillis)} - ${formatClock(programme.endUtcMillis)}", style = LiveType.TimeMono.copy(color = LiveColors.FgDim))
            Text(programme.title, style = LiveType.CellTitle.copy(color = LiveColors.Fg), maxLines = 2, overflow = TextOverflow.Ellipsis)
            programme.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = LiveType.BodySynopsis.copy(color = LiveColors.FgDim), maxLines = if (landscapeCompact) 1 else 3, overflow = TextOverflow.Ellipsis)
            }
            return@Column
        }
        ChannelIdentityRow(channel = channel, variantCount = variantCount, onOpenVariants = onOpenVariants)
        NowCard(
            channel = channel,
            clockTickMillis = clockTickMillis,
            nowNext = nowNext,
            landscapeCompact = landscapeCompact,
        )
        if (landscapeSpec?.showNextProgramme != false) {
            NextRow(nowNext = nowNext)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelIdentityRow(
    channel: EnrichedChannel?,
    variantCount: Int,
    onOpenVariants: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (channel != null) {
            ChannelLogo(channel = channel, size = 30.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                if (channel.genre.name.isNotBlank()) {
                    Text(
                        text = formatGenreName(channel.genre.name),
                        style = LiveType.SectionTag.copy(color = LiveColors.FgMute),
                    )
                }
                Text(
                    text = channel.name,
                    style = LiveType.ChannelName.copy(color = LiveColors.Fg),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (channel.quality == Quality.K4) {
                        QualityBadge(channel.quality)
                    }
                    if (variantCount > 1) {
                        SourceBadge(variantCount, onOpenVariants)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LiveColors.Panel),
            )
            Text(
                text = "—",
                style = LiveType.ChannelName.copy(color = LiveColors.FgMute),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceBadge(count: Int, onOpenVariants: (() -> Unit)?) {
    val accentColor = liveAccent()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(LiveColors.Panel)
            .then(if (onOpenVariants != null) Modifier.clickable { onOpenVariants() } else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(stringResource(R.string.live_label_sources, count), style = LiveType.Badge.copy(color = accentColor))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QualityBadge(q: Quality) {
    if (q == Quality.UNKNOWN) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(LiveColors.Panel)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(q.label, style = LiveType.Badge.copy(color = LiveColors.Fg))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LangBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(LiveColors.Panel)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text.uppercase(), style = LiveType.Badge.copy(color = LiveColors.FgDim))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NowCard(
    channel: EnrichedChannel?,
    clockTickMillis: Long,
    nowNext: IptvNowNext?,
    landscapeCompact: Boolean = false,
) {
    val accentColor = liveAccent()
    val now = nowNext?.now
    val landscapeSpec = if (landscapeCompact) landscapePhoneMiniPlayerSpec() else null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 0.dp,
                vertical = if (landscapeCompact) 5.dp else 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(if (landscapeCompact) 2.dp else 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.live_badge_now), style = LiveType.SectionTag.copy(color = accentColor))
            Text(
                text = formatTimeWindow(now),
                style = LiveType.TimeMono.copy(color = LiveColors.Fg),
            )
            Spacer(Modifier.weight(1f))
            val remaining = remainingLabel(now)
            if (remaining.isNotBlank()) {
                Text(
                    text = remaining,
                    style = LiveType.TimeMono.copy(color = accentColor),
                )
            }
        }
        Text(
            text = now?.title ?: channel?.name ?: stringResource(R.string.live_empty_no_programme),
            style = LiveType.ProgramTitle.copy(color = LiveColors.Fg),
            maxLines = if (landscapeCompact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        val description = now?.description
        if (landscapeSpec?.showDescription != false && !description.isNullOrBlank()) {
            Text(
                text = description,
                style = LiveType.BodySynopsis.copy(color = LiveColors.FgDim),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val progress = progressOf(now?.takeIf { clockTickMillis >= 0L })
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = LiveColors.Panel,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NextRow(nowNext: IptvNowNext?) {
    val next = nowNext?.next ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.live_badge_next), style = LiveType.SectionTag.copy(color = LiveColors.FgMute))
        Text(
            text = formatClock(next.startUtcMillis),
            style = LiveType.TimeMono.copy(color = LiveColors.FgDim),
        )
        Text(
            text = next.title,
            style = LiveType.CellTitle.copy(color = LiveColors.FgDim),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

internal fun formatTimeWindow(p: IptvProgram?): String {
    if (p == null) return "—"
    return "${formatClock(p.startUtcMillis)} – ${formatClock(p.endUtcMillis)}"
}

internal fun formatClock(utcMillis: Long): String {
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = utcMillis
    return "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
}

internal fun remainingLabel(p: IptvProgram?): String {
    if (p == null) return ""
    val now = System.currentTimeMillis()
    if (now !in p.startUtcMillis..p.endUtcMillis) return ""
    val minsLeft = ((p.endUtcMillis - now) / 60_000L).coerceAtLeast(0L)
    return if (minsLeft >= 60) "${minsLeft / 60}h ${minsLeft % 60}m left" else "${minsLeft}m left"
}

internal fun progressOf(p: IptvProgram?): Float? {
    if (p == null) return null
    val span = (p.endUtcMillis - p.startUtcMillis).toFloat()
    if (span <= 0f) return null
    val done = (System.currentTimeMillis() - p.startUtcMillis).toFloat()
    return (done / span).coerceIn(0f, 1f)
}
