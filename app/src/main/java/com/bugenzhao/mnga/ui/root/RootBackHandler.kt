package com.bugenzhao.mnga.ui.root

import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.xah.container.model.SharedContainerState
import com.xah.navigation.controller.NavigationController
import kotlin.coroutines.cancellation.CancellationException

/**
 * System-back owner for the SharedNav stack (v1.5.1).
 *
 * Phase 4 relied on the handler SharedNavHost composes internally
 * (`DefaultBackHandler` -> the library's `NavigationBackHandler`). On a
 * real device that wiring silently degraded to discrete back presses:
 * the pop animation played, but only after the finger was released —
 * gesture start/progress never visibly drove the transition. The
 * library picks its handler deep inside the host composition and calls
 * `PredictiveBackHandler` conditionally (an if/else against a plain
 * `BackHandler`), while AndroidX resolves an in-progress gesture to
 * the *last composed enabled* handler; any ordering/precedence upset
 * in that chain degrades predictive back to a plain press with no
 * error anywhere.
 *
 * This handler therefore lives at the app root, composed *after*
 * SharedNavHost so it is the last back handler in the main tree, calls
 * both handler composables unconditionally (activity of each is
 * governed by its `enabled` flag, per the AndroidX guidance), and
 * disables the host's internal one (`backHandler = {}` at the call
 * site; the library's other internal handler, FloatingBackHandler, is
 * a no-op here because the app never installs a floating controller).
 * The gesture itself is still driven entirely by SharedNav's public
 * predictive API on [NavigationController] — the same calls the
 * library's own handler makes.
 *
 * Enable matrix (see [predictiveBackEnabled] / [discreteBackEnabled]):
 *  - stack size > 1, SDK 34+: predictive handler (progress follows the
 *    finger, cancel restores, completion confirms the pop);
 *  - stack size > 1, SDK < 34: plain handler (no progress events exist
 *    below 34; discrete pop, same as the library fallback);
 *  - at the root (stack size 1) both are disabled: Android 13+ falls
 *    through to the system's back-to-home preview, and below 13 the
 *    root double-tap-to-exit BackHandler in [NavigationHost] (whose
 *    enabled condition, size <= 1, is disjoint from this one) owns it.
 */
@Composable
fun SharedNavBackHandler(controller: NavigationController) {
    // SnapshotStateList read: recomposes (and re-enables) as the stack
    // grows/shrinks, exactly like the library handler's canPop() read.
    val canPop = controller.stack.size > 1
    PredictiveBackHandler(
        enabled = predictiveBackEnabled(Build.VERSION.SDK_INT, canPop)
    ) { backEvents ->
        var state: SharedContainerState? = null
        try {
            val transiting = controller.isTransitioning
            if (!transiting) {
                state = controller.startPredictiveBackShared()
                Log.i(TAG, "gesture started (shared=${state != null})")
            }
            var progressed = false
            backEvents.collect { backEvent ->
                if (!transiting) {
                    if (!progressed) {
                        progressed = true
                        Log.i(TAG, "first progress=${backEvent.progress}")
                    }
                    controller.updatePredictiveBackShared(
                        backEvent.progress,
                        Offset.Zero,
                        state,
                    )
                }
            }
            if (transiting) {
                controller.pop()
            } else {
                Log.i(TAG, "gesture completed")
                controller.confirmPredictiveBackShared(state)
            }
        } catch (e: CancellationException) {
            Log.i(TAG, "gesture cancelled")
            controller.cancelPredictiveBackShared(state)
            throw e
        }
    }
    BackHandler(enabled = discreteBackEnabled(Build.VERSION.SDK_INT, canPop)) {
        controller.pop()
    }
}

private const val TAG = "PredictiveBack"

/** Predictive progress events only exist on API 34+ (and the library
 * gates its own predictive machinery the same way). */
internal fun predictiveBackEnabled(sdkInt: Int, canPop: Boolean): Boolean =
    canPop && sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/** Below 34 the same stack position is served by a discrete handler. */
internal fun discreteBackEnabled(sdkInt: Int, canPop: Boolean): Boolean =
    canPop && sdkInt < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
