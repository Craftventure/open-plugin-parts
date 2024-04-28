package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.isVisibleToPlayer
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.generated.cvdata.tables.pojos.Achievement
import net.craftventure.database.generated.cvdata.tables.pojos.AchievementCategory
import net.craftventure.database.generated.cvdata.tables.pojos.AchievementProgress
import net.craftventure.database.type.AchievementType
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.time.format.DateTimeFormatter

class AchievementsMenu(
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
    var categoryFilter: AchievementCategory? = null
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
            val items = mutableListOf<InventoryItem>()
            val achievements = MainRepositoryProvider.achievementRepository.cachedItems
                .map {
                    it to MainRepositoryProvider.achievementProgressRepository.getUnlockedOnly(
                        player.uniqueId,
                        it.id!!
                    )
                }
                .sortedWith(
                    compareBy(
                        { it.first.isHistoric == true },
                        { (it.second?.count ?: 0) == 0 },
                        { it.first.displayName })
                )
            for (achievementPair in achievements) {
                val achievement = achievementPair.first
                val rewardedAchievement = achievementPair.second
                if (achievement.isVisibleToPlayer(player, rewardedAchievement)) {
                    val unlocked = (rewardedAchievement?.count ?: 0) > 0

                    val itemStack = achievement.itemstackId?.let {
                        MainRepositoryProvider.itemStackDataRepository.findSilent(it)?.itemStack
                    }
                        ?: ItemStack(
                            when {
                                achievement.isHistoric!! -> Material.CYAN_TERRACOTTA
                                unlocked -> Material.LIME_TERRACOTTA
                                else -> Material.RED_TERRACOTTA
                            }
                        )

                    itemStack.displayNameWithBuilder {
                        text(achievement.displayName ?: "")
                        when (achievement.type) {
                            AchievementType.SECRET -> accented(" (Secret)")
                            AchievementType.ROAMING_DISPLAY_IF_UNLOCKED -> accented(" (Optional)")
                            else -> {}
                        }
                    }

                    val loreBuilder = ComponentBuilder.LoreBuilder()
                    if (achievement.isHistoric!!) {
                        loreBuilder.error("This achievement is historic")
                        loreBuilder.moveToBlankLine()
                    }

                    if (achievement.vcWorth != 0) {
                        loreBuilder.accented("${achievement.vcWorth}")
                        loreBuilder.text(achievement.bankAccountType!!.emoji, color = NamedTextColor.WHITE)
                    }

                    if (unlocked) {
                        if (achievement.description != null) {
                            loreBuilder.emptyLines(1)
                            loreBuilder.text(achievement.description!!)
                        }
                    } else if (achievement.mysteryDescription != null) {
                        loreBuilder.emptyLines(1)
                        loreBuilder.text(achievement.mysteryDescription!!)
                    }

                    if (rewardedAchievement != null) {
                        loreBuilder.emptyLines(1)
                        if (unlocked)
                            loreBuilder.accented(
                                "Unlocked at " + rewardedAchievement.unlockedAt?.format(
                                    DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                                )
                            )
//                        else
//                            loreBuilder.accented(
//                                "Previously unlocked at " + rewardedAchievement.unlockedAt?.format(
//                                    DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
//                                )
//                            )

                        if (rewardedAchievement.count!! > 1) {
                            loreBuilder.moveToBlankLine()
                            loreBuilder.accented("Rewarded ${if (rewardedAchievement.count!! > 999) "999+ (showoff!)" else rewardedAchievement.count.toString()} times")
                        }
                    }

//                    if (player.isCrew()) {
//                        if (lore.isNotEmpty())
//                            lore += "\n\n"
//                        lore += CVChatColor.MENU_DEFAULT_LORE_ACCENT + "[Crew] Unlocked by ${MainRepositoryProvider.achievementProgressRepository.totalUnlockCount(achievement.id)}"
//                    }

//                    if (achievement.vcWorth != 0) {
//                        if (lore.isNotEmpty())
//                            lore += "\n"
//                        lore += CVChatColor.MENU_DEFAULT_LORE_ACCENT + "Worth ${achievement.vcWorth} ${achievement.bankAccountType.nameForAmount(achievement.vcWorth)}"
//                    }

                    itemStack.lore(loreBuilder.buildLineComponents())

                    val item = InventoryItem(
                        achievement,
                        rewardedAchievement,
                        itemStack
                    )
                    items.add(item)
                } else {
                    if (player.isCrew())
                        items.add(InventoryItem(
                            achievement,
                            rewardedAchievement,
                            ItemStack(Material.BARRIER).apply {
                                displayNameWithBuilder { text("[Crew debug] " + achievement.displayName) }
                                loreWithBuilder {
                                    text("Type: ${achievement.type!!.name}", endWithBlankLine = true)
                                    text(
                                        "BalanceType: ${achievement.vcWorth} ${
                                            achievement.bankAccountType!!.nameForAmount(
                                                achievement.vcWorth!!
                                            )
                                        }", endWithBlankLine = true
                                    )
                                    text("Id: ${achievement.id}", endWithBlankLine = true)
                                    text("Item: ${achievement.itemstackId}", endWithBlankLine = true)
                                    text("Category: ${achievement.category}", endWithBlankLine = true)
                                    text("Enabled: ${achievement.enabled}", endWithBlankLine = true)
                                    text("Historic: ${achievement.isHistoric}", endWithBlankLine = true)
                                    text(
                                        "Unlocked description: ${achievement.description}",
                                        endWithBlankLine = true
                                    )
                                    text("Locked description: ${achievement.mysteryDescription}")
                                }
                            }
                        ))
                }
            }
            this.items = items.sortedBy { it.item.type == Material.BARRIER }
            loading = false
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle(categoryFilter?.displayName ?: "Achievements", items.isEmpty())
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    private fun filteredItems() = items.asSequence()
        .filter {
            categoryFilter == null || categoryFilter?.id.equals(it.achievement.category, ignoreCase = true)
        }

    override fun provideItems(): List<ItemStack> = filteredItems().map { it.item }.toList()

    override fun onStaticItemClicked(
        inventory: Inventory,
        position: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        if (inventory.getItem(position) == null) return

        if (position == 9) {
            categoryFilter = null
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)
    }

    data class InventoryItem(
        val achievement: Achievement,
        val rewardedAchievement: AchievementProgress?,
        val item: ItemStack
    )
}