package com.bugenzhao.mnga.ui.root

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.storage.ColorSchemeMode
import com.bugenzhao.mnga.storage.DockGlassMaterial
import com.bugenzhao.mnga.storage.GlassCustom

/**
 * The fully-resolved glass parameters every glass surface in the app
 * (tab dock, floating buttons, page capsule, image-viewer buttons) is
 * rendered with: veil alpha per color scheme, the Kyant effect chain
 * (saturation / blur / lens) and the specular highlight strength.
 *
 * Calibration follows Kyant0/AndroidLiquidGlass (the library doing the
 * rendering) and the three liquid-glass-recipes identities, cross-checked
 * against the sister c001apk fork's miuix16/17 spec table:
 *
 * - LIQUID: the Kyant recipe itself — extremely thin veil (dark
 *   #121212 α0.32 / light #FAFAFA α0.32), small blur 8 so the backdrop
 *   stays readable, saturation 1.5 (Kyant's `vibrancy()`), the full
 *   24/24dp lens with dispersion, the strongest highlight.
 * - CLEAR （毛玻璃）: the thinnest veil, the smallest blur (6) and the
 *   highest saturation (1.8) — the most transparent, airiest glass —
 *   with a medium lens (16/10) and a bright highlight.
 * - FROSTED （磨砂， the default): the thickest veil and the big blur
 *   (20) with restrained saturation (1.3) — matte — and only a whisper
 *   of lens (20/6). The veil alphas (0.55 dark / 0.58 light) plus the
 *   saturation keep it from collapsing into the flat white board the
 *   v1.7.0 approximation shipped with.
 *
 * The Classic scheme keeps its cream veil hue; its alphas preserve the
 * shipped per-material identity (frosted 0.88 / clear 0.45 / liquid
 * 0.22), the sepia theme needing a denser veil to read on its light
 * background.
 */
data class GlassSpec(
    val tintAlphaDark: Float,
    val tintAlphaLight: Float,
    val tintAlphaClassic: Float,
    val blurDp: Float,
    val saturation: Float,
    val refractionHeightDp: Float,
    val refractionAmountDp: Float,
    /** 0 = lens dispersion shader off; >0 = on (Kyant's dispersion is a
     * fixed-strength shader, so the value only gates it — its strength
     * corresponds to the recipes' ≈0.2 step). */
    val dispersion: Float,
    /** Kyant Highlight alpha after the user's highlight trim. */
    val highlightAlpha: Float,
    /** The user's highlight trim (0–2, 1 = preset); also scales the
     * dock's own rim-light bands. */
    val highlightScale: Float,
) {
    fun tintAlpha(scheme: DockScheme): Float = when (scheme) {
        DockScheme.Dark -> tintAlphaDark
        DockScheme.Light -> tintAlphaLight
        DockScheme.Classic -> tintAlphaClassic
    }

    val chromaticAberration: Boolean get() = dispersion > 0f

    /** Materialize this spec as a stored slider block (dark-anchored). */
    fun toCustom(): GlassCustom = GlassCustom(
        tintAlpha = tintAlphaDark,
        blurDp = blurDp,
        saturation = saturation,
        refractionHeightDp = refractionHeightDp,
        refractionAmountDp = refractionAmountDp,
        dispersion = dispersion,
    )
}

/** Veil hue per scheme; the material identity lives in the alpha. */
fun glassTintBase(scheme: DockScheme): Color = when (scheme) {
    DockScheme.Dark -> Color(0xFF121212)
    DockScheme.Light -> Color(0xFFFAFAFA)
    DockScheme.Classic -> Color(0xFFFFF6DF)
}

/** Foreground (icon/text) color that stays legible on the glass. */
fun glassContentColor(scheme: DockScheme): Color = when (scheme) {
    DockScheme.Dark -> Color(0xFFF2F2F7)
    DockScheme.Light -> Color(0xFF3A3A3C)
    DockScheme.Classic -> Color(0xFF6B5D3E)
}

/** The three preset rows of the calibration table (highlight trim = 1). */
fun glassPreset(material: DockGlassMaterial): GlassSpec = when (material) {
    DockGlassMaterial.FROSTED -> GlassSpec(
        tintAlphaDark = 0.55f,
        tintAlphaLight = 0.58f,
        tintAlphaClassic = 0.88f,
        blurDp = 20f,
        saturation = 1.3f,
        refractionHeightDp = 20f,
        refractionAmountDp = 6f,
        dispersion = 0f,
        highlightAlpha = 0.45f,
        highlightScale = 1f,
    )
    DockGlassMaterial.CLEAR -> GlassSpec(
        tintAlphaDark = 0.16f,
        tintAlphaLight = 0.18f,
        tintAlphaClassic = 0.45f,
        blurDp = 6f,
        saturation = 1.8f,
        refractionHeightDp = 16f,
        refractionAmountDp = 10f,
        dispersion = 0f,
        highlightAlpha = 0.8f,
        highlightScale = 1f,
    )
    DockGlassMaterial.LIQUID -> GlassSpec(
        tintAlphaDark = 0.32f,
        tintAlphaLight = 0.32f,
        tintAlphaClassic = 0.22f,
        blurDp = 8f,
        saturation = 1.5f,
        refractionHeightDp = 24f,
        refractionAmountDp = 24f,
        dispersion = 0.2f,
        highlightAlpha = 1.0f,
        highlightScale = 1f,
    )
}

