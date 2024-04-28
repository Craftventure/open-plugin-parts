package net.craftventure.chat.bungee.span

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.ChatSpan
import net.craftventure.chat.core.extension.generateGrawlix
import net.kyori.adventure.text.Component

class CurseWordChatSpan(startIndexInclusive: Int, endIndexInclusive: Int) :
    ChatSpan(startIndexInclusive, endIndexInclusive) {
    override fun appendTo(source: String, component: Component): Component {
//        logcat { "Applying grawlix to '$source' (${source.length})" }
        return component + Component.text(generateGrawlix(source.length))
            .hoverEvent(Component.text(source, CVTextColor.CHAT_HOVER))
    }
}