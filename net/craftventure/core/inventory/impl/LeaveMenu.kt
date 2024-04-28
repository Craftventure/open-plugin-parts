package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.applyAllHideItemFlags
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.core.serverevent.ProvideLeaveInfoEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class LeaveMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartRowOffset = 1,
    maxItemRows = 4,
    owner = player,
) {
    private var items by invalidatingAlways(emptyList<InventoryItem>())

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()

        val event = ProvideLeaveInfoEvent(player)
        Bukkit.getPluginManager().callEvent(event)
        this.items = event.data.map { entry ->
            InventoryItem(entry, entry.representation.clone()
                .applyAllHideItemFlags()
                .displayNameWithBuilder { text(entry.category.name) }
                .loreWithBuilder { text(entry.description) }
            )
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Leaving", false)
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    override fun provideItems(): List<ItemStack> = items.map { it.item }

    override fun onProvidedItemClicked(
        inventory: Inventory,
        index: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        super.onProvidedItemClicked(inventory, index, row, column, player, action)

        items.getOrNull(index)?.let {
            player.closeInventoryStack()
            if (!it.entry.action.invoke()) {
                player.sendMessage(CVTextColor.serverError + "Failed to execute leave command, it may have expired. Please retry")
            }
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

    data class InventoryItem(
        val entry: ProvideLeaveInfoEvent.Entry,
        val item: ItemStack
    )
}