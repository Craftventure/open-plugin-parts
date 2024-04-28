package net.craftventure.core.extension

import net.craftventure.bukkit.ktx.extension.clearMetaKey
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.bukkit.ktx.util.CraftventureKeys
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

fun ItemStack.getItemId(): String? = getMeta(CraftventureKeys.KEY_ITEM_ID)
fun ItemStack.clearId() = clearMetaKey(CraftventureKeys.KEY_ITEM_ID)
fun ItemStack.setItemId(id: String): ItemStack = setMeta(CraftventureKeys.KEY_ITEM_ID, id)

fun ItemStack.isMarkedAsWornItem(handleAirAsMarked: Boolean = false): Boolean =
    (handleAirAsMarked && type == Material.AIR) || getMeta(CraftventureKeys.MARK_WORN_ITEM) != null

fun ItemStack.markAsWornItem(): ItemStack = setMeta(CraftventureKeys.MARK_WORN_ITEM, "1")

fun ItemStack.getMeta(key: NamespacedKey): String? =
    if (hasItemMeta()) itemMeta?.persistentDataContainer?.get(key, PersistentDataType.STRING)
    else null

fun ItemStack.setMeta(key: NamespacedKey, value: String): ItemStack {
    updateMeta<ItemMeta> {
        persistentDataContainer.set(key, PersistentDataType.STRING, value)
    }
    return this
}

