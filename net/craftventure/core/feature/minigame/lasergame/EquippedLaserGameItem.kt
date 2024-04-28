package net.craftventure.core.feature.minigame.lasergame

import org.bukkit.inventory.ItemStack

data class EquippedLaserGameItem(
    val id: String,
    val representation: ItemStack,
    val item: LaserGameItem
) {
    fun applySwitchCooldown(cooldown: Long = 700) = item.applySwitchCooldown(cooldown)
}