package com.arflix.tv.ui.screens.tv.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The tablet guide's two-column workspace.
 *
 * Same construction as the TV version: the groups sit fixed in the left
 * column, player info and the grid are on the right. The difference is that
 * there is no animation or collapsing — the columns stay put, so the layout
 * settles instead of moving when focus changes. That is the entire point of
 * this variant.
 *
 * The geometry lives in [TabletGuideGeometry], which is plain numbers without
 * Compose or Android types, so decisions can be verified in a plain JVM test.
 */
@Composable
fun TabletGuideWorkspace(
    groups: @Composable () -> Unit,
    info: @Composable () -> Unit,
    guide: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    groupColumnWidthDp: Int = TabletGuideGeometry.GroupColumnDp,
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Left column: the groups, with the divider on the right edge.
        Box(
            modifier = Modifier
                .width(groupColumnWidthDp.dp)
                .fillMaxHeight()
                .background(LiveColors.Bg),
        ) {
            groups()
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(LiveColors.Divider),
        )
        // Right column: player info at the top, the guide fills the rest.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TabletGuideGeometry.InfoPanelDp.dp),
            ) {
                info()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                guide()
            }
        }
    }
}

/**
 * The tablet guide's measurements in dp, kept as plain numbers and functions
 * without Compose or Android dependencies — same pattern as [LiveGuideDensity],
 * so every decision can be verified in a plain JVM test instead of only on an
 * emulator. The reference tablet is 1280x800 dp, where the content height is
 * 720 dp (800 minus 24 dp status bar and 56 dp bottom navigation).
 */
internal object TabletGuideGeometry {

    /** Left column with the groups. Narrower than the TV's sidebar — the tablet needs the width for the grid. */
    const val GroupColumnDp = 220

    /** The info panel above the guide: the player and the program selected right now. */
    const val InfoPanelDp = 150

    /** The channel column in the grid, with number, logo and name. */
    const val ChannelColumnDp = 200

    /** Minimum touch target. A tablet is a touch screen — not a remote control. */
    const val MinTouchTargetDp = 48

    /** Height available to the grid: content minus the timeline ruler. Never negative. */
    fun guideHeightDp(contentHeightDp: Int, rulerHeightDp: Int): Int =
        (contentHeightDp - rulerHeightDp).coerceAtLeast(0)

    /** Number of whole channel rows that fit below the ruler. */
    fun visibleRows(contentHeightDp: Int, rulerHeightDp: Int, rowHeightDp: Int): Int =
        guideHeightDp(contentHeightDp, rulerHeightDp) / rowHeightDp.coerceAtLeast(1)

    /** Width available to the grid's timeline: screen minus group column and channel column. */
    fun timelineWidthDp(screenWidthDp: Int, groupColumnDp: Int = GroupColumnDp): Int =
        (screenWidthDp - groupColumnDp - ChannelColumnDp).coerceAtLeast(0)

    /** How many minutes the timeline holds at [pxPerMinute] dp per minute. */
    fun timelineMinutes(timelineWidthDp: Int, pxPerMinute: Int = 4): Int =
        timelineWidthDp / pxPerMinute.coerceAtLeast(1)
}
