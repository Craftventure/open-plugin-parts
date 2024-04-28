package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.hideAttributes
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.database.bukkit.extensions.itemRepresentation
import net.craftventure.database.type.BankAccountType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CraftventureMenu private constructor() : InventoryMenu() {
    init {
        rowsCount = 4
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
        )
        titleComponent = centeredTitle("Craftventure")
    }

    private val storesSlot = calculateZeroBasedIndex(1, 1)
    private val minigamesSlot = calculateZeroBasedIndex(1, 3)
    private val audioSlot = calculateZeroBasedIndex(1, 5)
    private val toggleSlot = calculateZeroBasedIndex(1, 7)

    private val buySlot = calculateZeroBasedIndex(3, 2)
    private val timeSlot = calculateZeroBasedIndex(3, 4)
    private val rulesSlot = calculateZeroBasedIndex(3, 6)

    override fun onResumed() {
        super.onResumed()

        registerResumedTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            invalidate()
        }, 0L, 20L))
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return

        when (position) {
            rulesSlot -> {
                executeSync {
                    player.openMenu(RulesMenu(player))
                }
            }

            buySlot -> {
                executeSync {
                    player.closeInventoryStack()
                    player.chat("/buy")
                }
            }

            toggleSlot -> {
                executeSync { player.openMenu(VisibilityMenu(player)) }
            }

            storesSlot -> {
                executeSync { player.openMenu(StoresMenu(player)) }
            }

            minigamesSlot -> {
                executeSync { player.openMenu(MinigamesMenu(player)) }
            }

            audioSlot -> {
                executeSync {
                    player.closeInventoryStack()
                    player.chat("/audio")
                }
            }
        }
    }

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)
        inventory.setItem(
            rulesSlot,
            ItemStack(Material.WRITABLE_BOOK).displayName(CVTextColor.MENU_DEFAULT_TITLE + "Rules")
        )

        val dateAmsterdam = LocalDateTime.now().atZone(ZoneId.of("Europe/Amsterdam"))
            .format(DateTimeFormatter.ofPattern("HH'h' mm'm' ss's' VV"))
        inventory.setItem(
            timeSlot, ItemStack(Material.CLOCK).displayName(CVTextColor.MENU_DEFAULT_TITLE + "Server time")
                .loreWithBuilder {
                    text("Current server time (24h)", endWithBlankLine = true)
                    text(dateAmsterdam, endWithBlankLine = true)
                    text("This is the time that will be used by default in all our communications")
                }
                .hideAttributes()
        )

        inventory.setItem(
            buySlot, BankAccountType.VC.itemRepresentation
                .displayName(CVTextColor.MENU_DEFAULT_TITLE + "VIP & Donations")
                .loreWithBuilder { text("Buy ranks, items or just donate money to the server") }
                .hideAttributes()
        )

        inventory.setItem(
            toggleSlot, ItemStack(Material.REPEATER)
                .displayName(CVTextColor.MENU_DEFAULT_TITLE + "Settings")
                .loreWithBuilder { text("Some basic settings that can be toggled") }
                .hideAttributes()
        )

        inventory.setItem(
            storesSlot, ItemStack(Material.ENDER_CHEST)
                .displayName(CVTextColor.MENU_DEFAULT_TITLE + "Park Shops")
                .loreWithBuilder { text("Find a shop to spend your VentureCoins") }
                .hideAttributes()
        )

        inventory.setItem(
            audioSlot, ItemStack(Material.JUKEBOX)
                .displayName(CVTextColor.MENU_DEFAULT_TITLE + "AudioServer")
                .loreWithBuilder { text("Send a link in chat to open the AudioServer. Then click it and connect to hear music from the park, use the parkmap and more") }
                .hideAttributes()
        )

        inventory.setItem(
            minigamesSlot, dataItem(Material.FIREWORK_STAR, 8)
                .displayName(CVTextColor.MENU_DEFAULT_TITLE + "Minigames")
                .loreWithBuilder { text("Find one of the minigames that you can play on Craftventure") }
                .hideAttributes()
        )
    }

    companion object {
        private var menu: CraftventureMenu? = null

        fun getInstance(): CraftventureMenu {
            if (menu == null)
                menu = CraftventureMenu()
            return menu!!
        }
    }
}