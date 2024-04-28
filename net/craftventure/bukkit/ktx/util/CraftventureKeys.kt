package net.craftventure.bukkit.ktx.util

import net.craftventure.bukkit.ktx.plugin.PluginProvider
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType


object CraftventureKeys {
    val KEY_ITEM_ID by lazy { NamespacedKey(PluginProvider.plugin, "itemId") }
    val MARK_WORN_ITEM by lazy { NamespacedKey(PluginProvider.plugin, "wim") }
    val KEY_OPERATOR_CONTROL by lazy { NamespacedKey(PluginProvider.plugin, "operatorControl") }
    val KEY_OPERATOR_SLOT by lazy { NamespacedKey(PluginProvider.plugin, "operatorSlot") }

    val ID_ITEM_CRAFTVENTURE_MENU = "cv:menu/main"
    val ID_ITEM_MENU_NEXT = "cv:menu/next"
    val ID_ITEM_MENU_PREVIOUS = "cv:menu/previous"
    val ID_ITEM_MENU_CLOSE = "cv:menu/close"
    val ID_ITEM_MENU_UP = "cv:menu/up"
    val ID_ITEM_ATTRACTIONS_MENU = "cv:menu/rides"
    val ID_ITEM_PROFILE_MENU = "cv:menu/player"
    val ID_ITEM_RESERVED = "cv:menu/reserved"

    val TILE_STATE_SHOP_ID by lazy { NamespacedKey(PluginProvider.plugin, "interactOpenShopId") }
    var PersistentDataContainer.shopId: String?
        get() = get(TILE_STATE_SHOP_ID, PersistentDataType.STRING)
        set(value) {
            if (value != null) set(TILE_STATE_SHOP_ID, PersistentDataType.STRING, value)
            else remove(TILE_STATE_SHOP_ID)
        }

    val TILE_STATE_BOOK by lazy { NamespacedKey(PluginProvider.plugin, "itemId") }
    val TILE_STATE_ALLOW_INTERACT by lazy { NamespacedKey(PluginProvider.plugin, "allowInteract") }

    val KEY_SEAT_MARKER by lazy { NamespacedKey(PluginProvider.plugin, "isSeat") }
    var Entity.isSeat: Boolean
        get() = persistentDataContainer.get(KEY_SEAT_MARKER, PersistentDataType.BYTE) == 1.toByte()
        set(value) = persistentDataContainer.set(KEY_SEAT_MARKER, PersistentDataType.BYTE, if (value) 1 else 0)
}
