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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugenzhao.mnga.storage.ColorSchemeMode
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

private enum class DockScheme { Dark, Light, Classic }

private data class DockColors(
    val glass: Color,
    val border: Color,
    val pill: Color,
    val unselected: Color,
    val highlight: Color,
)

/**
 * Palette of the dock per scheme, following the "frosted glass" recipe
 * (thick milky veil, bright rim, soft shadow, inset top highlight). There
 * is no backdrop-blur module available in the pinned Miuix 0.8.8, and Haze
 * 2.0.1/1.7.3 both resolve to JetBrains Compose 1.12 artifacts
 * (org.jetbrains.compose.ui, no capability metadata to dedupe against this
 * AndroidX-Compose build) plus a newer Kotlin stdlib than the project's —
 * a duplicate-class/linkage risk that cannot be compile-verified locally,
 * so the veil is an opaque-ish milky fill instead of a sampled blur. Two
 * recipe knobs therefore have no equivalent in this pass and are noted as
 * gaps: the backdrop blur radius (recipe ≈ CSS 18–26px, i.e. ~20–24dp —
 * the value to use if a real blur lands later) and the ~1.5x saturation
 * boost of the sampled backdrop (the recipe's key "premium glass" knob;
 * with no sampling there is nothing to saturate).
 */
private fun dockColors(scheme: DockScheme): DockColors = when (scheme) {
    DockScheme.Dark -> DockColors(
        glass = Color(0xFFD4D5DB).copy(alpha = 0.48f),
        border = Color.White.copy(alpha = 0.28f),
        pill = Color.Black.copy(alpha = 0.18f),
        unselected = Color(0xFFF2F2F7),
        highlight = Color.White.copy(alpha = 0.30f),
    )
    DockScheme.Light -> DockColors(
        glass = Color.White.copy(alpha = 0.62f),
        border = Color.Black.copy(alpha = 0.07f),
        pill = Color.Black.copy(alpha = 0.10f),
        unselected = Color(0xFF3A3A3C),
        highlight = Color.White.copy(alpha = 0.50f),
    )
    DockScheme.Classic -> DockColors(
        glass = Color(0xFFFFF6DF).copy(alpha = 0.88f),
        border = Color(0xFFD9C48F).copy(alpha = 0.8f),
        pill = Color(0xFF4A3B18).copy(alpha = 0.14f),
        unselected = Color(0xFF6B5D3E),
        highlight = Color.White.copy(alpha = 0.45f),
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
    modifier: Modifier = Modifier,
) {
    val scheme = when (colorSchemeMode) {
        ColorSchemeMode.CLASSIC -> DockScheme.Classic
        ColorSchemeMode.DARK -> DockScheme.Dark
        ColorSchemeMode.LIGHT -> DockScheme.Light
        ColorSchemeMode.AUTO ->
            if (isSystemInDarkTheme()) DockScheme.Dark else DockScheme.Light
    }
    val colors = dockColors(scheme)
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
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.border, shape),
    ) {
        // Inset top highlight: a soft light band just inside the capsule's
        // top edge, fading down — the frosted-glass recipe's rim light.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(14.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.highlight, Color.Transparent),
                    ),
                ),
        )
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
