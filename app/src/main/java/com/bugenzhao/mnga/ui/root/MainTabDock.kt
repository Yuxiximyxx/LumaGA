package com.bugenzhao.mnga.ui.root

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import com.bugenzhao.mnga.storage.DockGlassMaterial
import com.bugenzhao.mnga.ui.components.glassTintColor
import com.bugenzhao.mnga.ui.components.liquidGlass
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

/** Height of the floating tab dock capsule. */
val TabDockHeight = 68.dp

/** Width of one tab slot inside the dock capsule. */
val TabDockSlotWidth = 72.dp

/** Horizontal padding between the capsule edge and the first/last slot. */
val TabDockInnerPadding = 6.dp

/** Absolute cap on the dock width, however many tabs it holds. */
val TabDockMaxWidth = 380.dp

/**
 * Dock width rule (v1.7.2, unified across the sister forks): the
 * capsule hugs its tabs — [tabCount] slots of [TabDockSlotWidth] plus
 * the inner padding on both sides — capped at the screen width minus
 * 32dp and at [TabDockMaxWidth]. Fewer tabs ⇒ a narrower capsule (the
 * old fixed ≈58%-of-screen width squeezed many-tab docks and left
 * two-tab docks mostly empty).
 */
fun tabDockWidth(tabCount: Int, screenWidth: Dp): Dp {
    val natural = TabDockSlotWidth * tabCount + TabDockInnerPadding * 2
    val screenCap = (screenWidth - 32.dp).coerceAtLeast(0.dp)
    return minOf(natural, screenCap, TabDockMaxWidth)
}

/** Gap between the dock's bottom edge and the system navigation-bar area. */
val TabDockBottomGap = 10.dp

/**
 * Bottom content padding tab-root scroll containers need so their last item
 * can scroll fully clear of the floating dock: dock height + its bottom gap
 * + breathing room + the device's own navigation-bar inset (the dock floats
 * above that inset via navigationBarsPadding in the root shell).
 */
@Composable
fun tabDockContentBottomPadding(): Dp =
    TabDockHeight + TabDockBottomGap + 16.dp +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

enum class DockScheme { Dark, Light, Classic }
internal data class DockColors(
    val border: Color,
    val pill: Color,
    val unselected: Color,
    /** Top inset highlight (brightest right at the capsule's top edge). */
    val highlight: Color,
    /** Height of the top highlight band; narrower reads as sharper. */
    val highlightHeight: Dp = 14.dp,
    /** Brightest color of the bottom-edge highlight line; Transparent = off. */
    val bottomHighlight: Color = Color.Transparent,
    /** Tint of the diagonal sheen wash (top-left); Transparent = off. */
    val sheen: Color = Color.Transparent,
)

/**
 * Rim/pill palette of the dock per color scheme × glass material. The
 * sampled backdrop and the veil tint come from the resolved [GlassSpec]
 * (see GlassSpec.kt for the calibration table); these colors are drawn
 * over the glass: the selection pill, the border and the rim-light bands
 * (whose alphas the user's highlight trim scales at draw time).
 *
 * - FROSTED （磨砂玻璃， the default): the soft, wide top highlight and
 *   the faintest rim — matte.
 * - CLEAR （毛玻璃）: a brighter and narrower (sharper) top highlight and
 *   a more luminous rim — airier.
 * - LIQUID （液态玻璃）: the strongest rim, highlight lines on BOTH the
 *   top and bottom edges and a faint diagonal sheen wash over the
 *   refracted sample.
 */
