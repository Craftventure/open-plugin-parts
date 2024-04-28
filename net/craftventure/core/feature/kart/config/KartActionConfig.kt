package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.instrument.InstrumentType
import net.craftventure.core.feature.kart.actions.*

@JsonClass(generateAdapter = true)
data class KartActionConfig(
    val type: String,
    val data: String? = null,
    val timeout: Int = 5000,
    val allowPitch: Boolean = false,
    val volume: Float = 1f,
) {
    fun toAction() = when (type) {
        "tank/turret" -> TankTurretAction()
        "atat/head" -> AtAtAction()
        "panda/roll" -> PandaRollAction()
        "nbssong/stop" -> StopNbsSongAction()
        "nbssong/start" -> StartNbsSongAction(InstrumentType.valueOf(data!!))
        "horn" -> KartHornAction(data!!.split(","), timeout, allowPitch, volume)
        else -> null
    }
}