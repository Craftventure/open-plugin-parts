package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.ItemStackUtils2
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.manager.CoinBoostManager
import net.craftventure.database.type.ItemType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class ServerCoinBoosterListMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
    private var activeBoosts: List<CoinBoostManager.CoinBoost> = ArrayList()
    private var coinsPerMinute = 0L
    private var items = emptyList<ItemStack>()
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
            updateItems()
            loading = false
            triggerUpdateIfHasViewers()
        }
    }

    private fun triggerUpdateIfHasViewers() {
        executeAsync {
            updateItems()
        }
        if (hasViewers()) {
            executeSync(20) {
                //                Logger.info("Updating")
                triggerUpdateIfHasViewers()
            }
        }
    }

    private fun updateItems() {
        val player = requireOwner()
        coinsPerMinute = CoinBoostManager.getCoinRewardPerMinute(player).toLong()
        activeBoosts = CoinBoostManager.getAllBoosts(player)
        invalidate()

        val items = mutableListOf<ItemStack>()

        for (boost in activeBoosts) {
            val itemStack = ItemStack(Material.LEGACY_DOUBLE_PLANT)
            ItemStackUtils2.addEnchantmentGlint(itemStack)
            ItemStackUtils2.hideEnchants(itemStack)
            itemStack.displayName(CVTextColor.MENU_DEFAULT_TITLE + boost.displayName())
            itemStack.lore(boost.displayDescription())
            items.add(itemStack)
        }

        this.items = items
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Active Coinboosters", false)
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    override fun provideItems(): List<ItemStack> = items

    override fun onStaticItemClicked(
        inventory: Inventory,
        position: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return

        if (position == 5) {
            executeSync {
                val menu = OwnedItemsMenu(
                    player,
                    OwnedItemsMenu.SimpleItemFilter(
                        arrayOf(ItemType.COIN_BOOSTER, ItemType.SERVER_COIN_BOOSTER),
                        displayName = "(Server)Coin Boosters"
                    ),
                    slot = null
                )
                player.openMenu(menu)
            }
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)

        val info = ItemStack(Material.ENDER_CHEST)
        info.displayName(CVTextColor.MENU_DEFAULT_TITLE + "Coinboosters")
        info.lore(listOf(CVTextColor.MENU_DEFAULT_LORE_ACTION + "Click to view list of activatable coinboosters"))
        inventory.setItem(5, info)
    }
}