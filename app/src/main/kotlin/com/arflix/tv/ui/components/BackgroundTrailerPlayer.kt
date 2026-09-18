package com.arflix.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import kotlinx.coroutines.delay

/** Delayed home preview, using the same unobscured, interactive player as details. */
@Composable
fun BackgroundTrailerPlayer(
    youtubeKey: String,
    delayMs: Long = 2000L,
    soundEnabled: Boolean = false,
    onVisibilityChanged: (Boolean) -> Unit = {},
    onClose: () -> Unit
) {
    var showPlayer by remember(youtubeKey) { mutableStateOf(false) }
    val visibilityChanged by rememberUpdatedState(onVisibilityChanged)
    DisposableEffect(showPlayer) {
        visibilityChanged(showPlayer)
        onDispose { visibilityChanged(false) }
    }
    LaunchedEffect(youtubeKey, delayMs) {
        delay(delayMs.coerceAtLeast(0L))
        showPlayer = true
    }
    if (showPlayer) {
        key(youtubeKey) {
            YouTubeTrailerModal(youtubeKey, soundEnabled = soundEnabled, onClose = onClose)
        }
    }
}
