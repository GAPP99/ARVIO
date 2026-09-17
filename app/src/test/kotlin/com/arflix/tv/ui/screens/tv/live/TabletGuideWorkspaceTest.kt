package com.arflix.tv.ui.screens.tv.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tablet-geometrien er variantens kontrakt. Referencetabletten er 1280x800 dp
 * med 720 dp indhold (800 minus 24 dp statuslinje og 56 dp bundnavigation),
 * lineal 36 dp og rækkehøjde 52 dp.
 */
class TabletGuideWorkspaceTest {

    private val indholdDp = 720
    private val linealDp = 36
    private val raekkeDp = 52
    private val skaermBreddeDp = 1280

    @Test
    fun referencetablettenViserMindstTiRaekker() {
        // Pointen med tablet-varianten: gitteret skal føles som et gitter,
        // ikke som fem rækker på et forbryderisk stort lærred.
        val raekker = TabletGuideGeometry.visibleRows(indholdDp, linealDp, raekkeDp)

        assertThat(raekker).isAtLeast(10)
    }

    @Test
    fun tidslinjenViserMindstTreTimerPaaReferencetabletten() {
        // Med 4 dp pr. minut skal der være plads til en hel aften uden at
        // brugeren skal scrolle for at se hvad der kommer.
        val minutter = TabletGuideGeometry.timelineMinutes(
            TabletGuideGeometry.timelineWidthDp(skaermBreddeDp),
        )

        assertThat(minutter).isAtLeast(180)
    }

    @Test
    fun raekkehojdenErEtGyldigtBeroeringsmaal() {
        // En tablet er en berøringsskærm — rækken skal kunne rammes med en finger.
        assertThat(raekkeDp).isAtLeast(TabletGuideGeometry.MinTouchTargetDp)
    }

    @Test
    fun guideHoejdenBliverAldrigNegativ() {
        // Absurd lave højder giver et tomt gitter, ikke et nedbrud.
        assertThat(TabletGuideGeometry.guideHeightDp(0, linealDp)).isEqualTo(0)
        assertThat(TabletGuideGeometry.guideHeightDp(10, linealDp)).isEqualTo(0)
        assertThat(TabletGuideGeometry.guideHeightDp(-500, linealDp)).isEqualTo(0)

        // Og dermed heller ikke et negativt rækkeantal.
        assertThat(TabletGuideGeometry.visibleRows(0, linealDp, raekkeDp)).isEqualTo(0)
    }

    @Test
    fun enBredereSkaermGiverEnLaengereTidslinje() {
        val smal = TabletGuideGeometry.timelineWidthDp(800)
        val bred = TabletGuideGeometry.timelineWidthDp(skaermBreddeDp)

        assertThat(bred).isGreaterThan(smal)
        assertThat(TabletGuideGeometry.timelineMinutes(bred))
            .isGreaterThan(TabletGuideGeometry.timelineMinutes(smal))
    }
}
