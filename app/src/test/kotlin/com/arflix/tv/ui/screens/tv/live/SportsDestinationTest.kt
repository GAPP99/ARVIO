package com.arflix.tv.ui.screens.tv.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Sportsindgangen lå før som sin egen række i gruppelistens top, langt fra
 * genrefilteret `g-sports` som den lignede. Nu ligger de to ved siden af
 * hinanden inde i accordion: "Sports" er kampguiden, "Sports · Global" er
 * kanallisten.
 */
class SportsDestinationTest {

    private val tomSektion = LiveSection("x", "X", emptyList())

    private fun træ(vararg alleBørn: LiveCategory) = LiveCategoryTree(
        top = listOf(
            LiveCategory("fav", "Favorites", 3, CategoryIcon.Favorite),
            LiveCategory("all", "All Channels", 233, CategoryIcon.All, children = alleBørn.toList()),
        ),
        global = tomSektion,
        countries = tomSektion,
        adult = tomSektion,
    )

    @Test
    fun kampguidenLiggerLigeFoerKanallisten() {
        val før = træ(
            LiveCategory("g-4k", "4K | Ultra HD", 12, CategoryIcon.Grid),
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
            LiveCategory("g-news", "News", 8, CategoryIcon.Grid),
        )

        val børn = før.withSportsDestination().byId("all")!!.children

        assertThat(børn.map { it.id })
            .containsExactly("g-4k", SPORTS_GUIDE_CATEGORY, SPORTS_CHANNEL_CATEGORY, "g-news")
            .inOrder()
    }

    @Test
    fun kunKanallistenHarEnTaelling() {
        // Tallet er det eneste der skiller de to rækker ad på et blik: kanallisten
        // har 41 kanaler, kampguiden tæller kampe og viser derfor ingenting.
        val børn = træ(
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
        ).withSportsDestination().byId("all")!!.children

        val guide = børn.single { it.id == SPORTS_GUIDE_CATEGORY }
        assertThat(guide.label).isEqualTo("Sports")
        assertThat(guide.count).isEqualTo(0)

        val kanaler = børn.single { it.id == SPORTS_CHANNEL_CATEGORY }
        assertThat(kanaler.label).isEqualTo("Sports · Global")
        assertThat(kanaler.count).isEqualTo(41)
    }

    @Test
    fun sportsraekkenLiggerIkkeLaengereITopppen() {
        val resultat = træ(
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
        ).withSportsDestination()

        assertThat(resultat.top.map { it.id }).containsExactly("fav", "all").inOrder()
    }

    @Test
    fun playlisteUdenSportskanalerFaarIngenTomRaekke() {
        val børn = træ(
            LiveCategory("g-news", "News", 8, CategoryIcon.Grid),
        ).withSportsDestination().byId("all")!!.children

        assertThat(børn.map { it.id }).containsExactly("g-news")
        assertThat(børn.none { it.id == SPORTS_GUIDE_CATEGORY }).isTrue()
    }

    @Test
    fun telefonensFladeListeFaarKampguidenITopppen() {
        // Karrusellen på telefonen viser kun `top` — ligger kampguiden inde i
        // accordion, kan den slet ikke nås derfra.
        val resultat = træ(
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
        ).withSportsDestination(inAccordion = false)

        assertThat(resultat.top.map { it.id })
            .containsExactly("fav", "all", SPORTS_GUIDE_CATEGORY).inOrder()
        // Kanallisten bliver liggende hvor den er.
        assertThat(resultat.byId("all")!!.children.map { it.id })
            .containsExactly(SPORTS_CHANNEL_CATEGORY)
    }

    @Test
    fun denFladeListeFaarKampguidenOgsaaUdenSportskanaler() {
        val resultat = træ(
            LiveCategory("g-news", "News", 8, CategoryIcon.Grid),
        ).withSportsDestination(inAccordion = false)

        assertThat(resultat.top.map { it.id }).contains(SPORTS_GUIDE_CATEGORY)
    }

    @Test
    fun gentagenAnvendelseAendrerIkkeResultatet() {
        // Træet bygges om hver gang kanalerne opdateres, så funktionen skal kunne
        // køres på sit eget resultat uden at lægge en kampguide mere ind.
        val start = træ(
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
        )
        val enGang = start.withSportsDestination()
        val toGange = enGang.withSportsDestination()

        assertThat(toGange.byId("all")!!.children.map { it.id })
            .isEqualTo(enGang.byId("all")!!.children.map { it.id })
        assertThat(toGange.byId("all")!!.children.count { it.id == SPORTS_GUIDE_CATEGORY })
            .isEqualTo(1)
    }
}
