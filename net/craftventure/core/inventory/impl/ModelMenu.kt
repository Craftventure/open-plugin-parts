package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.setItemId
import net.craftventure.core.inventory.InventoryConstants
import net.craftventure.core.inventory.LayoutInventoryMenu
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.math.max

class ModelMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartRowOffset = 2,
    maxItemRows = 3,
    owner = player,
) {
    private val modelType = listOf(
        ItemStack(Material.DIAMOND_SWORD).withEquipTitle(),
        ItemStack(Material.DIAMOND_HOE).withEquipTitle(),
        ItemStack(Material.IRON_SWORD).withEquipTitle(),
        ItemStack(Material.POTION).withEquipTitle(),
        ItemStack(Material.FIREWORK_STAR).withEquipTitle(),
        ItemStack(Material.SNOWBALL).withEquipTitle(),
        ItemStack(Material.STICK).withEquipTitle(),
        ItemStack(Material.LEATHER_HORSE_ARMOR).withEquipTitle(),
        ItemStack(Material.BREAD).withEquipTitle(),
        ItemStack(Material.POTION).withEquipTitle(),
        ItemStack(Material.COOKIE).withEquipTitle(),
        ItemStack(Material.MUSHROOM_STEW).withEquipTitle(),
        ItemStack(Material.HONEY_BOTTLE).withEquipTitle(),
        ItemStack(Material.COOKED_CHICKEN).withEquipTitle(),
        ItemStack(Material.RABBIT_STEW).withEquipTitle(),
        ItemStack(Material.APPLE).withEquipTitle(),
    )
    private var modelTypeOffset by invalidatingUnequality(0)

    private fun ItemStack.withEquipTitle() = this.apply {
        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Switch to ${type.name}")
    }

    private var items = emptyList<InventoryItem>()
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var generating: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var baseItem = ItemStack(Material.DIAMOND_SWORD)
        set(value) {
            if (field != value) {
                field = value
                regenerateItems()
            }
        }

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row2,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        regenerateItems()
    }

    private fun regenerateItems() {
        generating = true
        updateTitle()
        executeAsync {
            val items = mutableListOf<InventoryItem>()

            for (i in 1 until 500) {
                val item = ItemStack(baseItem.type)
                item.displayName(CVTextColor.MENU_DEFAULT_TITLE + "Model $i")
                item.updateMeta<ItemMeta> {
                    setCustomModelData(i)
                }
                items.add(InventoryItem(item))
            }

            this.items = items
            invalidate()
            generating = false
            updateTitle()
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle(
            baseItem.type.name.lowercase().replaceFirstChar { it.uppercase() },
            items.isEmpty()
        )
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
            val item = it.item
            player.inventory.addItem(item)
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

        val item = inventory.getItem(position) ?: return

        if (position == 9) modelTypeOffset = max(modelTypeOffset - 5, 0)
        if (position == 17) modelTypeOffset += 5

        when (position) {
            10, 11, 12, 13, 14, 15, 16 -> baseItem = item
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)

        inventory.setItem(9, InventoryConstants.getPreviousPageButton(modelTypeOffset, 100).setItemId("a"))
        for (i in 10..16) {
            inventory.setItem(i, modelType.getOrNull((i + modelTypeOffset) - 10))
        }

        inventory.setItem(17, InventoryConstants.getNextPageButton(modelTypeOffset, 100).setItemId("a"))
    }

    data class InventoryItem(
        val item: ItemStack
    )
}