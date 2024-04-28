package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.extension.isCrew
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

class CrewLampManager : Listener {
    private var locations = listOf(
        Location(Bukkit.getWorld("world"), 72.00, 51.00, -823.00),
    )
    private var on = false

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        join(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        leave(event.player)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        leave(event.player)
    }

    private fun join(player: Player) {
        update()
    }

    private fun leave(player: Player) {
        update()
    }

    private fun update() {
        val crewOnline = Bukkit.getOnlinePlayers().any { it.isCrew() && it.isConnected() }

//        Logger.console("CrewLamp $crewOnline vs $on")

        if (crewOnline != on) {
            on = crewOnline
            val data =
                if (on) Material.REDSTONE_LAMP.createBlockData("[lit=true]") else Material.REDSTONE_LAMP.createBlockData()
            locations.forEach { location ->
                location.block.setBlockData(data, false)
            }
        }
    }
}
