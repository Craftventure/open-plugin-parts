package net.craftventure.database.bukkit.extensions

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.database.type.EquippedItemSlot

fun EquippedItemSlot.itemStack() = when (this) {
    EquippedItemSlot.HELMET -> MaterialConfig.GUI_HELMET
    EquippedItemSlot.HAIRSTYLE -> MaterialConfig.GUI_HELMET
    EquippedItemSlot.CHESTPLATE -> MaterialConfig.GUI_CHESTPLATE
    EquippedItemSlot.LEGGINGS -> MaterialConfig.GUI_LEGGINGS
    EquippedItemSlot.BOOTS -> MaterialConfig.GUI_BOOTS
    EquippedItemSlot.BALLOON -> MaterialConfig.GUI_BALLOON
    EquippedItemSlot.COSTUME -> MaterialConfig.GUI_COSTUME
    EquippedItemSlot.HANDHELD -> MaterialConfig.GUI_HAND
    EquippedItemSlot.CONSUMPTION -> MaterialConfig.GUI_HAND
    EquippedItemSlot.TITLE -> MaterialConfig.GUI_TITLE
    EquippedItemSlot.LASER_GAME_A -> MaterialConfig.GUI_GUN
    EquippedItemSlot.LASER_GAME_B -> MaterialConfig.GUI_GUN
    EquippedItemSlot.SHOULDER_PET_LEFT -> MaterialConfig.GUI_SHOULDER_PET
    EquippedItemSlot.SHOULDER_PET_RIGHT -> MaterialConfig.GUI_SHOULDER_PET
    EquippedItemSlot.INSTRUMENT -> MaterialConfig.GUI_SHOULDER_PET
}.clone()