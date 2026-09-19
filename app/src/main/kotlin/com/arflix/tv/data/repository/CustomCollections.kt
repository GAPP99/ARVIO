package com.arflix.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.arflix.tv.data.model.CollectionGroupKind
import com.arflix.tv.data.model.CollectionSourceConfig
import com.arflix.tv.data.model.CollectionSourceKind
import com.arflix.tv.data.model.CollectionTileShape
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import java.util.Locale

/** One imported collection = one Home rail with its folders as tiles. */
internal data class CustomCollectionRail(
    val key: String,
    val title: String,
    val packId: String,
    val packName: String,
    val entries: List<CollectionTemplateEntry>
)

/**
 * User-installed collections, stored per device.
 *
 * Accepts the Nuvio collections export format (a JSON array of collections, each
 * with `folders` and `sources`), a single collection object, or a wrapper
 * `{ "name": "...", "collections": [ ... ] }`. Each imported document becomes a
 * "pack" that can be removed as a whole from Settings → Catalogs.
 *
 * State is loaded synchronously from SharedPreferences in [init] (called from
 * `ArflixApplication.onCreate`) because `MediaRepository.buildPreinstalledDefaults`
 * — which merges these into the catalog list — is not a suspend function.
 */
internal object CustomCollections {
    private const val PREFS_NAME = "arvio_custom_collections"
    private const val KEY_SOURCES = "sources_v1"
    private const val KEY_BUILT_IN_ENABLED = "built_in_collections_enabled"
    const val PACK_ID_PREFIX = "usercol_"

    @androidx.annotation.Keep
    private data class StoredSource(
        val packId: String,
        val name: String,
        val url: String?,
        val json: String
    )

    private val gson = Gson()

    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var sources: List<StoredSource> = emptyList()
    @Volatile private var railsCache: List<CustomCollectionRail> = emptyList()

