package com.bugenzhao.mnga.ui.root

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugenzhao.mnga.storage.DockGlassMaterial
import com.bugenzhao.mnga.ui.components.liquidGlass
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlin.math.roundToInt

/** Height of the floating tab dock capsule. */
val TabDockHeight = 68.dp

/** Width of one tab slot inside the dock capsule. */
val TabDockSlotWidth = 72.dp

/** Horizontal padding between the capsule edge and the first/last slot. */
val TabDockInnerPadding = 6.dp

/** Absolute cap on the dock width, however many tabs it holds. */
val TabDockMaxWidth = 380.dp

/**
 * Dock width rule (v1.7.2, unified across the sister forks): the
 * capsule hugs its tabs — [tabCount] slots of [TabDockSlotWidth] plus
 * the inner padding on both sides — capped at the screen width minus
 * 32dp and at [TabDockMaxWidth]. Fewer tabs ⇒ a narrower capsule (the
 * old fixed ≈58%-of-screen width squeezed many-tab docks and left
 * two-tab docks mostly empty).
 */
fun tabDockWidth(tabCount: Int, screenWidth: Dp): Dp {
    val natural = TabDockSlotWidth * tabCount + TabDockInnerPadding * 2
    val screenCap = (screenWidth - 32.dp).coerceAtLeast(0.dp)
    return minOf(natural, screenCap, TabDockMaxWidth)
}

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

enum class DockScheme { Dark, Light, Classic }
internal data class DockColors(
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
 * Rim/pill palette of the dock per color scheme × glass material. The
 * sampled backdrop and the veil tint come from the resolved [GlassSpec]
 * (see GlassSpec.kt for the calibration table); these colors are drawn
 * over the glass: the selection pill, the border and the rim-light bands
 * (whose alphas the user's highlight trim scales at draw time).
 *
 * - FROSTED （磨砂玻璃， the default): the soft, wide top highlight and
 *   the faintest rim — matte.
 * - CLEAR （毛玻璃）: a brighter and narrower (sharper) top highlight and
 *   a more luminous rim — airier.
 * - LIQUID （液态玻璃）: the strongest rim, highlight lines on BOTH the
 *   top and bottom edges and a faint diagonal sheen wash over the
 *   refracted sample.
 */
internal fun dockColors(
    scheme: DockScheme,
    material: DockGlassMaterial,
): DockColors = when (scheme) {
    DockScheme.Dark -> when (material) {
        DockGlassMaterial.FROSTED -> DockColors(
            border = Color.White.copy(alpha = 0.28f),
            pill = Color.Black.copy(alpha = 0.18f),
            unselected = Color(0xFFF2F2F7),
            highlight = Color.White.copy(alpha = 0.30f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            border = Color.White.copy(alpha = 0.45f),
            pill = Color.Black.copy(alpha = 0.14f),
            unselected = Color(0xFFF2F2F7),
            highlight = Color.White.copy(alpha = 0.55f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
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
            border = Color.Black.copy(alpha = 0.07f),
            pill = Color.Black.copy(alpha = 0.10f),
            unselected = Color(0xFF3A3A3C),
            highlight = Color.White.copy(alpha = 0.50f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            border = Color.Black.copy(alpha = 0.12f),
            pill = Color.Black.copy(alpha = 0.08f),
            unselected = Color(0xFF3A3A3C),
            highlight = Color.White.copy(alpha = 0.80f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
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
            border = Color(0xFFD9C48F).copy(alpha = 0.8f),
            pill = Color(0xFF4A3B18).copy(alpha = 0.14f),
            unselected = Color(0xFF6B5D3E),
            highlight = Color.White.copy(alpha = 0.45f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            border = Color(0xFFD9C48F).copy(alpha = 0.95f),
            pill = Color(0xFF4A3B18).copy(alpha = 0.12f),
            unselected = Color(0xFF6B5D3E),
            highlight = Color.White.copy(alpha = 0.65f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
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
 * The floating tab dock of the main shell (v1.7.0), replacing the full-width
 * Miuix NavigationBar: a capsule whose width adapts to its tab count (see
 * [tabDockWidth]), horizontally centered, hovering above the content
 * (which scrolls underneath it) just above the system gesture area.
 * The selected tab is a dark oval holding an
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
    glassMaterial: DockGlassMaterial,
    spec: GlassSpec,
    scheme: DockScheme,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    debugShowSample: Boolean = false,
) {
    val colors = dockColors(scheme, glassMaterial)
    // The user's highlight trim scales the dock's own rim-light bands on
    // top of the per-material palette (the Kyant specular highlight got
    // the same trim inside the resolved spec).
    val hlScale = spec.highlightScale
    val topHighlight = colors.highlight.copy(
        alpha = (colors.highlight.alpha * hlScale).coerceAtMost(1f),
    )
    val bottomHighlight = colors.bottomHighlight.copy(
        alpha = (colors.bottomHighlight.alpha * hlScale).coerceAtMost(1f),
    )
    val sheen = colors.sheen.copy(
        alpha = (colors.sheen.alpha * hlScale).coerceAtMost(1f),
    )
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(percent = 50)
    val pillFraction by animateFloatAsState(
        targetValue = if (homeSelected) 0f else 1f,
        animationSpec = tween(260),
        label = "tabDockPill",
    )
    val tabCount = 2
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current
        .screenWidthDp.dp
    Box(
        modifier
            .width(tabDockWidth(tabCount, screenWidth))
            .height(TabDockHeight)
            // The glass itself: Kyant samples the recorded root content
            // (saturation / blur / lens per the resolved spec) and draws
            // the veil tint over the sample as its surface; the rim and
            // highlight bands below are drawn over that.
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                spec = spec,
                scheme = scheme,
                shadowRadius = 16.dp,
                debugShowSample = debugShowSample,
                debugColor = Color.Magenta,
            )
            .clip(shape)
            .border(1.dp, colors.border, shape),
    ) {
        // Diagonal sheen wash (liquid glass only; Transparent otherwise):
        // a faint gloss running from the top-left corner, reinforcing the
        // liquid recipe's reflective surface over the refracted sample.
        if (sheen.alpha > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(sheen, Color.Transparent),
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
                        listOf(topHighlight, Color.Transparent),
                    ),
                ),
        )
        // Bottom-edge highlight line (liquid glass only; Transparent
        // otherwise): brightest right at the capsule's bottom edge, the
        // second rim light that makes the liquid surface read as wet.
        if (bottomHighlight.alpha > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, bottomHighlight),
                        ),
                    ),
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(TabDockInnerPadding)) {
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