/**
 * The single resolution point for the effective spec: with no custom
 * block and a neutral highlight trim the result is the preset, value for
 * value (upgrades keep the look they had). A custom block replaces the
 * absolute fields; its dark-anchored tint scales the preset's light and
 * classic alphas by the same ratio. The highlight trim multiplies the
 * highlight alpha (clamped to 1) on top of either source.
 */
fun resolveGlassSpec(
    material: DockGlassMaterial,
    custom: GlassCustom?,
    highlightScale: Float,
): GlassSpec {
    val preset = glassPreset(material)
    val base = if (custom == null) {
        preset
    } else {
        val tintRatio = if (preset.tintAlphaDark > 0f) {
            custom.tintAlpha / preset.tintAlphaDark
        } else {
            1f
        }
        preset.copy(
            tintAlphaDark = custom.tintAlpha,
            tintAlphaLight = (preset.tintAlphaLight * tintRatio).coerceIn(0f, 1f),
            tintAlphaClassic = (preset.tintAlphaClassic * tintRatio).coerceIn(0f, 1f),
            blurDp = custom.blurDp,
            saturation = custom.saturation,
            refractionHeightDp = custom.refractionHeightDp,
            refractionAmountDp = custom.refractionAmountDp,
            dispersion = custom.dispersion,
        )
    }
    if (highlightScale == 1f) return base
    return base.copy(
        highlightAlpha = (base.highlightAlpha * highlightScale).coerceIn(0f, 1f),
        highlightScale = highlightScale,
    )
}

/** Slider ranges of the glass fine-tuning section (settings). */
object GlassRanges {
    val TINT_ALPHA = 0f..0.8f
    val REFRACTION_AMOUNT_DP = 0f..48f
    val REFRACTION_HEIGHT_DP = 0f..40f
    val BLUR_DP = 0f..30f
    val SATURATION = 1.0f..2.2f
    val DISPERSION = 0f..0.5f
    val HIGHLIGHT_SCALE = 0f..2f
}

fun dockSchemeFor(mode: ColorSchemeMode, systemInDark: Boolean): DockScheme = when (mode) {
    ColorSchemeMode.CLASSIC -> DockScheme.Classic
    ColorSchemeMode.DARK -> DockScheme.Dark
    ColorSchemeMode.LIGHT -> DockScheme.Light
    ColorSchemeMode.AUTO -> if (systemInDark) DockScheme.Dark else DockScheme.Light
}

/** Everything a glass surface needs, resolved from the live settings. */
data class GlassSettings(
    val material: DockGlassMaterial,
    val custom: GlassCustom?,
    val highlightScale: Float,
    val spec: GlassSpec,
    val scheme: DockScheme,
    /** Diagnostic mode (settings toggle): render the raw sample. */
    val debugShowSample: Boolean = false,
)

/**
 * Subscribe to the glass settings and resolve the effective spec.
 * Reading through this helper is what makes slider drags take effect
 * app-wide the moment they happen.
 */
@Composable
fun rememberGlassSettings(): GlassSettings {
    val prefs = App.prefs
    val materialRaw by prefs.tabDockGlassMaterialRaw.flow.collectAsState()
    val colorSchemeRaw by prefs.colorSchemeRaw.flow.collectAsState()
    val customEnabled by prefs.glassCustomEnabled.flow.collectAsState()
    val cTint by prefs.glassCustomTintAlpha.flow.collectAsState()
    val cBlur by prefs.glassCustomBlur.flow.collectAsState()
    val cSaturation by prefs.glassCustomSaturation.flow.collectAsState()
    val cRefrHeight by prefs.glassCustomRefractionHeight.flow.collectAsState()
    val cRefrAmount by prefs.glassCustomRefractionAmount.flow.collectAsState()
    val cDispersion by prefs.glassCustomDispersion.flow.collectAsState()
    val highlightScale by prefs.glassHighlightScale.flow.collectAsState()
    val debugShowSample by prefs.glassDebugShowSample.flow.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val material = DockGlassMaterial.fromRaw(materialRaw)
    val custom = if (customEnabled) {
        GlassCustom(
            tintAlpha = cTint,
            blurDp = cBlur,
            saturation = cSaturation,
            refractionHeightDp = cRefrHeight,
            refractionAmountDp = cRefrAmount,
            dispersion = cDispersion,
        )
    } else {
        null
    }
    return GlassSettings(
        material = material,
        custom = custom,
        highlightScale = highlightScale,
        spec = resolveGlassSpec(material, custom, highlightScale),
        scheme = dockSchemeFor(ColorSchemeMode.fromRaw(colorSchemeRaw), systemDark),
        debugShowSample = debugShowSample,
    )
}
