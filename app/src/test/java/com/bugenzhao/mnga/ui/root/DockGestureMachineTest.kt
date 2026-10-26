package com.bugenzhao.mnga.ui.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract of the dock gesture state machine (v1.7.5) — the tap
 * cases are the point of the rewrite: a tap is a Down followed by an
 * Up with no Move between, and it must produce a [ReleaseSelect] for
 * the pressed slot purely from the gesture's own positions. In
 * v1.7.3/v1.7.4 the release target was derived from the pill
 * animation's asynchronously updated targetValue, which a fast tap
 * outran — drags worked, taps selected the old slot and navigated
 * nowhere. These tests pin the behavior without any Compose runtime.
 *
 * Geometry used below: two slots of 100px (slot 0 = [0,100), slot 1 =
 * [100,200)), LTR unless stated otherwise.
 */
class DockGestureMachineTest {

    private fun machine(isLtr: Boolean = true) =
        DockGestureMachine(slotWidthPx = 100f, tabCount = 2, isLtr = isLtr)

    private fun assertDragTo(intent: DockGestureIntent, value: Float, deltaPx: Float) {
        assertTrue("expected DragTo, was $intent", intent is DockGestureIntent.DragTo)
        intent as DockGestureIntent.DragTo
        assertEquals(value, intent.value, 1e-4f)
        assertEquals(deltaPx, intent.deltaPx, 1e-4f)
    }

    @Test
    fun `case 1 - tap on the other tab selects it on release`() {
        val m = machine()
        assertEquals(
            listOf(DockGestureIntent.PressStarted(1)),
            m.onEvent(DockGestureEvent.Down(xPx = 150f, currentIndex = 0)),
        )
        // No Move at all — the exact sequence that navigated nowhere
        // in v1.7.4.
        assertEquals(
            listOf(DockGestureIntent.ReleaseSelect(1)),
            m.onEvent(DockGestureEvent.Up),
        )
    }

    @Test
    fun `case 2 - tap on the current tab settles without switching`() {
        val m = machine()
        assertEquals(
            listOf(DockGestureIntent.PressStarted(0)),
            m.onEvent(DockGestureEvent.Down(xPx = 50f, currentIndex = 0)),
        )
        // ReleaseSelect(current) is the settle signal: the executor
        // sets the selection to the already-selected tab, so nothing
        // navigates — but the press/release animation still ran.
        assertEquals(
            listOf(DockGestureIntent.ReleaseSelect(0)),
            m.onEvent(DockGestureEvent.Up),
        )
    }

    @Test
    fun `case 3 - drag across slots selects the slot nearest at release`() {
        val m = machine()
        assertEquals(
            listOf(DockGestureIntent.PressStarted(0)),
            m.onEvent(DockGestureEvent.Down(xPx = 50f, currentIndex = 0)),
        )
        // Anchored at the press: value = pressed slot + travel / slot.
        val move1 = m.onEvent(DockGestureEvent.Move(xPx = 140f))
        assertEquals(1, move1.size)
        assertDragTo(move1[0], value = 0.9f, deltaPx = 90f)
        // Past the last slot the value coerces to the range end.
        val move2 = m.onEvent(DockGestureEvent.Move(xPx = 170f))
        assertEquals(1, move2.size)
        assertDragTo(move2[0], value = 1.0f, deltaPx = 30f)
        assertEquals(
            listOf(DockGestureIntent.ReleaseSelect(1)),
            m.onEvent(DockGestureEvent.Up),
        )
    }

    @Test
    fun `case 4 - cancel returns to the tab selected when the press began`() {
        val m = machine()
        m.onEvent(DockGestureEvent.Down(xPx = 150f, currentIndex = 0))
        val move = m.onEvent(DockGestureEvent.Move(xPx = 60f))
        assertEquals(1, move.size)
        assertDragTo(move[0], value = 0.1f, deltaPx = -90f)
        assertEquals(
            listOf(DockGestureIntent.ReleaseCancel),
            m.onEvent(DockGestureEvent.Cancel),
        )
        assertEquals(0, m.gestureHomeIndex)
        // The machine is idle again: late events decide nothing.
        assertTrue(m.onEvent(DockGestureEvent.Move(xPx = 150f)).isEmpty())
        assertTrue(m.onEvent(DockGestureEvent.Up).isEmpty())
    }

    @Test
    fun `case 5 - down on the slot boundary or outside the strip uses the nearest slot`() {
        // Slots are contiguous; the boundary x=100 is the only "gap"
        // and belongs to the slot starting there.
        assertEquals(
            listOf(DockGestureIntent.PressStarted(1)),
            machine().onEvent(DockGestureEvent.Down(xPx = 100f, currentIndex = 0)),
        )
        // Beyond either end of the strip: clamp to the nearest slot.
        assertEquals(
            listOf(DockGestureIntent.PressStarted(1)),
            machine().onEvent(DockGestureEvent.Down(xPx = 260f, currentIndex = 0)),
        )
        assertEquals(
            listOf(DockGestureIntent.PressStarted(0)),
            machine().onEvent(DockGestureEvent.Down(xPx = -30f, currentIndex = 1)),
        )
    }

    @Test
    fun `events while idle are ignored`() {
        val m = machine()
        assertTrue(m.onEvent(DockGestureEvent.Move(xPx = 150f)).isEmpty())
        assertTrue(m.onEvent(DockGestureEvent.Up).isEmpty())
        assertTrue(m.onEvent(DockGestureEvent.Cancel).isEmpty())
    }

    @Test
    fun `rtl mirrors the slot geometry`() {
        val m = machine(isLtr = false)
        // In RTL slot 0 is at the RIGHT: the leftmost 100px is tab 1.
        assertEquals(
            listOf(DockGestureIntent.PressStarted(1)),
            m.onEvent(DockGestureEvent.Down(xPx = 10f, currentIndex = 1)),
        )
        // Dragging right in RTL moves the value toward slot 0.
        val move = m.onEvent(DockGestureEvent.Move(xPx = 110f))
        assertEquals(1, move.size)
        assertDragTo(move[0], value = 0.0f, deltaPx = 100f)
        assertEquals(
            listOf(DockGestureIntent.ReleaseSelect(0)),
            m.onEvent(DockGestureEvent.Up),
        )
    }
}
