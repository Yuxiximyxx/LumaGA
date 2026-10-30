package com.bugenzhao.mnga.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 *
 * [shadowColor] optionally pins the cast shadow's ambient/spot color
 * (the tab dock's aligned rim model uses it — see MainTabDock); null
 * keeps the plain default-color shadow every other surface shares.
 */
fun Modifier.liquidGlass(
    backdrop: LayerBackdrop,
    shape: Shape,
    spec: GlassSpec,
    scheme: DockScheme,
    shadowRadius: Dp = 12.dp,
    shadowColor: Color? = null,
    debugShowSample: Boolean = false,
    debugColor: Color = Color.Magenta,
): Modifier {
    val tint = glassTintColor(spec, scheme)
    val base = if (shadowColor != null) {
        shadow(
            shadowRadius, shape, clip = false,
            ambientColor = shadowColor, spotColor = shadowColor,
        )
    } else {
        shadow(shadowRadius, shape, clip = false)
    }
    if (debugShowSample && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Diagnostic mode (settings → Glass Debug): draw the recorded
        // backdrop texture raw — no saturation, blur, lens or tint —
        // with a bright outline. If the texture inside the outline
        // scrolls in sync with, and aligns seamlessly against, the
        // content around the surface, sampling is healthy and any
        // remaining "fake" look lives in the effect mix; a frozen,
        // shifted or wrong texture is visible immediately.
        return base
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {},
                highlight = { null },
                shadow = { null },
                onDrawSurface = {},
            )
            .border(2.dp, debugColor, shape)
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        base.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (spec.saturation != 1f) {
                    colorControls(saturation = spec.saturation)
                }
                if (spec.blurDp > 0f) {
                    blur(spec.blurDp.dp.toPx())
                }
                if (spec.refractionAmountDp > 0f && spec.refractionHeightDp > 0f) {
                    // The lens SDF needs the band within the corner radius
                    // and the amount within the surface's small side;
                    // clamp both so small buttons stay artifact-free.
                    lens(
                        refractionHeight = min(
                            spec.refractionHeightDp.dp.toPx(),
                            size.minDimension / 2f,
                        ),
                        refractionAmount = min(
                            spec.refractionAmountDp.dp.toPx(),
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
 * The NON-sampling frosted material: the same veil hue as [liquidGlass]
 * at the denser alpha the API < 31 fallback uses, plus a rim border
 * standing in for the lens edge the sampled version gets for free, and
 * the shared cast shadow. Nothing is recorded and nothing is sampled,
 * so the surface is deterministic by construction — it can only ever
 * show its own veil over whatever the window compositor puts beneath
 * it, never a texture, a stale frame or a shader artifact.
 *
 * Used where sampling has proven unreliable on device despite a
 * structurally sound backdrop: the topic-details header circles and
 * round reply button (v1.8.7). Their backdrop is a flat page-color
 * recording by crash-isolation design, yet under the liquid preset
 * (24/24dp lens + dispersion) the Kyant effect chain rendered faceted
 * ghosts and dispersed color wedges inside them — the header pair also
 * sits outside the recording's bounds — and the library exposes no
 * sample clamp/offset control to correct the mapping. Over the
 * details page's near-uniform background the frosted read is visually
 * the same material minus the artifacts.
 *
 * v1.8.8 dense-veil rule, decoupled from the sampled spec: the old
 * alpha (spec × 1.5 = 0.48 under the liquid preset) has NO blur
 * behind it, so whatever sits under the disc — post card edges and
 * text, even the disc's own cast shadow seen back through the fill —
 * read through sharply and pooled into a bright polygonal patch on
 * device (v1.8.7 still showed it). The veil now has a per-scheme
 * opacity floor of ~0.9, so the disc reads as one uniform milky
 * circle and nothing beneath it can show a shape through it.
 */
fun Modifier.frostedGlass(
    shape: Shape,
    spec: GlassSpec,
    scheme: DockScheme,
    shadowRadius: Dp = 12.dp,
): Modifier {
    val tint = glassTintColor(spec, scheme)
    val frostAlpha = maxOf(
        tint.alpha * 1.5f,
        when (scheme) {
            DockScheme.Dark -> 0.90f
            DockScheme.Light -> 0.92f
            DockScheme.Classic -> 0.92f
        },
    ).coerceAtMost(0.97f)
    val frost = tint.copy(alpha = frostAlpha)
    val rim = when (scheme) {
        DockScheme.Dark -> Color.White.copy(alpha = 0.28f)
        DockScheme.Light -> Color.White.copy(alpha = 0.55f)
        DockScheme.Classic -> Color.White.copy(alpha = 0.5f)
    }
    return this
        .shadow(shadowRadius, shape, clip = false)
        .background(frost, shape)
        .border(1.dp, rim, shape)
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
    debugShowSample: Boolean = false,
    debugColor: Color = Color.Magenta,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .liquidGlass(
                backdrop, shape, spec, scheme,
                shadowRadius = 8.dp,
                debugShowSample = debugShowSample,
                debugColor = debugColor,
            )
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * [GlassButton]'s non-sampling sibling on the [frostedGlass] material:
 * same geometry and shadow language, no backdrop. Used by the
 * topic-details round reply button (v1.8.7 — see [frostedGlass]).
 */
@Composable
fun FrostedGlassButton(
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
            .frostedGlass(shape, spec, scheme, shadowRadius = 8.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
