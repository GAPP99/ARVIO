package com.arflix.tv.ui.screens.tv.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.arflix.tv.util.profilesDataStore
import com.arflix.tv.util.settingsDataStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

data class LiveGuideSettings(
    val newDesign: Boolean = true,
    val rowCount: Int = 8,
    val allProviders: Boolean = true
)

@Composable
fun rememberLiveGuideSettings(): LiveGuideSettings {
    val context = LocalContext.current
    val settings by remember(context) {
        combine(context.profilesDataStore.data, context.settingsDataStore.data) { profilePrefs, settingsPrefs ->
            val profileId = profilePrefs[stringPreferencesKey("active_profile_id")].orEmpty().ifBlank { "default" }
            val prefix = "profile_${profileId}_"
            LiveGuideSettings(
                newDesign = settingsPrefs[booleanPreferencesKey("${prefix}guide_design_new")] ?: true,
                rowCount = (settingsPrefs[intPreferencesKey("${prefix}guide_row_count")] ?: 8).coerceIn(6, 10),
                allProviders = settingsPrefs[booleanPreferencesKey("${prefix}guide_all_providers")] ?: true
            )
        }.distinctUntilChanged()
    }.collectAsState(initial = LiveGuideSettings())
    return settings
}
