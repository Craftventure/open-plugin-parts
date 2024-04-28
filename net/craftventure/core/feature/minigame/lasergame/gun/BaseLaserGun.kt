package net.craftventure.core.feature.minigame.lasergame.gun

import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.core.CraftventureCore
import net.craftventure.core.feature.minigame.lasergame.LaserGameItem
import net.craftventure.core.utils.InterpolationUtils
import org.bukkit.Location

abstract class BaseLaserGun : LaserGameItem {
    protected fun doSound(location: Location) {
        val soundnumberLaser: Int = (1..5).random()
        val pitch = InterpolationUtils.linearInterpolate(0.8, 1.2, CraftventureCore.getRandom().nextDouble()).toFloat()

        location.world!!.playSound(
            location,
            "${SoundUtils.SOUND_PREFIX}:minigame.laser.laser.$soundnumberLaser",
            1f,
            pitch
        )
    }
}