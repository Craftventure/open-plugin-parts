package net.craftventure.core.extension

import net.minecraft.world.entity.item.ItemEntity
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftItem
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

fun Entity.hasSpectators() = this.world.players.any { it.spectatorTarget === this }

fun Entity.getSpectators() = this.world.players.filter { it.spectatorTarget === this }.toList()

fun Entity.boundingBox() = (this as CraftEntity).handle.boundingBox

fun Entity?.hasPassengers() = (this as? CraftEntity)?.handle?.passengers?.isNotEmpty() ?: false

fun Entity.getFirstPassenger() = passengers.firstOrNull()

fun Item.setAge(newAge: Int) {
    val entity = this as CraftItem
    (entity.handle as ItemEntity).age = newAge
}

fun Entity.setInvisible(invisible: Boolean) {
    if (this is LivingEntity) {
        if (invisible)
            addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 1, false, false, false))
        else
            removePotionEffect(PotionEffectType.INVISIBILITY)
    }
    (this as CraftEntity).handle?.isInvisible = invisible
}