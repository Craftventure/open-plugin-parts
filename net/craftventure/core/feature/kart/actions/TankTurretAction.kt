package net.craftventure.core.feature.kart.actions

import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.kart.KartAction
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

class TankTurretAction : KartAction {
    private var lastUse = -1L
    override fun execute(kart: Kart, type: KartAction.Type, target: Player?) {
        if (lastUse < System.currentTimeMillis() - 5000 /*|| CraftventureCore.isTestServer()*/) {
            lastUse = System.currentTimeMillis()

            val seat = kart.npcs.firstOrNull { it.settings.id == "barrel" } ?: return
            val location = seat.currentMatrix.toVector().toLocation(kart.startLocation.world!!)
            val quat = seat.currentMatrix.rotation
            location.yaw = quat.yaw.toFloat()
            location.pitch = quat.pitch.toFloat()
            val direction = location.direction.normalize()
            location.add(direction.clone().multiply(1.1))

            location.spawnParticleX(
                Particle.EXPLOSION_LARGE,
                count = 5,
                offsetX = 0.5,
                offsetY = 0.5,
                offsetZ = 0.5,
            )
            for (i in 0..5) {
                location.spawnParticleX(
                    Particle.EXPLOSION_NORMAL,
                    count = 0,
                    offsetX = direction.x,
                    offsetY = direction.y,
                    offsetZ = direction.z,
                    extra = 0.5
                )
                location.spawnParticleX(
                    Particle.CAMPFIRE_COSY_SMOKE,
                    count = 0,
                    offsetX = direction.x,
                    offsetY = direction.y,
                    offsetZ = direction.z,
                    extra = 0.5
                )
            }
            location.world!!.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 0.8f, 0.5f)
        }
    }
}
