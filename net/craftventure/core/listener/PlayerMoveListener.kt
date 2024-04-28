package net.craftventure.core.listener

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.metadata.PlayerLocationTracker
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.spigotmc.event.entity.EntityDismountEvent

class PlayerMoveListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleportMonitor(event: PlayerTeleportEvent) {
        if (event.player.getMetadata<PlayerLocationTracker>()?.updateLocation(event.to, isTeleport = true) == false) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMoveMonitor(event: PlayerMoveEvent) {
//        val from = event.from
        val to = event.to
//        if (from.x != to.x || from.y != to.y || from.z != to.z || from.yaw != to.yaw || from.pitch != to.pitch) {
        if (event.player.getMetadata<PlayerLocationTracker>()?.updateLocation(to, isTeleport = false) == false) {
            event.isCancelled = true
        }
//        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
//        Logger.debug("Player teleport $event")
        if (event.cause != PlayerTeleportEvent.TeleportCause.DISMOUNT) return
        val leaveLocation = event.player.getMetadata<PlayerLocationTracker>()?.getLeaveLocation() ?: return
        event.to = leaveLocation
    }

    private val dismountedPlayers = mutableListOf<Player>()

    @EventHandler
    fun onTickEnd(event: ServerTickEndEvent) {
//        Logger.debug("Tick end ${Bukkit.getCurrentTick()}")

        dismountedPlayers.forEach { player ->
            if (player.vehicle == null) {
                player.getMetadata<PlayerLocationTracker>()?.getLeaveLocation()?.let {
                    player.teleport(it)
                }
            }
//            Logger.debug(
//                "Can exit? ${player.isValid} ${player.isConnected()} ${player.name} ${
//                    player.getMetadata<PlayerLocationTracker>()?.getLeaveLocation()
//                }"
//            )
        }
        dismountedPlayers.clear()

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onPlayerDismount(event: EntityDismountEvent) {
        val player = event.entity as? Player ?: return
//        Logger.debug("Exit ${Bukkit.getCurrentTick()}")
        dismountedPlayers.add(player)
    }
}
