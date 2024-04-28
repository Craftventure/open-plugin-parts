package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.core.utils.OwnableItemUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.listener.ShopCacheListener
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class ShopOfferMenu(
    player: Player,
    val cachedShop: ShopCacheListener.CachedShop
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

            for (cachedOffer in cachedShop.cachedOffers) {
                if (!cachedOffer.shopOffer.enabled!! ||
                    cachedOffer.ownableItem.price!! <= 0 ||
                    cachedOffer.ownableItem.buyAmount!! <= 0
                ) {
                    continue
                }
                val owneds =
                    MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId, cachedOffer.ownableItem.id!!)
                val itemStack =
                    OwnableItemUtils.toItem(
                        cachedOffer.ownableItem,
                        cachedOffer.shopItemStackData,
                        player,
                        false,
                        ownedItems = owneds
                    )
                        ?: continue

                val item = InventoryItem(
                    cachedOffer,
                    itemStack,
                    cachedOffer.shopItemStackData,
                    owneds
                )
                items.add(item)
            }

            this.items = items

            loading = false
            updateTitle()
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("${cachedShop.shop.displayName}", loading)
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

        if (CraftventureCore.getInstance().isShuttingDown) {
            player.sendMessage(Translation.SHUTDOWN_PREPARING.getTranslation(player)!!)
            player.closeInventoryStack()
            return
        }

        val item = items.getOrNull(index)
        if (item != null) {
            if (item.getOwnableItem().consumable!!.allowOwningMultiple)
                executeSync { player.openMenu(BuyItemMenu(player, item.getOwnableItem().id!!)) }
            else {
                if (OwnableItemUtils.canEquip(player, item.getOwnableItem(), item.ownedItems)) {
                    executeSync {
                        player.closeInventoryStack()
                        executeAsync {
                            OwnableItemUtils.equip(item.getOwnableItem(), player)
                        }
                    }
                } else {
                    executeSync {
//                    player.closeInventoryStack()
                        val menu = ConfirmMenu(
                            player,
                            "Are you sure?",
                            ConfirmMenu.generateBuyItem(item.getOwnableItem(), item.data, item.item, 1)
                        ) {
                            executeAsync {
                                OwnableItemUtils.buy(item.getOwnableItem(), player)
                            }
                        }
                        player.openMenu(menu)
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

    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)
    }

    data class InventoryItem(
        val cachedOffer: ShopCacheListener.CachedOffer,
        val item: ItemStack,
        val data: ItemStackData,
        val ownedItems: List<PlayerOwnedItem>
    ) {
        fun getOwnableItem() = cachedOffer.ownableItem
    }
}