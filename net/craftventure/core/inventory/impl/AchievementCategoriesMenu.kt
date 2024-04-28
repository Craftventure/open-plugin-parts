package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.bukkit.ktx.util.SlotBackgroundManager
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.chat.bungee.util.ProgressBarGenerator
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.isVisibleToPlayer
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.repository.AchievementCategoryRepository
import net.craftventure.database.type.AchievementType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

class AchievementCategoriesMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
    private var items by invalidatingUnequality(emptyList<InventoryItem>())
    private var showHistoric by invalidatingUnequality(false) {
        scheduleLayout()
        applyAchievementMapping()
    }
    private var loading by invalidatingUnequality(true) {
        updateTitle()
    }

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()
        executeAsync {
            val rewardedAchievementDatabase = MainRepositoryProvider.achievementProgressRepository

            val categories = mutableListOf<AchievementCategoryRepository.CachedAchievementCategory>()
            for (cachedAchievementCategory in MainRepositoryProvider.achievementCategoryRepository.cachedCategoriesList) {
                if (cachedAchievementCategory.achievementCategory.type!!.isHideIfNoAchievementsUnlocked) {
                    for (achievement in cachedAchievementCategory.achievements) {
                        val rewardedAchievement = rewardedAchievementDatabase.getUnlockedOnly(player.uniqueId, achievement.id!!)
                        if (rewardedAchievement != null && achievement.isVisibleToPlayer(
                                player,
                                rewardedAchievement
                            )
                        ) {
                            categories.add(cachedAchievementCategory)
                            break
                        }
                    }
                } else {
                    categories.add(cachedAchievementCategory)
                }
            }

            categories.sortWith(compareBy<AchievementCategoryRepository.CachedAchievementCategory> { it.achievementCategory.isHistoric == true }.thenBy { it.categoryName })
//            categories.sortBy { it.categoryName }

            this.items = categories.map {
                val itemStack = (it.achievementCategory.itemstackId
                    ?.let { itemStackId ->
                        MainRepositoryProvider.itemStackDataRepository.cachedItems.firstOrNull { it.id == itemStackId }
                    }?.itemStack ?: ItemStack(Material.RED_TERRACOTTA))
                    .apply {
                        displayNameWithBuilder {
                            text(it.achievementCategory.displayName ?: "?")
                        }
                        lore(null)
                    }
                var visibleOptionalCount = 0
                var visibleCount = 0
                var unlockedCount = 0
                var unlockedSecretCount = 0
                var totalSecretsAvailable = 0
                var secretCount = 0

                for (achievement in it.achievements.filter { !it.isHistoric!! }) {
                    val rewardedAchievement = rewardedAchievementDatabase.getUnlockedOnly(player.uniqueId, achievement.id!!)

                    if (achievement.enabled!! && achievement.type == AchievementType.SECRET) {
                        totalSecretsAvailable++
                    }
                    if (achievement.isVisibleToPlayer(player, rewardedAchievement)) {
                        if (achievement.type == AchievementType.ROAMING_DISPLAY_IF_UNLOCKED)
                            visibleOptionalCount++
                        else if (achievement.type != AchievementType.SECRET)
                            visibleCount++
                        else
                            secretCount++

                        val hasUnlocked = rewardedAchievement?.count?.let { it > 0 } ?: false
                        if (hasUnlocked) {
                            if (achievement.type == AchievementType.SECRET)
                                unlockedSecretCount++
                            else if (achievement.type != AchievementType.ROAMING_DISPLAY_IF_UNLOCKED)
                                unlockedCount++
                        }
                    }
                }

                val loreBuilder = ComponentBuilder.LoreBuilder()
                if (it.achievementCategory.isHistoric!!) {
                    loreBuilder.error("This category is historic")
                    loreBuilder.emptyLines(1)
                }

                var percentageUnlocked = 0.0
                if (unlockedCount in 1..visibleCount)
                    percentageUnlocked = unlockedCount / visibleCount.toDouble() * 100

                if (itemStack.type == Material.RED_TERRACOTTA)
                    itemStack.type = when {
                        visibleCount == unlockedCount -> Material.LIME_TERRACOTTA
                        unlockedCount / visibleCount.toFloat() >= 0.66 -> Material.YELLOW_TERRACOTTA
                        unlockedCount / visibleCount.toFloat() >= 0.33 -> Material.ORANGE_TERRACOTTA
                        else -> Material.RED_TERRACOTTA
                    }
//                loreBuilder.accented("$unlockedCount/$visibleCount (${floor(percentageUnlocked).format(0)}%) unlocked")
//                loreBuilder.moveToBlankLine()
                if (visibleCount > 0 || unlockedCount > 0)
                    loreBuilder.component(
                        Component.text("", CVTextColor.MENU_DEFAULT_LORE_ACCENT)
                            .append(
                                ProgressBarGenerator.blockStyle(
                                    130,
                                    unlockedCount / max(visibleCount, unlockedCount).toDouble(),
                                    percentageText = true,
                                    progressTextProvider = {
                                        "$unlockedCount/$visibleCount (${
                                            floor(
                                                (unlockedCount / max(
                                                    visibleCount,
                                                    unlockedCount
                                                ).toDouble()) * 100
                                            ).roundToInt()
                                        }%)"
                                    },
                                    progressColor = NamedTextColor.DARK_GREEN,
                                    remainingColor = NamedTextColor.DARK_RED,
                                )
                            )
//                            .append(Component.text(" unlocked"))
                    )

                if (unlockedSecretCount > 0 || unlockedSecretCount < totalSecretsAvailable) {
                    loreBuilder.moveToBlankLine()
//                    loreBuilder.accented("${if (unlockedSecretCount > 1) "Secrets" else "Secret"} unlocked")
//                    loreBuilder.moveToBlankLine()
                    loreBuilder.component(
                        Component.text("Secrets unlocked", CVTextColor.MENU_DEFAULT_LORE_ACCENT)
                    )
                    loreBuilder.moveToBlankLine()
                    loreBuilder.component(
                        Component.text("", CVTextColor.MENU_DEFAULT_LORE_ACCENT)
                            .append(
                                ProgressBarGenerator.blockStyle(
                                    130,
                                    unlockedSecretCount / max(totalSecretsAvailable, unlockedSecretCount).toDouble(),
                                    percentageText = true,
                                    progressTextProvider = {
                                        "$unlockedSecretCount/$totalSecretsAvailable (${
                                            floor(
                                                (unlockedSecretCount / max(
                                                    totalSecretsAvailable,
                                                    unlockedSecretCount
                                                ).toDouble()) * 100
                                            ).roundToInt()
                                        }%)"
                                    },
                                    progressColor = NamedTextColor.DARK_GREEN,
                                    remainingColor = NamedTextColor.DARK_RED,
                                )
                            )
