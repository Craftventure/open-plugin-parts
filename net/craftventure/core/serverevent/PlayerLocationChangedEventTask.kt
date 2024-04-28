package net.craftventure.core.serverevent

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.CraftventureCore
import net.craftventure.core.metadata.PlayerLocationTracker
import org.bukkit.Bukkit
import org.bukkit.GameMode


object PlayerLocationChangedEventTask {
    fun init() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            for (player in Bukkit.getOnlinePlayers()) {
                if (player.isInsideVehicle || player.gameMode == GameMode.SPECTATOR) {
                    player.getMetadata<PlayerLocationTracker>()?.updateLocation(isTeleport = false)
                }
            }
        }, 1L, 1L)

        BackgroundService.add(object : BackgroundService.Animatable {
            override fun onAnimationUpdate() {
                val players = Bukkit.getOnlinePlayers().toTypedArray()
                for (player in players) {
                    player.getMetadata<PlayerLocationTracker>()?.handleDirtyAsync()
                }
            }
        })
    }
}