internal fun dockColors(
    scheme: DockScheme,
    material: DockGlassMaterial,
): DockColors = when (scheme) {
    DockScheme.Dark -> when (material) {
        DockGlassMaterial.FROSTED -> DockColors(
            border = Color.White.copy(alpha = 0.28f),
            pill = Color.Black.copy(alpha = 0.18f),
            unselected = Color(0xFFF2F2F7),
            highlight = Color.White.copy(alpha = 0.30f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            border = Color.White.copy(alpha = 0.45f),
            pill = Color.Black.copy(alpha = 0.14f),
            unselected = Color(0xFFF2F2F7),
            highlight = Color.White.copy(alpha = 0.55f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
            border = Color.White.copy(alpha = 0.60f),
            pill = Color.Black.copy(alpha = 0.12f),
            unselected = Color(0xFFF2F2F7),
            highlight = Color.White.copy(alpha = 0.65f),
            highlightHeight = 8.dp,
            bottomHighlight = Color.White.copy(alpha = 0.35f),
            sheen = Color.White.copy(alpha = 0.10f),
        )
    }
    DockScheme.Light -> when (material) {
        DockGlassMaterial.FROSTED -> DockColors(
            border = Color.Black.copy(alpha = 0.07f),
            pill = Color.Black.copy(alpha = 0.10f),
            unselected = Color(0xFF3A3A3C),
            highlight = Color.White.copy(alpha = 0.50f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            border = Color.Black.copy(alpha = 0.12f),
            pill = Color.Black.copy(alpha = 0.08f),
            unselected = Color(0xFF3A3A3C),
            highlight = Color.White.copy(alpha = 0.80f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
            border = Color.White.copy(alpha = 0.55f),
            pill = Color.Black.copy(alpha = 0.08f),
            unselected = Color(0xFF3A3A3C),
            highlight = Color.White.copy(alpha = 0.90f),
            highlightHeight = 8.dp,
            bottomHighlight = Color.White.copy(alpha = 0.50f),
            sheen = Color.White.copy(alpha = 0.18f),
        )
    }
    DockScheme.Classic -> when (material) {
        DockGlassMaterial.FROSTED -> DockColors(
            border = Color(0xFFD9C48F).copy(alpha = 0.8f),
            pill = Color(0xFF4A3B18).copy(alpha = 0.14f),
            unselected = Color(0xFF6B5D3E),
            highlight = Color.White.copy(alpha = 0.45f),
        )
        DockGlassMaterial.CLEAR -> DockColors(
            border = Color(0xFFD9C48F).copy(alpha = 0.95f),
            pill = Color(0xFF4A3B18).copy(alpha = 0.12f),
            unselected = Color(0xFF6B5D3E),
            highlight = Color.White.copy(alpha = 0.65f),
            highlightHeight = 9.dp,
        )
        DockGlassMaterial.LIQUID -> DockColors(
            border = Color(0xFFE9D9A8).copy(alpha = 0.95f),
            pill = Color(0xFF4A3B18).copy(alpha = 0.10f),
            unselected = Color(0xFF6B5D3E),
            highlight = Color.White.copy(alpha = 0.75f),
            highlightHeight = 8.dp,
            bottomHighlight = Color.White.copy(alpha = 0.40f),
            sheen = Color.White.copy(alpha = 0.14f),
        )
    }
}

/**
 * The floating tab dock of the main shell (v1.7.0), replacing the full-width
 * Miuix NavigationBar: a capsule whose width adapts to its tab count (see
 * [tabDockWidth]), horizontally centered, hovering above the content
 * (which scrolls underneath it) just above the system gesture area.
 * The selected tab is a dark oval holding an
 * accent-tinted icon stacked over its label; the unselected tab shows its
 * icon only. Tab semantics (which routes count as roots, push/pop
 * behavior) live in [MainTabBar] and are unchanged from the bar this
 * replaces.
 *
 * v1.7.3 — the selection oval becomes Kyant's liquid-glass pill, ported
 * from the library catalog's LiquidBottomTabs (the structure the sister
 * c001apk fork's draggable lens was also modeled on):
 *
 * - The pill is draggable: [DampedDragAnimation] (catalog spring set:
 *   value ζ1.0/k1000, press ζ1.0/k1000, scaleX ζ0.6/k250, scaleY
 *   ζ0.7/k250, velocity ζ0.5/k300, pressedScale 78/56) chases the finger
 *   with press growth and velocity jelly; on release it snaps to the
 *   nearest tab and navigates. Taps are zero-length drags through the
 *   same path (items carry semantics only, no pointer input — see
 *   DockItem). Since v1.7.5 the gesture decisions are made by the pure
 *   [DockGestureMachine] (JVM-unit-tested) and merely executed here;
 *   the v1.7.3/1.7.4 builds derived the tap target from the animation's
 *   asynchronously updated targetValue, which is why taps silently
 *   navigated nowhere while drags worked — see the note where the
 *   machine is wired in below.
 * - The pill is a real lens: it samples a COMBINED backdrop — the
 *   current page's own recording (v1.7.2's per-screen source, unchanged)
 *   plus a second, invisible copy of the tab row rendered accent-tinted
 *   over the same page sample (Kyant's tabsBackdrop trick: `alpha(0)`
 *   hides it on screen, `layerBackdrop` still records it, and a tint
 *   colorFilter on the copy's content makes every glyph accent). While
 *   pressed, the pill's lens (10dp band / 14dp amount × pressProgress —
 *   the Kyant pill norm, the same one the approved c001apk pill uses)
 *   warps and magnifies the glyphs it slides over; its chromatic
 *   aberration follows the resolved spec's dispersion gate, its
 *   specular highlight the spec's highlight alpha. At rest the lens is
 *   zero and the pill's surface is the familiar dark oval tint.
 * - The visible items are driven continuously by the pill's position:
 *   glyph tint lerps toward the accent and the label grows/fades in as
 *   the pill approaches, so a drag reads as one continuous motion
 *   across both tabs instead of a binary switch.
 * - The whole capsule rubber-bands with the drag (4dp EaseOut cap, the
 *   catalog's panel offset) and blooms slightly while pressed; the
 *   bloom is capped at +5% (the catalog's 16dp-of-width rule targets
 *   full-width bars and would over-inflate this narrow capsule).
 */
@Composable
fun FloatingTabDock(
    homeSelected: Boolean,
    onHomeClick: () -> Unit,
    onMineClick: () -> Unit,
    glassMaterial: DockGlassMaterial,
    spec: GlassSpec,
    scheme: DockScheme,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    debugShowSample: Boolean = false,
) {
    val colors = dockColors(scheme, glassMaterial)
    // The user's highlight trim scales the dock's own rim-light bands on
    // top of the per-material palette (the Kyant specular highlight got
    // the same trim inside the resolved spec).
    val hlScale = spec.highlightScale
    val topHighlight = colors.highlight.copy(
        alpha = (colors.highlight.alpha * hlScale).coerceAtMost(1f),
    )
    val bottomHighlight = colors.bottomHighlight.copy(
        alpha = (colors.bottomHighlight.alpha * hlScale).coerceAtMost(1f),
    )
    val sheen = colors.sheen.copy(
        alpha = (colors.sheen.alpha * hlScale).coerceAtMost(1f),
    )
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(percent = 50)
    val tabCount = 2
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val dockWidth = tabDockWidth(tabCount, screenWidth)
    val slotWidth = (dockWidth - TabDockInnerPadding * 2) / tabCount
    val density = LocalDensity.current
    val slotWidthPx = with(density) { slotWidth.toPx() }
    val dockWidthPx = with(density) { dockWidth.toPx() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    val animationScope = rememberCoroutineScope()
    val selectedIndex = if (homeSelected) 0 else 1
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density, dockWidthPx) {
        derivedStateOf {
            val fraction = (offsetAnimation.value / dockWidthPx).coerceIn(-1f, 1f)
            with(density) {
                4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }
    val onTabSelectedState = rememberUpdatedState { index: Int ->
        if (index == 0) onHomeClick() else onMineClick()
    }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
        )
    }
    // Gesture decisions live in the pure [DockGestureMachine] (pinned
    // by JVM unit tests); this layer only translates pointer events
    // into machine events and executes the machine's intents against
    // the animation engine, in the same order the pre-v1.7.5 callback
    // chain ran (retarget → press on landing; settle → release on
    // lift). Why the split (the v1.7.5 tap post-mortem): v1.7.3/1.7.4
    // derived the release-time navigation target from the value
    // Animatable's targetValue — animation state that updates
    // asynchronously. The catalog detector this used to run fires a
    // contractual zero-delta onDrag immediately after onDragStarted,
    // and the v1.7.4 intent variable recorded in onDragStarted was
    // overwritten by that very call (it recomputed the intent from
    // the not-yet-updated targetValue) before any real move arrived;
    // at lift, the recorded intent was the OLD slot, currentIndex
    // never changed, and the tap navigated nowhere. Drags masked the
    // bug because their continuous moves kept re-recording the intent
    // from live positions. The machine never reads animation state:
    // the intent comes from the gesture itself.
    val gestureMachine = remember(slotWidthPx, tabCount, isLtr) {
        DockGestureMachine(
            slotWidthPx = slotWidthPx,
            tabCount = tabCount,
            isLtr = isLtr,
        )
    }
    fun executeDockIntent(intent: DockGestureIntent) {
        when (intent) {
            is DockGestureIntent.PressStarted -> {
                // A press landing on the OTHER slot immediately
                // retargets the value spring to that slot — this is
                // what makes a plain tap on the other tab animate the
                // pill over and navigate on release, exactly like a
                // drag that ends there.
                if (intent.index != dampedDragAnimation.targetValue.roundToInt()) {
                    dampedDragAnimation.updateValue(intent.index.toFloat())
                }
                dampedDragAnimation.press()
            }
            is DockGestureIntent.DragTo -> {
                dampedDragAnimation.updateValue(intent.value)
                animationScope.launch {
                    offsetAnimation.snapTo(offsetAnimation.value + intent.deltaPx)
                }
            }
            is DockGestureIntent.ReleaseSelect -> {
                currentIndex = intent.index
                dampedDragAnimation.animateToValue(intent.index.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
                dampedDragAnimation.release()
            }
            DockGestureIntent.ReleaseCancel -> {
                dampedDragAnimation.animateToValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
                dampedDragAnimation.release()
            }
        }
    }
    // The gesture loop (Kyant's LiquidBottomTabs shape): the Initial
    // pass sees the landing before any other consumer, so a press
    // enters the machine the moment the finger lands — no touch slop;
    // a tap is simply a Down followed by an Up with no Move between.
    val dockGestureModifier = Modifier.pointerInput(gestureMachine) {
        awaitEachGesture {
            awaitFirstDown(false, PointerEventPass.Initial)
            val down = awaitFirstDown(false)
            gestureMachine.onEvent(
                DockGestureEvent.Down(down.position.x, currentIndex),
            ).forEach(::executeDockIntent)
            val pointerId = down.id
            var finished = false
            while (!finished) {
                val event = awaitPointerEvent()
                val change = event.changes.fastFirstOrNull { it.id == pointerId }
                when {
                    change == null -> {
                        // The tracked pointer vanished from the
                        // stream (the gesture was interrupted).
                        gestureMachine.onEvent(DockGestureEvent.Cancel)
                            .forEach(::executeDockIntent)
                        finished = true
                    }
                    change.changedToUpIgnoreConsumed() -> {
                        gestureMachine.onEvent(DockGestureEvent.Up)
                            .forEach(::executeDockIntent)
                        finished = true
                    }
                    change.position != change.previousPosition -> {
                        gestureMachine.onEvent(
                            DockGestureEvent.Move(change.position.x),
                        ).forEach(::executeDockIntent)
                    }
                }
            }
        }
    }
    // External selection changes (system back, deep links) move the pill
    // without navigating again: the collector below is what navigates,
    // and the MainTabBar callbacks are route-guarded, so a sync that
    // lands on the already-current tab is a no-op there.
    LaunchedEffect(selectedIndex) {
        currentIndex = selectedIndex
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                dampedDragAnimation.animateToValue(index.toFloat())
                onTabSelectedState.value(index)
            }
    }

    Box(
        modifier
            .width(dockWidth)
            .height(TabDockHeight)
            .graphicsLayer {
                // Rubber band + press bloom for the whole capsule.
                translationX = panelOffset
                val bloom = 1f + 0.05f * dampedDragAnimation.pressProgress
                scaleX = bloom
                scaleY = bloom
            },
    ) {
        // The glass shell: Kyant samples the recorded page content
        // (saturation / blur / lens per the resolved spec) and draws the
        // veil tint over the sample as its surface; the rim and
        // highlight bands below are drawn over that.
        Box(
            Modifier
                .fillMaxSize()
                .liquidGlass(
                    backdrop = backdrop,
                    shape = shape,
                    spec = spec,
                    scheme = scheme,
                    shadowRadius = 16.dp,
                    debugShowSample = debugShowSample,
                    debugColor = Color.Magenta,
                )
                .clip(shape)
                .border(1.dp, colors.border, shape),
        ) {
            // Diagonal sheen wash (liquid glass only; Transparent
            // otherwise): a faint gloss running from the top-left corner,
            // reinforcing the liquid recipe's reflective surface over
            // the refracted sample.
            if (sheen.alpha > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(sheen, Color.Transparent),
                            ),
                        ),
                )
            }
            // Inset top highlight: a soft light band just inside the
            // capsule's top edge, fading down — the glass recipes' rim
            // light. Its height and brightness are part of the material
            // (see dockColors).
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(colors.highlightHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(topHighlight, Color.Transparent),
                        ),
                    ),
            )
            // Bottom-edge highlight line (liquid glass only; Transparent
            // otherwise): brightest right at the capsule's bottom edge,
            // the second rim light that makes the liquid surface read
            // as wet.
            if (bottomHighlight.alpha > 0f) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, bottomHighlight),
                            ),
                        ),
                )
            }
        }
        Box(Modifier.fillMaxSize().padding(TabDockInnerPadding)) {
            // Accent copy of the tab row (Kyant's tabsBackdrop feed):
            // invisible on screen (alpha 0 sits OUTSIDE the recording
            // node, so the recording is unaffected), it renders every
            // tab in its selected glyph style over the page sample with
            // the spec's glass chain (lens gated by pressProgress) and
            // records the composite; the pill samples it combined with
            // the page, so glyphs under the pill read accent and warp.
            Row(
                Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            if (spec.saturation != 1f) {
                                colorControls(saturation = spec.saturation)
                            }
                            if (spec.blurDp > 0f) {
                                blur(spec.blurDp.dp.toPx())
                            }
                            val p = dampedDragAnimation.pressProgress
                            if (spec.refractionAmountDp > 0f && spec.refractionHeightDp > 0f && p > 0f) {
                                lens(
                                    refractionHeight = minOf(
                                        spec.refractionHeightDp.dp.toPx() * p,
                                        size.minDimension / 2f,
                                    ),
                                    refractionAmount = minOf(
                                        spec.refractionAmountDp.dp.toPx() * p,
                                        size.minDimension,
                                    ),
                                    chromaticAberration = spec.chromaticAberration,
                                )
                            }
                        },
                        highlight = {
                            Highlight(
                                alpha = spec.highlightAlpha * dampedDragAnimation.pressProgress,
                            )
                        },
                        shadow = { null },
                        onDrawSurface = { drawRect(glassTintColor(spec, scheme)) },
                    )
                    .graphicsLayer { colorFilter = ColorFilter.tint(accent) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockTabGlyph(
                    icon = Icons.Filled.Home,
                    label = "主页",
                    tint = accent,
                    modifier = Modifier.weight(1f),
                )
                DockTabGlyph(
                    icon = Icons.Filled.Person,
                    label = "我的",
                    tint = accent,
                    modifier = Modifier.weight(1f),
                )
            }
            // The visible tab row: items carry semantics only (role /
            // selected / accessibility click — no pointer input, see
            // DockItem); taps and drags alike are owned by the gesture
            // layer below, their look driven continuously by the pill's
            // animated position.
            Row(Modifier.fillMaxSize()) {
                DockItem(
                    progress = (1f - abs(0f - dampedDragAnimation.value)).coerceIn(0f, 1f),
                    pressProgress = dampedDragAnimation.pressProgress,
                    selected = homeSelected,
                    icon = Icons.Filled.Home,
                    label = "主页",
                    accent = accent,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = { currentIndex = 0 },
                )
                DockItem(
                    progress = (1f - abs(1f - dampedDragAnimation.value)).coerceIn(0f, 1f),
                    pressProgress = dampedDragAnimation.pressProgress,
                    selected = !homeSelected,
                    icon = Icons.Filled.Person,
                    label = "我的",
                    accent = accent,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = { currentIndex = 1 },
                )
            }
            // The liquid pill itself: pure lens, no input of its own.
            // It sits at the first slot and is translated by the damped
            // drag value (the gesture layer below rides on top of the
            // whole dock and feeds the animation).
            val pillModifier = if (debugShowSample) {
                Modifier.drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { shape },
                    effects = {},
                    highlight = { null },
                    shadow = { null },
                    innerShadow = { null },
                    onDrawSurface = {},
                ).border(2.dp, Color.Magenta, shape)
            } else {
                Modifier.drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { shape },
                    effects = {
                        val p = dampedDragAnimation.pressProgress
                        lens(
                            refractionHeight = 10.dp.toPx() * p,
                            refractionAmount = 14.dp.toPx() * p,
                            chromaticAberration = spec.chromaticAberration,
                        )
                    },
                    highlight = {
                        Highlight(
                            alpha = spec.highlightAlpha * dampedDragAnimation.pressProgress,
                        )
                    },
                    shadow = {
                        Shadow(alpha = dampedDragAnimation.pressProgress)
                    },
                    innerShadow = {
                        val p = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 8.dp * p, alpha = p)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val p = dampedDragAnimation.pressProgress
                        drawRect(colors.pill, alpha = 1f - p)
                        drawRect(Color.Black.copy(alpha = 0.03f * p))
                    },
                )
            }
            Box(
                Modifier
                    .width(slotWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX =
                            if (isLtr) {
                                dampedDragAnimation.value * slotWidthPx
                            } else {
                                -dampedDragAnimation.value * slotWidthPx
                            }
                    }
                    .then(pillModifier),
            )
            // Gesture layer: covers the whole dock content area, so a
            // press anywhere on the dock (pill or not) starts the
            // gesture; a press with no travel is a tap and settles /
            // navigates on release through the same machine path.
            Box(
                Modifier
                    .fillMaxSize()
                    .then(dockGestureModifier),
            )
        }
    }
}

