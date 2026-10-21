package com.whopays.app.platform.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Motor de síntesis procedural de sonido (generador de ondas de audio en memoria).
 * Genera tonos armónicos en tiempo real acoplados a la física táctil sin requerir archivos MP3 externos.
 */
class ProceduralSoundEngine {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)
    private var droneJob: Job? = null

    /**
     * Emite un pop metálico percusivo en 100 Hz al detectar un nuevo dedo.
     */
    fun playTouchDownPop() {
        scope.launch {
            val durationMs = 40
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val frequency = 120.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / numSamples) // Decaimiento lineal
                val sample = (sin(2.0 * Math.PI * frequency * t) * envelope * Short.MAX_VALUE * 0.4).toInt()
                buffer[i] = sample.toShort()
            }

            playBuffer(buffer)
        }
    }

    /**
     * Emite el golpe final seco ("Titanium Thud") en 60 Hz con caída sub-grave.
     */
    fun playVerdictThud() {
        scope.launch {
            val durationMs = 200
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val frequency = 60.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = (1.0 - (i.toDouble() / numSamples)) * (1.0 - (i.toDouble() / numSamples))
                val sample = (sin(2.0 * Math.PI * frequency * t) * envelope * Short.MAX_VALUE * 0.85).toInt()
                buffer[i] = sample.toShort()
            }

            playBuffer(buffer)
        }
    }

    /**
     * Reproduce un búfer PCM directamente a través de AudioTrack en modo estático de baja latencia.
     */
    private fun playBuffer(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            // Liberar memoria tras la reproducción
            scope.launch {
                kotlinx.coroutines.delay(300)
                audioTrack.stop()
                audioTrack.release()
            }
        } catch (_: Exception) {
            // Silencioso en caso de restricciones de hardware
        }
    }

    /**
     * Detiene cualquier sonido procedural continuo.
     */
    fun stopAll() {
        droneJob?.cancel()
        droneJob = null
    }
}
