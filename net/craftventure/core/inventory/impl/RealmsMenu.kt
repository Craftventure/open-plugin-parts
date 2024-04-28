package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.teleportIfPermissioned
import net.craftventure.database.generated.cvdata.tables.pojos.Realm
import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class RealmsMenu(
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
            for (realm in MainRepositoryProvider.realmRepository.itemsPojo()) {
                if (!realm.enabled!!) continue

                val itemStack = ItemStack(Material.MINECART)
                itemStack.displayName(CVTextColor.MENU_DEFAULT_TITLE + realm.displayName)
                itemStack.loreWithBuilder {
                    if (realm.description != null) {
                        text(realm.description!!)
                        emptyLines(1)
                    }
                    action("Left click to warp")
                    action("Right click to view rides")

                }

                val warp = realm.warpId?.let { warpDatabase.findCachedByName(it) }
                val item = InventoryItem(
                    realm,
                    warp,
                    itemStack
                )
                items.add(item)
            }

            items.sortBy { it.realm.displayName }

            this.items = items
            loading = false
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Realms", items.isEmpty())
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
            if (action == InventoryAction.PICKUP_HALF) {
                val ridesMenu = RidesMenu(player)
                ridesMenu.realmFilter = it.realm
                executeSync { player.openMenu(ridesMenu) }
            } else {
                executeSync { it.warp?.teleportIfPermissioned(player) }
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
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)
    }

    data class InventoryItem(
        val realm: Realm,
        val warp: Warp? = null,
        val item: ItemStack
    )
}