/** One tab's glyph (icon over label), as the accent copy row renders it. */
@Composable
private fun DockTabGlyph(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DockItem(
    progress: Float,
    pressProgress: Float,
    selected: Boolean,
    icon: ImageVector,
    label: String,
    accent: Color,
    colors: DockColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isSelected = selected
    val clickAction = onClick
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            // Semantics only — NO pointer input on the items (v1.7.4,
            // kept in v1.7.5). In v1.7.3 the items carried foundation
            // `selectable`, whose click detector competed with the
            // full-dock drag detector for the same gesture (it consumes
            // the up in the Main pass); and the drag path's own tap
            // handling derived its target from animation state that a
            // fast tap outruns. Kyant's LiquidBottomTabs gives its
            // items' clickables a separate existence from the drag
            // detector; here the cleaner split is: items carry no
            // pointer input at all, a tap is simply a zero-length drag
            // owned end-to-end by the DockGestureMachine gesture layer
            // (press retargets on down, release settles + navigates).
            // The semantics onClick keeps the tabs operable for
            // accessibility services without touching the pointer path.
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = isSelected
                onClick { clickAction(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = lerp(colors.unselected, accent, progress),
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        val s = 1f + 0.2f * progress * pressProgress
                        scaleX = s
                        scaleY = s
                    },
            )
            // The label grows with the pill's proximity (height follows
            // progress, alpha follows progress³ — the sister FluxTie
            // dock's continuous label drive).
            Box(
                Modifier
                    .height(15.dp * progress)
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = accent,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.graphicsLayer {
                        alpha = progress.pow(3)
                    },
                )
            }
        }
    }
}
