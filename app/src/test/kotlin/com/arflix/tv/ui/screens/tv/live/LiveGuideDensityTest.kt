package com.arflix.tv.ui.screens.tv.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The spec is the guide's contract. These tests pin down the decisions that
 * would otherwise be repeated — and drift apart — across five composables.
 */
class LiveGuideDensityTest {

    @Test
    fun rowCountIsTheSameWithAndWithoutTopBar() {
        // The point of the whole rearrangement: moving right must not shift
        // the rows.
        val withTopBar = LiveGuideDensity.visibleRowCount(540, topBarVisible = true)
        val withoutTopBar = LiveGuideDensity.visibleRowCount(540, topBarVisible = false)

        assertThat(withTopBar).isEqualTo(withoutTopBar)
        assertThat(withTopBar).isEqualTo(8)
    }

    @Test
    fun infoPanelAbsorbsTheTopBarSpace() {
        val full = LiveGuideDensity.infoPanelHeightDp(topBarVisible = false)
        val withTopBar = LiveGuideDensity.infoPanelHeightDp(topBarVisible = true)

        assertThat(full - withTopBar).isEqualTo(LiveGuideDensity.TopBarDp)
    }

    @Test
    fun geometryAddsUpOnA1080pScreen() {
        val rows = LiveGuideDensity.visibleRowCount(540, topBarVisible = false)
        val used = LiveGuideDensity.infoPanelHeightDp(false) +
            LiveGuideDensity.RulerHeightDp +
            rows * LiveGuideDensity.RowHeightDp

        assertThat(used).isAtMost(540)
        // No meaningful waste at the bottom.
        assertThat(540 - used).isLessThan(LiveGuideDensity.RowHeightDp)
    }

    @Test
    fun rowHeightDividesUpTheChromeSpaceBeforeTheRowCount() {
        assertThat(LiveGuideDensity.rowHeightDp(8)).isEqualTo(38)
        assertThat(LiveGuideDensity.rowHeightDp(6)).isEqualTo(46)
        assertThat(LiveGuideDensity.rowHeightDp(10)).isEqualTo(30)
    }

    @Test
    fun rowHeightIsClampedToTheInterval() {
        assertThat(LiveGuideDensity.rowHeightDp(4)).isEqualTo(46)
        assertThat(LiveGuideDensity.rowHeightDp(50)).isEqualTo(28)
    }

    @Test
    fun allRowCountsBetweenSixAndTenGiveAReadableHeight() {
        (6..10).forEach { rowCount ->
            assertThat(LiveGuideDensity.rowHeightDp(rowCount)).isAtLeast(28)
            assertThat(LiveGuideDensity.rowHeightDp(rowCount)).isLessThan(47)
        }
    }

    @Test
    fun visibleRowCountFollowsTheChosenRowHeight() {
        assertThat(LiveGuideDensity.visibleRowCount(540, topBarVisible = false, rowCount = 8)).isEqualTo(8)
        assertThat(LiveGuideDensity.visibleRowCount(540, topBarVisible = false, rowCount = 6)).isEqualTo(6)
        assertThat(LiveGuideDensity.visibleRowCount(540, topBarVisible = false, rowCount = 10)).isEqualTo(10)
    }

    @Test
    fun channelRowShowsNoPillsOrProgressBar() {
        val spec = LiveGuideDensity.channelRowSpec()

        assertThat(spec.showQualityPill).isFalse()
        assertThat(spec.showLanguagePill).isFalse()
        assertThat(spec.showProgressBar).isFalse()
        assertThat(spec.showCatchupIcon).isFalse()
        // The quality does not disappear — it moves into the name.
        assertThat(spec.showQualitySuffix).isTrue()
    }

    @Test
    fun compactLayoutTurnsOnPillsProgressAndCatchup() {
        val spec = LiveGuideDensity.channelRowSpec(rowHeightDp = 52, compact = true)

        assertThat(spec.showQualityPill).isTrue()
        assertThat(spec.showLanguagePill).isTrue()
        assertThat(spec.showProgressBar).isTrue()
        assertThat(spec.showCatchupIcon).isTrue()
        assertThat(spec.showQualitySuffix).isFalse()
    }

    @Test
    fun channelRowKeepsNumberStarAndPlayingMarker() {
        val spec = LiveGuideDensity.channelRowSpec()

        assertThat(spec.showNumber).isTrue()
        assertThat(spec.showFavoriteStar).isTrue()
        assertThat(spec.showPlayingMarker).isTrue()
    }

    @Test
    fun compactLayoutHidesTheChannelNumber() {
        assertThat(LiveGuideDensity.channelRowSpec(compact = true).showNumber).isFalse()
    }

    @Test
    fun programCellShowsOnlyTheTitle() {
        val spec = LiveGuideDensity.programCellSpec(cellWidthDp = 240)

        assertThat(spec.showTitle).isTrue()
        assertThat(spec.showTimeFooter).isFalse()
        assertThat(spec.showDescription).isFalse()
        assertThat(spec.centerTitleVertically).isTrue()
    }

    @Test
    fun narrowCellsDropBadges() {
        assertThat(LiveGuideDensity.programCellSpec(cellWidthDp = 60).showBadges).isFalse()
        assertThat(LiveGuideDensity.programCellSpec(cellWidthDp = 240).showBadges).isTrue()
    }

    @Test
    fun rowHeightStaysUnderTheCanvasLimit() {
        // Above 60 dp ProgramCell falls out of the drawn path and lays out an
        // entire composite tree per cell. That would cost the scroll
        // performance worked for in docs/iptv-scroll-performance-2026-09-08.md.
        assertThat(LiveGuideDensity.RowHeightDp).isLessThan(60)
    }
}
