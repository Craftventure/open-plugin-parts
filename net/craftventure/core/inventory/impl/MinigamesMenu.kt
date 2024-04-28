package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.applyAllHideItemFlags
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigameManager
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.database.bukkit.extensions.teleportIfPermissioned
import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class MinigamesMenu(
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

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()
        updateItems()
    }

    private fun updateItems() {
        items = MinigameManager.all().map { game ->
            val warp = game.provideWarp()
            val loreBuilder = ComponentBuilder.LoreBuilder()
            loreBuilder.text(game.describeGameplay())
            loreBuilder.emptyLines(1)

            val duration = game.provideDuration()
            loreBuilder.accented(
                "Expected duration of${
                    when (duration.durationType) {
                        Minigame.DurationType.EXACT -> ""
                        Minigame.DurationType.MINIMUM -> " at least"
                        Minigame.DurationType.MAXIMUM -> " at most"
                    }
                } ${DateUtils.format(duration.duration.toMillis(), "?")}"
            )
            loreBuilder.emptyLines(1)

            val rewards = game.describeBalanceRewards()
            if (rewards.isNotEmpty()) {
                loreBuilder.text("Rewards:", endWithBlankLine = true)
                rewards.forEach { reward ->
                    loreBuilder.text("${reward.reward}")
                    loreBuilder.text(reward.accountType.emoji, color = NamedTextColor.WHITE)
                    loreBuilder.text(" ${reward.description}", endWithBlankLine = true)
                }
                loreBuilder.emptyLines(1)
            }

            if (game.isRunning) {
                val timeLeft = game.timeLeft ?: 0
                val formattedTime = DateUtils.format(timeLeft, "?")
                loreBuilder.accented("Finishing in $formattedTime\n\n")
            } else {
                val lobby = MinigameManager.lobbyById(game.internalName)
                if (lobby != null) {
                    loreBuilder.accented("Waiting for players (${lobby.queuedCount}/${lobby.maxPlayers})")
                    loreBuilder.emptyLines(1)
                } else
                    loreBuilder.accented("Waiting for players")
                loreBuilder.emptyLines(1)
            }

            if (warp != null) {
                loreBuilder.action("Click to warp")
            }

            InventoryItem(
                game,
                warp,
                game.represent().apply {
                    displayName(CVTextColor.MENU_DEFAULT_TITLE + game.displayName)
                    lore(loreBuilder.buildLineComponents())
                    applyAllHideItemFlags()
                }
            )
        }
    }

    override fun onResumed() {
        super.onResumed()
        registerResumedTask(
            Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
                updateItems()
            }, 0L, 20L)
        )
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Minigames of Craftventure")
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
            executeSync {
                player.closeInventoryStack()
                it.warp?.teleportIfPermissioned(player)
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
        val game: Minigame?,
        val warp: Warp?,
        val item: ItemStack
    )
}