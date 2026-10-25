package com.bugenzhao.mnga.ui.root

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugenzhao.mnga.storage.ColorSchemeMode
import com.bugenzhao.mnga.storage.DockGlassMaterial
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlin.math.roundToInt

/** Height of the floating tab dock capsule. */
val TabDockHeight = 68.dp

/** Gap between the dock's bottom edge and the system navigation-bar area. */
val TabDockBottomGap = 10.dp

/**
 * Bottom content padding tab-root scroll containers need so their last item
 * can scroll fully clear of the floating dock: dock height + its bottom gap
 * + breathing room + the device's own navigation-bar inset (the dock floats
 * above that inset via navigationBarsPadding in the root shell).
 */
@Composable
fun tabDockContentBottomPadding(): Dp =
    TabDockHeight + TabDockBottomGap + 16.dp +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

internal enum class DockScheme { Dark, Light, Classic }

internal data class DockColors(
    val glass: Color,
    val border: Color,
    val pill: Color,
    val unselected: Color,
    /** Top inset highlight (brightest right at the capsule's top edge). */
    val highlight: Color,
    /** Height of the top highlight band; narrower reads as sharper. */
    val highlightHeight: Dp = 14.dp,
    /** Brightest color of the bottom-edge highlight line; Transparent = off. */
    val bottomHighlight: Color = Color.Transparent,
    /** Tint of the diagonal sheen wash (top-left); Transparent = off. */
    val sheen: Color = Color.Transparent,
)

/**
 * Veil/rim palette of the dock per color scheme × glass material. The
 * sampled backdrop (blur / refraction / saturation) comes from Prismal —
 * see [dockGlassEffectParams] — and these colors are drawn over it: the
 * milky veil via the glass modifier's surface hook, plus the rim and
 * highlight bands. The three materials of wave2234/liquid-glass-recipes:
 *
 * - FROSTED （磨砂玻璃， the default): the thick milky veil and soft, wide
 *   top highlight the dock shipped with in v1.7.0 — its values below are
 *   that baseline, unchanged.
 * - CLEAR （毛玻璃）: veil thinned to about half of frosted, a brighter and
 *   narrower (sharper) top highlight and a more luminous rim — airier.
 * - LIQUID （液态玻璃）: veil thinned to about half of clear again, the
 *   strongest rim, highlight lines on BOTH the top and bottom edges and a
 *   faint diagonal sheen wash over the (on API 33+) truly refractive
 *   sample.
 */
