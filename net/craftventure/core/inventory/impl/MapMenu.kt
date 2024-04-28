package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class MapMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartRowOffset = 1,
    maxItemRows = 4,
    owner = player,
) {
    private var items = emptyList<MapEntry>()
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
            val items = MainRepositoryProvider.mapEntriesRepository.itemsPojo()
            this.items = items
            invalidate()
            updateTitle()
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Maps", items.isEmpty())
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    override fun provideItems(): List<ItemStack> = items.map {
        val mapView = Bukkit.getMap(it.mapId!!)
        val item = if (mapView != null) {
            ItemStack(Material.FILLED_MAP, 1, it.mapId!!.toShort()).apply {
                displayName(CVTextColor.MENU_DEFAULT_TITLE + "Map ${it.mapId}: ${it.name}")
            }
        } else {
            ItemStack(Material.BARRIER).apply {
                displayName(CVTextColor.MENU_DEFAULT_TITLE + "Map ${it.mapId} (not found)")
            }
        }
        return@map item
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

        items.getOrNull(index)?.let {
            val item = ItemStack(Material.FILLED_MAP, 1, it.mapId!!.toShort()).apply {
                displayName(CVTextColor.MENU_DEFAULT_TITLE + "Map ${it.mapId}: ${it.name}")
            }
            player.inventory.addItem(item)
            player.sendMessage(CVTextColor.serverNotice + "Map added to your inventory")
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