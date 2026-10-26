package com.bugenzhao.mnga.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.ui.root.DockScheme
import com.bugenzhao.mnga.ui.root.GlassSpec
import com.bugenzhao.mnga.ui.root.glassTintBase
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import kotlin.math.min

/** The veil color a glass surface paints over its sampled backdrop. */
fun glassTintColor(spec: GlassSpec, scheme: DockScheme): Color =
    glassTintBase(scheme).copy(alpha = spec.tintAlpha(scheme))

/**
 * Liquid glass over [backdrop] (a Kyant layer recording of the content
 * beneath), following the library's effect order — color filter
 * (saturation) ⇒ blur ⇒ lens — with its specular [Highlight] and the
 * material's veil tint as the surface. The cast shadow is a plain
 * Compose shadow (Kyant's own shadow element stays off) so every glass
 * surface in the app shares one shadow language.
 *
 * Degradation: effects need API 31+ (blur/saturation) and the lens needs
 * API 33+ — Kyant no-ops the unsupported steps itself. Below API 31 the
 * surface falls back to a denser plain tint (a frost approximation with
 * no sampled backdrop at all).
 */
fun Modifier.liquidGlass(
    backdrop: LayerBackdrop,
    shape: Shape,
    spec: GlassSpec,
    scheme: DockScheme,
    shadowRadius: Dp = 12.dp,
): Modifier {
    val tint = glassTintColor(spec, scheme)
    val base = shadow(shadowRadius, shape, clip = false)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        base.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (spec.saturation != 1f) {
                    colorControls(saturation = spec.saturation)
                }
                if (spec.blurDp > 0f) {
                    blur(spec.blurDp.toPx())
                }
                if (spec.refractionAmountDp > 0f && spec.refractionHeightDp > 0f) {
                    // The lens SDF needs the band within the corner radius
                    // and the amount within the surface's small side;
                    // clamp both so small buttons stay artifact-free.
                    lens(
                        refractionHeight = min(
                            spec.refractionHeightDp.toPx(),
                            size.minDimension / 2f,
                        ),
                        refractionAmount = min(
                            spec.refractionAmountDp.toPx(),
                            size.minDimension,
                        ),
                        chromaticAberration = spec.chromaticAberration,
                    )
                }
            },
            highlight = { Highlight(alpha = spec.highlightAlpha) },
            shadow = { null },
            onDrawSurface = { drawRect(tint) },
        )
    } else {
        base.background(
            tint.copy(alpha = (tint.alpha * 1.5f).coerceAtMost(0.95f)),
            shape,
        )
    }
}

/**
 * A floating glass button: [size] × [size] (or as given) of liquid glass
 * with centered content. Used for the topic-list refresh FAB, the
 * page-indicator siblings and the image viewer's overlay buttons — all
 * following the one glass material setting.
 */
@Composable
fun GlassButton(
    backdrop: LayerBackdrop,
    spec: GlassSpec,
    scheme: DockScheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .liquidGlass(backdrop, shape, spec, scheme, shadowRadius = 8.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
