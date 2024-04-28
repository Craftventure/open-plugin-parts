package net.craftventure.core.extension

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import org.bukkit.command.CommandSender

fun CommandSender.sendDatabaseFailError(exception: Exception? = null) {
    sendMessage(CVTextColor.serverError + "Failed to execute action")
}