package net.craftventure.bukkit.ktx.extension

import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.inventory.meta.FireworkEffectMeta
import org.bukkit.inventory.meta.FireworkMeta


operator fun FireworkMeta.plusAssign(effect: FireworkEffect) {
    addEffect(effect)
}

fun FireworkEffectMeta.setColor(color: Color) {
    effect = FireworkEffect.builder().withColor(color).build()
}