internal fun dockColors(
    scheme: DockScheme,
    material: DockGlassMaterial,
): DockColors = when (scheme) {
    DockScheme.Dark -> when (material) {
        DockGlassMaterial.FROSTED -> DockColors(
            glass = Color(0xFFD4D5DB).copy(alpha = 0.48f),
            border = Color.White.copy(alpha = 0.28f),
            pill = Color.Black.copy(alpha = 0.18f),
            unselected = Color(0xFFF2F2F7),
            highlight = Color.White.copy(alpha = 0.30f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            glass = Color(0xFFD4D5DB).copy(alpha = 0.24f),
            border = Color.White.copy(alpha = 0.45f),
            pill = Color.Black.copy(alpha = 0.14f),
            unselected = Color(0xFFF2F2F7),
            highlight = Color.White.copy(alpha = 0.55f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
            glass = Color(0xFFD4D5DB).copy(alpha = 0.13f),
            border = Color.White.copy(alpha = 0.60f),
            pill = Color.Black.copy(alpha = 0.12f),
            unselected = Color(0xFFF2F2F7),
            highlight = Color.White.copy(alpha = 0.65f),
            highlightHeight = 8.dp,
            bottomHighlight = Color.White.copy(alpha = 0.35f),
            sheen = Color.White.copy(alpha = 0.10f),
        )
    }
    DockScheme.Light -> when (material) {
        DockGlassMaterial.FROSTED -> DockColors(
            glass = Color.White.copy(alpha = 0.62f),
            border = Color.Black.copy(alpha = 0.07f),
            pill = Color.Black.copy(alpha = 0.10f),
            unselected = Color(0xFF3A3A3C),
            highlight = Color.White.copy(alpha = 0.50f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            glass = Color.White.copy(alpha = 0.32f),
            border = Color.Black.copy(alpha = 0.12f),
            pill = Color.Black.copy(alpha = 0.08f),
            unselected = Color(0xFF3A3A3C),
            highlight = Color.White.copy(alpha = 0.80f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
            glass = Color.White.copy(alpha = 0.16f),
            border = Color.White.copy(alpha = 0.55f),
            pill = Color.Black.copy(alpha = 0.08f),
            unselected = Color(0xFF3A3A3C),
            highlight = Color.White.copy(alpha = 0.90f),
            highlightHeight = 8.dp,
            bottomHighlight = Color.White.copy(alpha = 0.50f),
            sheen = Color.White.copy(alpha = 0.18f),
        )
    }
    DockScheme.Classic -> when (material) {
        DockGlassMaterial.FROSTED -> DockColors(
            glass = Color(0xFFFFF6DF).copy(alpha = 0.88f),
            border = Color(0xFFD9C48F).copy(alpha = 0.8f),
            pill = Color(0xFF4A3B18).copy(alpha = 0.14f),
            unselected = Color(0xFF6B5D3E),
            highlight = Color.White.copy(alpha = 0.45f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            glass = Color(0xFFFFF6DF).copy(alpha = 0.45f),
            border = Color(0xFFD9C48F).copy(alpha = 0.95f),
            pill = Color(0xFF4A3B18).copy(alpha = 0.12f),
            unselected = Color(0xFF6B5D3E),
            highlight = Color.White.copy(alpha = 0.65f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
            glass = Color(0xFFFFF6DF).copy(alpha = 0.22f),
            border = Color(0xFFE9D9A8).copy(alpha = 0.95f),
            pill = Color(0xFF4A3B18).copy(alpha = 0.10f),
            unselected = Color(0xFF6B5D3E),
            highlight = Color.White.copy(alpha = 0.75f),
            highlightHeight = 8.dp,
            bottomHighlight = Color.White.copy(alpha = 0.40f),
            sheen = Color.White.copy(alpha = 0.14f),
        )
    }
}

/**
 * Prismal effect-chain parameters per glass material — the part of each
 * recipe that acts on the sampled backdrop (the root shell's content
 * layer). Prismal degrades them by device tier automatically: API 33+
 * runs the full AGSL chain (blur + edge refraction + chromatic
 * aberration), API 31–32 keeps the RenderEffect blur and color grading,
 * API 25–30 turns the blur request into a legacy frost overlay.
 *
 * - FROSTED: the recipes' big blur (CSS 18–26px ≈ 22dp) under the thick
 *   veil; no saturation boost and no specular layer — matte.
 * - CLEAR: medium blur (CSS 12–18px ≈ 13dp), the recipe's key knob —
 *   saturation 1.5 on the sample — and the library's specular rim light.
 * - LIQUID: the recipes' tiny blur (CSS 2–4px ≈ 3dp) so the backdrop
 *   stays readable, edge refraction at PrismalLiquidGlass.applyBase's
 *   calibrated 16/32dp lens with a touch of chromatic aberration,
 *   saturation 1.6 and the specular rim light.
 */
internal data class DockGlassEffectParams(
    val blurDp: Dp,
    val saturation: Float,
    val refractionHeightDp: Dp,
    val refractionAmountDp: Dp,
    val chromaticAberration: Float,
    val specular: Boolean,
)

internal fun dockGlassEffectParams(material: DockGlassMaterial): DockGlassEffectParams =
    when (material) {
        DockGlassMaterial.FROSTED -> DockGlassEffectParams(
            blurDp = 22.dp,
            saturation = 1.0f,
            refractionHeightDp = 0.dp,
            refractionAmountDp = 0.dp,
            chromaticAberration = 0f,
            specular = false,
        )
        DockGlassMaterial.CLEAR -> DockGlassEffectParams(
            blurDp = 13.dp,
            saturation = 1.5f,
            refractionHeightDp = 0.dp,
            refractionAmountDp = 0.dp,
            chromaticAberration = 0f,
            specular = true,
        )
        DockGlassMaterial.LIQUID -> DockGlassEffectParams(
            blurDp = 3.dp,
            saturation = 1.6f,
            refractionHeightDp = 16.dp,
            refractionAmountDp = 32.dp,
            chromaticAberration = 0.15f,
            specular = true,
        )
    }

/**
 * The floating tab dock of the main shell (v1.7.0), replacing the full-width
 * Miuix NavigationBar: a capsule about 58% of the screen width, horizontally
 * centered, hovering above the content (which scrolls underneath it) just
 * above the system gesture area. The selected tab is a dark oval holding an
 * accent-tinted icon stacked over its label; the unselected tab shows its
 * icon only. The oval slides between the two tabs. Tab semantics (which
 * routes count as roots, push/pop behavior) live in [MainTabBar] and are
 * unchanged from the bar this replaces.
 */
@Composable
fun FloatingTabDock(
    homeSelected: Boolean,
    onHomeClick: () -> Unit,
    onMineClick: () -> Unit,
    colorSchemeMode: ColorSchemeMode,
    glassMaterial: DockGlassMaterial,
    backdrop: PrismalGlassLayer,
    modifier: Modifier = Modifier,
) {
    // Subscribe to the backdrop layer's position so the glass redraws once
    // the recorded content layer is placed (Prismal's sampling contract).
    backdrop.readSamplingState()
    val scheme = when (colorSchemeMode) {
        ColorSchemeMode.CLASSIC -> DockScheme.Classic
        ColorSchemeMode.DARK -> DockScheme.Dark
        ColorSchemeMode.LIGHT -> DockScheme.Light
        ColorSchemeMode.AUTO ->
            if (isSystemInDarkTheme()) DockScheme.Dark else DockScheme.Light
    }
    val colors = dockColors(scheme, glassMaterial)
    val effectParams = dockGlassEffectParams(glassMaterial)
    val density = LocalDensity.current
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(percent = 50)
    val pillFraction by animateFloatAsState(
        targetValue = if (homeSelected) 0f else 1f,
        animationSpec = tween(260),
        label = "tabDockPill",
    )
    Box(
        modifier
            .fillMaxWidth(0.58f)
            .widthIn(max = 380.dp)
            .height(TabDockHeight)
            .shadow(16.dp, shape, clip = false)
            // The glass itself: Prismal samples the recorded root content
            // (blur / saturation / refraction per material) and the milky
            // veil tint is drawn over the sample as its surface. The
            // library clips the whole element to the capsule, so the tint
            // cannot spill outside it. Our own depth shadow above stays —
            // the library's depth shadow is disabled to keep one shadow.
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = { PrismalCapsule() },
                effects = {
                    applyPrismalGlassEffects(
                        density = density,
                        adaptiveLuminance = false,
                        luminance = 0.5f,
                        blurRadiusPx = with(density) { effectParams.blurDp.toPx() },
                        refractionHeightPx = with(density) {
                            effectParams.refractionHeightDp.toPx()
                        },
                        refractionAmountPx = with(density) {
                            effectParams.refractionAmountDp.toPx()
                        },
                        saturation = effectParams.saturation,
                        chromaticAberration = effectParams.chromaticAberration,
                        useVibrancy = false,
                    )
                },
                specular = if (effectParams.specular) {
                    { PrismalSpecular.Default }
                } else {
                    null
                },
                depthShadow = null,
                onDrawSurface = { drawRect(colors.glass) },
            )
            .clip(shape)
            .border(1.dp, colors.border, shape),
    ) {
        // Diagonal sheen wash (liquid glass only; Transparent otherwise):
        // a faint gloss running from the top-left corner, reinforcing the
        // liquid recipe's reflective surface over the refracted sample.
        if (colors.sheen.alpha > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(colors.sheen, Color.Transparent),
                        ),
                    ),
            )
        }
        // Inset top highlight: a soft light band just inside the capsule's
        // top edge, fading down — the glass recipes' rim light. Its height
        // and brightness are part of the material (see dockColors).
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(colors.highlightHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.highlight, Color.Transparent),
                    ),
                ),
        )
        // Bottom-edge highlight line (liquid glass only; Transparent
        // otherwise): brightest right at the capsule's bottom edge, the
        // second rim light that makes the liquid surface read as wet.
        if (colors.bottomHighlight.alpha > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colors.bottomHighlight),
                        ),
                    ),
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val itemWidth = maxWidth / 2
            // Sliding selection oval, drawn under the items.
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            x = (pillFraction * itemWidth.toPx()).roundToInt(),
                            y = 0,
                        )
                    }
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(colors.pill),
            )
            Row(Modifier.fillMaxSize()) {
                DockItem(
                    selected = homeSelected,
                    icon = Icons.Filled.Home,
                    label = "主页",
                    accent = accent,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = onHomeClick,
                )
                DockItem(
                    selected = !homeSelected,
                    icon = Icons.Filled.Person,
                    label = "我的",
                    accent = accent,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = onMineClick,
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    accent: Color,
    colors: DockColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else colors.unselected,
                modifier = Modifier.size(22.dp),
            )
            if (selected) {
                Text(
                    text = label,
                    color = accent,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
