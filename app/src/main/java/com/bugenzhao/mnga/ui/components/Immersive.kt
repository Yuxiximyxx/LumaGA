package com.bugenzhao.mnga.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Scaffold variant for edge-to-edge immersive layout.
 *
 * The window already draws behind the system bars (see `enableEdgeToEdge`
 * in MainActivity). The default Scaffold pads its content above the
 * navigation bar; this variant excludes the navigation bar from the content
 * insets so scrollable content slides *under* the transparent navigation bar.
 *
 * Screens using this must add [immersiveBottomPadding] to their scrollable's
 * `contentPadding` so the last item can still scroll fully above the bar.
 */
@Composable
fun ImmersiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        content = content,
    )
}

/**
 * Bottom padding equal to the navigation bar height. Add to a scrollable's
 * `contentPadding` (e.g. `contentPadding + immersiveBottomPadding()`) when
 * the screen uses [ImmersiveScaffold] so trailing content isn't permanently
 * hidden behind the bar.
 */
@Composable
fun immersiveBottomPadding(): PaddingValues =
    WindowInsets.navigationBars.asPaddingValues()

/**
 * Combine two [PaddingValues] by summing each side.
 */
@Composable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection) +
                other.calculateStartPadding(layoutDirection),
        top = calculateTopPadding() + other.calculateTopPadding(),
        end = calculateEndPadding(layoutDirection) +
                other.calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + other.calculateBottomPadding(),
    )
}
