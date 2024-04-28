package net.craftventure.core.inventory

import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.hideAttributes
import net.craftventure.bukkit.ktx.extension.hideEnchants
import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.extension.setItemId
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material


object InventoryConstants {
    val RIGHT_CLICK_APPEND = NamedTextColor.GRAY + "(Right Click)"
    val NAME_ITEM_CRAFTVENTURE_MENU = CVTextColor.MENU_DEFAULT_TITLE + "Craftventure " + RIGHT_CLICK_APPEND
    val NAME_ITEM_ATTRACTIONS_MENU = CVTextColor.MENU_DEFAULT_TITLE + "Attractions " + RIGHT_CLICK_APPEND
    val NAME_ITEM_PROFILE_MENU = CVTextColor.MENU_DEFAULT_TITLE + "My Profile " + RIGHT_CLICK_APPEND

    val NAME_ITEM_CLOSE = CVTextColor.MENU_DEFAULT_TITLE + "Close"
    val NAME_ITEM_UP = CVTextColor.MENU_DEFAULT_TITLE + "Back to "
    val NAME_ITEM_ONE_PAGE_BACK = CVTextColor.MENU_DEFAULT_TITLE + "Previous page"
    val NAME_ITEM_ONE_PAGE_FORWARD = CVTextColor.MENU_DEFAULT_TITLE + "Next page"

    fun getPreviousPageButton(currentPage: Int, totalPages: Int) =
        dataItem(Material.STICK, if (currentPage <= 0) 10 else 11).apply {
            hideAttributes()
            hideEnchants()
            displayName(NAME_ITEM_ONE_PAGE_BACK)
            setItemId(CraftventureKeys.ID_ITEM_MENU_PREVIOUS)
        }

    fun getNextPageButton(currentPage: Int, totalPages: Int) =
        dataItem(Material.STICK, if (currentPage >= totalPages - 1) 12 else 13).apply {
            hideAttributes()
            hideEnchants()
            displayName(NAME_ITEM_ONE_PAGE_FORWARD)
            setItemId(CraftventureKeys.ID_ITEM_MENU_NEXT)
        }
}
