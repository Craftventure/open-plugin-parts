package net.craftventure.core.listener

import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Particle
import org.bukkit.entity.Donkey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent


class PettingListener : Listener {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (event.rightClicked is Donkey) {
            if (event.rightClicked.name.startsWith("davey", true)) {
                (event.rightClicked as? Donkey)?.apply {
                    setBaby()
                    ageLock = true
                }
                executeAsync {
                    val result = MainRepositoryProvider.achievementProgressRepository
                        .reward(event.player.uniqueId, "petting_donkey")
                    if (result) {
                        executeSync {
                            event.rightClicked.location.clone().add(event.clickedPosition).spawnParticleX(
                                Particle.HEART,
                                range = 5.0
                            )
                        }
                    }
                }
                event.isCancelled = true
            }
        }
    }
}