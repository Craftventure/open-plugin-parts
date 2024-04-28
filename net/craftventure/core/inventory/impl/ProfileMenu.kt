package net.craftventure.core.inventory.impl

import com.destroystokyo.paper.profile.PlayerProfile
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.hideAttributes
import net.craftventure.bukkit.ktx.extension.toSkullItem
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.extension.toName
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemRepresentation
import net.craftventure.database.generated.cvdata.tables.pojos.GuestStat
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.ItemType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

class ProfileMenu(
    player: Player,
    val target: UUID = player.uniqueId,
    val showAsOther: Boolean = target != player.uniqueId
) : InventoryMenu(
    owner = player,
) {
    private var targetName: String? = null
    private var targetStat: GuestStat? = null
    private var gameProfile: PlayerProfile? = null

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
        )
        rowsCount = 2
        updateTitle()
        loadData()
    }

    private fun loadData() {
        executeAsync {
            targetName = target.toName()
            targetStat = MainRepositoryProvider.guestStatRepository.findSilent(target)
            gameProfile = Bukkit.getPlayer(target)?.playerProfile
            updateTitle()
            invalidate()
        }
    }

    private fun updateTitle() {
        if (isOtherPlayer()) {
            titleComponent = when {
                targetName != null -> centeredTitle("Profile of $targetName")
                else -> centeredTitle("Loading...")
            }
        } else {
            titleComponent = centeredTitle("My Profile")
        }
    }

    fun isOtherPlayer() = showAsOther

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return

        when (position) {
            11 -> {
                executeSync { player.openMenu(AchievementCategoriesMenu(player)) }
            }

            13 -> {
                executeSync { player.openMenu(EquipmentMenu(player)) }
            }

            15 -> {
//                if (CraftventureCore.isTestServer())
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
//                else
//                    CoinBoosterMenu(player).open(player)
            }
        }
    }

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)
        if (!isOtherPlayer()) {
            inventory.setItem(
                11,
                ItemStack(Material.EMERALD)
                    .displayName(CVTextColor.MENU_DEFAULT_TITLE + "Achievements")
                    .loreWithBuilder { text("View all achievements you've unlocked throughout your Craftventure playtime") }
                    .hideAttributes()
            )
            inventory.setItem(
                13,
                ItemStack(Material.LEATHER_CHESTPLATE)
                    .displayName(CVTextColor.MENU_DEFAULT_TITLE + "Equipment")
                    .loreWithBuilder { text("Change your equipment like your armor, balloons, titles and more") }
                    .hideAttributes()
            )
            inventory.setItem(
                15,
                BankAccountType.VC.itemRepresentation
                    .displayName(CVTextColor.MENU_DEFAULT_TITLE + "Coinboosters")
                    .loreWithBuilder { text("Manage/view your coinboosters") }
                    .hideAttributes()
            )
        } else {
            val skullItem = gameProfile?.toSkullItem() ?: ItemStack(Material.PLAYER_HEAD).apply {
                displayName(CVTextColor.MENU_DEFAULT_TITLE + (targetName ?: "Unknown"))
                targetStat?.let { targetStat ->
                    loreWithBuilder {
                        text("Total online time ${DateUtils.format(targetStat.totalOnlineTime!! * 1000L, "?")}")
                    }
                }
            }
            inventory.setItem(3, skullItem)
        }
    }
}