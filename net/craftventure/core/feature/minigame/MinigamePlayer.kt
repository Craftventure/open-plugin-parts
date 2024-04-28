package net.craftventure.core.feature.minigame

import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.core.extension.isAfk
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent

data class MinigamePlayer<META>(
    val player: Player,
    val metadata: META
) {
    var standby = false
    var allowNextTeleport = false
        private set

    fun teleportTo(location: Location) {
        allowNextTeleport()
        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
    }

    fun allowNextTeleport() {
        allowNextTeleport = true
    }

    fun teleported() {
        allowNextTeleport = false
    }

    fun isValid() = player.isConnected() && !player.isAfk()
}