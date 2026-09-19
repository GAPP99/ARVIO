package com.arflix.tv.data.repository

import com.arflix.tv.data.model.CatalogConfig
import com.arflix.tv.data.model.CatalogKind
import com.arflix.tv.data.model.CatalogSourceType
import com.arflix.tv.data.model.CollectionSourceKind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomCollectionsTest {

    // Nuvio collections export shape: array of collections -> folders -> sources.
    private val nuvioExport = """
        [
          {
            "id": "studios", "title": "Studios", "viewMode": "TABBED_GRID",
            "folders": [
              { "id": "pixar", "title": "Pixar", "coverImageUrl": "https://x/pixar.jpg", "tileShape": "POSTER", "hideTitle": true,
                "sources": [ { "provider": "tmdb", "tmdbSourceType": "COMPANY", "tmdbId": 3, "mediaType": "MOVIE" } ] },
              { "id": "netflix", "title": "Netflix",
                "sources": [ { "provider": "addon", "addonId": "aio", "type": "series", "catalogId": "streaming.nfx" } ] },
              { "id": "il", "title": "Israeli",
                "sources": [ { "provider": "tmdb", "tmdbSourceType": "DISCOVER", "mediaType": "TV",
                               "filters": { "withOriginCountry": "IL", "releaseDateGte": "2020-01-01" } } ] }
            ]
          },
          {
            "id": "lists", "title": "Lists",
            "folders": [
              { "id": "hp", "title": "Harry Potter",
                "sources": [ { "provider": "tmdb", "tmdbSourceType": "COLLECTION", "tmdbId": 1241, "mediaType": "MOVIE" } ] },
              { "id": "trakt", "title": "Trakt list",
                "sources": [ { "provider": "trakt", "traktListId": 12345 } ] },
              { "id": "empty", "title": "Unsupported only",
                "sources": [ { "provider": "unknown" } ] }
            ]
          }
        ]
    """.trimIndent()

    @After
    fun tearDown() {
        CustomCollections.rails().map { it.packId }.distinct().forEach { CustomCollections.remove(it) }
        CustomCollections.setBuiltInEnabled(true)
    }

    @Test
    fun `installs a Nuvio export as one rail per collection`() {
        assertTrue(CustomCollections.looksLikeCollections(nuvioExport))
        val result = CustomCollections.install(nuvioExport, url = "https://example.com/collections.json")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().second)

        val rails = CustomCollections.rails()
        assertEquals(listOf("Studios", "Lists"), rails.map { it.title })
        assertEquals(3, rails[0].entries.size)
        // Folders whose sources are all unsupported are skipped.
        assertEquals(2, rails[1].entries.size)

        val pixar = rails[0].entries[0]
        assertEquals(CollectionSourceKind.TMDB_DISCOVER, pixar.sources.single().kind)
        assertEquals(mapOf("with_companies" to "3"), pixar.sources.single().discoverParams)
        assertTrue(pixar.hideTitle)

        val israeli = rails[0].entries[2].sources.single()
        assertEquals("tv", israeli.mediaType)
        assertEquals("IL", israeli.discoverParams?.get("with_origin_country"))
        assertEquals("2020-01-01", israeli.discoverParams?.get("first_air_date.gte"))

        assertEquals(CollectionSourceKind.ADDON_CATALOG, rails[0].entries[1].sources.single().kind)
        assertEquals(1241, rails[1].entries[0].sources.single().tmdbCollectionId)
        assertEquals("12345", rails[1].entries[1].sources.single().traktListId)
    }

    @Test
    fun `imported collections are valid and can replace the built-in ones`() {
        CustomCollections.install(nuvioExport, url = null)
        val rail = CustomCollections.rails().first()
        val tile = rail.entries.first()

        assertTrue(CollectionTemplateManifest.isValidCollectionConfig(railConfig(rail.key)))
        assertTrue(CollectionTemplateManifest.isValidCollectionConfig(tileConfig(tile.id)))

        CustomCollections.setBuiltInEnabled(false)
        assertTrue(CollectionTemplateManifest.railOrder.isEmpty())
        assertTrue(CollectionTemplateManifest.entries.all { it.railKey != null })
        assertTrue(CollectionTemplateManifest.isValidCollectionConfig(tileConfig(tile.id)))

        val defaults = MediaRepository.buildPreinstalledDefaults()
        assertTrue(defaults.any { it.collectionRailKey == rail.key && it.kind == CatalogKind.COLLECTION_RAIL })
        assertFalse(defaults.any { it.kind == CatalogKind.COLLECTION_RAIL && it.collectionRailKey == null })
    }

    @Test
    fun `removing the pack removes its rails`() {
        CustomCollections.install(nuvioExport, url = null)
        val packId = CustomCollections.rails().first().packId
        assertTrue(CustomCollections.isCustomPack(packId))
        assertTrue(CustomCollections.remove(packId))
        assertTrue(CustomCollections.rails().isEmpty())
    }

    @Test
    fun `rejects documents that are not collections`() {
        assertFalse(CustomCollections.looksLikeCollections("""{"id":"pack","name":"x","catalogs":[]}"""))
        assertTrue(CustomCollections.install("not json", null).isFailure)
    }

    private fun railConfig(key: String) = CatalogConfig(
        id = "collection_rail_$key", title = "r", sourceType = CatalogSourceType.PREINSTALLED,
        kind = CatalogKind.COLLECTION_RAIL, collectionRailKey = key
    )

    private fun tileConfig(id: String) = CatalogConfig(
        id = id, title = "t", sourceType = CatalogSourceType.PREINSTALLED, kind = CatalogKind.COLLECTION
    )
}
