package net.craftventure.chat.bungee.span

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.ChatSpan
import net.kyori.adventure.text.Component

class PlayerTagSpan(
    startIndexInclusive: Int,
    endIndexInclusive: Int,
    val name: String,
    val effectHandler: (() -> Unit)? = null,
) : ChatSpan(startIndexInclusive, endIndexInclusive) {
    override fun applyEffects() {
        super.applyEffects()
        effectHandler?.invoke()
    }

    override fun appendTo(source: String, component: Component): Component {
        return component + Component.text("$name@", CVTextColor.serverNotice)
    }
}