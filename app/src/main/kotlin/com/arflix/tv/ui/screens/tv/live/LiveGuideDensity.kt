package com.arflix.tv.ui.screens.tv.live

/**
 * Tætheds- og indholdsspec for TV-guiden.
 *
 * Holdt som rene tal og datatyper uden Compose- eller Android-afhængigheder, så
 * hver beslutning kan efterprøves i en almindelig JVM-test i stedet for kun på
 * en emulator. Samme mønster som [LiveTvStartup] og `liveTvMiniPlayerLayout`.
 *
 * Baggrunden: guiden viste fem-ti tekstelementer pr. række — nummer, logo, navn,
 * stjerne, catch-up-ikon, progress-streg, kvalitets-pill, sprog-pill, equalizer,
 * og i programcellen både badge, titel, beskrivelse, starttid og varighed. Det
 * var støjen, ikke skriftstørrelsen, der gjorde guiden svær at overskue.
 * Specen herunder bestemmer ét sted hvad der faktisk tegnes.
 *
 * Målene er i dp mod et 960x540 dp-lærred, altså 1080p ved densitet 2.0.
 */
object LiveGuideDensity {

    /** Højden af AppTopBar-båndet, jf. LiveDims.ContentTopInset. */
    const val TopBarDp = 74

    /** Tidslinjens hoved. Datoen bor i venstre stub, ikke i en separat bjælke. */
    const val RulerHeightDp = 36

    /**
     * Fast rækkehøjde. Rækkerne må ikke skifte størrelse når man bevæger sig
     * mellem grupper og guide — det er dét, der får layoutet til at falde til ro.
     * 38 dp giver otte rækker på en 1080p-skærm.
     *
     * Bemærk at værdien bevidst holdes under 60 dp: over den grænse falder
     * [ProgramCell] ud af sin tegnede canvas-sti og begynder at layoute et helt
     * komposit-træ pr. celle.
     */
    const val RowHeightDp = 38

    fun rowHeightDp(rowCount: Int): Int =
        ((540 - TopBarDp - InfoPanelWithTopBarDp - RulerHeightDp) / rowCount.coerceAtLeast(6))
            .coerceIn(28, 46)

    /** Kanalkolonnen. Bredere end før, fordi navnet nu bærer kvalitets-suffikset. */
    const val ChannelColumnDp = 248

    /** Info-panelet over guiden. Det er panelet — ikke rækkerne — der giver plads
     *  til topbaren, så rækkeantallet er det samme i alle fokus-trin. */
    const val InfoPanelFullDp = 200
    const val InfoPanelWithTopBarDp = InfoPanelFullDp - TopBarDp

    fun infoPanelHeightDp(topBarVisible: Boolean): Int =
        if (topBarVisible) InfoPanelWithTopBarDp else InfoPanelFullDp

    /** Antal hele kanalrækker der er plads til under panel og lineal. */
    fun visibleRowCount(screenHeightDp: Int, topBarVisible: Boolean, rowCount: Int = 8): Int {
        val chrome = (if (topBarVisible) TopBarDp else 0) +
            infoPanelHeightDp(topBarVisible) + RulerHeightDp
        return ((screenHeightDp - chrome) / rowHeightDp(rowCount)).coerceAtLeast(0)
    }

    // ── kanalrækken ────────────────────────────────────────────────────────

    data class ChannelRowSpec(
        val showNumber: Boolean,
        val showQualitySuffix: Boolean,
        val showQualityPill: Boolean,
        val showLanguagePill: Boolean,
        val showProgressBar: Boolean,
        val showCatchupIcon: Boolean,
        val showFavoriteStar: Boolean,
        val showPlayingMarker: Boolean,
    )

    fun channelRowSpec(rowHeightDp: Int = RowHeightDp, compact: Boolean = false): ChannelRowSpec =
        ChannelRowSpec(
            showNumber = !compact,
            showQualitySuffix = rowHeightDp < 48,
            showQualityPill = rowHeightDp >= 48,
            showLanguagePill = rowHeightDp >= 48,
            showProgressBar = rowHeightDp >= 48,
            showCatchupIcon = rowHeightDp >= 48,
            showFavoriteStar = true,
            showPlayingMarker = true,
        )

    // ── programcellen ──────────────────────────────────────────────────────

    /** Mellemrum mellem celler. 1 dp læste som en streg; 3 dp læser som luft. */
    const val CellGutterDp = 3
    const val CellRadiusDp = 5

    /** Under denne bredde er der ikke plads til andet end en forkortet titel. */
    const val BadgeMinCellWidthDp = 180

    data class ProgramCellSpec(
        val showTitle: Boolean,
        val showTimeFooter: Boolean,
        val showDescription: Boolean,
        val showBadges: Boolean,
        val centerTitleVertically: Boolean,
    )

    /**
     * Cellen viser kun titlen. Start- og sluttid stod tidligere i hver eneste
     * celle, selvom tidslinjen ovenover og info-panelet siger det samme —
     * tre gentagelser af den samme oplysning pr. skærmbillede.
     */
    fun programCellSpec(
        rowHeightDp: Int = RowHeightDp,
        cellWidthDp: Int,
    ): ProgramCellSpec = ProgramCellSpec(
        showTitle = true,
        showTimeFooter = false,
        showDescription = false,
        showBadges = cellWidthDp >= BadgeMinCellWidthDp,
        centerTitleVertically = rowHeightDp < 60,
    )
}
