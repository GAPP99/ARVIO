package com.arflix.tv.ui.screens.tv.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Bundarkets geometri er telefon-guidens kontrakt: videoen skal være synlig
 * når arket er kollapset, arket skal dække den når det er udfoldet, og der
 * skal altid være plads til et læsbart antal rækker. Regnestykkerne her er de
 * samme som composablen forankrer sit træk i.
 */
class TouchGuideSheetTest {

    // Reference-telefonen: 411x834 dp, hvor 834 er indholdet efter statuslinje
    // og bundnavigation. Kategorikarrusellen er 46 dp og rækkerne 56 dp.
    private val telefonBreddeDp = 411
    private val telefonHoejdeDp = 834
    private val kategorierHoejdeDp = 46
    private val raekkeHoejdeDp = 56

    @Test
    fun kollapsetViserMindstOtteRaekkerPaaTelefonen() {
        val topDp = TouchGuideSheetGeometry.collapsedTopDp(telefonBreddeDp)
        val guideHoejdeDp = TouchGuideSheetGeometry.guideHeightDp(
            telefonHoejdeDp, topDp, kategorierHoejdeDp,
        )

        // 231 dp video - 35 overlap = 196 dp til arket; 834 - 196 - 22 - 46
        // = 570 dp guide, og 570 / 56 = 10 hele rækker.
        val raekker = TouchGuideSheetGeometry.visibleRows(guideHoejdeDp, raekkeHoejdeDp)

        assertThat(raekker).isAtLeast(8)
    }

    @Test
    fun udfoldetViserFlereRaekkerEndKollapset() {
        fun raekker(topDp: Int) = TouchGuideSheetGeometry.visibleRows(
            TouchGuideSheetGeometry.guideHeightDp(telefonHoejdeDp, topDp, kategorierHoejdeDp),
            raekkeHoejdeDp,
        )

        assertThat(raekker(TouchGuideSheetGeometry.expandedTopDp()))
            .isGreaterThan(raekker(TouchGuideSheetGeometry.collapsedTopDp(telefonBreddeDp)))
    }

    @Test
    fun kollapsetToppenLiggerOverVideoensBund() {
        // Arket skal overlappe videoen, så de afrundede hjørner dækker dens
        // bundkant i stedet for at møde den med en hård skillelinje.
        assertThat(TouchGuideSheetGeometry.collapsedTopDp(telefonBreddeDp))
            .isLessThan(TouchGuideSheetGeometry.videoHeightDp(telefonBreddeDp))
    }

    @Test
    fun udfoldetToppenErSkærmensTop() {
        assertThat(TouchGuideSheetGeometry.expandedTopDp()).isEqualTo(0)
    }

    @Test
    fun guideHoejdenBliverAldrigNegativ() {
        // Absurde skærme: 0x0 op til 4x4 dp — hverken video, overlap eller
        // chrome kan presses ned under nul.
        (0..4).forEach { breddeDp ->
            (0..4).forEach { indholdHoejdeDp ->
                val topDp = TouchGuideSheetGeometry.collapsedTopDp(breddeDp)
                assertThat(
                    TouchGuideSheetGeometry.guideHeightDp(indholdHoejdeDp, topDp, kategorierHoejdeDp)
                ).isAtLeast(0)
            }
        }
    }

    @Test
    fun synligeRaekkerTaelerIkkeHalveOgKrasjerIkkePaaNul() {
        // 0 dp rækkehøjde (absurd input) må ikke dele med nul; under én hel
        // række tælles som nul, præcis én som én.
        assertThat(TouchGuideSheetGeometry.visibleRows(570, 0)).isEqualTo(0)
        assertThat(TouchGuideSheetGeometry.visibleRows(0, raekkeHoejdeDp)).isEqualTo(0)
        assertThat(TouchGuideSheetGeometry.visibleRows(raekkeHoejdeDp - 1, raekkeHoejdeDp)).isEqualTo(0)
        assertThat(TouchGuideSheetGeometry.visibleRows(raekkeHoejdeDp, raekkeHoejdeDp)).isEqualTo(1)
    }

    @Test
    fun videoHoejdenErSekstenNiAfBredden() {
        // 411 * 9 / 16 = 231,19 — afrundes ned til 231 hele dp.
        assertThat(TouchGuideSheetGeometry.videoHeightDp(telefonBreddeDp)).isEqualTo(231)
    }
}
