package net.craftventure.chat.bungee.span

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.ChatSpan
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEventSource

class RideChatSpan(
    startIndexInclusive: Int,
    endIndexInclusive: Int,
    val displayName: String,
    val warpId: String?,
    val description: HoverEventSource<Component>?,
) : ChatSpan(startIndexInclusive, endIndexInclusive) {
    override fun appendTo(source: String, component: Component): Component {
        return component + Component.text(displayName, CVTextColor.serverNotice)
            .let {
                if (description != null)
                    it.hoverEvent(description)
                else
                    it
            }
            .let {
                if (warpId != null)
                    it.clickEvent(ClickEvent.runCommand("/warp $warpId"))
                else
                    it
            }
    }
}