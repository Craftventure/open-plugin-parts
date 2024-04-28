package net.craftventure.bukkit.ktx.extension

import org.bukkit.inventory.EntityEquipment

fun EntityEquipment.isNotEmpty() = !isEmpty()

fun EntityEquipment.isEmpty() = helmet?.takeIfNotAir() == null &&
        chestplate?.takeIfNotAir() == null &&
        leggings?.takeIfNotAir() == null &&
        boots?.takeIfNotAir() == null &&
        itemInMainHand.takeIfNotAir() == null &&
        itemInOffHand.takeIfNotAir() == null