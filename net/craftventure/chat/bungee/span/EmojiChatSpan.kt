package net.craftventure.chat.bungee.span

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.ChatSpan
import net.craftventure.database.extension.actualAliases
import net.craftventure.database.generated.cvdata.tables.pojos.Emoji
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style

class EmojiChatSpan(
    startIndexInclusive: Int,
    endIndexInclusive: Int,
    val emoji: Emoji,
) : ChatSpan(
    startIndexInclusive,
    endIndexInclusive
) {
    override fun appendTo(source: String, component: Component): Component {
//        val fontKey = if (emoji.font != null) Key.key(emoji.font!!) else null
        return component + Component.text(emoji.emoji!!)
            .style(Style.style()/*.font(fontKey)*/.color(NamedTextColor.WHITE).build())
            .hoverEvent(
                Component.text(
                    "${emoji.actualAliases.first()} (click to copy alias to the clipboard)",
                    CVTextColor.CHAT_HOVER
                )
            )
            .clickEvent(ClickEvent.copyToClipboard(source))
    }
}