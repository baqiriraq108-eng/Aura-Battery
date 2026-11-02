package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Custom Material 3 Expressive Tactile Bounce modifier.
 * Provides fine-tuned physics depth and spring rebound on press.
 */
fun Modifier.expressiveBounceClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.93f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // Physics spring with medium bounce and lively responsiveness
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ExpressiveBounceScale"
    )

    // Physical depth translation
    val depthTranslation by animateDpAsState(
        targetValue = if (isPressed && enabled) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ExpressiveBounceDepth"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            translationY = depthTranslation.toPx()
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Custom physical bounce handles the visual cue cleanly
            enabled = enabled
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}
