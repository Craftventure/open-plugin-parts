package net.craftventure.bukkit.ktx.util

import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component


fun Audience.sendServerError(message: String) = sendServerError(message.parseWithCvMessage())
fun Audience.sendServerError(message: Component) {
    sendMessage(message.style { it.color(CVTextColor.serverError) })
}

fun Audience.sendServerNotice(message: String) = sendServerNotice(message.parseWithCvMessage())
fun Audience.sendServerNotice(message: Component) {
    sendMessage(message.style { it.color(CVTextColor.serverNotice) })
}

fun Audience.sendServerNoticeAccent(message: String) = sendServerNoticeAccent(message.parseWithCvMessage())
fun Audience.sendServerNoticeAccent(message: Component) {
    sendMessage(message.style { it.color(CVTextColor.serverNoticeAccent) })
}