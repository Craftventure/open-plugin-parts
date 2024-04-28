package net.craftventure.core.inventory.impl

import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.inventory.InventoryMenu
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory

class LoadingMenu(
    player: Player,
    val allowNavigation: Boolean = true,
) : InventoryMenu(
    owner = player,
) {
    init {
        rowsCount = 1
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
        )

        titleComponent = centeredTitle("Loading...")
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return
    }

    override fun onLayout(inventory: Inventory) {
        if (allowNavigation)
            addNavigationButtons(inventory)
    }
}