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
 * Tablet-guidens to-spaltede arbejdsflade.
 *
 * Samme opbygning som TV-versionen: grupperne står fast i venstre spalte,
 * afspiller-info og gitter ligger til højre. Forskellen er at der ikke er
 * animation eller sammenklapning — spalterne står fast, så layoutet falder
 * til ro i stedet for at flytte sig når fokus skifter. Det er hele pointen
 * med varianten.
 *
 * Geometrien bor i [TabletGuideGeometry], som er rene tal uden Compose- eller
 * Android-typer, så beslutningerne kan efterprøves i en almindelig JVM-test.
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
        // Venstre spalte: grupperne, med skillelinjen i højre kant.
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
        // Højre spalte: afspiller-info øverst, guiden fylder resten.
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
 * Tablet-guidens mål i dp, holdt som rene tal og funktioner uden Compose- eller
 * Android-afhængigheder — samme mønster som [LiveGuideDensity], så hver
 * beslutning kan efterprøves i en almindelig JVM-test i stedet for kun på en
 * emulator. Referencetabletten er 1280x800 dp, hvor indholdshøjden er 720 dp
 * (800 minus 24 dp statuslinje og 56 dp bundnavigation).
 */
internal object TabletGuideGeometry {

    /** Venstre spalte med grupperne. Smalere end TV'ets sidebar — tabletten har brug for bredden til gitteret. */
    const val GroupColumnDp = 220

    /** Info-panelet over guiden: afspiller og det program der er valgt nu. */
    const val InfoPanelDp = 150

    /** Kanalkolonnen i gitteret, med nummer, logo og navn. */
    const val ChannelColumnDp = 200

    /** Mindste berøringsmål. En tablet er en berøringsskærm — ikke en fjernbetjening. */
    const val MinTouchTargetDp = 48

    /** Højden gitteret kan bruge: indholdet minus tidslinjens lineal. Aldrig negativ. */
    fun guideHeightDp(contentHeightDp: Int, rulerHeightDp: Int): Int =
        (contentHeightDp - rulerHeightDp).coerceAtLeast(0)

    /** Antal hele kanalrækker der er plads til under linealen. */
    fun visibleRows(contentHeightDp: Int, rulerHeightDp: Int, rowHeightDp: Int): Int =
        guideHeightDp(contentHeightDp, rulerHeightDp) / rowHeightDp.coerceAtLeast(1)

    /** Bredden gitterets tidslinje kan bruge: skærmen minus gruppekolonne og kanalkolonne. */
    fun timelineWidthDp(screenWidthDp: Int, groupColumnDp: Int = GroupColumnDp): Int =
        (screenWidthDp - groupColumnDp - ChannelColumnDp).coerceAtLeast(0)

    /** Hvor mange minutter tidslinjen rumer ved [pxPerMinute] dp pr. minut. */
    fun timelineMinutes(timelineWidthDp: Int, pxPerMinute: Int = 4): Int =
        timelineWidthDp / pxPerMinute.coerceAtLeast(1)
}
