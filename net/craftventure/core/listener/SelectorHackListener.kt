package net.craftventure.core.listener

import net.craftventure.core.CraftventureCore
import net.craftventure.core.command.CommandExecutorUtils
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.command.BlockCommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerCommandEvent

class SelectorHackListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onServerCommand(event: ServerCommandEvent) {
        val command = event.command.trimStart('/')
//        val commandLabel = Bukkit.getCommandAliases().get(command.fi)
        val commandLabel = command.split(" ").firstOrNull() ?: return
        val handler = Bukkit.getPluginCommand(commandLabel) ?: return
        val commandPlugin = handler.plugin

        if (commandPlugin is CraftventureCore) {
            for (argument in command.split(" ")) {
                if (argument.length < 2) continue
                val selectorType = argument.substring(0, 2)
                when (selectorType) {
                    "@a", "@r", "@s", "@e", "@p" -> {
                        val block = (event.sender as? BlockCommandSender)?.block
                        if (block != null) {
//                            Logger.debug("Selector $argument")
                            try {
                                val targets = CommandExecutorUtils.getCommandBlockTargets(event.sender, argument)
                                for (target in targets) {
                                    Bukkit.dispatchCommand(event.sender, command.replace(argument, target.name))
                                }
                                return
                            } catch (e: Exception) {
                                Logger.warn("Failed to execute command $command with selector $argument at ${block.x} ${block.y} ${block.z}")
//                                e.printStackTrace()
                            }
//                            Logger.debug("Matched entities [${CommandUtils.getTargets(event.sender, argument).joinToString(", ") { it.name }}]")
                        }
//                        Logger.debug("Selector '$argument' of type '$selectorType'")
                    }
                }
            }
//            Logger.debug("Command '$command'")// (label=${handler.label})")
        }
    }
}