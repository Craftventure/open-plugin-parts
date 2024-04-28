package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.hideAttributes
import net.craftventure.bukkit.ktx.extension.hideEnchants
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.feature.dressingroom.DressingRoomPlayerState
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.popMenu
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.type.EquippedItemSlot
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class DressingRoomPreviewPickerSlotSelectorMenu(
    player: Player,
    private val item: DressingRoomPreviewPickerMenu.InventoryItem,
    private val slotChoices: List<EquippedItemSlot>,
    private val state: DressingRoomPlayerState,
) : LayoutInventoryMenu(
    itemStartRowOffset = 1,
    maxItemRows = 4,
    owner = player,
) {
    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Pick an equipment slot")
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    override fun provideItems(): List<ItemStack> = slotChoices.map {
        it.itemStack()
            .displayName(item.item.displayName())
            .loreWithBuilder {
                action("Click to preview this item as ${it.displayName}")
            }
            .hideAttributes()
            .hideEnchants()
    }

    override fun onProvidedItemClicked(
        inventory: Inventory,
        index: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        super.onProvidedItemClicked(inventory, index, row, column, player, action)

        slotChoices.getOrNull(index)?.let { slot ->
            state.setEquipment(
                slot,
                EquipmentManager.EquippedItemData.of(item.ownableItem, player, slot, ownCountOverride = 1)
            )
            player.popMenu()
        }
    }

    override fun onStaticItemClicked(
        inventory: Inventory,
        position: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)
    }
}