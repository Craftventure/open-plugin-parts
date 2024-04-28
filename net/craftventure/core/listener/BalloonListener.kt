package net.craftventure.core.listener

import net.craftventure.core.async.executeSync
import net.craftventure.core.feature.balloon.BalloonManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent

class BalloonListener : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        BalloonManager.remove(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onVehicleEnter(event: VehicleEnterEvent) {
        val player = event.entered as? Player ?: return
        BalloonManager.invalidate(player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onVehicleExit(event: VehicleExitEvent) {
        val player = event.exited as? Player ?: return
        executeSync { BalloonManager.invalidate(player) }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityMount(event: EntityMountEvent) {
        val player = event.entity as? Player ?: return
        BalloonManager.invalidate(player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDismount(event: EntityDismountEvent) {
        val player = event.entity as? Player ?: return
        executeSync { BalloonManager.invalidate(player) }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        executeSync { BalloonManager.invalidate(event.player) }
    }
}
