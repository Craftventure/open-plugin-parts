package net.craftventure.chat.bungee.span

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.ChatSpan
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor

class UrlChatSpan(startIndexInclusive: Int, endIndexInclusive: Int) :
    ChatSpan(startIndexInclusive, endIndexInclusive) {
    override fun appendTo(source: String, component: Component): Component {
        return component + Component.text(source, NamedTextColor.WHITE)
            .hoverEvent(Component.text("Click to open $source", CVTextColor.CHAT_HOVER).asHoverEvent())
            .clickEvent(ClickEvent.openUrl(source))
    }
}