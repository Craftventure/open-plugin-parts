package net.craftventure.bukkit.ktx.extension

import cloud.commandframework.context.CommandContext
import net.craftventure.bukkit.ktx.util.sendServerError
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun CommandContext<CommandSender>.requirePlayerOrSendMessageAndReturnNull(): Player? {
    val commandSender = this.sender
    if (commandSender !is Player) {
        commandSender.sendServerError("You must be a player")
        return null
    }
    return commandSender
}