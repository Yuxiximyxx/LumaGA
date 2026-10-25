package com.bugenzhao.mnga.ui.nav

import androidx.compose.ui.graphics.TransformOrigin
import com.xah.navigation.controller.NavigationController
import com.xah.navigation.model.anim.EffectLevel
import com.xah.navigation.model.anim.effect.PageEffectFrame
import com.xah.navigation.model.anim.effect.PageEffects
import kotlin.math.max

/**
 * Page effects for the app's standard push/pop transitions: SharedNav's
 * stock Default geometry for the *background* page (covered page scales
 * to 0.875 with blur and dim, restoring as it is uncovered), but a
 * card-style treatment for the *foreground* page instead of the stock
 * one.
 *
 * The stock Default foreground scales the page between **0 and 1**
 * around a pivot at (0.5, 0.275) — designed as a launcher-style
 * grow-from-a-point. On plain (non-shared, non-gesture) pops that reads
 * as a bug: the exiting page collapses into a small rectangle floating
 * at the top-center of the screen instead of leaving like a card
 * (user-reported on the Personal Center subpages, e.g. History).
 *
 * Here the foreground scale is floored at the predictive-back minimum
 * ([NavigationController.DEFAULT_SHARED_MAX_PRECENT], 0.875) around the
 * screen center, and the progress range below that floor — the start of
 * a push and the tail of a pop — is traded for a fade. Within the
 * predictive-gesture band (progress in [0.875, 1]) the frame is exactly
 * the stock one, so the gesture preview keeps the geometry the user
 * approved in v1.5.0/v1.5.1: current page shrinks to a 0.875 rounded
 * card over the restored previous page, following the finger.
 *
 * Shared-container transitions are unaffected: while the registry is
 * running, the host suppresses page effects and the overlay draws the
 * morph.
 */
class CardPageEffects(delegate: PageEffects) :
    PageEffects(delegate.backgroundEffect, delegate.foregroundEffect) {

    override fun foreground(progress: Float, level: EffectLevel): PageEffectFrame {
        val frame = super.foreground(progress, level)
        val minScale = NavigationController.DEFAULT_SHARED_MAX_PRECENT
        val p = progress.coerceIn(0f, 1f)
        val tail = if (p < minScale) p / minScale else 1f
        return frame.copy(
            scale = max(frame.scale, minScale),
            alpha = frame.alpha * tail,
            position = TransformOrigin.Center,
        )
    }
}
