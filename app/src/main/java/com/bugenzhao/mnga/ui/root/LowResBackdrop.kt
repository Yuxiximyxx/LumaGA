package com.bugenzhao.mnga.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.Backdrop
import kotlin.math.roundToInt

/**
 * A [Backdrop] that records its source subtree at a reduced linear
 * resolution — the v1.8.5-exp experiment for the topic-details screen.
 *
 * Why this exists: the details screen's consumers (inline reply bar,
 * header round buttons, in-screen menus) want to sample the real post
 * content, but the full-resolution content recording (Kyant's
 * `layerBackdrop`, v1.7.1→v1.8.2) is the one code path that crashes
 * heavy topics on open — twice device-confirmed (v1.8.1 isolation
 * fixed it, the v1.8.2 single-layer retry brought it back). Since
 * v1.8.3 the screen records only a flat page-color layer instead.
 * This class retries the real recording with the raster load cut by
 * [scaleFactor]² (at 0.25: 1/16 the pixels): the backing texture is
 * smaller and every rasterized draw op lands on 1/16 the pixels. The
 * draw-op *count* of the recorded subtree is unchanged — if the crash
 * turns out to be op-bound rather than raster-bound, this experiment
 * fails on device and the flat isolation stays the final form.
 *
 * Why a hand-rolled [Backdrop] instead of Kyant's `LayerBackdrop`:
 * Kyant's recording node always records at the node's full pixel
 * size (its internal `recordLayer` passes the draw scope's size) and
 * `LayerBackdrop.layerCoordinates` is library-internal, so neither
 * the texture size nor the sampling transform can be adjusted from
 * the app. The [Backdrop] interface itself is public, so this class
 * re-implements the same two halves with the scale folded in:
 *
 * - Recording ([lowResLayerBackdrop]): draws the content normally,
 *   then records it into [graphicsLayer] at
 *   `round(size × scaleFactor)` with the canvas pre-scaled by
 *   [scaleFactor] about the top-left, so texture texel (x, y) holds
 *   content point (x / scaleFactor, y / scaleFactor).
 * - Sampling ([drawBackdrop]): Kyant's own math — translate by the
 *   negated offset of the consumer within the recording node — plus
 *   a `1 / scaleFactor` scale about that same origin, which maps the
 *   shrunken texture back onto the full-size content rect. Consumers
 *   therefore see the real content, in place, just softer (their own
 *   blur/lens chain sits on top as usual). Only blur is lost, never
 *   alignment: the transform pair is exact, not an approximation.
 *
 * One deliberate simplification versus Kyant's `LayerBackdrop`: the
 * `layerBlock` inverse transform is not applied. Kyant uses it to
 * counteract a consumer's own graphics-layer transform while
 * sampling; every consumer of THIS backdrop goes through
 * `Modifier.liquidGlass`, which never passes a layerBlock (the only
 * layerBlock consumer in the app is the tab dock, sampling its own
 * full-res `LayerBackdrop`). If a layerBlock consumer ever targets
 * this backdrop, its sampling will not counteract its own layer
 * transform — a visual offset during that transform, not a crash.
 */
@Stable
class LowResLayerBackdrop internal constructor(
    val graphicsLayer: GraphicsLayer,
    val scaleFactor: Float,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    internal var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        val coordinates = coordinates ?: return
        val layerCoordinates = layerCoordinates ?: return
        withTransform({
            val offset =
                try {
                    layerCoordinates.localPositionOf(coordinates)
                } catch (_: Exception) {
                    // Same fallback as Kyant's LayerBackdrop: outer
                    // transformations can make localPositionOf throw.
                    coordinates.positionInWindow() - layerCoordinates.positionInWindow()
                }
            translate(-offset.x, -offset.y)
            // The texture holds the content at scaleFactor; magnify it
            // back about the recording node's top-left (the point the
            // translate just moved to the origin) so texel
            // (scaleFactor × p) lands exactly on content point p.
            scale(1f / scaleFactor, 1f / scaleFactor, Offset.Zero)
        }) {
            drawLayer(graphicsLayer)
        }
    }
}

@Composable
fun rememberLowResLayerBackdrop(scaleFactor: Float): LowResLayerBackdrop {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer, scaleFactor) {
        LowResLayerBackdrop(graphicsLayer, scaleFactor)
    }
}

/**
 * The recording half of [LowResLayerBackdrop] — the counterpart of
 * Kyant's `Modifier.layerBackdrop`, except the layer is recorded at
 * the backdrop's reduced resolution. Attach to the subtree root that
 * should be sampled; it lives and dies with that subtree, exactly
 * like the full-res recording nodes elsewhere in the app.
 *
 * The parameter is typed as the [Backdrop] interface (call sites hold
 * the backdrop widened to the interface, like every other backdrop in
 * the app) and dispatched safely: a non-[LowResLayerBackdrop] value
 * records nothing and the modifier is a pass-through. The recording
 * node itself needs the concrete class (it reads `scaleFactor` and
 * writes `layerCoordinates` while recording), hence the cast here
 * rather than in the node — same dispatch shape as
 * `rememberScreenBackdropSourceModifier`.
 */
fun Modifier.lowResLayerBackdrop(backdrop: Backdrop): Modifier {
    val lowRes = backdrop as? LowResLayerBackdrop ?: return this
    return this then LowResLayerBackdropElement(lowRes)
}

private class LowResLayerBackdropElement(
    val backdrop: LowResLayerBackdrop,
) : ModifierNodeElement<LowResLayerBackdropNode>() {

    override fun create(): LowResLayerBackdropNode {
        return LowResLayerBackdropNode(backdrop)
    }

    override fun update(node: LowResLayerBackdropNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop = backdrop
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "lowResLayerBackdrop"
        properties["backdrop"] = backdrop
        properties["scaleFactor"] = backdrop.scaleFactor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LowResLayerBackdropElement) return false
        return backdrop == other.backdrop
    }

    override fun hashCode(): Int {
        return backdrop.hashCode()
    }
}

private class LowResLayerBackdropNode(
    var backdrop: LowResLayerBackdrop,
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    override fun ContentDrawScope.draw() {
        drawContent()
        val nodeScope = this
        val scale = backdrop.scaleFactor
        val scaledSize = IntSize(
            (size.width * scale).roundToInt().coerceAtLeast(1),
            (size.height * scale).roundToInt().coerceAtLeast(1),
        )
        // DrawScope.GraphicsLayer.record retargets THIS scope's
        // canvas into the layer for the duration of the block, and
        // the block shares this scope's drawContext — so the canvas
        // scale applied here is the transform the replayed content
        // (drawn through the node's own scope, exactly like Kyant's
        // onDraw) lands under. Content point p is stored at texel
        // (scale × p) in a (scale × size) texture.
        backdrop.graphicsLayer.record(scaledSize) {
            withTransform({ scale(scale, scale, Offset.Zero) }) {
                nodeScope.drawContent()
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            backdrop.layerCoordinates = coordinates
        }
    }

    override fun onDetach() {
        backdrop.layerCoordinates = null
    }
}
