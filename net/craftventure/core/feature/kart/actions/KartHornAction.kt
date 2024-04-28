package net.craftventure.core.feature.kart.actions

import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.kart.KartAction
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import kotlin.math.absoluteValue

class KartHornAction(
    val sounds: List<String>,
    val timeout: Int = 5000,
    val allowPitch: Boolean = false,
    val volume: Float,
) : KartAction {
    private var lastHorn = -1L
    override fun execute(kart: Kart, type: KartAction.Type, target: Player?) {
        if (sounds.isEmpty()) return
        if (lastHorn < System.currentTimeMillis() - timeout) {
            lastHorn = System.currentTimeMillis()
            val sound = sounds.random()
            val location = kart.player.location
            if (allowPitch) {
                val pitch = kart.player.location.pitch.absoluteValue.coerceIn(0f, 90f) / 45f
                location.world!!.playSound(location, sound, SoundCategory.AMBIENT, volume, pitch)
            } else
                location.world!!.playSound(location, sound, SoundCategory.AMBIENT, volume, 1f)
        }
    }
}
