package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.CraftventureKeys.isSeat
import net.craftventure.bukkit.ktx.util.CraftventureKeys.shopId
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.kyori.adventure.text.Component
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class EntityEditorMenu(
    val player: Player,
    private val entity: Entity
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
    var helpers = listOf(
        EntityStateHelper(
            { player, entity -> true },
            { player, entity ->
                ItemStack(Material.TNT).displayName(
                    Component.text(
                        "[DANGEROUS] Delete Entity",
                        CVTextColor.serverError
                    )
                )
            },
            { player, entity ->
                entity.remove()
                player.closeInventoryStack()
            }
        ),
        EntityStateHelper(
            { player, entity -> true },
            { player, entity ->
                ItemStack(Material.MINECART).displayName(
                    Component.text(
                        "Mount entity",
                        CVTextColor.MENU_DEFAULT_TITLE
                    )
                )
            },
            { player, entity -> entity.addPassenger(player) }
        ),
        EntityStateHelper(
            { player, entity -> entity is ItemFrame },
            { player, entity ->
                entity as ItemFrame
                ItemStack(Material.NETHERITE_SWORD).displayName(
                    Component.text(
                        "Make ${if (entity.isFixed) "unfixed" else "fixed"}",
                        CVTextColor.MENU_DEFAULT_TITLE
                    )
                )
            },
            { player, entity ->
                entity as ItemFrame
                entity.isFixed = !entity.isFixed
            }
        ),
        EntityStateHelper(
            { player, entity -> entity is ItemFrame },
            { player, entity ->
                entity as ItemFrame
                ItemStack(Material.POTION).displayName(
                    Component.text(
                        "Make ${if (entity.isVisible) "invisible" else "visible"}",
                        CVTextColor.MENU_DEFAULT_TITLE
                    )
                )
            },
            { player, entity ->
                entity as ItemFrame
                entity.isVisible = !entity.isVisible
            }
        ),
        EntityStateHelper(
            { player, entity -> true },
            { player, entity ->
                val shopId = entity.persistentDataContainer.shopId
                ItemStack(Material.POTION).displayName(
                    Component.text(
                        if (shopId == null) "Setup shoplink" else "Remove shoplink",
                        CVTextColor.MENU_DEFAULT_TITLE
                    )
                )
            },
            { player, entity ->
                if (entity.persistentDataContainer.shopId != null)
                    entity.persistentDataContainer.shopId = null
                else {
//                    player.sendMessage(CVTextColor.serverError + "This feature is temporarily disabled")
                    AnvilGUI.Builder()
                        .plugin(PluginProvider.plugin)
                        .onClose {

                        }
                        .onClick { slot, snapshot ->
                            entity.persistentDataContainer.shopId = snapshot.text
                            listOf(AnvilGUI.ResponseAction.close())
                        }
//                        .onComplete { player, name ->
//                            entity.persistentDataContainer.shopId = name
//                            AnvilGUI.Response.close()
//                        }
                        .itemLeft(ItemStack(Material.STONE))
                        .open(player)
                }
            }
        ),
        EntityStateHelper(
            { player, entity -> true },
            { player, entity ->
                ItemStack(Material.ACACIA_BOAT).displayName(
                    Component.text(
                        if (entity.isSeat) "Unmark as seat" else "Mark as seat",
                        CVTextColor.MENU_DEFAULT_TITLE
                    )
                )
            },
            { player, entity ->
                entity.isSeat = !entity.isSeat
                invalidate()
            }
        ),
    )
    val activeHelpers: List<EntityStateHelper>
        get() = helpers.filter { it.active(player, entity) }

    init {
        updateTitle()
    }

    private fun updateTitle() {
        titleComponent = centeredTitle("Entity Editor ${page + 1}/$maxPages")
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    override fun provideItems(): List<ItemStack> = activeHelpers.map { it.layout(player, entity) }
    override fun onProvidedItemClicked(
        inventory: Inventory,
        index: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        super.onProvidedItemClicked(inventory, index, row, column, player, action)
        activeHelpers.getOrNull(index)?.let {
            it.click(player, entity)
            invalidate()
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
        if (position == 1) {
            invalidate()
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)

        dataItem(Material.ARROW, 3).apply {
            displayName(Component.text("Refresh state", CVTextColor.MENU_DEFAULT_TITLE))
            inventory.setItem(1, this)
        }
    }

    data class EntityStateHelper(
        val active: (Player, Entity) -> Boolean,
        val layout: (Player, Entity) -> ItemStack,
        val click: (Player, Entity) -> Unit,
    )
}