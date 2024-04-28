package net.craftventure.bukkit.ktx.extension

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


fun Component.sendTo(player: Player) {
    player.sendMessage(this)
}

fun ItemStack.displayNamePlain() =
    this.itemMeta?.displayName()?.let { PlainTextComponentSerializer.plainText().serialize(it) }