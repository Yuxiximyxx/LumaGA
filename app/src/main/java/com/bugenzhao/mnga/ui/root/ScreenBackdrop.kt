package com.bugenzhao.mnga.ui.root

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Per-screen backdrop plumbing for the floating faces that live inside a
 * screen — popup menus above all (v1.7.6), the generalisation of the
 * dock's v1.7.2 architecture ("the Dock standard" the user approved in
 * v1.7.5):
 *
 * - the **recorder** is the host screen's own scrolling content root,
 *   recorded for the screen's whole composition lifetime — never
 *   attached/detached when a consumer appears;
 * - the **consumer** sits outside that recorded subtree;
 * - the effect chain is the one shared [resolveGlassSpec] resolution.
 *
 * Why a screen-level [CompositionLocal]: Miuix 0.8.8's SuperListPopup
 * does not compose its content at the anchor call site. `PopupLayout`
 * (MiuixPopupUtils) stores the content lambda into a `PopupState` via
 * rememberUpdatedState + SideEffect, and the Scaffold's `MiuixPopupHost`
 * composes it later, inside the host slot of the (root) Scaffold.
 * Composition locals therefore resolve at the HOST, not at the call
 * site — a local provided around the `MiuixDropdownMenu` call never
 * reaches the panel. Providing [LocalScreenBackdrop] above the screen's
 * outermost Scaffold covers both the content and the popup host, which
 * are both its descendants, so the panel reads the screen's backdrop.
 *
 * The 1.7.3 crash geometry is excluded by construction: the consumer
 * (popup host slot of the Scaffold) is a sibling of the recorded content
 * root, never inside it, and the recording node lives and dies with the
 * screen, not with any menu. Where a screen already records a backdrop
 * for another consumer (the tab roots record the dock's, TopicList its
 * FAB's, TopicDetails its page capsule's), that same recording is the
 * screen backdrop — one recording, several consumers, all outside the
 * recorded subtree.
 *
 * Full-surface glass audit (v1.8.1) — what is NOT backdrop glass, and
 * why each item stays that way:
 * - **Miuix SuperBottomSheet panels** (post editor, attachments,
 *   jump-to-floor selector, signature editor): Miuix 0.8.8 composes
 *   them in their own Dialog window. A backdrop texture lives in the
 *   activity window's graphics layer; another window cannot sample
 *   it — the same hard limit as the v1.7.4 menu crash, not a styling
 *   choice. The in-window alternative would be Miuix's Overlay*
 *   family, which needs an overlay host installed at the root plus a
 *   per-sheet migration of dismissal/IME semantics; not attempted
 *   blind (no runtime verification available for the editor flow).
 * - **WindowDialog dialogs** (settings pickers, login, cache, update
 *   flow, delete confirmations, …): separate windows, same limit.
 *   Menus anchored *inside* such windows take the tint fallback via
 *   the `windowPopup` path in MiuixMenu.kt for the same reason.
 * - **Switches** (Miuix SuperSwitch): drawn by the library with no
 *   backdrop hook, and every switch sits inside the scrolling content
 *   that is itself the recorded subtree — a backdrop consumer there
 *   would sample its own recording, the forbidden geometry above.
 *   A tint-styled app-drawn switch would be a global settings-UI
 *   restyle; it needs the user's visual sign-off first, not a blind
 *   swap.
 * - **Attached top bars** on secondary screens (Miuix
 *   SmallTopAppBar / Material TopAppBar in Scaffold slots): attached
 *   chrome, not floating surfaces — content does not scroll beneath
 *   them, so there is nothing for glass to sample. The app's glass
 *   top elements are the floating in-content headers, which are
 *   already glass.
 * - **System Toasts** (`android.widget.Toast`): rendered by the
 *   system, not the app. The app's own toast host IS glass (it reads
 *   the registry above).
 * - Screens that publish no screen backdrop (search, history,
 *   messages, settings, misc) have no in-window floating consumers —
 *   their menus/dialogs are all window-based (above) — so recording
 *   one would buy nothing; if a floating consumer is ever added to
 *   such a screen, it joins this plumbing first.
 * The image viewer's buttons are the one Dialog-window exception that
 * IS true glass: the viewer records and consumes inside its own
 * window (see ImageViewer.kt), which works because the sampled
 * content lives in the same window.
 *
 * Second sweep (v1.8.4) — the header control layer joins:
 * - The topic list's header round buttons (home-forum picker, range,
 *   more, search) and its unselected filter chips are glass now: the
 *   header and chip row sit in the Column ABOVE that screen's
 *   recorded list box — consumers outside the recorded subtree,
 *   sampling the texture's top edge (page background), the same
 *   frosted read as the topic screen's consumers. The selected chip
 *   stays solid primary (a state indicator, not a floating surface).
 * - The home tab's TopAppBar icon buttons (picker, bell, search,
 *   more) are glass circles sampling this screen's published
 *   backdrop; the bar is a Scaffold slot, sibling of the recorded
 *   content. The topic screen's header star / more circles joined
 *   the same way (over its flat, crash-isolated backdrop).
 * - NOT converted, by geometry: the forum directory's title-row
 *   buttons and the favorites / personal-center headers live INSIDE
 *   their screens' recorded scrolling content (the tab roots record
 *   the whole scrolling root for the dock) — glass there would be a
 *   consumer inside its own recording, the forbidden geometry.
 *   Same for every in-list row button everywhere.
 */
val LocalScreenBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Which participating screen is on top, for root-level consumers (the
 * toast host) that sit outside every screen subtree and so cannot read
 * [LocalScreenBackdrop]. Screens register their backdrop for their
 * composition lifetime via [ScreenBackdropProvider]; the registry is a
 * stack in composition order, so its top is the most recently composed
 * still-alive screen — with keep-alive navigation that tracks the
 * navigation stack: pushes register later (on top), pops dispose (and
 * unregister) the leaving screen.
 *
 * Generic over the backdrop type so the stack semantics are unit
 * testable without a graphics layer; production uses
 * `ScreenBackdropRegistry<LayerBackdrop>`.
 */
class ScreenBackdropRegistry<T : Any> {
    private val entries = mutableStateListOf<T>()

    /** The topmost participating screen's backdrop, if any. */
    val activeBackdrop: T? get() = entries.lastOrNull()

    internal fun register(backdrop: T) {
        if (backdrop !in entries) entries.add(backdrop)
    }

    internal fun unregister(backdrop: T) {
        entries.remove(backdrop)
    }
}

/** Provided once by the root, above both the navigation surface and the
 * global overlays, so screens can register and the toast host can read. */
val LocalScreenBackdropRegistry =
    compositionLocalOf<ScreenBackdropRegistry<LayerBackdrop>?> { null }

/**
 * Publishes [backdrop] as this screen's [LocalScreenBackdrop] (menus in
 * this screen sample it) and registers it with the root
 * [ScreenBackdropRegistry] (toasts sample the topmost screen's) for the
 * provider's composition lifetime. Must wrap the screen's outermost
 * Scaffold — see the file doc for why.
 *
 * A null [backdrop] is a pass-through (no local provided, nothing
 * registered): consumers in the subtree then take their tint fallback.
 */
@Composable
fun ScreenBackdropProvider(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    val registry = LocalScreenBackdropRegistry.current
    DisposableEffect(registry, backdrop) {
        if (backdrop != null) registry?.register(backdrop)
        onDispose {
            if (backdrop != null) registry?.unregister(backdrop)
        }
    }
    if (backdrop == null) {
        content()
    } else {
        CompositionLocalProvider(LocalScreenBackdrop provides backdrop, content = content)
    }
}

/**
 * The recording modifier for a screen's scrolling content root: records
 * into the screen backdrop published by the enclosing
 * [ScreenBackdropProvider], or is a no-op when the screen does not
 * participate. Mirrors `rememberDockBackdropSourceModifier`. The
 * recording always carries its page-background underlay (see
 * [recordedPageBackground]).
 */
@Composable
fun rememberScreenBackdropSourceModifier(): Modifier {
    val backdrop = LocalScreenBackdrop.current
    return if (backdrop != null) {
        Modifier.layerBackdrop(backdrop).recordedPageBackground()
    } else {
        Modifier
    }
}

/**
 * The page background as an underlay drawn INSIDE a recording node,
 * beneath its content — the Miuix Scaffold's default container color
 * (`colorScheme.surface`), the very value every screen backs its
 * content with, read from the same theme so it can never drift from
 * what is on screen.
 *
 * Why it must live inside the recording (v1.7.8): Kyant records exactly
 * what the recording node draws. That container color is painted by an
 * ANCESTOR of every screen's content root (the screen's Scaffold), so
 * recordings used to hold only the scrolling content itself. Over the
 * screens' undrawn regions — spacing, list tails, and above all the
 * ≈94dp of bottom content padding the tab roots keep clear of the
 * dock — the texture stayed transparent, and a glass surface sampling
 * it blurred / refracted see-through pixels to near-black: the dock
 * read as a smoked-black capsule whatever the material, its backdrop
 * invisible rather than veiled. The sister forks record whole pages
 * (background included), which is why the identical spec read clear
 * there. Painting the background as the recording's own first layer
 * makes the texture an opaque page again; on screen nothing changes,
 * the same color already shows through those regions.
 */
@Composable
fun Modifier.recordedPageBackground(): Modifier =
    this.background(MiuixTheme.colorScheme.surface)
