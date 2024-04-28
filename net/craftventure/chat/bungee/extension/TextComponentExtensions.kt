package net.craftventure.chat.bungee.extension

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

operator fun Component.plus(other: Component) = this.append(other)
operator fun TextComponent.plus(other: Component) = this.append(other)

operator fun Component.plus(other: String?) = this.append(Component.text(other ?: ""))
operator fun TextComponent.plus(other: String?) =
    if (other != null) this.append(Component.text(other)) else this.append(Component.empty())

fun TextComponent.appendNewline() = append(Component.newline())

operator fun TextColor.plus(text: String?) = Component.text(text ?: "", this)

fun Component.append(text: String) = plus(text)
fun TextColor.append(text: String) = plus(text)

fun Component.asPlainText() = PlainTextComponentSerializer.plainText().serialize(this)