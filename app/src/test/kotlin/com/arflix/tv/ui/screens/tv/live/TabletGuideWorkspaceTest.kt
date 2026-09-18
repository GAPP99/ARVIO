package com.arflix.tv.ui.screens.tv.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The tablet geometry is the variant's contract. The reference tablet is
 * 1280x800 dp with 720 dp of content (800 minus a 24 dp status bar and 56 dp
 * bottom navigation), a 36 dp ruler and 52 dp rows.
 */
class TabletGuideWorkspaceTest {

    private val contentDp = 720
    private val rulerDp = 36
    private val rowDp = 52
    private val screenWidthDp = 1280

    @Test
    fun theReferenceTabletShowsAtLeastTenRows() {
        // The point of the tablet variant: the grid must feel like a grid, not
        // like five rows on an outrageously large canvas.
        val rows = TabletGuideGeometry.visibleRows(contentDp, rulerDp, rowDp)

        assertThat(rows).isAtLeast(10)
    }

    @Test
    fun theTimelineShowsAtLeastThreeHoursOnTheReferenceTablet() {
        // At 4 dp per minute there must be room for a whole evening without
        // the user having to scroll to see what comes next.
        val minutes = TabletGuideGeometry.timelineMinutes(
            TabletGuideGeometry.timelineWidthDp(screenWidthDp),
        )

        assertThat(minutes).isAtLeast(180)
    }

    @Test
    fun theRowHeightIsAValidTouchTarget() {
        // A tablet is a touchscreen — the row must be hittable with a finger.
        assertThat(rowDp).isAtLeast(TabletGuideGeometry.MinTouchTargetDp)
    }

    @Test
    fun guideHeightIsNeverNegative() {
        // Absurdly low heights give an empty grid, not a crash.
        assertThat(TabletGuideGeometry.guideHeightDp(0, rulerDp)).isEqualTo(0)
        assertThat(TabletGuideGeometry.guideHeightDp(10, rulerDp)).isEqualTo(0)
        assertThat(TabletGuideGeometry.guideHeightDp(-500, rulerDp)).isEqualTo(0)

        // And therefore never a negative row count either.
        assertThat(TabletGuideGeometry.visibleRows(0, rulerDp, rowDp)).isEqualTo(0)
    }

    @Test
    fun aWiderScreenGivesALongerTimeline() {
        val narrow = TabletGuideGeometry.timelineWidthDp(800)
        val wide = TabletGuideGeometry.timelineWidthDp(screenWidthDp)

        assertThat(wide).isGreaterThan(narrow)
        assertThat(TabletGuideGeometry.timelineMinutes(wide))
            .isGreaterThan(TabletGuideGeometry.timelineMinutes(narrow))
    }
}
