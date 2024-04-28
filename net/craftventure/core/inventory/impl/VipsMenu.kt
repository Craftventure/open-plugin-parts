package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.displayNamePlain
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.extension.toName
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta


class VipsMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
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
            val api = LuckPermsProvider.get()
            this.items =
                api.userManager.getWithPermission(api.nodeBuilderRegistry.forInheritance().group("vip").build().key)
                    .join()
                    .map { item ->
                        val name = api.userManager.lookupUsername(item.holder).join() ?: item.holder.toName()
                        ItemStack(Material.PLAYER_HEAD)
                            .displayName(CVTextColor.vip + "VIP $name")
                            .apply {
                                this.updateMeta<SkullMeta> {
                                    val player = Bukkit.getPlayer(item.holder)// ?: Bukkit.getOfflinePlayer(item.holder)
                                    if (player?.hasPlayedBefore() == true) {
                                        this.owningPlayer = player
                                    } else {
                                        type = Material.ZOMBIE_HEAD
                                    }
                                }
                            }
//                            .displayName(CVChatColor.MENU_DEFAULT_TITLE + logItem.at.split("T").first())
//                            .lore(CVChatColor.MENU_DEFAULT_LORE + logItem.message)
                    }
                    .sortedBy { it.displayNamePlain() }
            loading = false
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("The VIPs of Craftventure", items.isEmpty())
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
        if (inventory.getItem(position) == null) return

        if (position == 5) {
            executeSync { player.openMenu(AchievementCategoriesMenu(player)) }
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)
    }
}