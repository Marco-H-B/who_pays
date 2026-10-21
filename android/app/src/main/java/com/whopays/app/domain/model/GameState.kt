package com.whopays.app.domain.model

/**
 * Máquina de estados finitos que gobierna el ciclo de vida de una ronda de selección.
 */
sealed class GameState {
    /** Estado en reposo: No hay suficientes dedos para arrancar la ronda (< 2 dedos). */
    object Idle : GameState()

    /**
     * Fase de tensión: Hay 2 o más dedos colocados de forma ininterrumpida.
     * @param progress Valor de 0.0 a 1.0 que representa el avance temporal continuo.
     * @param elapsedMs Milisegundos transcurridos desde que se alcanzó el quorum (mínimo de jugadores).
     */
    data class Tension(
        val progress: Float,
        val elapsedMs: Long
    ) : GameState()

    /**
     * Fase de veredicto: Se cumplieron los 3.000 segundos y se determinó el perdedor.
     * @param loserId Identificador del dedo seleccionado por el motor aleatorio.
     */
    data class Verdict(
        val loserId: Int
    ) : GameState()

    /**
     * Bloqueo por Paywall: Se detectaron 6 o más dedos en la versión gratuita.
     */
    object Paywall : GameState()
}
