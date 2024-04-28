package net.craftventure.core.inventory.impl

import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.manager.MailManager
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.item
import net.craftventure.database.generated.cvdata.tables.pojos.Mail
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class MailsMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartRowOffset = 1,
    maxItemRows = 4,
    owner = player,
) {
    private var items = emptyList<InventoryItem>()
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()
        executeAsync {
            val items = MainRepositoryProvider.mailRepository.getMailsFor(player.uniqueId)
                .sortedByDescending { it.sendDate }
                .map {
                    InventoryItem(it, it.item)
                }
            this.items = items
            invalidate()
            updateTitle()
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Mails", items.isEmpty())
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
            MailManager.open(player, it.mail.id!!)
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
        val mail: Mail,
        val item: ItemStack
    )
}