package com.bugenzhao.mnga.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.ui.root.GlassOverlayRegistration
import com.bugenzhao.mnga.ui.root.LocalContentBackdrop
import com.bugenzhao.mnga.ui.root.rememberGlassSettings
import com.bugenzhao.mnga.ui.root.withLensScale

/**
 * The glass skin of a floating overlay face (v1.7.3): a full-size layer
 * meant to sit *under* a face's own content, sampling the recorded
 * content backdrop (see GlassOverlayBackdrop.kt for who records, and
 * when) through the shared [liquidGlass] chain — the resolved spec of
 * the user's glass material, its lens weakened by [lensScale] per the
 * face's size class (menus: [com.bugenzhao.mnga.ui.root.GlassVariantScale.MEDIUM]).
 *
 * Composing this also registers the face as a backdrop consumer, which
 * is what switches the shell's on-demand recording on; the face's own
 * composition lifetime (for Miuix popups: exactly the shown period,
 * exit animation included) bounds the recording.
 *
 * Only usable for faces rendered in the main window; where no content
 * backdrop is provided this draws nothing and the face keeps whatever
 * background its container paints.
 */
@Composable
fun GlassPanelBackground(
    shape: Shape,
    lensScale: Float,
    debugColor: Color,
    modifier: Modifier = Modifier,
) {
    val backdrop = LocalContentBackdrop.current ?: return
    val glass = rememberGlassSettings()
    GlassOverlayRegistration()
    Box(
        modifier
            .fillMaxSize()
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                spec = glass.spec.withLensScale(lensScale),
                scheme = glass.scheme,
                shadowRadius = 0.dp,
                debugShowSample = glass.debugShowSample,
                debugColor = debugColor,
            ),
    )
}
