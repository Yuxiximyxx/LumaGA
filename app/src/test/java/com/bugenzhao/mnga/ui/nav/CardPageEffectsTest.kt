package com.bugenzhao.mnga.ui.nav

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.xah.navigation.anim.effect.DefaultPageEffects
import com.xah.navigation.model.anim.EffectLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Geometry contract of [CardPageEffects] (v1.6.1).
 *
 * Regression context: with SharedNav's stock Default page effects, the
 * foreground page scales 0↔1 around a (0.5, 0.275) pivot, so a plain
 * pop collapsed the exiting page into a small rectangle floating at
 * the top-center of the screen (user-reported on the Personal Center
 * subpages, e.g. History exiting over the blurred Mine page).
 * [CardPageEffects] floors the foreground scale at the card minimum
 * (0.875) around the screen center and trades the range below the
 * floor for a fade; inside the predictive-gesture band the frame must
 * stay identical to stock, and the background page must be untouched.
 */
class CardPageEffectsTest {

    private val delegate = DefaultPageEffects(corner = 20.dp)
    private val effects = CardPageEffects(delegate)

    @Test
    fun `foreground never shrinks below the card minimum`() {
        for (p in listOf(0f, 0.1f, 0.25f, 0.5f, 0.8f, 0.875f)) {
            val frame = effects.foreground(p, EffectLevel.HIGH)
            assertEquals("progress=$p", 0.875f, frame.scale, 1e-4f)
        }
    }

    @Test
    fun `foreground matches the stock frame inside the gesture band`() {
        for (p in listOf(0.875f, 0.9f, 0.95f, 1f)) {
            val stock = delegate.foreground(p, EffectLevel.HIGH)
            val frame = effects.foreground(p, EffectLevel.HIGH)
            assertEquals("progress=$p scale", stock.scale, frame.scale, 1e-4f)
            assertEquals("progress=$p alpha", stock.alpha, frame.alpha, 1e-4f)
            assertEquals("progress=$p blur", stock.blur, frame.blur)
            assertEquals("progress=$p maskDark", stock.maskDark, frame.maskDark)
        }
    }

    @Test
    fun `foreground pivot is the screen center at every progress`() {
        for (p in listOf(0f, 0.3f, 0.875f, 1f)) {
            assertEquals(TransformOrigin.Center, effects.foreground(p, EffectLevel.HIGH).position)
        }
    }

    @Test
    fun `foreground fades out through the tail of a pop`() {
        assertEquals(0f, effects.foreground(0f, EffectLevel.HIGH).alpha, 1e-4f)
        val mid = effects.foreground(0.4375f, EffectLevel.HIGH).alpha
        assertTrue("mid-tail alpha in (0, 1): $mid", mid > 0f && mid < 1f)
        assertEquals(1f, effects.foreground(1f, EffectLevel.HIGH).alpha, 1e-4f)
    }

    @Test
    fun `background frame is the stock one`() {
        for (p in listOf(0f, 0.5f, 0.875f, 1f)) {
            val stock = delegate.background(p, EffectLevel.HIGH)
            val frame = effects.background(p, EffectLevel.HIGH)
            assertEquals("progress=$p scale", stock.scale, frame.scale, 1e-4f)
            assertEquals("progress=$p blur", stock.blur, frame.blur)
            assertEquals("progress=$p maskDark", stock.maskDark, frame.maskDark)
            assertEquals("progress=$p alpha", stock.alpha, frame.alpha, 1e-4f)
        }
    }
}
