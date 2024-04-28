package net.craftventure.core.feature.kart.inputcontroller

import net.craftventure.core.feature.kart.Kart


interface KartController {
    /**
     * 1 = left, -1 = right
     */
    fun sideways(): Float

    /**
     * 1 = forward, -1 = backward
     */
    fun forward(): Float

    fun isHandbraking(): Boolean
    fun isDismounting(): Boolean

    fun start(kart: Kart)
    fun stop()
    fun resetValues()
}
