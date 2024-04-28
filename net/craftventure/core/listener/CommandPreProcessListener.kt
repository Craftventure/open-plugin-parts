package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.PermissionChecker
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

class CommandPreProcessListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerChat(event: PlayerCommandPreprocessEvent) {
        if (event.message != null && event.message.isNotEmpty()) {
            val parts = event.message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.isNotEmpty()) {
                val command = parts[0]
//                Logger.info("Command $command")
                if (command.contains(":") && !PermissionChecker.isCrew(event.player)) {
                    event.isCancelled = true
                    ChatUtils.nope(event.player)
                }/* else if (command == "/vanish" && !event.player.isCrew()) {
                    event.message = event.message.replaceFirst("/vanish", "/vanish<routed>")
                }*/
            }
        }
    }
}
