package com.bugenzhao.mnga.ui.root

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.storage.DockGlassMaterial
import com.bugenzhao.mnga.storage.GlassCustom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract of the app's glass materials (v1.7.1, Kyant Backdrop).
 *
 * The materials are distinguished on two layers: the resolved
 * [GlassSpec] (veil alphas + the saturation/blur/lens effect chain,
 * calibrated to Kyant's liquid-glass recipe) and the dock's rim palette
 * (dockColors). These tests pin both, plus the spec-resolution
 * semantics the settings sliders rely on: no custom block ⇒ the preset,
 * value for value; a custom block replaces the absolute fields with the
 * preset's per-scheme tint ratios preserved; the highlight trim
 * multiplies the highlight on top of either.
 */
class GlassSpecTest {

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
    fun `preset table matches the Kyant calibration`() {
        val frosted = glassPreset(DockGlassMaterial.FROSTED)
        assertEquals(0.55f, frosted.tintAlphaDark, 0f)
        assertEquals(0.58f, frosted.tintAlphaLight, 0f)
        assertEquals(0.88f, frosted.tintAlphaClassic, 0f)
        assertEquals(20f, frosted.blurDp, 0f)
        assertEquals(1.3f, frosted.saturation, 0f)
        assertEquals(20f, frosted.refractionHeightDp, 0f)
        assertEquals(6f, frosted.refractionAmountDp, 0f)
        assertEquals(0f, frosted.dispersion, 0f)
        assertEquals(0.45f, frosted.highlightAlpha, 0f)

        val clear = glassPreset(DockGlassMaterial.CLEAR)
        assertEquals(0.16f, clear.tintAlphaDark, 0f)
        assertEquals(0.18f, clear.tintAlphaLight, 0f)
        assertEquals(0.45f, clear.tintAlphaClassic, 0f)
        assertEquals(6f, clear.blurDp, 0f)
        assertEquals(1.8f, clear.saturation, 0f)
        assertEquals(16f, clear.refractionHeightDp, 0f)
        assertEquals(10f, clear.refractionAmountDp, 0f)
        assertEquals(0f, clear.dispersion, 0f)
        assertEquals(0.8f, clear.highlightAlpha, 0f)

        val liquid = glassPreset(DockGlassMaterial.LIQUID)
        assertEquals(0.32f, liquid.tintAlphaDark, 0f)
        assertEquals(0.32f, liquid.tintAlphaLight, 0f)
        assertEquals(0.22f, liquid.tintAlphaClassic, 0f)
        assertEquals(8f, liquid.blurDp, 0f)
        assertEquals(1.5f, liquid.saturation, 0f)
        assertEquals(24f, liquid.refractionHeightDp, 0f)
        assertEquals(24f, liquid.refractionAmountDp, 0f)
        assertEquals(0.2f, liquid.dispersion, 0f)
        assertEquals(1.0f, liquid.highlightAlpha, 0f)
    }

    @Test
    fun `material identities stay ordered and uncollapsed`() {
        val frosted = glassPreset(DockGlassMaterial.FROSTED)
        val clear = glassPreset(DockGlassMaterial.CLEAR)
        val liquid = glassPreset(DockGlassMaterial.LIQUID)
        // Veil: frosted thickest, clear thinnest (dark and light alike).
        assertTrue(frosted.tintAlphaDark > liquid.tintAlphaDark)
        assertTrue(liquid.tintAlphaDark > clear.tintAlphaDark)
        assertTrue(frosted.tintAlphaLight > liquid.tintAlphaLight)
        assertTrue(liquid.tintAlphaLight > clear.tintAlphaLight)
        // Blur: frosted biggest, liquid small, clear smallest.
        assertTrue(frosted.blurDp > liquid.blurDp)
        assertTrue(liquid.blurDp > clear.blurDp)
        // Saturation: clear highest, frosted most restrained.
        assertTrue(clear.saturation > liquid.saturation)
        assertTrue(liquid.saturation > frosted.saturation)
        // Lens: liquid strongest, frosted only a whisper.
        assertTrue(liquid.refractionAmountDp > clear.refractionAmountDp)
        assertTrue(clear.refractionAmountDp > frosted.refractionAmountDp)
        // Highlight: liquid strongest, frosted matte.
        assertTrue(liquid.highlightAlpha > clear.highlightAlpha)
        assertTrue(clear.highlightAlpha > frosted.highlightAlpha)
        // Dispersion is liquid-only, and it gates the shader.
        assertTrue(liquid.chromaticAberration)
        assertTrue(!clear.chromaticAberration)
        assertTrue(!frosted.chromaticAberration)
    }

    @Test
    fun `tint bases follow the Kyant recipe hues`() {
        assertEquals(Color(0xFF121212), glassTintBase(DockScheme.Dark))
        assertEquals(Color(0xFFFAFAFA), glassTintBase(DockScheme.Light))
        assertEquals(Color(0xFFFFF6DF), glassTintBase(DockScheme.Classic))
    }

    @Test
    fun `no custom and neutral trim resolves to the preset exactly`() {
        for (material in DockGlassMaterial.entries) {
            assertEquals(
                glassPreset(material),
                resolveGlassSpec(material, custom = null, highlightScale = 1f),
            )
        }
    }

    @Test
    fun `custom block replaces absolutes and scales scheme tints by ratio`() {
        val custom = GlassCustom(
            tintAlpha = 0.40f,
            blurDp = 12f,
            saturation = 1.9f,
            refractionHeightDp = 30f,
            refractionAmountDp = 33f,
            dispersion = 0.3f,
        )
        val spec = resolveGlassSpec(DockGlassMaterial.FROSTED, custom, highlightScale = 1f)
        assertEquals(0.40f, spec.tintAlphaDark, 0f)
        // Frosted's light/dark ratio (0.58/0.55) carries over.
        assertEquals(0.58f * (0.40f / 0.55f), spec.tintAlphaLight, 1e-6f)
        assertEquals(0.88f * (0.40f / 0.55f), spec.tintAlphaClassic, 1e-6f)
        assertEquals(12f, spec.blurDp, 0f)
        assertEquals(1.9f, spec.saturation, 0f)
        assertEquals(30f, spec.refractionHeightDp, 0f)
        assertEquals(33f, spec.refractionAmountDp, 0f)
        assertEquals(0.3f, spec.dispersion, 0f)
        assertTrue(spec.chromaticAberration)
        // Untouched by the custom block: the preset's highlight.
        assertEquals(0.45f, spec.highlightAlpha, 0f)
    }

    @Test
    fun `preset survives a toCustom round-trip`() {
        for (material in DockGlassMaterial.entries) {
            val preset = glassPreset(material)
            assertEquals(
                preset,
                resolveGlassSpec(material, preset.toCustom(), highlightScale = 1f),
            )
        }
    }

    @Test
    fun `highlight trim multiplies and clamps`() {
        val halved = resolveGlassSpec(DockGlassMaterial.LIQUID, null, highlightScale = 0.5f)
        assertEquals(0.5f, halved.highlightAlpha, 0f)
        assertEquals(0.5f, halved.highlightScale, 0f)
        val doubled = resolveGlassSpec(DockGlassMaterial.LIQUID, null, highlightScale = 2f)
        // Liquid already sits at alpha 1.0; the trim clamps, not wraps.
        assertEquals(1.0f, doubled.highlightAlpha, 0f)
        assertEquals(2f, doubled.highlightScale, 0f)
        val off = resolveGlassSpec(DockGlassMaterial.CLEAR, null, highlightScale = 0f)
        assertEquals(0f, off.highlightAlpha, 0f)
    }

    @Test
    fun `rim light strengthens from frosted to clear to liquid`() {
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

    @Test
    fun `menu width cap is the agreed 280dp`() {
        assertEquals(280.dp, com.bugenzhao.mnga.ui.components.MiuixMenuMaxWidth)
    }

    @Test
    fun `lens variant scaling weakens big faces and caps the large band`() {
        val liquid = glassPreset(DockGlassMaterial.LIQUID) // lens 24 / 24
        // SMALL is the identity (same instance).
        assertTrue(liquid.withLensScale(GlassVariantScale.SMALL) === liquid)
        val medium = liquid.withLensScale(GlassVariantScale.MEDIUM)
        assertEquals(24f * 0.6f, medium.refractionHeightDp, 0f)
        assertEquals(24f * 0.6f, medium.refractionAmountDp, 0f)
        val large = liquid.withLensScale(GlassVariantScale.LARGE)
        assertEquals(GlassVariantScale.LARGE_BAND_CAP_DP, large.refractionHeightDp, 0f)
        assertEquals(24f * 0.35f, large.refractionAmountDp, 0.001f)
        // Only the lens changes; the material identity stays.
        assertEquals(liquid.tintAlphaDark, large.tintAlphaDark, 0f)
        assertEquals(liquid.blurDp, large.blurDp, 0f)
        assertEquals(liquid.saturation, large.saturation, 0f)
        assertEquals(liquid.dispersion, large.dispersion, 0f)
    }
}
