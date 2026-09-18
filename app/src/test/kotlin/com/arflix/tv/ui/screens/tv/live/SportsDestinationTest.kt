package com.arflix.tv.ui.screens.tv.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The sports entry used to sit as its own row at the top of the group list,
 * far from the `g-sports` genre filter it resembled. Now the two sit side by
 * side inside the accordion: "Sports" is the match guide, "Sports · Global" is
 * the channel list.
 */
class SportsDestinationTest {

    private val emptySection = LiveSection("x", "X", emptyList())

    private fun tree(vararg allChildren: LiveCategory) = LiveCategoryTree(
        top = listOf(
            LiveCategory("fav", "Favorites", 3, CategoryIcon.Favorite),
            LiveCategory("all", "All Channels", 233, CategoryIcon.All, children = allChildren.toList()),
        ),
        global = emptySection,
        countries = emptySection,
        adult = emptySection,
    )

    @Test
    fun sportsGuideSitsRightBeforeTheChannelList() {
        val before = tree(
            LiveCategory("g-4k", "4K | Ultra HD", 12, CategoryIcon.Grid),
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
            LiveCategory("g-news", "News", 8, CategoryIcon.Grid),
        )

        val children = before.withSportsDestination().byId("all")!!.children

        assertThat(children.map { it.id })
            .containsExactly("g-4k", SPORTS_GUIDE_CATEGORY, SPORTS_CHANNEL_CATEGORY, "g-news")
            .inOrder()
    }

    @Test
    fun onlyTheChannelListHasACount() {
        // The count is the only thing that tells the two rows apart at a
        // glance: the channel list has 41 channels, the sports guide counts
        // matches and therefore shows nothing.
        val children = tree(
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
        ).withSportsDestination().byId("all")!!.children

        val guide = children.single { it.id == SPORTS_GUIDE_CATEGORY }
        assertThat(guide.label).isEqualTo("Sports")
        assertThat(guide.count).isEqualTo(0)

        val channels = children.single { it.id == SPORTS_CHANNEL_CATEGORY }
        assertThat(channels.label).isEqualTo("Sports · Global")
        assertThat(channels.count).isEqualTo(41)
    }

    @Test
    fun theSportsRowIsNoLongerAtTheTop() {
        val result = tree(
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
        ).withSportsDestination()

        assertThat(result.top.map { it.id }).containsExactly("fav", "all").inOrder()
    }

    @Test
    fun playlistWithoutSportsChannelsGetsNoEmptyRow() {
        val children = tree(
            LiveCategory("g-news", "News", 8, CategoryIcon.Grid),
        ).withSportsDestination().byId("all")!!.children

        assertThat(children.map { it.id }).containsExactly("g-news")
        assertThat(children.none { it.id == SPORTS_GUIDE_CATEGORY }).isTrue()
    }

    @Test
    fun thePhonesFlatListGetsTheSportsGuideAtTheTop() {
        // The phone carousel only shows `top` — with the sports guide inside
        // the accordion it cannot be reached from there at all.
        val result = tree(
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
        ).withSportsDestination(inAccordion = false)

        assertThat(result.top.map { it.id })
            .containsExactly("fav", "all", SPORTS_GUIDE_CATEGORY).inOrder()
        // The channel list stays where it is.
        assertThat(result.byId("all")!!.children.map { it.id })
            .containsExactly(SPORTS_CHANNEL_CATEGORY)
    }

    @Test
    fun theFlatListGetsTheSportsGuideEvenWithoutSportsChannels() {
        val result = tree(
            LiveCategory("g-news", "News", 8, CategoryIcon.Grid),
        ).withSportsDestination(inAccordion = false)

        assertThat(result.top.map { it.id }).contains(SPORTS_GUIDE_CATEGORY)
    }

    @Test
    fun repeatedApplicationDoesNotChangeTheResult() {
        // The tree is rebuilt every time the channels update, so the function
        // must be able to run on its own result without adding another sports
        // guide.
        val initial = tree(
            LiveCategory(SPORTS_CHANNEL_CATEGORY, "Sports · Global", 41, CategoryIcon.Sport),
        )
        val once = initial.withSportsDestination()
        val twice = once.withSportsDestination()

        assertThat(twice.byId("all")!!.children.map { it.id })
            .isEqualTo(once.byId("all")!!.children.map { it.id })
        assertThat(twice.byId("all")!!.children.count { it.id == SPORTS_GUIDE_CATEGORY })
            .isEqualTo(1)
    }
}
