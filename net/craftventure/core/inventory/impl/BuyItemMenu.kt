package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.displayNamePlain
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.core.utils.OwnableItemUtils
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.craftventure.temporary.getOwnableItemMetadata
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BuyItemMenu(
    player: Player,
    val itemId: String
) : InventoryMenu(
    owner = player,
) {
    private var item: InventoryItem? = null
    private var itemStack: ItemStack? = null
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
    private var multiplyAmount: Int = 1
        set(value) {
            if (field != value) {
                field = value.coerceIn(1, MAX_MULTIPLY)
                updateTitle()
                updateItem()
            }
        }

    init {
        rowsCount = 1
        closeButtonIndex = StaticSlotIndex(1)
        updateTitle()
        executeAsync {
            val ownedItems = MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId, itemId)
            val ownableItem = MainRepositoryProvider.ownableItemRepository.findCached(itemId)
            val itemStackData =
                if (ownableItem != null) MainRepositoryProvider.itemStackDataRepository
                    .findSilent(ownableItem.guiItemStackDataId!!)
                else null

            if (ownableItem != null && itemStackData != null && (ownableItem.price ?: -1) > 0) {
//                Logger.debug("Loaded item")
                this.item = InventoryItem(
                    ownableItem,
                    itemStackData,
                    ownedItems
                )
                updateItem()
            } else {
                val meta = ownableItem?.getOwnableItemMetadata()
//                logcat { "Meta? ${meta != null} ${meta?.tebexPackageId} ${ownableItem?.metadata}" }
                if (meta?.tebexPackageId != null) {
                    player.sendTitleWithTicks(subtitle = "See the link in the chat to buy this item", stay = 20 * 5)
                    player.sendMessage(
                        CVTextColor.serverNotice + "Buy this package at " + (Component.text("https://craftventure.buycraft.net/")
                            .clickEvent(
                                ClickEvent.openUrl("https://craftventure.buycraft.net/")
                            ))
                    )
                } else {
                    player.sendTitleWithTicks(subtitle = "This item is not for sale", stay = 20 * 5)
                }
                executeSync { player.closeInventoryStack() }
            }
        }
    }

    private fun updateItem() {
        executeAsync {
            //            Logger.debug("Updating item")
            val item = this.item
            if (item != null)
                this.itemStack = OwnableItemUtils.toItem(
                    ownableItem = item.ownableItem,
                    shopItemStackData = item.itemStack,
                    player = requireOwner(),
                    forceVisible = false,
                    multiplyAmount = multiplyAmount,
                    ownedItems = item.ownedItems
                )
            else
                this.itemStack = null
            loading = false
        }
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return
        if (action == InventoryAction.NOTHING) return

        if (CraftventureCore.getInstance().isShuttingDown) {
            player.sendMessage(Translation.SHUTDOWN_PREPARING.getTranslation(player)!!)
            player.closeInventoryStack()
            return
        }

//        Logger.debug("onItemClicked $inventory $action")
//        if (true) return
        when (position) {
            3 -> if (multiplyAmount > 1) multiplyAmount--
            4 -> {
                val item = this.item
                if (item != null) {
                    if (OwnableItemUtils.canEquip(player, item.ownableItem, item.ownedItems)) {
                        executeSync {
                            player.closeInventoryStack()
                            executeAsync {
                                OwnableItemUtils.equip(item.ownableItem, player)
                            }
                        }
                    } else {
                        executeSync {
//                        player.closeInventoryStack()
                            val confirmMenu = ConfirmMenu(
                                player,
                                "Are you sure?",
                                ConfirmMenu.generateBuyItem(
                                    item.ownableItem,
                                    item.itemStack,
                                    itemStack ?: ItemStack(Material.BARRIER),
                                    amount = multiplyAmount
                                )
                            ) {
                                executeAsync {
                                    OwnableItemUtils.buy(item.ownableItem, player, multiplyAmount)
                                }
                            }
                            player.openMenu(confirmMenu)
                        }
                    }
                }
            }

            5 -> multiplyAmount++
        }
    }

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)
        val item = this.item
        if (item != null) {
            inventory.setItem(4, itemStack)

            if (item.ownableItem.consumable!!.allowOwningMultiple) {
                if (multiplyAmount > 1) {
                    inventory.setItem(3, ItemStack(Material.RED_CONCRETE).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Less of this item")
                    })
                } else {
                    inventory.setItem(3, ItemStack(Material.AIR))
                }
                if (multiplyAmount < MAX_MULTIPLY)
                    inventory.setItem(5, ItemStack(Material.GREEN_CONCRETE).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "More of this item")
                    })
            } else {
                inventory.setItem(3, ItemStack(Material.AIR))
                inventory.setItem(5, ItemStack(Material.AIR))
            }
        }
    }

    private fun updateTitle() {
        titleComponent = when {
            loading -> centeredTitle("(loading)")
            else -> centeredTitle(item?.itemStack?.itemStack?.displayNamePlain() ?: "Untitled item")
        }
    }

    private class InventoryItem(
        val ownableItem: OwnableItem,
        val itemStack: ItemStackData,
        val ownedItems: List<PlayerOwnedItem>
    )

    companion object {
        const val MAX_MULTIPLY = 20
    }
}