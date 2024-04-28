package net.craftventure.bukkit.ktx.extension

import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent

operator fun ComponentBuilder.plusAssign(text: String) {
    append(TextComponent.fromLegacyText(text), ComponentBuilder.FormatRetention.FORMATTING)
}

operator fun ComponentBuilder.plusAssign(text: TextComponent) {
    append(text, ComponentBuilder.FormatRetention.FORMATTING)
}

operator fun ComponentBuilder.plusAssign(texts: Array<TextComponent>) {
    append(texts, ComponentBuilder.FormatRetention.FORMATTING)
}