    @Volatile
    var builtInEnabled: Boolean = true
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        builtInEnabled = p.getBoolean(KEY_BUILT_IN_ENABLED, true)
        sources = runCatching {
            gson.fromJson<List<StoredSource>>(
                p.getString(KEY_SOURCES, null) ?: "[]",
                object : TypeToken<List<StoredSource>>() {}.type
            ).orEmpty()
        }.getOrDefault(emptyList())
        rebuild()
    }

    fun rails(): List<CustomCollectionRail> = railsCache

    fun entries(): List<CollectionTemplateEntry> = railsCache.flatMap { it.entries }

    fun hasRail(key: String): Boolean = railsCache.any { it.key == key && it.entries.isNotEmpty() }

    fun isCustomPack(packId: String?): Boolean =
        packId != null && sources.any { it.packId == packId }

    fun setBuiltInEnabled(enabled: Boolean) {
        builtInEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_BUILT_IN_ENABLED, enabled)?.apply()
    }

    /** True when [json] parses as a collections document (as opposed to a catalog pack manifest). */
    fun looksLikeCollections(json: String): Boolean =
        runCatching { collectionObjects(JsonParser.parseString(json)).isNotEmpty() }.getOrDefault(false)

    /**
     * Installs (or re-installs, when the same URL / content was imported before)
     * a collections document. Returns the pack name and the number of rails added.
     */
    fun install(json: String, url: String?): Result<Pair<String, Int>> {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid JSON"))
        val collections = collectionObjects(root)
        if (collections.isEmpty()) {
            return Result.failure(IllegalArgumentException("No collections found"))
        }
        val packId = PACK_ID_PREFIX + sha256Short(url?.trim()?.lowercase(Locale.US) ?: json)
        val name = (root as? JsonObject)?.str("name")
            ?: collections.singleOrNull()?.str("title")
            ?: url?.let { runCatching { java.net.URI(it).host }.getOrNull() }
            ?: "Imported collections"
        val rails = parseRails(collections, packId, name)
        if (rails.none { it.entries.isNotEmpty() }) {
            return Result.failure(IllegalArgumentException("No supported folders/sources found"))
        }
        val stored = StoredSource(packId = packId, name = name, url = url, json = json)
        sources = sources.filterNot { it.packId == packId } + stored
        persist()
        return Result.success(name to rails.size)
    }

    fun remove(packId: String): Boolean {
        if (sources.none { it.packId == packId }) return false
        sources = sources.filterNot { it.packId == packId }
        persist()
        return true
    }

    private fun persist() {
        prefs?.edit()?.putString(KEY_SOURCES, gson.toJson(sources))?.apply()
        rebuild()
    }

    private fun rebuild() {
        railsCache = sources.flatMap { source ->
            runCatching {
                parseRails(collectionObjects(JsonParser.parseString(source.json)), source.packId, source.name)
            }.getOrDefault(emptyList())
        }.filter { it.entries.isNotEmpty() }
    }

    // ── Parsing ─────────────────────────────────────────────────────────

    private fun collectionObjects(root: JsonElement): List<JsonObject> {
        val array: JsonArray? = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject && root.asJsonObject.get("collections")?.isJsonArray == true ->
                root.asJsonObject.getAsJsonArray("collections")
            root.isJsonObject && root.asJsonObject.get("folders")?.isJsonArray == true ->
                JsonArray().apply { add(root) }
            else -> null
        }
        return array?.mapNotNull { el ->
            (el as? JsonObject)?.takeIf { it.get("folders")?.isJsonArray == true && it.str("title") != null }
        }.orEmpty()
    }

    private fun parseRails(collections: List<JsonObject>, packId: String, packName: String): List<CustomCollectionRail> {
        return collections.mapIndexed { index, collection ->
            val collectionId = collection.str("id") ?: "c$index"
            val railKey = "custom_${packId}_${slugify(collectionId)}"
            val entries = (collection.arr("folders") ?: JsonArray()).mapIndexedNotNull { folderIndex, el ->
                val folder = el as? JsonObject ?: return@mapIndexedNotNull null
                parseFolder(folder, folderIndex, railKey, packId, packName)
            }
            CustomCollectionRail(
                key = railKey,
                title = collection.str("title") ?: packName,
                packId = packId,
                packName = packName,
                entries = entries
            )
        }
    }

    private fun parseFolder(
        folder: JsonObject,
        index: Int,
        railKey: String,
        packId: String,
        packName: String
    ): CollectionTemplateEntry? {
        val title = folder.str("title") ?: return null
        val sourcesJson = folder.arr("sources")?.takeIf { it.size() > 0 }
            ?: folder.arr("catalogSources")
            ?: JsonArray()
        val sources = sourcesJson.flatMap { el -> (el as? JsonObject)?.let(::parseSource).orEmpty() }
        if (sources.isEmpty()) return null
        val folderId = folder.str("id") ?: "f$index"
        val cover = folder.str("coverImageUrl")
        val focusGif = folder.str("focusGifUrl")
            ?.takeIf { folder.get("focusGifEnabled")?.takeIf { it.isJsonPrimitive }?.asBoolean != false }
        return CollectionTemplateEntry(
            id = "collection_${railKey}_${slugify(folderId)}",
            title = title,
            group = CollectionGroupKind.NETWORK,
            coverImageUrl = cover ?: focusGif.orEmpty(),
            tileShape = if (folder.str("tileShape").equals("POSTER", ignoreCase = true)) {
                CollectionTileShape.POSTER
            } else {
                CollectionTileShape.LANDSCAPE
            },
            // Without artwork the tile would be blank, so always show the title then.
            hideTitle = (folder.get("hideTitle")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false) &&
                (cover != null || focusGif != null),
            heroVideoUrl = folder.str("heroVideoUrl"),
            sources = sources,
            listMetadata = emptyList(),
            railKey = railKey,
            description = folder.str("description"),
            heroImageUrl = folder.str("heroBackdropUrl"),
            focusGifUrl = focusGif,
            clearLogoUrl = folder.str("titleLogoUrl"),
            packId = packId,
            packName = packName
        )
    }

    private fun parseSource(source: JsonObject): List<CollectionSourceConfig> {
        return when (source.str("provider")?.lowercase(Locale.US) ?: "addon") {
            "addon" -> {
                val type = source.str("type") ?: return emptyList()
                val catalogId = source.str("catalogId") ?: return emptyList()
                listOf(
                    CollectionSourceConfig(
                        kind = CollectionSourceKind.ADDON_CATALOG,
                        mediaType = type,
                        addonId = source.str("addonId"),
                        addonCatalogType = type,
                        addonCatalogId = catalogId
                    )
                )
            }
            "trakt" -> {
                val listId = source.get("traktListId")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                    ?.takeIf { it.isNotEmpty() && it != "0" } ?: return emptyList()
                listOf(CollectionSourceConfig(kind = CollectionSourceKind.TRAKT_LIST, traktListId = listId))
            }
            "tmdb" -> parseTmdbSource(source)
            "mdblist" -> {
                val slug = source.str("slug") ?: source.str("mdblistSlug") ?: return emptyList()
                listOf(CollectionSourceConfig(kind = CollectionSourceKind.MDBLIST_PUBLIC, mdblistSlug = slug))
            }
            else -> emptyList()
        }
    }

    private fun parseTmdbSource(source: JsonObject): List<CollectionSourceConfig> {
        val sourceType = source.str("tmdbSourceType")?.uppercase(Locale.US) ?: return emptyList()
        val tmdbId = source.get("tmdbId")?.takeIf { it.isJsonPrimitive }?.asInt
        val isTv = source.str("mediaType")?.uppercase(Locale.US) == "TV"
        val media = if (isTv) "tv" else "movie"
        val sortBy = source.str("sortBy")?.takeUnless { it == "original" }
        return when (sourceType) {
            "COLLECTION" -> tmdbId?.let {
                listOf(CollectionSourceConfig(kind = CollectionSourceKind.TMDB_COLLECTION, tmdbCollectionId = it))
            }.orEmpty()
            "LIST" -> tmdbId?.let {
                listOf(CollectionSourceConfig(kind = CollectionSourceKind.TMDB_LIST, tmdbListId = it))
            }.orEmpty()
            else -> {
                val params = LinkedHashMap<String, String>()
                when (sourceType) {
                    "COMPANY" -> params["with_companies"] = tmdbId?.toString() ?: return emptyList()
                    "NETWORK" -> params["with_networks"] = tmdbId?.toString() ?: return emptyList()
                    "PERSON" -> params[if (isTv) "with_people" else "with_cast"] = tmdbId?.toString() ?: return emptyList()
                    "DIRECTOR" -> params["with_crew"] = tmdbId?.toString() ?: return emptyList()
                    "DISCOVER" -> Unit
                    else -> return emptyList()
                }
                (source.get("filters") as? JsonObject)?.let { putDiscoverFilters(it, isTv, params) }
                listOf(
                    CollectionSourceConfig(
                        kind = CollectionSourceKind.TMDB_DISCOVER,
                        mediaType = media,
                        sortBy = sortBy,
                        discoverParams = params
                    )
                )
            }
        }
    }

    private fun putDiscoverFilters(filters: JsonObject, isTv: Boolean, params: MutableMap<String, String>) {
        fun put(key: String, value: String?) {
            if (!value.isNullOrBlank()) params[key] = value
        }
        put("with_genres", filters.str("withGenres"))
        put("without_genres", filters.str("withoutGenres"))
        put(if (isTv) "first_air_date.gte" else "primary_release_date.gte", filters.str("releaseDateGte"))
        put(if (isTv) "first_air_date.lte" else "primary_release_date.lte", filters.str("releaseDateLte"))
        put("vote_average.gte", filters.str("voteAverageGte"))
        put("vote_average.lte", filters.str("voteAverageLte"))
        put("vote_count.gte", filters.str("voteCountGte"))
        put("with_original_language", filters.str("withOriginalLanguage"))
        put("with_origin_country", filters.str("withOriginCountry"))
        put("with_keywords", filters.str("withKeywords"))
        put("without_keywords", filters.str("withoutKeywords"))
        put("with_companies", filters.str("withCompanies"))
        put("without_companies", filters.str("withoutCompanies"))
        if (isTv) put("with_networks", filters.str("withNetworks"))
        put(if (isTv) "first_air_date_year" else "primary_release_year", filters.str("year"))
        put("watch_region", filters.str("watchRegion"))
        put("with_watch_providers", filters.str("withWatchProviders"))
        put("without_watch_providers", filters.str("withoutWatchProviders"))
    }

    private fun JsonObject.arr(key: String): JsonArray? = get(key) as? JsonArray

    private fun JsonObject.str(key: String): String? {
        val el = get(key) ?: return null
        if (!el.isJsonPrimitive) return null
        return el.asString.trim().takeIf { it.isNotEmpty() }
    }

    private fun slugify(value: String): String =
        value.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "x" }

    private fun sha256Short(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .take(6).joinToString("") { "%02x".format(it) }
}
