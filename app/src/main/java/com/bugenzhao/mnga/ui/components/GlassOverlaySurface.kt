package com.bugenzhao.mnga.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.bugenzhao.mnga.ui.root.rememberGlassSettings

/**
 * The background layer of a floating overlay face (menus).
 *
 * v1.7.4 hotfix — live sampling REMOVED from popups. In v1.7.3 this
 * sampled a backdrop recorded by the navigation-host wrapper, switched
 * on on-demand when the first consumer (a menu) composed. That wiring
 * is structurally unsafe: the menu itself is composed inside the
 * recorded subtree (each screen's Scaffold popup host lives inside
 * NavigationHost), so the consumer sat inside its own recording — the
 * wrapper's record pass re-drew the popup, whose glass then drew the
 * very layer being recorded — and the recording node was attached to /
 * detached from a live ancestor of the open popup at runtime. On device
 * every ⋮ menu crashed on open. See the v1.7.4 notes.
 *
 * What remains is the prescribed degradation, and nothing else: the
 * resolved spec's tint, clipped to the face shape, painted over
 * whatever background the container itself draws. No backdrop is read,
 * no state is flipped, nothing on the menu-open path can throw. A
 * future re-land of true menu glass needs a backdrop recorded by a
 * subtree that provably does not contain the popup host (e.g. recorded
 * inside each screen's content root, like the dock's) — not a
 * shell-level recording.
 *
 * [lensScale] / [debugColor] are kept in the signature so call sites
 * stay unchanged; they only mattered to the removed sampling chain.
 */
@Composable
fun GlassPanelBackground(
    shape: Shape,
    @Suppress("UNUSED_PARAMETER") lensScale: Float,
    @Suppress("UNUSED_PARAMETER") debugColor: Color,
    modifier: Modifier = Modifier,
) {
    val glass = rememberGlassSettings()
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(glassTintColor(glass.spec, glass.scheme), shape),
    )
}
