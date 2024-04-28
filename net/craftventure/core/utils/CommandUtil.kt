package net.craftventure.core.utils

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

object CommandUtil {
    fun sendToBungee(context: Player, fullCommand: String): Boolean {
        try {
            val b = ByteArrayOutputStream()
            val out = DataOutputStream(b)

            out.writeUTF("run")
            out.writeUTF(fullCommand)

            context.sendPluginMessage(PluginProvider.getInstance(), "craftventure:command", b.toByteArray())
            Logger.debug("Send to bungee for ${context.name}: $fullCommand")
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun getVisiblePlayers(commandSender: CommandSender) = if (commandSender is Player && !commandSender.isCrew()) {
        Bukkit.getOnlinePlayers().filter { commandSender.canSee(it) }
    } else Bukkit.getOnlinePlayers()
}