//                                .append(Component.text(" secrets unlocked"))
                    )
                }

                if (visibleOptionalCount > 0) {
                    loreBuilder.moveToBlankLine()
                    loreBuilder.accented("${visibleOptionalCount} optional unlocked")
                }

//                if (player.isOwner())
//                    lore += "\nOwner only: $unlockedSecretCount of $secretCount unlocked"

                if (it.achievementCategory.description != null) {
                    loreBuilder.emptyLines(1)
                    loreBuilder.text(it.achievementCategory.description!!)
                }
                itemStack.amount = max(unlockedCount, 1)
                itemStack.lore(loreBuilder.buildLineComponents())

                InventoryItem(it, itemStack, unlockedCount / max(visibleCount, unlockedCount).toDouble())
            }
            loading = false
            applyAchievementMapping()
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Achievement Categories", items.isEmpty())
    }

    override fun onPageChanged() {
        super.onPageChanged()
        applyAchievementMapping()
        updateTitle()
    }

    private fun applyAchievementMapping() {
        slotBackgroundManager.clearTag("ach")
        val items = filteredItems
        slotToItemMapping().forEach { (slot, item) ->
            val item = items.getOrNull(item)
            if (item != null) {
//                logcat { "Setting slot $slot to ${item.progress.format(2)}" }
                slotBackgroundManager.setSlot(
                    SlotBackgroundManager.slotIndex(slot),
                    FontCodes.Slot.underlayProgress(item.progress) ?: FontCodes.Slot.underlayProgress16,
                    if (item.progress.isNaN())
                        NamedTextColor.DARK_GRAY
                    else if (!item.progress.isNaN() && item.progress >= 1)
                        NamedTextColor.GREEN
                    else
                        NamedTextColor.DARK_RED,
                    tag = "ach"
                )
            }
        }

        triggerSlotChanges()
    }

    private val filteredItems get() = items.filter { if (showHistoric) true else it.cachedAchievementCategory.achievementCategory.isHistoric != true }

    override fun provideItems(): List<ItemStack> = filteredItems.map { it.item }

    override fun onProvidedItemClicked(
        inventory: Inventory,
        index: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        super.onProvidedItemClicked(inventory, index, row, column, player, action)

        val category = items.getOrNull(index)
        if (category != null)
            AchievementsMenu(player).apply {
                categoryFilter = category.cachedAchievementCategory.achievementCategory
                executeSync { player.openMenu(this) }
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
        super.onStaticItemClicked(inventory, position, row, column, player, action)

        if (position == 7) {
            showHistoric = !showHistoric
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)

        inventory.setItem(
            7,
            ItemStack(if (showHistoric) Material.OAK_SAPLING else Material.DEAD_BUSH)
                .displayName(CVTextColor.MENU_DEFAULT_TITLE + (if (showHistoric) "Hide historic (removed) achievements" else "Show historic (removed) achievements"))
                .loreWithBuilder { text("Only historic achievements which you unlocked will be shown if enabled") }
        )
    }

    data class InventoryItem(
        val cachedAchievementCategory: AchievementCategoryRepository.CachedAchievementCategory,
        val item: ItemStack,
        val progress: Double,
    )
}