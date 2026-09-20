package com.bugenzhao.mnga.ui.nav

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

/**
 * Duration of [com.bugenzhao.mnga.ui.root.NavigationHost] enter fade.
 * First-page loads should wait this out so network/parse/recompositions
 * do not contend with the push animation for main-thread / GPU time.
 */
const val NAV_ENTER_TRANSITION_MS = 180L

/**
 * Yield a frame, then wait for the NavHost enter transition to finish.
 * Call from [androidx.compose.runtime.LaunchedEffect] before kicking off
 * heavy [com.bugenzhao.mnga.model.PagingDataSource.initialLoad] work on a
 * freshly pushed screen. No-ops meaningfully when the screen was restored
 * with data already loaded (callers should still gate on `notLoaded`).
 */
suspend fun awaitEnterTransitionSettled() {
    yield()
    delay(NAV_ENTER_TRANSITION_MS)
}
