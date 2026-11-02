package com.aurax.imagery.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * One set of motion curves for the whole app, so everything moves the same way: quick to start,
 * long gentle landing, no wobble. Springs are close to critically damped on purpose; a visible
 * bounce reads as toy-like, a soft settle reads as premium.
 */
object AuraMotion {
    /** Material "emphasized decelerate": fast out of the blocks, long soft landing. For things entering. */
    val Enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** "Emphasized accelerate": for things leaving. */
    val Exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Press feedback: instant in, silky out. */
    fun <T> press() = spring<T>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow * 1.4f)

    /** Panels, menus, lenses settling into place. A faint, single, barely visible overshoot at most. */
    fun <T> settle() = spring<T>(dampingRatio = 0.86f, stiffness = 360f)
}
