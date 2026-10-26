package com.bugenzhao.mnga.ui.root

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * One pointer event of a dock gesture, already translated from the
 * Compose pointer stream by the gesture layer in MainTabDock. [xPx] is
 * the pointer's horizontal position in the gesture layer's coordinate
 * space (the dock's inner content strip: slot 0 starts at x = 0 and the
 * strip is `tabCount × slotWidthPx` wide).
 */
sealed interface DockGestureEvent {
    /**
     * The finger landed. [currentIndex] is the tab selected at that
     * moment — the machine remembers it as the gesture's home slot
     * (a [DockGestureEvent.Cancel] returns there, and a
     * [DockGestureIntent.ReleaseSelect] equal to it means "settle, do
     * not navigate"). It is only consulted for this event.
     */
    data class Down(val xPx: Float, val currentIndex: Int) : DockGestureEvent

    /** The finger moved to [xPx]. */
    data class Move(val xPx: Float) : DockGestureEvent

    /** The finger lifted: settle on the nearest slot and select it. */
    data object Up : DockGestureEvent

    /** The gesture was interrupted: settle back on the home slot. */
    data object Cancel : DockGestureEvent
}

/**
 * One decision of [DockGestureMachine], executed verbatim by the
 * Compose layer (press / retarget / release the [DampedDragAnimation],
 * update the selection). The machine is the ONLY place gesture
 * decisions are made — in particular the selection target of a release
 * is derived from the gesture's own tracked positions, never read back
 * from animation state (reading `Animatable.targetValue` at release
 * time was the v1.7.3/v1.7.4 tap bug: a tap's release can fire before
 * the press's `animateTo` has even started, so the animation still
 * reports the OLD slot and the tap selects nothing).
 */
sealed interface DockGestureIntent {
    /**
     * A press began on slot [index]: start the press animation and
     * retarget the pill to that slot. A tap is a press followed by an
     * [DockGestureEvent.Up] with no moves in between — there is no
     * separate click path.
     */
    data class PressStarted(val index: Int) : DockGestureIntent

    /**
     * The drag target moved to [value] (in slot units, coerced to the
     * slot range). [deltaPx] is the raw horizontal finger movement
     * since the previous event, for the capsule's rubber-band offset.
     */
    data class DragTo(val value: Float, val deltaPx: Float) : DockGestureIntent

    /**
     * The finger lifted: select slot [index] (the slot nearest the
     * tracked drag value; for a zero-travel tap, the pressed slot).
     * If [index] equals the currently selected tab the executor just
     * settles the pill there — no navigation.
     */
    data class ReleaseSelect(val index: Int) : DockGestureIntent

    /**
     * The gesture was cancelled: settle the pill back on the home
     * slot (the selection from when the press began), no navigation.
     */
    data object ReleaseCancel : DockGestureIntent
}

/**
 * Pure gesture-decision state machine for the floating tab dock —
 * deliberately free of any Compose/Android dependency so the dock's
 * tap/drag behavior is pinned by plain JVM unit tests
 * (DockGestureMachineTest). The Compose layer only translates pointer
 * events into [DockGestureEvent]s and executes the returned
 * [DockGestureIntent]s.
 *
 * Model (mirroring Kyant's LiquidBottomTabs interaction, where a tap
 * is a zero-length drag): a press starts the moment the finger lands —
 * no touch slop anywhere — with the intent slot taken from the landing
 * position. Moves move the drag value continuously, anchored at the
 * press (pressed slot + travel since the landing point), so the value
 * cannot drift when an animation is still in flight. Release selects
 * the slot nearest the tracked value; cancel returns to the home slot.
 */
class DockGestureMachine(
    private val slotWidthPx: Float,
    private val tabCount: Int,
    private val isLtr: Boolean,
) {
    private var pressed = false
    private var homeIndex = 0
    private var pressedIndex = 0
    private var downXPx = 0f
    private var lastXPx = 0f
    private var value = 0f

    /** Whether a press is currently being tracked. */
    val isPressed: Boolean get() = pressed

    fun onEvent(event: DockGestureEvent): List<DockGestureIntent> = when (event) {
        is DockGestureEvent.Down -> {
            // A Down while already pressed re-anchors (the Compose
            // layer tracks a single pointer, so this only matters for
            // defensive reuse).
            pressed = true
            homeIndex = event.currentIndex.coerceIn(0, tabCount - 1)
            pressedIndex = slotIndexAt(event.xPx)
            downXPx = event.xPx
            lastXPx = event.xPx
            value = pressedIndex.toFloat()
            listOf(DockGestureIntent.PressStarted(pressedIndex))
        }

        is DockGestureEvent.Move -> {
            if (!pressed) {
                emptyList()
            } else {
                val deltaPx = event.xPx - lastXPx
                lastXPx = event.xPx
                value = (pressedIndex + (event.xPx - downXPx) / slotWidthPx * direction)
                    .coerceIn(0f, (tabCount - 1).toFloat())
                listOf(DockGestureIntent.DragTo(value, deltaPx))
            }
        }

        DockGestureEvent.Up -> {
            if (!pressed) {
                emptyList()
            } else {
                pressed = false
                val index = value.roundToInt().coerceIn(0, tabCount - 1)
                listOf(DockGestureIntent.ReleaseSelect(index))
            }
        }

        DockGestureEvent.Cancel -> {
            if (!pressed) {
                emptyList()
            } else {
                pressed = false
                listOf(DockGestureIntent.ReleaseCancel)
            }
        }
    }

    /** The home slot of the in-flight gesture (selection at press time). */
    val gestureHomeIndex: Int get() = homeIndex

    private val direction: Float get() = if (isLtr) 1f else -1f

    /**
     * Slot under horizontal position [xPx]. Slots are contiguous
     * (slot i spans `[i × slotWidth, (i+1) × slotWidth)`), so a landing
     * exactly on a boundary — the only "gap" between slots — belongs
     * to the slot starting there; positions outside the strip clamp to
     * the nearest end slot. In RTL slot 0 sits at the strip's right
     * edge, so the raw index is mirrored.
     */
    private fun slotIndexAt(xPx: Float): Int {
        val raw = floor(xPx / slotWidthPx).toInt()
        val index = if (isLtr) raw else tabCount - 1 - raw
        return index.coerceIn(0, tabCount - 1)
    }
}
