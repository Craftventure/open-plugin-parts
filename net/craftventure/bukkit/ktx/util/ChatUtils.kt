package net.craftventure.bukkit.ktx.util

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import org.bukkit.command.CommandSender


object ChatUtils {
    val ID_RIDE = "ride"
    val ID_INTRO_TEXT = "intro_text"
    val ID_AUDIOSERVER_AREA_NAME = "audioserver_area_name"
    val ID_RESPACK_DENIED = "respack_denied"
    val ID_MINIGAME = "minigame"
    val ID_CASINO = "casino"
    val ID_RIDE_QUEUE = "ride_queue"
    val ID_KART_EXIT_WARNING = "kart_exit_warning"
    val ID_SHOP_MESSAGES = "shop_messages"
    val ID_GENERAL_NOTICE = "general_notice"

    fun nope(sender: CommandSender) {
        sender.sendMessage(CVTextColor.serverError + "Nope")
    }

    fun onlyPlayers(sender: CommandSender) {
        sender.sendMessage(CVTextColor.serverError + "This command is only usable by players")
    }
}
