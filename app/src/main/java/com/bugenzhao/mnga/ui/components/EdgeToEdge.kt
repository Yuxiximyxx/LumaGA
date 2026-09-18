package com.bugenzhao.mnga.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Content window insets for immersive Scaffolds: safe drawing on horizontal + top
 * (status bars / display cutouts) but **not** the navigation bar, so content can
 * draw under the system nav bar. TopAppBar still consumes statusBars via Scaffold.
 *
 * Lists and scrollable content should add [navigationBarsBottom] /
 * [withNavigationBarsBottom] to their `contentPadding` (or use
 * [androidx.compose.foundation.layout.navigationBarsPadding] on fixed controls
 * like FABs) so interactive controls and list ends remain reachable — do **not**
 * also pad the Scaffold content lambda for the nav bar, or you will double-pad.
 *
 * Typical usage:
 * ```
 * Scaffold(contentWindowInsets = ImmersiveScaffoldContentWindowInsets) { padding ->
 *     LazyColumn(
 *         Modifier.padding(padding),
 *         contentPadding = PaddingValues(...).withNavigationBarsBottom(),
 *     ) { ... }
 * }
 * ```
 */
val ImmersiveScaffoldContentWindowInsets: WindowInsets
    @Composable
    get() = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
    )

/** Bottom inset of the system navigation bars, as [Dp]. */
@Composable
@ReadOnlyComposable
fun navigationBarsBottom(): Dp {
    val density = LocalDensity.current
    return with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
}

/**
 * Returns a copy of this [PaddingValues] with the navigation-bars bottom inset
 * (plus optional [extra]) added to `bottom`. Uses [LocalLayoutDirection] for
 * start/end so RTL layouts stay correct.
 */
@Composable
fun PaddingValues.withNavigationBarsBottom(extra: Dp = 0.dp): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val navBottom = navigationBarsBottom()
    return PaddingValues(
        start = calculateStartPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + navBottom + extra,
    )
}
