package com.arflix.tv.ui.screens.tv.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Specen er guidens kontrakt. Testene her fastholder de beslutninger der
 * ellers ville blive gentaget — og glide fra hinanden — i fem composables.
 */
class LiveGuideDensityTest {

    @Test
    fun raekkeantalletErDetSammeMedOgUdenTopbar() {
        // Pointen med hele omlægningen: at gå til højre må ikke flytte rækkerne.
        val medTopbar = LiveGuideDensity.visibleRowCount(540, topBarVisible = true)
        val udenTopbar = LiveGuideDensity.visibleRowCount(540, topBarVisible = false)

        assertThat(medTopbar).isEqualTo(udenTopbar)
        assertThat(medTopbar).isEqualTo(8)
    }

    @Test
    fun infoPanelAbsorbererTopbarensPlads() {
        val fuldt = LiveGuideDensity.infoPanelHeightDp(topBarVisible = false)
        val medTopbar = LiveGuideDensity.infoPanelHeightDp(topBarVisible = true)

        assertThat(fuldt - medTopbar).isEqualTo(LiveGuideDensity.TopBarDp)
    }

    @Test
    fun geometrienGaarOpPaaEtTusindeOgFirsPHundredeSkaerm() {
        val rows = LiveGuideDensity.visibleRowCount(540, topBarVisible = false)
        val brugt = LiveGuideDensity.infoPanelHeightDp(false) +
            LiveGuideDensity.RulerHeightDp +
            rows * LiveGuideDensity.RowHeightDp

        assertThat(brugt).isAtMost(540)
        // Intet nævneværdigt spild i bunden.
        assertThat(540 - brugt).isLessThan(LiveGuideDensity.RowHeightDp)
    }

    @Test
    fun raekkehojdenDelerChromepladsenUdFoerRaekeantallet() {
        assertThat(LiveGuideDensity.rowHeightDp(8)).isEqualTo(38)
        assertThat(LiveGuideDensity.rowHeightDp(6)).isEqualTo(46)
        assertThat(LiveGuideDensity.rowHeightDp(10)).isEqualTo(30)
    }

    @Test
    fun raekkehojdenKlemmesTilIntervallet() {
        assertThat(LiveGuideDensity.rowHeightDp(4)).isEqualTo(46)
        assertThat(LiveGuideDensity.rowHeightDp(50)).isEqualTo(28)
    }

    @Test
    fun alleRaekeantalMellemSeksOgTiGiverEnLaesbarHoejde() {
        (6..10).forEach { rowCount ->
            assertThat(LiveGuideDensity.rowHeightDp(rowCount)).isAtLeast(28)
            assertThat(LiveGuideDensity.rowHeightDp(rowCount)).isLessThan(47)
        }
    }

    @Test
    fun synligtRaekeantalFoelgerDenValgteRaekehojde() {
        assertThat(LiveGuideDensity.visibleRowCount(540, topBarVisible = false, rowCount = 8)).isEqualTo(8)
        assertThat(LiveGuideDensity.visibleRowCount(540, topBarVisible = false, rowCount = 6)).isEqualTo(6)
        assertThat(LiveGuideDensity.visibleRowCount(540, topBarVisible = false, rowCount = 10)).isEqualTo(10)
    }

    @Test
    fun kanalraekkenViserIkkePillsOgProgressStreg() {
        val spec = LiveGuideDensity.channelRowSpec()

        assertThat(spec.showQualityPill).isFalse()
        assertThat(spec.showLanguagePill).isFalse()
        assertThat(spec.showProgressBar).isFalse()
        assertThat(spec.showCatchupIcon).isFalse()
        // Kvaliteten forsvinder ikke — den flytter ind i navnet.
        assertThat(spec.showQualitySuffix).isTrue()
    }

    @Test
    fun kompaktLayoutTaenderPillsProgressOgCatchup() {
        val spec = LiveGuideDensity.channelRowSpec(rowHeightDp = 52, compact = true)

        assertThat(spec.showQualityPill).isTrue()
        assertThat(spec.showLanguagePill).isTrue()
        assertThat(spec.showProgressBar).isTrue()
        assertThat(spec.showCatchupIcon).isTrue()
        assertThat(spec.showQualitySuffix).isFalse()
    }

    @Test
    fun kanalraekkenBeholderNummerStjerneOgAfspilningsmarkoer() {
        val spec = LiveGuideDensity.channelRowSpec()

        assertThat(spec.showNumber).isTrue()
        assertThat(spec.showFavoriteStar).isTrue()
        assertThat(spec.showPlayingMarker).isTrue()
    }

    @Test
    fun kompaktLayoutSkjulerKanalnummeret() {
        assertThat(LiveGuideDensity.channelRowSpec(compact = true).showNumber).isFalse()
    }

    @Test
    fun programcellenViserKunTitlen() {
        val spec = LiveGuideDensity.programCellSpec(cellWidthDp = 240)

        assertThat(spec.showTitle).isTrue()
        assertThat(spec.showTimeFooter).isFalse()
        assertThat(spec.showDescription).isFalse()
        assertThat(spec.centerTitleVertically).isTrue()
    }

    @Test
    fun smalleCellerDropperBadges() {
        assertThat(LiveGuideDensity.programCellSpec(cellWidthDp = 60).showBadges).isFalse()
        assertThat(LiveGuideDensity.programCellSpec(cellWidthDp = 240).showBadges).isTrue()
    }

    @Test
    fun raekkehoejdenHolderSigUnderCanvasGraensen() {
        // Over 60 dp falder ProgramCell ud af den tegnede sti og layouter et
        // helt komposit-træ pr. celle. Det ville koste den scroll-ydelse der
        // blev arbejdet frem i docs/iptv-scroll-performance-2026-09-08.md.
        assertThat(LiveGuideDensity.RowHeightDp).isLessThan(60)
    }
}
