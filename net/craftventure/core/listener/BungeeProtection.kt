package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.util.Logger
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import java.util.*


class BungeeProtection : Listener {

    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        if (PluginProvider.isTestServer()) return
        val player = event.player
        val offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:${player.name}").toByteArray(Charsets.UTF_8))

        if (player.uniqueId == offlineUuid) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_BANNED,
                CVTextColor.serverError + "You are blocked by the Craftventure Firewall. If you feel this is in error, contact the Craftventure crew"
            )
            try {
                val message =
                    "${player.name} tried to join with an offline UUID with addresss ${event.address.hostAddress}," +
                            " realAddress ${event.realAddress.hostAddress}"
                Logger.severe(message, logToCrew = false)
                Logger.capture(IllegalStateException(message))
            } catch (e: Exception) {
            }
        }
    }
}