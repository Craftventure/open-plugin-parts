package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.isAllowed
import net.craftventure.database.bukkit.extensions.itemRepresentation
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.bukkit.extensions.teleportIfPermissioned
import net.craftventure.database.generated.cvdata.tables.pojos.Store
import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import net.craftventure.database.type.BankAccountType
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class StoresMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
    private var items = emptyList<InventoryItem>()
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var loading: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateTitle()
            }
        }

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()
        executeAsync {
            val items = mutableListOf<InventoryItem>()
            val warpDatabase = MainRepositoryProvider.warpRepository

            for (store in MainRepositoryProvider.storeRepository.cachedItems) {
                val warp = store.warpId?.let { warpDatabase.findCachedByName(it) }

                val baseItem =
                    store.itemStackDataId?.let { MainRepositoryProvider.itemStackDataRepository.findCached(it) }?.itemStack

                val itemStack: ItemStack = baseItem ?: BankAccountType.VC.itemRepresentation

                itemStack.displayName(CVTextColor.MENU_DEFAULT_TITLE + (store.displayName ?: ""))
                val loreBuilder = ComponentBuilder.LoreBuilder()
                store.description?.let {
                    loreBuilder.text(it)
                    loreBuilder.emptyLines(1)
                }

                if (warp != null && warp.isAllowed(player)) {
                    loreBuilder.action("Click to warp")
                }

                itemStack.lore(loreBuilder.buildLineComponents())

                val item = InventoryItem(
                    store,
                    warp,
                    itemStack
                )
                items.add(item)
            }

            items.sortBy { it.store.displayName }

            this.items = items
            loading = false
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Shops of Craftventure", items.isEmpty())
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
            executeSync {
                player.closeInventoryStack()
                it.warp?.teleportIfPermissioned(player)
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
        val store: Store,
        val warp: Warp?,
        val item: ItemStack
    )
}