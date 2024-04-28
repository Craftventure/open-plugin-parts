package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.manager.visibility.VisibilityMeta
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class VisibilityMenu(
    player: Player
) : InventoryMenu(
    owner = player,
) {
    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
        )
        rowsCount = 2
        titleComponent = centeredTitle("Visibility Management")
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return

        val visibilityMeta = player.getOrCreateMetadata { VisibilityMeta(player) }
//        Logger.info("Item $position pressed on meta $visibilityMeta")
        when (position) {
            9 -> {
                visibilityMeta.toggleHideAllPlayers()
                invalidate()
            }

            10 -> {
                visibilityMeta.toggleHideOnAudioMap()
                invalidate()
            }

            11 -> {
                visibilityMeta.toggleVanish()
                invalidate()
            }
        }
    }

    override fun onLayout(inventory: Inventory) {
        val player = requireOwner()
        addNavigationButtons(inventory)
        // 20/22/24
        val visibilityMeta = player.getOrCreateMetadata { VisibilityMeta(player) }

        val toggledPlayersStack = ItemStack(
            when {
                visibilityMeta.hideAllPlayers -> Material.GREEN_TERRACOTTA
                else -> Material.RED_TERRACOTTA
            }
        )
        toggledPlayersStack.displayName(CVTextColor.MENU_DEFAULT_TITLE + "Toggle players")
        toggledPlayersStack.loreWithBuilder {
            text("Switches between player visibility for your current session.")
            emptyLines(1)
            labeled(
                "Current player visibility",
                if (visibilityMeta.hideAllPlayers) "all non-crew is hidden" else "everyone is visible"
            )
            labeled("Persistence", "Session")
        }

        inventory.setItem(9, toggledPlayersStack)

        val audioServerVisibilityStack = ItemStack(
            when {
                visibilityMeta.hiddenOnAudioMap -> Material.GREEN_TERRACOTTA
                else -> Material.RED_TERRACOTTA
            }
        )
        audioServerVisibilityStack.displayName(CVTextColor.MENU_DEFAULT_TITLE + "Toggle AudioServer map visibility")
        audioServerVisibilityStack.loreWithBuilder {
            text("Hide yourself on the AudioServer map.")
            emptyLines(1)
            labeled("Current visibility on the map", if (visibilityMeta.hiddenOnAudioMap) "hidden" else "visible")
            labeled("Persistence", "AudioServer Session")
        }

        inventory.setItem(10, audioServerVisibilityStack)

        if (player.isCrew()) {
            val vanishedStack = ItemStack(
                when {
                    visibilityMeta.vanished -> Material.GREEN_TERRACOTTA
                    else -> Material.RED_TERRACOTTA
                }
            )
            vanishedStack.displayName(CVTextColor.MENU_DEFAULT_TITLE + "Vanish")
            vanishedStack.loreWithBuilder {
                text("Switches between vanish state for your current session.")
                emptyLines(1)
                labeled("Current state", if (visibilityMeta.vanished) "vanished for non-crew" else "visible")
                labeled("Persistence", "Session")
            }

            inventory.setItem(11, vanishedStack)
        }
    }
}