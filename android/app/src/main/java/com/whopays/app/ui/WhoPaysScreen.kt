package com.whopays.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whopays.app.domain.model.FingerTouch
import com.whopays.app.domain.model.GameState
import com.whopays.app.domain.physics.HarmonicOscillator
import com.whopays.app.domain.physics.SecureRandomSelector
import com.whopays.app.platform.audio.ProceduralSoundEngine
import com.whopays.app.platform.haptics.HapticMotor
import com.whopays.app.ui.components.HeaderCapsule
import com.whopays.app.ui.components.MultitouchCanvas
import com.whopays.app.ui.components.PaywallModal
import com.whopays.app.ui.components.VerdictOverlay
import com.whopays.app.ui.theme.FingerPalette
import com.whopays.app.ui.theme.TitaniumBg
import com.whopays.app.ui.theme.TitaniumMuted
import kotlinx.coroutines.isActive

/**
 * Pantalla principal del juego.
 * Gobierna el ciclo de vida continuo mediante tiempo monótono de hardware (withFrameNanos)
 * garantizando que la ronda dure exactamente 3.000 segundos sin importar la tasa de refresco (60/90/120 Hz).
 */
@Composable
fun WhoPaysScreen() {
    val context = LocalContext.current

    val hapticMotor = remember { HapticMotor(context) }
    val soundEngine = remember { ProceduralSoundEngine() }
    val oscillator = remember { HarmonicOscillator() }
    val selector = remember { SecureRandomSelector() }

    val fingers = remember { mutableStateMapOf<Int, FingerTouch>() }
    var gameState by remember { mutableStateOf<GameState>(GameState.Idle) }
    var breathingOffset by remember { mutableFloatStateOf(0f) }
    var isMultiplayer by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var ttlSeconds by remember { mutableIntStateOf(180) }

    var tensionStartNanos by remember { mutableLongStateOf(0L) }
    val tensionDurationNanos = 3_000_000_000L // 3.000 segundos atómicos

    // Temporizador TTL de la ventana de cobro (180 segundos)
    LaunchedEffect(showPaywall) {
        if (showPaywall) {
            ttlSeconds = 180
            while (isActive && ttlSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                ttlSeconds--
            }
            if (ttlSeconds == 0) {
                showPaywall = false
            }
        }
    }

    // Bucle continuo de renderizado a nivel de fotograma (invarianza 60/90/120 Hz)
    LaunchedEffect(gameState, fingers.size) {
        if (gameState is GameState.Tension) {
            var lastPulseStep = 0
            while (isActive && gameState is GameState.Tension) {
                withFrameNanos { nowNanos ->
                    val elapsedNanos = nowNanos - tensionStartNanos
                    val progress = (elapsedNanos.toFloat() / tensionDurationNanos).coerceIn(0f, 1f)
                    val elapsedSeconds = elapsedNanos / 1_000_000_000f

                    breathingOffset = oscillator.calculateBreathingOffset(elapsedSeconds, progress)

                    // Emisión de pulsos hápticos crecientes a medida que aumenta la tensión
                    val currentStep = (progress * 12).toInt()
                    if (currentStep > lastPulseStep) {
                        lastPulseStep = currentStep
                        hapticMotor.pulseTension(progress)
                    }

                    if (elapsedNanos >= tensionDurationNanos) {
                        // Veredicto alcanzado: seleccionar perdedor
                        val fingerList = fingers.values.toList()
                        if (fingerList.size >= 2) {
                            val loserIdx = selector.selectLoserIndex(fingerList.size)
                            val loserFinger = fingerList[loserIdx]
                            gameState = GameState.Verdict(loserFinger.id)
                            hapticMotor.verdictThud()
                            soundEngine.playVerdictThud()
                        } else {
                            gameState = GameState.Idle
                        }
                    } else {
                        gameState = GameState.Tension(
                            progress = progress,
                            elapsedMs = elapsedNanos / 1_000_000L
                        )
                    }
                }
            }
        }
    }

    fun onPointerDown(id: Int, x: Float, y: Float) {
        if (gameState is GameState.Verdict) {
            // Reiniciar ronda anterior si tocan de nuevo
            gameState = GameState.Idle
            fingers.clear()
        }

        val colorIndex = (id + fingers.size) % FingerPalette.size
        val newFinger = FingerTouch(
            id = id,
            x = x,
            y = y,
            color = FingerPalette[colorIndex]
        )
        fingers[id] = newFinger

        hapticMotor.touchDown()
        soundEngine.playTouchDownPop()

        // Regla de Negocio: Si se detectan 6 o más dedos en la versión gratuita, activar Paywall
        if (fingers.size >= 6) {
            gameState = GameState.Idle
            hapticMotor.cancel()
            soundEngine.stopAll()
            showPaywall = true
            return
        }

        // Si hay al menos 2 dedos, arrancar la ventana de tensión de 3.000 segundos
        if (fingers.size >= 2 && gameState !is GameState.Tension) {
            tensionStartNanos = System.nanoTime()
            gameState = GameState.Tension(progress = 0f, elapsedMs = 0L)
        }
    }

    fun onPointerMove(id: Int, x: Float, y: Float) {
        val current = fingers[id]
        if (current != null) {
            fingers[id] = current.copy(x = x, y = y)
        }
    }

    fun onPointerUp(id: Int) {
        fingers.remove(id)

        // Si se retiran dedos y quedan menos de 2 durante la tensión, cancelar la ronda
        if (gameState is GameState.Tension && fingers.size < 2) {
            gameState = GameState.Idle
            hapticMotor.cancel()
            soundEngine.stopAll()
            breathingOffset = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TitaniumBg)
    ) {
        // Lienzo de interacción multitáctil
        MultitouchCanvas(
            fingers = fingers,
            gameState = gameState,
            breathingOffset = breathingOffset,
            onPointerDown = ::onPointerDown,
            onPointerMove = ::onPointerMove,
            onPointerUp = ::onPointerUp
        )

        // Cápsula superior unificada de 36dp
        HeaderCapsule(
            isMultiplayer = isMultiplayer,
            onModeChange = { multiplayer ->
                if (multiplayer) {
                    showPaywall = true
                } else {
                    isMultiplayer = false
                }
            },
            onAuthClick = {
                showPaywall = true
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // Guía en reposo cuando no hay dedos
        if (gameState is GameState.Idle && fingers.isEmpty()) {
            Text(
                text = "Coloquen sus dedos en la pantalla",
                color = TitaniumMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
        }

        // Superposición de Veredicto
        VerdictOverlay(
            isVisible = gameState is GameState.Verdict,
            onReset = {
                gameState = GameState.Idle
                fingers.clear()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Modal de Cobro (Paywall) con TTL de 3 minutos
        PaywallModal(
            isVisible = showPaywall,
            ttlSecondsRemaining = ttlSeconds,
            onDismiss = { showPaywall = false },
            onPurchaseSinglePass = {
                // Integración de Google Play Billing o Lemon Squeezy
                showPaywall = false
            },
            onSubscribePro = {
                // Integración de Suscripción PRO
                showPaywall = false
            }
        )
    }
}
