package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.colorFromHex
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.metadata.GenericPlayerMeta
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent


class AprilFoolsListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) {
            event.player.getMetadata<GenericPlayerMeta>()?.tryFart()?.let { fart ->
                if (fart && DateUtils.isAprilFools) {
                    fart(event.player)
                }
            }
        }
    }

    companion object {
        fun fart(player: Player) {
            player.world.playSound(
                player.location,
                "${SoundUtils.SOUND_PREFIX}:random.fart" + (CraftventureCore.getRandom().nextInt(2) + 1),
                0.2f,
                1f
            )

            executeAsync {
                MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "fart")
            }

            val task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
                player.location.clone().add(0.0, 0.8, 0.0).add(player.location.direction.normalize().multiply(-0.5))
                    .spawnParticleX(
                        Particle.REDSTONE,
                        15,
                        0.1, 0.1, 0.1,
                        data = Particle.DustOptions(colorFromHex("#593a12"), 0.4f)
                    )
            }, 1L, 1L)

            Bukkit.getScheduler()
                .scheduleSyncDelayedTask(CraftventureCore.getInstance(), { Bukkit.getScheduler().cancelTask(task) }, 8)
        }
    }
}