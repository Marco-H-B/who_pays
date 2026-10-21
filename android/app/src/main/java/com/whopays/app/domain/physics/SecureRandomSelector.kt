package com.whopays.app.domain.physics

import java.security.SecureRandom

/**
 * Selector aleatorio criptográficamente seguro (CSPRNG).
 * Garantiza que la elección del comensal perdedor sea matemáticamente
 * impredecible y no manipulable, eliminando cualquier sesgo.
 */
class SecureRandomSelector {

    private val secureRandom = SecureRandom()

    /**
     * Selecciona el índice del perdedor dentro de una lista de tamaño N.
     *
     * @param count Cantidad total de dedos o participantes activos (N >= 2).
     * @return Índice uniforme en el intervalo [0, count - 1].
     */
    fun selectLoserIndex(count: Int): Int {
        require(count >= 2) { "Se requieren al menos 2 participantes para seleccionar." }
        return secureRandom.nextInt(count)
    }
}
