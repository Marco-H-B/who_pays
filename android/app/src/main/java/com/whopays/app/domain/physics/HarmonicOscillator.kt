package com.whopays.app.domain.physics

import kotlin.math.sin

/**
 * Motor físico de oscilación armónica amortiguada.
 * Modela la respuesta elástica de los aros de titanio alrededor de cada dedo.
 * Es invariante respecto a la tasa de refresco (60 Hz, 90 Hz o 120 Hz)
 * gracias al uso estricto del delta time (tiempo transcurrido entre cuadros).
 */
class HarmonicOscillator(
    private val mass: Float = 1.0f,
    private val damping: Float = 14.0f,
    private val stiffness: Float = 180.0f
) {
    /**
     * Calcula el nuevo radio y velocidad tras un intervalo continuo de tiempo dt.
     *
     * @param currentRadius Radio actual en píxeles.
     * @param targetRadius Radio objetivo (ej. 65px en reposo, 85px en tensión máxima).
     * @param velocity Velocidad actual de expansión o contracción.
     * @param dt Delta time (segundos transcurridos desde el cuadro anterior).
     * @return Par ordenado con (nuevoRadio, nuevaVelocidad).
     */
    fun step(
        currentRadius: Float,
        targetRadius: Float,
        velocity: Float,
        dt: Float
    ): Pair<Float, Float> {
        val displacement = currentRadius - targetRadius
        val springForce = -stiffness * displacement
        val dampingForce = -damping * velocity
        val acceleration = (springForce + dampingForce) / mass

        val newVelocity = velocity + acceleration * dt
        val newRadius = currentRadius + newVelocity * dt

        return Pair(newRadius, newVelocity)
    }

    /**
     * Genera la fluctuación orgánica de respiración durante la fase de tensión.
     * La frecuencia se acelera gradualmente de 3.5 Hz a 10.0 Hz de forma calmada y continua.
     *
     * @param elapsedSeconds Segundos continuos transcurridos.
     * @param progress Avance de la barra de tensión de 0.0 a 1.0.
     * @return Amplitud adicional en píxeles (-6f a +6f).
     */
    fun calculateBreathingOffset(elapsedSeconds: Float, progress: Float): Float {
        val frequency = 3.5f + (progress * 6.5f)
        val amplitude = 4f + (progress * 4f)
        return sin(elapsedSeconds * 2.0 * Math.PI * frequency).toFloat() * amplitude
    }
}
