package com.arflix.tv.ui.screens.tv.live

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The geometry behind the phone guide's bottom sheet, kept as plain numbers
 * without Compose or Android types — same pattern as [LiveGuideDensity], so
 * every decision can be verified in a plain JVM test.
 *
 * The video stays fixed at the top with a 16:9 height. The sheet has two
 * anchors: collapsed, where the top sits just below the video minus an
 * overlap so the rounded corners cover the video's bottom edge, and
 * expanded, where the sheet covers the video completely. The guides live in
 * the dp left over after the handle and the categories.
 */
internal object TouchGuideSheetGeometry {

    /** The handle band of the sheet. A centered bar; the rest is air and touch surface. */
    const val HandleHeightDp = 22

    /**
     * How far the sheet slides up over the video when collapsed. The overlap
     * makes the rounded corners sit across the video's bottom edge instead of
     * drawing a hard dividing line in the middle of the picture.
     */
    const val SheetOverlapDp = 35

    /** The video's height at full width — 16:9, rounded down to whole dp. */
    fun videoHeightDp(widthDp: Int): Int = widthDp * 9 / 16

    /** The sheet's top when collapsed: just below the video, minus the overlap. */
    fun collapsedTopDp(widthDp: Int): Int = videoHeightDp(widthDp) - SheetOverlapDp

    /** The sheet's top when expanded: it covers the video completely. */
    fun expandedTopDp(): Int = 0

    /** The visible guide area below handle and categories, for a given sheet top. */
    fun guideHeightDp(contentHeightDp: Int, sheetTopDp: Int, categoriesHeightDp: Int): Int =
        (contentHeightDp - sheetTopDp - HandleHeightDp - categoriesHeightDp).coerceAtLeast(0)

    /** How many whole rows fit — half rows do not count. */
    fun visibleRows(guideHeightDp: Int, rowHeightDp: Int): Int =
        if (rowHeightDp <= 0) 0 else guideHeightDp / rowHeightDp
}

/** The sheet's two resting positions. */
private enum class TouchGuideSheetPosition { Collapsed, Expanded }

/** Velocity threshold between drag and snap — the material 125 dp response limit. */
private const val DragVelocityThresholdDp = 125

/**
 * The phone guide: video fixed at the top, and a bottom sheet with categories
 * and guide that is dragged up over it with [androidx.compose.foundation.gestures.anchoredDraggable].
 *
 * The sheet has two anchors — collapsed (top just below the video minus
 * [TouchGuideSheetGeometry.SheetOverlapDp] of overlap, so the rounded corners
 * cover the video's bottom) and expanded (top at 0, i.e. all the way up). A tap
 * on the handle toggles between them; otherwise the sheet follows the finger
 * and snaps on release. The content from the top is the handle, [categories]
 * and [guide] filling the rest. [player] never moves — the sheet slides over it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TouchGuideSheet(
    player: @Composable () -> Unit,
    categories: @Composable () -> Unit,
    guide: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthDp = maxWidth.value.roundToInt()

        // The anchors are computed in the same integer geometry the tests cover.
        val collapsedTopPx = with(density) {
            TouchGuideSheetGeometry.collapsedTopDp(widthDp).dp.toPx()
        }
        val velocityThresholdPx = with(density) { DragVelocityThresholdDp.dp.toPx() }

        val state = remember {
            AnchoredDraggableState(
                initialValue = TouchGuideSheetPosition.Collapsed,
                positionalThreshold = { distance -> distance * 0.5f },
                velocityThreshold = { velocityThresholdPx },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        // Same instance as long as the width stays put — updateAnchors no-ops
        // via equals instead of tearing an in-flight drag apart.
        val anchors = remember(collapsedTopPx) {
            DraggableAnchors {
                TouchGuideSheetPosition.Collapsed at collapsedTopPx
                TouchGuideSheetPosition.Expanded at TouchGuideSheetGeometry.expandedTopDp().toFloat()
            }
        }
        SideEffect { state.updateAnchors(anchors) }

        // The video. It never moves — the sheet sits on top of it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TouchGuideSheetGeometry.videoHeightDp(widthDp).dp),
        ) {
            player()
        }

        // The bottom sheet. offset{} reads the drag offset during layout, so the
        // drag itself does not recompose the whole screen per frame.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    val top = state.offset
                    IntOffset(
                        x = 0,
                        y = if (top.isNaN()) collapsedTopPx.roundToInt() else top.roundToInt(),
                    )
                }
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(LiveColors.Bg)
                .drawBehind {
                    // 1 dp top edge — where video meets sheet.
                    drawLine(
                        color = LiveColors.Divider,
                        start = Offset(0f, 0.5f),
                        end = Offset(size.width, 0.5f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .anchoredDraggable(state = state, orientation = Orientation.Vertical),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TouchGuideSheetGeometry.HandleHeightDp.dp)
                        .clickable {
                            scope.launch {
                                state.animateTo(
                                    if (state.targetValue == TouchGuideSheetPosition.Collapsed) {
                                        TouchGuideSheetPosition.Expanded
                                    } else {
                                        TouchGuideSheetPosition.Collapsed
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .background(Color(0xFF3A3F47), RoundedCornerShape(2.dp)),
                    )
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    categories()
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    guide()
                }
            }
        }
    }
}
