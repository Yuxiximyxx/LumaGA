package com.bugenzhao.mnga.ui.root

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract of the adaptive dock width (v1.7.2): width = tab count ×
 * slot width + inner padding on both sides, capped at screen width
 * − 32dp and at the absolute [TabDockMaxWidth]. Fewer tabs ⇒ narrower
 * capsule; the slot a tab actually gets is never squeezed below
 * [TabDockSlotWidth] by the natural-width term.
 */
class TabDockWidthTest {

    @Test
    fun `two tabs on a phone-width screen`() {
        // 2 × 72 + 2 × 6 = 156dp — the shipped two-tab dock.
        assertEquals(156.dp, tabDockWidth(2, 390.dp))
    }

    @Test
    fun `four tabs get roomier, never squeezed to a fixed fraction`() {
        // 4 × 72 + 12 = 300dp (the old fixed 58% of 390dp was ≈226dp).
        assertEquals(300.dp, tabDockWidth(4, 390.dp))
    }

    @Test
    fun `screen cap keeps 32dp of total margin`() {
        // Natural 8 × 72 + 12 = 588dp, screen 360dp ⇒ 360 − 32 = 328dp.
        assertEquals(328.dp, tabDockWidth(8, 360.dp))
    }

    @Test
    fun `absolute cap wins on wide screens`() {
        assertEquals(TabDockMaxWidth, tabDockWidth(8, 1200.dp))
    }

    @Test
    fun `slot geometry constants stay in the unified range`() {
        assertEquals(72.dp, TabDockSlotWidth)
        assertEquals(6.dp, TabDockInnerPadding)
        assertEquals(68.dp, TabDockHeight)
    }
}
