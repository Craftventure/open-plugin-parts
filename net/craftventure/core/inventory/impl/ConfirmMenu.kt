package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeSync
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.type.BankAccountType
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class ConfirmMenu(
    player: Player,
    inventoryTitle: String,
    val confirmItem: ItemStack,
    val confirmedHandler: () -> Unit,
) : InventoryMenu(
    owner = player,
) {
    init {
        rowsCount = 1
        closeButtonIndex = StaticSlotIndex(1)
        this.titleComponent = centeredTitle(inventoryTitle)
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return
        if (action == InventoryAction.NOTHING) return
        when (position) {
            4 -> {
                executeSync {
                    player.closeInventoryStack()
                    confirmedHandler()
                }
            }
        }
    }

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)
        inventory.setItem(4, confirmItem)
    }

    companion object {
        fun generateBuyItem(
            ownableItem: OwnableItem,
            data: ItemStackData,
            itemStack: ItemStack,
            amount: Int,
            price: Int = ownableItem.price!! * amount,
            balanceType: BankAccountType = ownableItem.bankAccountType!!,
        ) =
            itemStack.clone()
                .apply {
//                    val lore: List<String> = listOfNotNull(this.displayName()) + (this.lore ?: emptyList())
                    displayName(CVTextColor.MENU_DEFAULT_TITLE + "Confirm buying this item")
                    loreWithBuilder {
                        accented("Priced at $price")
                        text(balanceType.emoji, color = NamedTextColor.WHITE)
                    }
//                    setLore(lore)
                }
    }
}