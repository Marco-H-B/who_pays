package com.whopays.app.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.whopays.app.domain.model.FingerTouch
import com.whopays.app.domain.model.GameState
import com.whopays.app.ui.theme.TitaniumWhite

/**
 * Lienzo multitáctil capacitivo de alta fidelidad.
 * Procesa hasta 10 contactos concurrentes a 60/90/120 Hz sin bloqueos ni retrasos.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MultitouchCanvas(
    fingers: Map<Int, FingerTouch>,
    gameState: GameState,
    breathingOffset: Float,
    onPointerDown: (id: Int, x: Float, y: Float) -> Unit,
    onPointerMove: (id: Int, x: Float, y: Float) -> Unit,
    onPointerUp: (id: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                val action = event.actionMasked
                val index = event.actionIndex
                val pointerId = event.getPointerId(index)

                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        val x = event.getX(index)
                        val y = event.getY(index)
                        onPointerDown(pointerId, x, y)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val pointerCount = event.pointerCount
                        for (i in 0 until pointerCount) {
                            val id = event.getPointerId(i)
                            val x = event.getX(i)
                            val y = event.getY(i)
                            onPointerMove(id, x, y)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        onPointerUp(pointerId)
                        true
                    }
                    else -> false
                }
            }
    ) {
        val loserId = (gameState as? GameState.Verdict)?.loserId

        fingers.values.forEach { finger ->
            val isLoser = loserId != null && finger.id == loserId
            val isFaded = loserId != null && finger.id != loserId

            val alpha = when {
                isFaded -> 0.1f
                isLoser -> 1.0f
                else -> 0.9f
            }

            val baseRadius = 55f
            val dynamicRadius = if (gameState is GameState.Tension) {
                baseRadius + 15f + breathingOffset
            } else if (isLoser) {
                baseRadius + 28f
            } else {
                baseRadius
            }

            val center = Offset(finger.x, finger.y)

            // Núcleo central brillante
            drawCircle(
                color = finger.color.copy(alpha = alpha),
                radius = 24f,
                center = center
            )

            // Aro de titanio externo con oscilación armónica
            drawCircle(
                color = finger.color.copy(alpha = alpha * 0.75f),
                radius = dynamicRadius,
                center = center,
                style = Stroke(width = if (isLoser) 6f else 3.5f)
            )

            // Anillo sutil de resplandor
            if (gameState is GameState.Tension || isLoser) {
                drawCircle(
                    color = finger.color.copy(alpha = alpha * 0.25f),
                    radius = dynamicRadius + 14f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }
        }
    }
}
