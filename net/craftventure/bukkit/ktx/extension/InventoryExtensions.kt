package net.craftventure.bukkit.ktx.extension

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

fun Inventory.updateItem(slot: Int, itemStack: ItemStack?) {
    if (this.getItem(slot) != itemStack) {
        this.setItem(slot, itemStack)
    }
}

operator fun Inventory.set(i: Int, value: ItemStack?) {
    setItem(i, value)
}

operator fun Inventory.get(i: Int) = getItem(i)