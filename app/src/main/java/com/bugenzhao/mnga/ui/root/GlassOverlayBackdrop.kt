package com.bugenzhao.mnga.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kyant.backdrop.backdrops.LayerBackdrop

/**
 * Sampling for floating glass faces other than the tab dock (v1.7.3):
 * dropdown menus and toasts.
 *
 * Why a second backdrop, recorded on demand: Kyant's `layerBackdrop`
 * re-renders the recorded subtree on every draw of the recording node,
 * so a permanently-recording full-screen wrapper around the navigation
 * host is a standing frame cost (the very structure v1.7.2 removed for
 * the dock). Menus and toasts are transient, so the shell records the
 * host into [LocalContentBackdrop] only while at least one consumer is
 * composed ([GlassOverlayState.activeConsumers] > 0, driven by
 * [GlassOverlayRegistration]). The recording node is the same host
 * wrapper the dock falls back to — never both at once for this
 * backdrop, and the dock's own sampling (DockBackdrop.kt) is untouched.
 *
 * Who can consume: only faces rendered in the SAME window as the host.
 * Miuix's Super* popups/sheets render through the root Scaffold's popup
 * host in the main window, and the app's toasts are plain composables
 * in the root — both fine. Faces in platform windows (WindowDialog,
 * WindowListPopup) cannot sample across windows and are deliberately
 * left unglassed; see the v1.7.3 inventory in MiuixMenu.kt.
 */
val LocalContentBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/** Reference count of composed glass-overlay consumers. */
class GlassOverlayState {
    var activeConsumers by mutableIntStateOf(0)
        internal set
}

val LocalGlassOverlayState = compositionLocalOf<GlassOverlayState?> { null }

@Composable
fun rememberGlassOverlayState(): GlassOverlayState = remember { GlassOverlayState() }

/**
 * Marks a glass overlay face as composed: while any registration is
 * alive the shell records the content backdrop. Place it inside the
 * face's own composition (which for Miuix popups exists exactly while
 * the popup is shown, exit animation included), so recording starts the
 * same frame the face appears and stops when it is gone.
 */
@Composable
fun GlassOverlayRegistration() {
    val state = LocalGlassOverlayState.current ?: return
    DisposableEffect(state) {
        state.activeConsumers++
        onDispose { state.activeConsumers-- }
    }
}
