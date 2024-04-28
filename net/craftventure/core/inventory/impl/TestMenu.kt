package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.SlotBackgroundManager
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class TestMenu(
    player: Player,
    private val id: Int
) : InventoryMenu(owner = player) {
    init {
        slotBackgroundManager.setSlot(
            SlotBackgroundManager.slotIndex(0),
            FontCodes.Slot.buttonBackground
        )
        this.titleComponent = Component.text("Inv $id")
    }

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)
        inventory.setItem(1, ItemStack(Material.STONE).displayName(Component.text("Go deeper")))
        inventory.setItem(2, ItemStack(Material.STONE).displayName(Component.text("Replace current")))
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (position == 1) {
            player.getMetadata<InventoryTrackingMeta>()?.push(TestMenu(player, id + 1))
            return
        } else if (position == 2) {
            player.getMetadata<InventoryTrackingMeta>()?.replaceCurrent(TestMenu(player, id + 1))
            return
        }
        super.onItemClicked(inventory, position, player, action)
    }
}