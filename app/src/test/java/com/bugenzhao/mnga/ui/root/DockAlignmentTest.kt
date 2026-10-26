package com.bugenzhao.mnga.ui.root

import androidx.compose.ui.graphics.Color
import com.bugenzhao.mnga.storage.DockGlassMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract of the v1.7.8 dock glass alignment with the sister forks.
 *
 * The three-fork audit found the spec table itself (veil / blur /
 * saturation / lens — see GlassSpecTest) already identical across the
 * forks; the visible gap was in three places: the LIQUID shell's
 * surround values (dual-peak rim, halo base, pinned shadow — the
 * sister dock's, in place of the hand-drawn border/bands) and the
 * eight-step halo decay table, both pinned here. The resting pill's
 * fill is also pinned here — at the v1.7.7 values, NOT the sister
 * fork's: v1.7.8 shipped the aligned fill (#26262B α0.60 / #3C3D42
 * α0.80) over a full-chain pad and the selected glyphs sank on
 * device, because in this fork's tabsBackdrop stack the glyphs reach
 * the screen through the pill's sampled texture, where a heavy fill
 * and any blur land directly on them (v1.7.9 hotfix). The sampling
 * fix (page background inside every recording) is a
 * composition-structure change and has no JVM-level surface; it is
 * covered by the on-device checks in the v1.7.8 release notes.
 */
class DockAlignmentTest {

    @Test
    fun `liquid pill fill is the restored near-black film`() {
        // v1.7.9 hotfix: NOT the sister fork's fork.3 fill. In this
        // fork the selected glyphs are delivered inside the pill's
        // sampled texture, so #26262B α0.60 (and the pad's blur) sank
        // them on device; the v1.7.7 film is the readable recipe here.
        assertEquals(
            Color.Black.copy(alpha = 0.12f),
            dockColors(DockScheme.Dark, DockGlassMaterial.LIQUID).pill,
        )
        assertEquals(
            Color.Black.copy(alpha = 0.08f),
            dockColors(DockScheme.Light, DockGlassMaterial.LIQUID).pill,
        )
    }

    @Test
    fun `non-liquid pills keep their shipped identity`() {
        // Only the audited LIQUID fills moved; frosted/clear (and the
        // Classic scheme entirely) keep the palette they shipped with.
        assertEquals(
            Color.Black.copy(alpha = 0.18f),
            dockColors(DockScheme.Dark, DockGlassMaterial.FROSTED).pill,
        )
        assertEquals(
            Color.Black.copy(alpha = 0.14f),
            dockColors(DockScheme.Dark, DockGlassMaterial.CLEAR).pill,
        )
        assertEquals(
            Color.Black.copy(alpha = 0.10f),
            dockColors(DockScheme.Light, DockGlassMaterial.FROSTED).pill,
        )
        assertEquals(
            Color(0xFF4A3B18).copy(alpha = 0.10f),
            dockColors(DockScheme.Classic, DockGlassMaterial.LIQUID).pill,
        )
    }

    @Test
    fun `aligned surround matches the sister dock values`() {
        val dark = dockAlignedSurround(DockScheme.Dark)
        assertEquals(Color.White.copy(alpha = 0.22f), dark.rimTop)
        assertEquals(Color.White.copy(alpha = 0.10f), dark.rimBottom)
        assertEquals(Color.White.copy(alpha = 0.05f), dark.haloBase)
        assertEquals(Color.Black.copy(alpha = 0.22f), dark.shadowColor)

        val light = dockAlignedSurround(DockScheme.Light)
        assertEquals(Color.White.copy(alpha = 0.90f), light.rimTop)
        assertEquals(Color.Black.copy(alpha = 0.10f), light.rimBottom)
        assertEquals(Color.Black.copy(alpha = 0.045f), light.haloBase)
        assertEquals(Color.Black.copy(alpha = 0.10f), light.shadowColor)
    }

    @Test
    fun `halo layers are the eight-step outward decay`() {
        assertEquals(8, DockHaloLayers.size)
        assertEquals(0.75f to 0.30f, DockHaloLayers.first())
        assertEquals(6f to 0.033f, DockHaloLayers.last())
        // Widths step outward, factors decay monotonically — together
        // they composite into a gradient glow, never a hard ring.
        for (i in 1 until DockHaloLayers.size) {
            assertTrue(DockHaloLayers[i].first > DockHaloLayers[i - 1].first)
            assertTrue(DockHaloLayers[i].second < DockHaloLayers[i - 1].second)
        }
        // The outermost layer is effectively a whisper over the base.
        assertEquals(0.033f, DockHaloLayers.last().second, 0f)
    }

    @Test
    fun `aligned rim widths and highlight factor are pinned`() {
        assertEquals(1.5f, DockAlignedRimWidth.value, 0f)
        assertEquals(3f, DockAlignedRimGlowWidth.value, 0f)
        assertEquals(0.25f, DockAlignedRimGlowFactor, 0f)
        assertEquals(0.85f, DockLiquidHighlightScale, 0f)
    }
}
