package com.bugenzhao.mnga.ui.root

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.storage.DockGlassMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract of the tab dock's glass materials (v1.7.0).
 *
 * The dock renders without backdrop sampling (see MainTabDock.kt), so the
 * three materials of liquid-glass-recipes are distinguished by veil
 * thickness, rim brightness and edge highlights only. These tests pin the
 * mapping so the materials can never collapse into relabels of each other:
 * FROSTED must stay byte-identical to the look the dock shipped with,
 * CLEAR thins the veil to about half of frosted, LIQUID to about half of
 * clear again while turning on the bottom-edge highlight and the sheen
 * that the other two keep off.
 */
class DockGlassMaterialTest {

    @Test
    fun `material raw mapping round-trips and falls back to frosted`() {
        assertEquals(DockGlassMaterial.CLEAR, DockGlassMaterial.fromRaw(0))
        assertEquals(DockGlassMaterial.FROSTED, DockGlassMaterial.fromRaw(1))
        assertEquals(DockGlassMaterial.LIQUID, DockGlassMaterial.fromRaw(2))
        assertEquals(DockGlassMaterial.FROSTED, DockGlassMaterial.fromRaw(99))
        assertEquals(DockGlassMaterial.FROSTED, DockGlassMaterial.fromRaw(-1))
        // The storage default is FROSTED (the shipped v1.7.0 look).
        assertEquals(1, DockGlassMaterial.FROSTED.raw)
    }

    @Test
    fun `frosted stays identical to the shipped dock palette`() {
        val dark = dockColors(DockScheme.Dark, DockGlassMaterial.FROSTED)
        assertEquals(Color(0xFFD4D5DB).copy(alpha = 0.48f), dark.glass)
        assertEquals(Color.White.copy(alpha = 0.28f), dark.border)
        assertEquals(Color.Black.copy(alpha = 0.18f), dark.pill)
        assertEquals(Color.White.copy(alpha = 0.30f), dark.highlight)
        assertEquals(14.dp, dark.highlightHeight)

        val light = dockColors(DockScheme.Light, DockGlassMaterial.FROSTED)
        assertEquals(Color.White.copy(alpha = 0.62f), light.glass)
        assertEquals(Color.Black.copy(alpha = 0.07f), light.border)
        assertEquals(Color.White.copy(alpha = 0.50f), light.highlight)

        val classic = dockColors(DockScheme.Classic, DockGlassMaterial.FROSTED)
        assertEquals(Color(0xFFFFF6DF).copy(alpha = 0.88f), classic.glass)
        assertEquals(Color(0xFFD9C48F).copy(alpha = 0.8f), classic.border)
        assertEquals(Color.White.copy(alpha = 0.45f), classic.highlight)
    }

    @Test
    fun `veil thins strictly from frosted to clear to liquid in every scheme`() {
        for (scheme in DockScheme.entries) {
            val frosted = dockColors(scheme, DockGlassMaterial.FROSTED).glass.alpha
            val clear = dockColors(scheme, DockGlassMaterial.CLEAR).glass.alpha
            val liquid = dockColors(scheme, DockGlassMaterial.LIQUID).glass.alpha
            assertTrue("$scheme frosted>clear", frosted > clear)
            assertTrue("$scheme clear>liquid", clear > liquid)
            // "About half each step" is part of the recipe distinction.
            assertTrue("$scheme clear≈frosted/2", clear < frosted * 0.65f)
            assertTrue("$scheme liquid≈clear/2", liquid < clear * 0.65f)
        }
    }

    @Test
    fun `edge light strengthens from frosted to clear to liquid`() {
        for (scheme in DockScheme.entries) {
            val frosted = dockColors(scheme, DockGlassMaterial.FROSTED)
            val clear = dockColors(scheme, DockGlassMaterial.CLEAR)
            val liquid = dockColors(scheme, DockGlassMaterial.LIQUID)
            assertTrue("$scheme highlight alpha", liquid.highlight.alpha >= clear.highlight.alpha)
            assertTrue(
                "$scheme highlight alpha vs frosted",
                clear.highlight.alpha > frosted.highlight.alpha,
            )
            assertTrue(
                "$scheme highlight sharpens",
                frosted.highlightHeight > clear.highlightHeight &&
                    clear.highlightHeight > liquid.highlightHeight,
            )
        }
    }

    @Test
    fun `bottom highlight and sheen are liquid-only`() {
        for (scheme in DockScheme.entries) {
            for (material in listOf(DockGlassMaterial.FROSTED, DockGlassMaterial.CLEAR)) {
                val colors = dockColors(scheme, material)
                assertEquals("$scheme $material bottom", 0f, colors.bottomHighlight.alpha, 0f)
                assertEquals("$scheme $material sheen", 0f, colors.sheen.alpha, 0f)
            }
            val liquid = dockColors(scheme, DockGlassMaterial.LIQUID)
            assertTrue("$scheme liquid bottom on", liquid.bottomHighlight.alpha > 0f)
            assertTrue("$scheme liquid sheen on", liquid.sheen.alpha > 0f)
        }
    }
}
