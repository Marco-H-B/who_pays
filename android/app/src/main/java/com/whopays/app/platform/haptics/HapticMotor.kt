package com.whopays.app.platform.haptics

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Controlador de retroalimentación táctil por hardware (motor de vibración háptico).
 * Modula la intensidad de los pulsos para simular la tensión creciente del titanio
 * y el impacto seco final de la decisión ("The Labor Illusion").
 */
class HapticMotor(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Emite un clic sutil instantáneo al apoyar un dedo sobre la pantalla.
     */
    fun touchDown() {
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    /**
     * Emite un pulso táctil durante la fase de tensión.
     * La intensidad aumenta exponencialmente en función del avance continuo de la ronda.
     *
     * @param progress Avance de 0.0 a 1.0.
     */
    fun pulseTension(progress: Float) {
        if (vibrator?.hasVibrator() != true) return

        // Amplitud de 30 a 255 (intensidad de hardware)
        val amplitude = (30 + (progress * progress * 225)).toInt().coerceIn(1, 255)
        val durationMs = (10 + (progress * 15)).toLong()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    /**
     * Emite el golpe final seco y solemne ("Titanium Thud") sobre el dedo seleccionado.
     */
    fun verdictThud() {
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }

    /**
     * Detiene inmediatamente cualquier vibración activa en el dispositivo.
     */
    fun cancel() {
        vibrator?.cancel()
    }
}
