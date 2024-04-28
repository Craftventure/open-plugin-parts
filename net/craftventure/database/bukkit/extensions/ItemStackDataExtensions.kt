package net.craftventure.database.bukkit.extensions

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.displayNamePlain
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.bukkit.listener.ItemStackDataCacheListener
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.type.EquippedItemSlot
import net.craftventure.database.type.ItemType
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

fun ItemStackData.createItemStack(): ItemStack? {
    try {
        val cachedItemStack = ItemStack.deserializeBytes(itemstack!!)
        if (overridenTitle != null) cachedItemStack.displayNameWithBuilder { text(overridenTitle!!) }
        else cachedItemStack.displayName(null)

        if (overridenLore != null) cachedItemStack.loreWithBuilder { text(overridenLore!!) }
        else cachedItemStack.lore(null)

        cachedItemStack.updateMeta<ItemMeta> {
            addItemFlags(*ItemFlag.values())
        }

        return cachedItemStack
    } catch (e: Exception) {
        Logger.capture(e)
    }
    return null
}

val ItemStackData.itemStack: ItemStack?
    get() {
        return itemStackUncloned?.clone()
    }

val ItemStackData.itemStackUncloned: ItemStack?
    get() {
        return ItemStackDataCacheListener.items[id]
    }

fun ItemStackData.getItemStack(ownableItem: OwnableItem): ItemStack? {
    return getItemStack(ownableItem.type!!)
}

fun ItemStackData.getItemStack(itemType: ItemType): ItemStack? {
    val itemStack = itemStack
    try {
        itemStack?.displayName(
            Component.text(
                (itemStack.displayNamePlain() ?: "Untitled item").trim(),
                CVTextColor.MENU_DEFAULT_TITLE
            )
        )
    } catch (e: Exception) {
        Logger.capture(e)
    }
    return itemStack
}

fun ItemStackData.getItemStack(slot: EquippedItemSlot): ItemStack? {
    val itemStack = itemStack
    try {
        itemStack?.displayName(
            Component.text(
                (itemStack.displayNamePlain() ?: "Untitled item").trim(),
                CVTextColor.MENU_DEFAULT_TITLE
            )
        )
    } catch (e: Exception) {
        Logger.capture(e)
    }
    return itemStack
}