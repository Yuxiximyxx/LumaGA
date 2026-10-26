package com.bugenzhao.mnga.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.ui.root.LocalScreenBackdrop
import com.bugenzhao.mnga.ui.root.rememberGlassSettings
import com.bugenzhao.mnga.ui.root.withLensScale

/**
 * The background layer of a floating overlay face (menus).
 *
 * v1.7.6 — true glass again, on the Dock-standard architecture this
 * time (see ScreenBackdrop.kt). The panel samples the backdrop the
 * HOST SCREEN records from its own scrolling content root for its
 * whole composition lifetime: the recorder predates the menu (the
 * texture is warm before the first menu can open — no first-frame
 * sampling switch-on), the recording node never attaches or detaches
 * with the menu, and the consumer — the Scaffold popup host's content —
 * is a sibling of the recorded content root, never inside it. That is
 * exactly the structure the dock, the topic-list FAB and the details
 * page capsule already run on.
 *
 * History, so the wiring is not "simplified" back into the crash:
 * v1.7.3 sampled a backdrop recorded by the navigation-host wrapper,
 * switched ON when the first consumer (a menu) composed — the menu
 * sat inside its own recording (each screen's Scaffold popup host
 * lives inside NavigationHost), so the wrapper's record pass re-drew
 * the popup, whose glass drew the very layer being recorded, and the
 * recording node was attached to / detached from a live ancestor of
 * the open popup at runtime. Every ⋮ menu crashed on open (v1.7.4
 * removed it and fell back to tint).
 *
 * Degradation, by construction: on screens that do not publish a
 * screen backdrop (anchors inside sheets/dialogs, unconverted
 * screens) [LocalScreenBackdrop] is null and the panel is the
 * resolved spec's tint over the library's own panel background — the
 * v1.7.4 form, which cannot throw and never shows a hole. Sampling
 * itself flows through [liquidGlass], whose own fallbacks (no
 * effects below API 31) apply unchanged.
 */
@Composable
fun GlassPanelBackground(
    shape: Shape,
    lensScale: Float,
    debugColor: Color,
    modifier: Modifier = Modifier,
) {
    val glass = rememberGlassSettings()
    val backdrop = LocalScreenBackdrop.current
    if (backdrop != null) {
        Box(
            modifier
                .fillMaxSize()
                .liquidGlass(
                    backdrop = backdrop,
                    shape = shape,
                    spec = glass.spec.withLensScale(lensScale),
                    scheme = glass.scheme,
                    shadowRadius = 12.dp,
                    debugShowSample = glass.debugShowSample,
                    debugColor = debugColor,
                ),
        )
    } else {
        Box(
            modifier
                .fillMaxSize()
                .clip(shape)
                .background(glassTintColor(glass.spec, glass.scheme), shape),
        )
    }
}
