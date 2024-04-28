package net.craftventure.bukkit.ktx.extension

import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.EquipmentSlot

fun ArmorStand.disableManipulations() {
    addDisabledSlots(EquipmentSlot.HEAD)
}