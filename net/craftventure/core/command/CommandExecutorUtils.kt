package net.craftventure.core.command

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import org.bukkit.Bukkit
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

object CommandExecutorUtils {
    fun getCommandBlockTargets(commandSender: CommandSender, selector: String): List<Entity> {
        if (commandSender is BlockCommandSender) {
            return Bukkit.getServer().selectEntities(commandSender, selector)
        }
        return emptyList()
    }

    fun getPlayerByName(playerName: String, commandSender: CommandSender): Player? {
        val playerList = Bukkit.matchPlayer(playerName)
        if (playerList.size == 1)
            return playerList[0]
        if (playerList.size > 1) {
            commandSender.sendMessage(CVTextColor.serverError + "Multiple players match by the name " + playerName)
            return null
        }
        commandSender.sendMessage(CVTextColor.serverError + "No players matched by the name " + playerName)
        return null
    }

    fun getPlayerByName(playerName: String?): Player? {
        if (playerName == null) {
            return null
        }
        val playerList = Bukkit.matchPlayer(playerName)
        if (playerList.size == 1)
            return playerList[0]
        return if (playerList.size > 1) {
            null
        } else null
    }

    fun getPlayerOrConsoleByName(playerName: String): CommandSender? {
        if ("console".equals(playerName, ignoreCase = true)) {
            return Bukkit.getConsoleSender()
        }
        val playerList = Bukkit.matchPlayer(playerName)
        if (playerList.size == 1)
            return playerList[0]
        return if (playerList.size > 1) {
            null
        } else null
    }
}
