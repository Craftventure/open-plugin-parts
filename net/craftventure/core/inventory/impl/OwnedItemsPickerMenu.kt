package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayNamePlain
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.database.VirtualItemsProvider
import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.manager.CoinBoostManager
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.popMenu
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.generated.cvdata.tables.pojos.Title
import net.craftventure.database.type.ItemType
import net.craftventure.database.wrap.WrappedPlayerOwnedItem
import net.craftventure.database.wrap.WrappedPlayerOwnedItem.Companion.wrap
import net.craftventure.temporary.getOwnableItemMetadata
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class OwnedItemsPickerMenu(
    player: Player,
    itemFilter: OwnedItemsMenu.ItemFilter? = null,
    private val onItemPicked: (OwnableItem) -> Unit,
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
    private var coinsPerMinute = 0L
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
    var itemFilter: OwnedItemsMenu.ItemFilter? = itemFilter
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var sort: Sort = Sort.BUYDATE
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private enum class Sort {
        BUYDATE,
        NAME
    }

    private data class Item(
        val id: String,
        val owned: List<WrappedPlayerOwnedItem>,
        val ownableItem: OwnableItem?,
        val itemStackData: ItemStackData?
    ) {
        val sort: String = itemStackData?.itemStack?.displayNamePlain() ?: id
    }

    private data class ItemKey(
        val itemId: String,
        val virtual: Boolean,
        val metadata: OwnableItemMetadata?
    )

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()
        executeAsync {
            val items = mutableListOf<InventoryItem>()
            coinsPerMinute = CoinBoostManager.getCoinRewardPerMinute(player).toLong()

            val rawPlayedOwnedItems = (MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId).wrap() +
                    VirtualItemsProvider.provideVirtualItems(player))

            val playerOwnedItems = rawPlayedOwnedItems
                .groupBy {
                    ItemKey(
                        it.ownedItemId!!,
                        if (it is WrappedPlayerOwnedItem) it.isVirtual else false,
                        it.getOwnableItemMetadata()
                    )
                }
                .filter { it.value.isNotEmpty() }
                .mapValues { data ->
                    val key = data.value.first().ownedItemId!!
                    val ownableItem = MainRepositoryProvider.ownableItemRepository.findCached(key)
                    val itemStackData = if (ownableItem != null)
                        MainRepositoryProvider.itemStackDataRepository.findCached(ownableItem.guiItemStackDataId!!)
                    else
                        null
                    Item(
                        key,
                        data.value.sortedBy { it.source.buyDate!! },
                        ownableItem,
                        itemStackData
                    )
                }

            val sortedItems = when (sort) {
                Sort.BUYDATE -> playerOwnedItems.values.sortedWith(compareBy<Item> { it.owned.first().buyDate }
                    .thenBy { it.owned.first().isVirtual })

                else -> playerOwnedItems.values.sortedWith(compareBy<Item> { it.sort }
                    .thenBy { it.owned.first().isVirtual })
            }

            for (item in sortedItems) {
                val distinctItems = item.owned.groupBy { it.getOwnableItemMetadata() }

                val ownableItem = item.ownableItem
                if (ownableItem == null) {
                    // TODO: Unknown item, perhaps some deleted/legacy item
                    continue
                }
                if (itemFilter != null && !itemFilter.matches(item.ownableItem)) {
                    continue
                }
                distinctItems.values.forEach { distinctedPlayerOwnedItems ->
                    val playerOwnedItem = distinctedPlayerOwnedItems.first()
                    if (ownableItem.enabled!!) {
                        val itemStackData = item.itemStackData
                        if (itemStackData != null) {
                            val title =
                                if (ownableItem.type == ItemType.TITLE)
                                    MainRepositoryProvider.titleRepository.findCached(
                                        ownableItem.id!!.replace(
                                            "title_",
                                            ""
                                        )
                                    )
                                else null

                            val item = InventoryItem(
                                playerOwnedItem,
                                ownableItem,
                                title,
                                itemStackData,
                                OwnedItemsMenu.createItem(
                                    playerOwnedItem,
                                    ownableItem,
                                    title,
                                    itemStackData,
                                    distinctedPlayerOwnedItems.size,
                                    actionBuilder = {},
                                )
                            )
                            items.add(item)
                        }
                    }
                }
            }

            this.items = items

            loading = false
        }
    }

    private fun itemTitle(): String {
        return when {
            itemFilter != null -> itemFilter?.displayName ?: "My items"
            else -> "My items"
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle(itemTitle(), loading)
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    private fun filteredItems() = items.asSequence()/*.filter {
//        itemFilter == null || itemFilter == it.
    }*/

    override fun provideItems(): List<ItemStack> = filteredItems().map { it.item }.toList()

    override fun onProvidedItemClicked(
        inventory: Inventory,
        index: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        super.onProvidedItemClicked(inventory, index, row, column, player, action)

        val item = filteredItems().toList().getOrNull(index)
        if (item != null) {
            player.popMenu()
            onItemPicked(item.ownableItem)
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

//        val itemStack = inventory.getItem(position)
//        if (itemStack != null) {
//            val id = itemStack.getItemId()
//            if (id == ITEM_NAME_RETURN_TO_EQUIPMENT) {
//                executeSync { EquipmentMenu(player).open(player) }
//            }
//        }

        if (itemFilter != null && (itemFilter!!.matches(ItemType.COIN_BOOSTER) || itemFilter!!.matches(ItemType.SERVER_COIN_BOOSTER)) && position == 5) {
            executeSync { player.openMenu(ServerCoinBoosterListMenu(player)) }
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)
    }

    data class InventoryItem(
        val playerOwnedItem: WrappedPlayerOwnedItem?,
        val ownableItem: OwnableItem,
        val title: Title?,
        val itemStackData: ItemStackData?,
        val item: ItemStack
    )
}