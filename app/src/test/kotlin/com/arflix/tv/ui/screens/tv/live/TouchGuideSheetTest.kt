package com.arflix.tv.ui.screens.tv.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The bottom sheet's geometry is the phone guide's contract: the video must be
 * visible when the sheet is collapsed, the sheet must cover it when expanded,
 * and there must always be room for a readable number of rows. The arithmetic
 * here is the same the composable anchors its drag in.
 */
class TouchGuideSheetTest {

    // Reference phone: 411x834 dp, where 834 is the content after status bar
    // and bottom navigation. The category carousel is 46 dp and the rows 56 dp.
    private val phoneWidthDp = 411
    private val phoneHeightDp = 834
    private val categoriesHeightDp = 46
    private val rowHeightDp = 56

    @Test
    fun collapsedShowsAtLeastEightRowsOnThePhone() {
        val topDp = TouchGuideSheetGeometry.collapsedTopDp(phoneWidthDp)
        val guideHeightDp = TouchGuideSheetGeometry.guideHeightDp(
            phoneHeightDp, topDp, categoriesHeightDp,
        )

        // 231 dp video - 35 overlap = 196 dp for the sheet; 834 - 196 - 22 - 46
        // = 570 dp of guide, and 570 / 56 = 10 full rows.
        val rows = TouchGuideSheetGeometry.visibleRows(guideHeightDp, rowHeightDp)

        assertThat(rows).isAtLeast(8)
    }

    @Test
    fun expandedShowsMoreRowsThanCollapsed() {
        fun rows(topDp: Int) = TouchGuideSheetGeometry.visibleRows(
            TouchGuideSheetGeometry.guideHeightDp(phoneHeightDp, topDp, categoriesHeightDp),
            rowHeightDp,
        )

        assertThat(rows(TouchGuideSheetGeometry.expandedTopDp()))
            .isGreaterThan(rows(TouchGuideSheetGeometry.collapsedTopDp(phoneWidthDp)))
    }

    @Test
    fun collapsedTopSitsAboveTheVideoBottom() {
        // The sheet must overlap the video so its rounded corners cover the
        // video's bottom edge instead of meeting it with a hard dividing line.
        assertThat(TouchGuideSheetGeometry.collapsedTopDp(phoneWidthDp))
            .isLessThan(TouchGuideSheetGeometry.videoHeightDp(phoneWidthDp))
    }

    @Test
    fun expandedTopIsTheScreenTop() {
        assertThat(TouchGuideSheetGeometry.expandedTopDp()).isEqualTo(0)
    }

    @Test
    fun guideHeightIsNeverNegative() {
        // Absurd screens: 0x0 up to 4x4 dp — neither video, overlap nor chrome
        // can be pushed below zero.
        (0..4).forEach { widthDp ->
            (0..4).forEach { contentHeightDp ->
                val topDp = TouchGuideSheetGeometry.collapsedTopDp(widthDp)
                assertThat(
                    TouchGuideSheetGeometry.guideHeightDp(contentHeightDp, topDp, categoriesHeightDp)
                ).isAtLeast(0)
            }
        }
    }

    @Test
    fun visibleRowsDoesNotCountHalvesOrCrashOnZero() {
        // A 0 dp row height (absurd input) must not divide by zero; anything
        // under one full row counts as zero, exactly one as one.
        assertThat(TouchGuideSheetGeometry.visibleRows(570, 0)).isEqualTo(0)
        assertThat(TouchGuideSheetGeometry.visibleRows(0, rowHeightDp)).isEqualTo(0)
        assertThat(TouchGuideSheetGeometry.visibleRows(rowHeightDp - 1, rowHeightDp)).isEqualTo(0)
        assertThat(TouchGuideSheetGeometry.visibleRows(rowHeightDp, rowHeightDp)).isEqualTo(1)
    }

    @Test
    fun videoHeightIsSixteenByNineOfTheWidth() {
        // 411 * 9 / 16 = 231.19 — rounded down to 231 whole dp.
        assertThat(TouchGuideSheetGeometry.videoHeightDp(phoneWidthDp)).isEqualTo(231)
    }
}
