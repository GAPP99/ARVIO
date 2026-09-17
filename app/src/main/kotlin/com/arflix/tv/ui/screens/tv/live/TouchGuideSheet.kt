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
 * Geometrien bag telefon-guidens bundark, holdt som rene tal uden Compose-
 * eller Android-typer — samme mønster som [LiveGuideDensity], så hver
 * beslutning kan efterprøves i en almindelig JVM-test.
 *
 * Videoen står fast øverst med 16:9-højde. Arket har to ankre: kollapset,
 * hvor toppen ligger lige under videoen minus et overlap så de afrundede
 * hjørner dækker videoens bundkant, og udfoldet, hvor arket dækker videoen
 * helt. Guiderne står i de dp der er tilbage efter greb og kategorier.
 */
internal object TouchGuideSheetGeometry {

    /** Grebsbåndet i arket. En centreret streg, resten er luft og trykflade. */
    const val HandleHeightDp = 22

    /**
     * Hvor langt arket glider op over videoen når det er kollapset. Overlappet
     * får de afrundede hjørner til at ligge hen over videoens bundkant i stedet
     * for at tegne en hård skillelinje midt i billedet.
     */
    const val SheetOverlapDp = 35

    /** Videoens højde ved fuld bredde — 16:9, afrundet ned til hele dp. */
    fun videoHeightDp(widthDp: Int): Int = widthDp * 9 / 16

    /** Arkets top når det er kollapset: lige under videoen, minus overlap. */
    fun collapsedTopDp(widthDp: Int): Int = videoHeightDp(widthDp) - SheetOverlapDp

    /** Arkets top når det er udfoldet: det dækker videoen helt. */
    fun expandedTopDp(): Int = 0

    /** Det synlige guideareal under greb og kategorier ved et givet ark-top. */
    fun guideHeightDp(contentHeightDp: Int, sheetTopDp: Int, categoriesHeightDp: Int): Int =
        (contentHeightDp - sheetTopDp - HandleHeightDp - categoriesHeightDp).coerceAtLeast(0)

    /** Hvor mange hele rækker der er plads til — halve rækker tælles ikke. */
    fun visibleRows(guideHeightDp: Int, rowHeightDp: Int): Int =
        if (rowHeightDp <= 0) 0 else guideHeightDp / rowHeightDp
}

/** Arkets to hvilepositioner. */
private enum class TouchGuideSheetPosition { Collapsed, Expanded }

/** Hastighedsovergangen mellem træk og snap — materialens 125 dp-svarelsesgrænse. */
private const val DragVelocityThresholdDp = 125

/**
 * Telefon-guiden: videoen fast øverst, og et bundark med kategorier og guide
 * der trækkes op hen over den med [androidx.compose.foundation.gestures.anchoredDraggable].
 *
 * Arket har to ankre — kollapset (toppen lige under videoen minus
 * [TouchGuideSheetGeometry.SheetOverlapDp] overlap, så de afrundede hjørner
 * dækker videoens bund) og udfoldet (toppen i 0, altså hele vejen op). Et tryk
 * på grebet skifter mellem dem; ellers følger arket fingeren og snapper ved
 * slip. Indholdet ovenfra er grebet, [categories] og [guide] som fylder
 * resten. [player] flytter sig aldrig — arket glider hen over den.
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

        // Ankrene regnes i den samme heltals-geometri som testene dækker.
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
        // Samme instans så længe bredden står stille — updateAnchors no-op'er
        // via equals i stedet for at rive et igangværende træk i stykker.
        val anchors = remember(collapsedTopPx) {
            DraggableAnchors {
                TouchGuideSheetPosition.Collapsed at collapsedTopPx
                TouchGuideSheetPosition.Expanded at TouchGuideSheetGeometry.expandedTopDp().toFloat()
            }
        }
        SideEffect { state.updateAnchors(anchors) }

        // Videoen. Den flytter sig aldrig — arket lægger sig ovenpå.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TouchGuideSheetGeometry.videoHeightDp(widthDp).dp),
        ) {
            player()
        }

        // Bundarket. offset{} læser træk-offsettet under layout, så selve
        // trækket ikke recomposer hele skærmen pr. frame.
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
                    // 1 dp topkant — mødet mellem video og ark.
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
