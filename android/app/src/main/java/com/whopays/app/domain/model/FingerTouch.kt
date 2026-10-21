package com.whopays.app.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Representa el contacto físico de un dedo sobre la pantalla capacitiva.
 * Se identifica de forma única mediante su puntero de hardware (pointerId).
 */
data class FingerTouch(
    val id: Int,
    val x: Float,
    val y: Float,
    val color: Color,
    val touchDownNanos: Long = System.nanoTime(),
    val currentRadius: Float = 60f,
    val targetRadius: Float = 60f,
    val velocity: Float = 0f
)
