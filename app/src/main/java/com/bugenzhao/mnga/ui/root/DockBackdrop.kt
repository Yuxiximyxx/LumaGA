package com.bugenzhao.mnga.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.bugenzhao.mnga.ui.nav.Route
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

/**
 * Who records the tab dock's [LayerBackdrop] (v1.7.2).
 *
 * In v1.7.1 the only recording node was a shell-level box wrapping the
 * whole SharedNav host. That records every keep-alive page plus the
 * navigation library's transition scaffolding (per-page effect layers),
 * and the full-screen subtree is re-recorded on every content draw —
 * the heaviest, most indirect sample the dock could take, and the one
 * whose texture can lag or sit stale behind the glass (the "pasted
 * image" look reported on device).
 *
 * Now the four tab-root screens (v1.8.0; two before) — the only places
 * the dock is visible — record their own scrolling content root
 * directly (ForumList's grid, PersonalCenter's list, …), the same
 * "sample the real content directly beneath" structure the sister
 * FluxTie fork uses. The shell wrapper only records while some other
 * destination is on top (the dock is hidden then; this keeps the
 * texture warm and correct for the frames around tab-root transitions).
 *
 * Exactly one node records at a time: the decision is a pure function
 * of the current route, shared by the shell and both screens, so the
 * handover cannot race.
 *
 * v1.7.8: every recording (here, in [rememberScreenBackdropSourceModifier]
 * and at the raw recording sites) paints the page background as its own
 * first layer — see [recordedPageBackground] for why a texture without
 * it turned the dock into a smoked-black capsule.
 */
val LocalDockBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/** The routes whose screens record the dock backdrop themselves. */
fun Route?.isDockBackdropScreen(): Boolean =
    this is Route.ForumList || this is Route.Forums ||
        this is Route.Favorites || this is Route.PersonalCenter

/**
 * Whether the shell wrapper (not a screen) records the dock backdrop.
 *
 * TopicDetails is excluded (v1.8.1 crash isolation): the shell
 * recording wraps the whole navigation host, so while a topic is open
 * it re-records the rendered post content (tables, inline images,
 * quote layers — the richest subtree in the app) into a graphics layer
 * on every content draw. That recording, introduced together with the
 * details screen's own content recording in v1.7.1, is the regression
 * window for the "some topics crash on open since v1.7.1" device
 * report (v1.7.0 opens the same topics fine); the details screen's own
 * recording was reduced to its page background in the same version.
 * The dock is hidden while a topic is open, and the tab roots record
 * themselves again on return, so skipping the shell recording here
 * costs nothing on screen.
 */
fun dockShellRecordsBackdrop(current: Route?): Boolean =
    !current.isDockBackdropScreen() && current !is Route.TopicDetails

/**
 * The recording modifier for a tab-root screen's content root: records
 * into the shared dock backdrop while this screen is the current
 * destination, and is a no-op otherwise (covered keep-alive instances
 * of the same screen must not overwrite the visible page's sample).
 * The recording carries the page-background underlay
 * ([recordedPageBackground]).
 */
@Composable
fun rememberDockBackdropSourceModifier(isCurrentScreen: Boolean): Modifier {
    val backdrop = LocalDockBackdrop.current
    return if (isCurrentScreen && backdrop != null) {
        Modifier.layerBackdrop(backdrop).recordedPageBackground()
    } else {
        Modifier
    }
}
