package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.displayNamePlain
import net.craftventure.bukkit.ktx.extension.hideAttributes
import net.craftventure.bukkit.ktx.extension.hideEnchants
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.database.ItemStackLoader
import net.craftventure.core.database.VirtualItemsProvider
import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.ktx.extension.takeIfNotBlank
import net.craftventure.core.manager.CoinBoostManager
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.popMenu
import net.craftventure.core.utils.OwnableItemUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.getItemStack
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.generated.cvdata.tables.pojos.Title
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.EquippedItemSlot
import net.craftventure.database.type.ItemType
import net.craftventure.database.wrap.WrappedPlayerOwnedItem
import net.craftventure.database.wrap.WrappedPlayerOwnedItem.Companion.wrap
import net.craftventure.temporary.getOwnableItemMetadata
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class OwnedItemsMenu(
    player: Player,
    itemFilter: ItemFilter? = null,
    slot: EquippedItemSlot?
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
    var itemFilter: ItemFilter? = itemFilter
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var slot: EquippedItemSlot? = slot
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
                                createItem(
                                    playerOwnedItem,
                                    ownableItem,
                                    title,
                                    itemStackData,
                                    distinctedPlayerOwnedItems.size
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
            executeSync {
                player.popMenu()
                player.openMenu(LoadingMenu(player, allowNavigation = false))

                executeAsync {
                    OwnableItemUtils.equip(player, item.playerOwnedItem!!.source, item.ownableItem, slot)

                    executeSync {
                        player.popMenu()
                    }
                }
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

//        if (showEquipmentMenu)
//            inventory.setItem(
//                5, MaterialConfig.GUI_CHESTPLATE.clone().displayName(ITEM_NAME_RETURN_TO_EQUIPMENT)
//                    .setItemId(ITEM_NAME_RETURN_TO_EQUIPMENT)
//                    .hideAttributes()
//            )

        if (itemFilter != null && (itemFilter!!.matches(ItemType.SERVER_COIN_BOOSTER) || itemFilter!!.matches(ItemType.COIN_BOOSTER))) {
            val info = ItemStack(Material.ENDER_CHEST).displayName(CVTextColor.MENU_DEFAULT_TITLE + "Info")
            info.loreWithBuilder {
                text("Current reward")
                text("$coinsPerMinute", CVTextColor.MENU_DEFAULT_LORE)
                text(BankAccountType.VC.emoji, NamedTextColor.WHITE)
                moveToBlankLine()
                text("This is the amount of VentureCoins(")
                text(BankAccountType.VC.emoji, NamedTextColor.WHITE, addSpace = false)
                text(") that you will gain every minute when being non-AFK", addSpace = false)
                emptyLines(1)
                accented("Some ways to increase this amount:")
                moveToBlankLine()
                accented("- VIPs receive 1")
                text(BankAccountType.VC.emoji, NamedTextColor.WHITE)
                accented(" per minute as a bonus (see /buy)")
                moveToBlankLine()
                accented("- Personal coinboosters from the coin booster shop (max of 1 active at a time)")
                moveToBlankLine()
                accented("- Server coinboosters (see /buy)")
                moveToBlankLine()
                accented("- Some items may reward a 1")
                text(BankAccountType.VC.emoji, NamedTextColor.WHITE)
                accented(" per minute bonus")
                moveToBlankLine()
                accented("- Nitro boosters on Discord receive a 1")
                text(BankAccountType.VC.emoji, NamedTextColor.WHITE)
                accented(" per minute bonus (your account must be linked by /discord link)")
                moveToBlankLine()
                emptyLines(1)
                action("Click to view list of active coinboosters")
            }
            inventory.setItem(5, info)
        }
    }

    data class InventoryItem(
        val playerOwnedItem: WrappedPlayerOwnedItem?,
        val ownableItem: OwnableItem,
        val title: Title?,
        val itemStackData: ItemStackData?,
        val item: ItemStack
    )

    interface ItemFilter {
        val displayName: String

        fun matches(itemType: ItemType): Boolean
        fun matches(ownableItem: OwnableItem): Boolean
    }

    class SimpleItemFilter constructor(
        val itemTypes: Array<ItemType>?,
        override val displayName: String = itemTypes!!.joinToString(", ") { it.displayNamePlural },
    ) : ItemFilter {
        override fun matches(itemType: ItemType) = itemTypes != null && itemType in itemTypes
        override fun matches(ownableItem: OwnableItem) = itemTypes == null || ownableItem.type in itemTypes

        companion object {
            fun from(itemType: ItemType) = SimpleItemFilter(arrayOf(itemType), itemType.displayName)
        }
    }

    companion object {
        fun createItem(
            playerOwnedItem: WrappedPlayerOwnedItem?,
            ownableItem: OwnableItem,
            title: Title?,
            itemStackData: ItemStackData?,
            amount: Int,
            actionBuilder: ComponentBuilder.LoreBuilder.() -> Unit = {
                if (ownableItem.consumable!!.consumeUponUsage) {
                    emptyLines()
                    accented("Consumed upon usage")

                    if (ownableItem.type!!.consumeViaMenu)
                        action("Click to consume 1 item")
                }
            },
        ): ItemStack {
//        Logger.debug("Adding ${ownableItem.id}")
            val itemStack = itemStackData?.getItemStack(ownableItem.type!!)!!
            itemStack.amount = amount
            itemStack.displayNameWithBuilder {
                text(
                    title?.displayName ?: itemStackData.overridenTitle ?: itemStack.displayNamePlain()?.trim()
                        ?.takeIfNotBlank() ?: "Untitled Item"
                )
            }

            val meta = ItemStackLoader.getMeta(ownableItem, playerOwnedItem!!.source)
            if (meta != null) {
                ItemStackLoader.apply(itemStack, meta)
            }

            itemStack.loreWithBuilder {
                if (playerOwnedItem.isVirtual)
                    accented("Virtual item").moveToBlankLine()

                if (amount > 1 && ownableItem.consumable!!.allowOwningMultiple)
                    accented("${amount}x")
                accented(ownableItem.type!!.nameForAmount(amount))
                moveToBlankLine()

                if (ownableItem.type == ItemType.TITLE && title != null) {
                    emptyLines()
                    accented("Display above your head:").moveToBlankLine()
                    component((title.title ?: "<Broken>").parseWithCvMessage().decoration(TextDecoration.ITALIC, false))
                    emptyLines()

                    if (title.description != null) {
                        accented("Description:").moveToBlankLine()
                        text(title.description!!)
                    }
                }

                if (ownableItem.type == ItemType.COIN_BOOSTER || ownableItem.type == ItemType.SERVER_COIN_BOOSTER) {
                    val booster =
                        MainRepositoryProvider.coinBoosterRepository.cachedItems.firstOrNull { it.id == ownableItem.id }
                    if (booster != null) {
                        emptyLines()
                        accented("${if (booster.server!!) "Server" else "Personal"} coin booster")
                    }
                }

                itemStackData.overridenLore?.let {
                    emptyLines()
                    text(it)
                }

                if (ownableItem.type!!.equippable) {
                    if (playerOwnedItem != null && playerOwnedItem.paidPrice!! > 0) {
                        emptyLines()
                        accented("Bought for")
                        text(ownableItem.bankAccountType!!.emoji, color = NamedTextColor.WHITE)
                        accented("${playerOwnedItem.paidPrice!! * amount}")
                    }
                }

                emptyLines()
                meta?.describe(this)
//                if (!describedMeta.isNullOrEmpty()) {
//                    emptyLines()
//                    describedMeta.forEach {
//                        moveToBlankLine()
//                        component(it)
//                    }
//                }

                actionBuilder()
            }
            return itemStack.hideAttributes().hideEnchants()
        }
    }
}