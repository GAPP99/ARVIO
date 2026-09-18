package com.arflix.tv.ui.screens.tv.live

/**
 * Density and content spec for the TV guide.
 *
 * Kept as plain numbers and data types with no Compose or Android dependencies,
 * so every decision can be verified in a plain JVM test instead of only on an
 * emulator. Same pattern as [LiveTvStartup] and `liveTvMiniPlayerLayout`.
 *
 * The background: the guide showed five to ten text elements per row — number,
 * logo, name, star, catch-up icon, progress bar, quality pill, language pill,
 * equalizer, and in the program cell badge, title, description, start time and
 * duration. It was the noise, not the font size, that made the guide hard to
 * take in. The spec below decides in one place what actually gets drawn.
 *
 * Measurements are in dp against a 960x540 dp canvas, i.e. 1080p at density 2.0.
 */
object LiveGuideDensity {

    /** Height of the AppTopBar band, cf. LiveDims.ContentTopInset. */
    const val TopBarDp = 74

    /** The timeline's header. The date lives in the left stub, not a separate bar. */
    const val RulerHeightDp = 36

    /**
     * Fixed row height. The rows must not resize when moving between groups
     * and guide — that is what lets the layout settle. 38 dp gives eight rows
     * on a 1080p screen.
     *
     * Note the value is deliberately kept below 60 dp: above that threshold
     * [ProgramCell] falls out of its drawn canvas path and starts laying out
     * a full composite tree per cell.
     */
    const val RowHeightDp = 38

    fun rowHeightDp(rowCount: Int): Int =
        ((540 - TopBarDp - InfoPanelWithTopBarDp - RulerHeightDp) / rowCount.coerceAtLeast(6))
            .coerceIn(28, 46)

    /** The channel column. Wider than before, because the name now carries the quality suffix. */
    const val ChannelColumnDp = 248

    /** The info panel above the guide. It is the panel — not the rows — that makes
     *  room for the top bar, so the row count is the same in every focus step. */
    const val InfoPanelFullDp = 200
    const val InfoPanelWithTopBarDp = InfoPanelFullDp - TopBarDp

    fun infoPanelHeightDp(topBarVisible: Boolean): Int =
        if (topBarVisible) InfoPanelWithTopBarDp else InfoPanelFullDp

    /** Number of whole channel rows that fit below the panel and the ruler. */
    fun visibleRowCount(screenHeightDp: Int, topBarVisible: Boolean, rowCount: Int = 8): Int {
        val chrome = (if (topBarVisible) TopBarDp else 0) +
            infoPanelHeightDp(topBarVisible) + RulerHeightDp
        return ((screenHeightDp - chrome) / rowHeightDp(rowCount)).coerceAtLeast(0)
    }

    // ── channel row ────────────────────────────────────────────────────────

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

    // ── program cell ───────────────────────────────────────────────────────

    /** Gap between cells. 1 dp read as a bar; 3 dp reads as air. */
    const val CellGutterDp = 3
    const val CellRadiusDp = 5

    /** Below this width there is only room for a truncated title. */
    const val BadgeMinCellWidthDp = 180

    data class ProgramCellSpec(
        val showTitle: Boolean,
        val showTimeFooter: Boolean,
        val showDescription: Boolean,
        val showBadges: Boolean,
        val centerTitleVertically: Boolean,
    )

    /**
     * The cell shows only the title. Start and end time used to sit in every
     * single cell, even though the timeline above and the info panel say the
     * same thing — three repetitions of the same information per screen.
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
