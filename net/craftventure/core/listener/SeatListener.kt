package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.util.CraftventureKeys.isSeat
import net.craftventure.core.extension.hasPassengers
import net.craftventure.core.serverevent.AfkStatusChangeEvent
import org.bukkit.GameMode
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.vehicle.VehicleDamageEvent
import org.bukkit.event.vehicle.VehicleEnterEvent

class SeatListener : Listener {
    private fun tryToSeat(seat: Entity, player: Player) {
//        Logger.debug("Try to enter seat?")
        if (!seat.hasPassengers()) {
            seat.addPassenger(player)
        }
    }

    @EventHandler
    fun onGoAfk(event: AfkStatusChangeEvent) {
        if (event.willBecomeAfk) {
            val vehicle = event.player.vehicle ?: return
            if (vehicle.isSeat) {
                event.player.leaveVehicle()
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        if (player.gameMode != GameMode.ADVENTURE) return
        val entity = event.entity
        if (entity.isSeat) {
            event.isCancelled = true
            tryToSeat(entity, player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked
        val player = event.player
//        if (player.gameMode != GameMode.ADVENTURE) return
        if (player.isCrew() && player.isSneaking) return
        if (entity.isSeat) {
            event.isCancelled = true
            tryToSeat(entity, player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        val player = event.player
//        if (player.gameMode != GameMode.ADVENTURE) return
        if (player.isCrew() && player.isSneaking) return
        if (entity.isSeat) {
            event.isCancelled = true
            tryToSeat(entity, player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onVehicleDamage(event: VehicleDamageEvent) {
        val entity = event.vehicle
        val player = event.attacker as? Player ?: return
//        if (player.gameMode != GameMode.ADVENTURE) return
        if (player.isCrew() && player.isSneaking) return
        if (entity.isSeat) {
            event.isCancelled = true
            tryToSeat(entity, player)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onVehicleEnter(event: VehicleEnterEvent) {
        val entity = event.vehicle
        val player = event.entered as? Player ?: return
//        if (player.gameMode != GameMode.ADVENTURE) return
        if (player.isCrew() && player.isSneaking) return
        if (entity.isSeat) {
            event.isCancelled = false
        }
    }
}
