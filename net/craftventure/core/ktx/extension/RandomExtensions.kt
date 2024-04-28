package net.craftventure.core.ktx.extension

import java.util.*

fun Random.nextDoubleRange(min: Double, max: Double): Double {
    assert(min < max)
    return min + (nextDouble() * (max - min))//.also {
    //        Logger.info("Double in range ${min.format(2)} - ${max.format(2)} returned ${it.format(2)}")
    //}
}