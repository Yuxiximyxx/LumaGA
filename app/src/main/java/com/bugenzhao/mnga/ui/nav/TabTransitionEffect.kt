package com.bugenzhao.mnga.ui.nav

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.xah.navigation.model.anim.TransitionEffect
import com.xah.navigation.model.anim.effect.BackgroundPageEffectState
import com.xah.navigation.model.anim.effect.EffectValue
import com.xah.navigation.model.anim.effect.ForegroundPageEffectState
import com.xah.navigation.model.anim.effect.PageEffect
import com.xah.navigation.model.anim.effect.PageEffects

/**
 * Transition for the two main tabs (Home ⇄ Mine).
 *
 * Until v1.7.5 this was the library's stock FadeTransitionEffect: the
 * incoming page fades in over a completely static, fully opaque
 * outgoing page (its PageEffects set no background alpha at all), with
 * symmetric 280ms in both directions — the outgoing side of the switch
 * reads as a hard swap rather than half of a crossfade, which is what
 * prompted the user's "add animations between pages" pass.
 *
 * This effect keeps the fade family (tab switches are not pushes in
 * spirit — no card geometry, no corners, no blur) but makes it a real
 * two-way crossfade in the app's transition language:
 * - the incoming page fades in while settling from a 0.985 scale;
 * - the outgoing page fades down to 0.45 alpha (over the opaque theme
 *   background the shell paints beneath the host) and recedes to 0.99,
 *   so both pages visibly move;
 * - timing is asymmetric like the dock's reappearance (v1.7.3): the
 *   enter runs 280ms, the exit a shorter 220ms, both easeOut.
 *
 * Both states mirror, so a pop — and the predictive-back preview of
 * leaving Mine — runs the same geometry in reverse. The standard
 * push/pop effect (Default + CardPageEffects) is untouched.
 */
data class TabTransitionEffect(
    override val pageEffect: PageEffects = TabPageEffects(),
    override val predictiveMinValue: Float = 0.75f,
    override val pushAnimation: AnimationSpec<Float> = tween(280, easing = EaseOut),
    override val popAnimation: AnimationSpec<Float> = tween(220, easing = EaseOut),
) : TransitionEffect

fun TabPageEffects(): PageEffects = PageEffects(
    backgroundEffect = BackgroundPageEffectState(
        enableMirror = true,
        effect = PageEffect(
            corner = EffectValue.const(RoundedCornerShape(0.dp)),
            alpha = EffectValue(start = 1f, end = 0.45f),
            scale = EffectValue(start = 1f, end = 0.99f),
        ),
    ),
    foregroundEffect = ForegroundPageEffectState(
        enableMirror = true,
        effect = PageEffect(
            corner = EffectValue.const(RoundedCornerShape(0.dp)),
            alpha = EffectValue(start = 0f, end = 1f),
            scale = EffectValue(start = 0.985f, end = 1f),
        ),
    ),
)
