package net.craftventure.chat.bungee.span

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.ChatSpan
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style

class UnicodeEmojiChatSpan(
    startIndexInclusive: Int,
    endIndexInclusive: Int,
) : ChatSpan(
    startIndexInclusive,
    endIndexInclusive
) {
    override fun appendTo(source: String, component: Component): Component {
        return component + Component.text(source)
            .style(Style.style().color(NamedTextColor.WHITE).build())
            .hoverEvent(
                Component.text(
                    "Click to copy this emoji",
                    CVTextColor.CHAT_HOVER
                )
            )
            .clickEvent(ClickEvent.copyToClipboard(source))
    }
}