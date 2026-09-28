package com.bugenzhao.mnga.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
 * Note: containerColor defaults to [Color.Unspecified], in which case
 * Scaffold's own default is used. This avoids @Composable calls in default
 * parameter values.
 */
@Composable
fun ImmersiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    content: @Composable (PaddingValues) -> Unit,
) {
    val insets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars)
    if (containerColor == Color.Unspecified) {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            contentWindowInsets = insets,
            content = content,
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            containerColor = containerColor,
            contentWindowInsets = insets,
            content = content,
        )
    }
}
