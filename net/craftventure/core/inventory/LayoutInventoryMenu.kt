package net.craftventure.core.inventory

import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.core.extension.getItemId
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

abstract class LayoutInventoryMenu(
    page: Int = 0,
    itemStartRowOffset: Int = 0,
    itemStartColumnOffset: Int = 0,
    maxItemRows: Int = 6,
    maxItemColumns: Int = 9,
    owner: Player?,
) : InventoryMenu(
    owner = owner,
) {
    protected var previousButtonIndex by invalidatingUnequality(LastRowSlotIndex(0, this))
    protected var nextButtonIndex by invalidatingUnequality(LastRowSlotIndex(8, this))

    var itemStartRowOffset: Int by invalidatingAlways(itemStartRowOffset)
    var itemStartColumnOffset: Int by invalidatingAlways(itemStartColumnOffset)
    var maxItemRows: Int by invalidatingAlways(maxItemRows)
    var maxItemColumns: Int by invalidatingAlways(maxItemColumns)

    var page: Int = page
        set(value) {
            val maxPages = maxPages - 1
            val safeValue = when {
                value < 0 -> 0
                value >= maxPages -> maxPages
                else -> value
            }
            if (field != safeValue) {
                field = safeValue
                onPageChanged()
                scheduleInventoryRecreation()
            }
        }

    protected open fun onPageChanged() {}

    val calculatedItemRowCount = Math.max(Math.min(rowsCount - itemStartRowOffset, maxItemRows), 0)
    val calculatedItemColumnCount = Math.max(Math.min(9 - itemStartColumnOffset, maxItemColumns), 0)

    val itemsPerPage: Int
        get() {
            return calculatedItemRowCount * calculatedItemColumnCount
        }

    val pageCount: Int
        get() {
            var pages = 1
            var remainingItems = provideItems().size
            while (remainingItems > 0) {
                remainingItems -= itemsPerPage
                if (remainingItems > 0)
                    pages++
            }
            return pages
        }

    val maxPages: Int
        get() {
            return Math.max(Math.ceil(provideItems().size / itemsPerPage.toDouble()).toInt(), 1)
        }

    protected abstract fun provideItems(): List<ItemStack>

    open fun onProvidedItemClicked(
        inventory: Inventory,
        index: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
    }

    open fun onStaticItemClicked(
        inventory: Inventory,
        position: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
    }

    final override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) {
            return
        }

        val row = position / 9
        val column = position % 9

//        Logger.info("Clicked $row $column")

//        Logger.info("row=$row $itemStartRowOffset $maxItemRows column=$column $itemStartColumnOffset $maxItemColumns")
        val staticRow = row < itemStartRowOffset || row >= itemStartRowOffset + maxItemRows
        val staticColumn = column < itemStartColumnOffset || column >= itemStartColumnOffset + maxItemColumns

        if (!staticRow && !staticColumn) {
            val itemRow = row - itemStartRowOffset
            val itemColumn = column - itemStartColumnOffset
            val subPosition = (page * itemsPerPage) + (itemRow * calculatedItemColumnCount) + itemColumn
            inventory.getItem(position)?.let {
                onProvidedItemClicked(inventory, subPosition, itemRow, itemColumn, player, action)
            }
//            Logger.info("Non static $itemRow $itemColumn $position")
        } else {
            onStaticItemClicked(inventory, position, row, column, player, action)
//            Logger.info("Static")
        }
    }

    final override fun onLayout(inventory: Inventory) {
        inventory.clear()

        val items = provideItems()
        slotToItemMapping().forEach { (slot, position) ->
            val item = items.getOrNull(position)
            if (item != null)
                inventory.setItem(slot, item)
        }
        onLayoutBase(inventory)
    }

    protected fun slotToItemMapping() = sequence<Pair<Int, Int>> {
        val columnCount = calculatedItemColumnCount
        val rowCount = calculatedItemRowCount
        val itemsPerPage = itemsPerPage

        for (x in 0 until columnCount) {
            for (y in 0 until rowCount) {
                val position = (page * itemsPerPage) + (y * columnCount) + x
                val realPosition = (itemStartRowOffset * 9) + (y * 9) + x + itemStartColumnOffset
                yield(realPosition to position)
            }
        }
    }

    abstract fun onLayoutBase(inventory: Inventory)

    override fun handleNavigationButtonsClick(
        inventory: Inventory,
        position: Int,
        player: Player,
        action: InventoryAction
    ): Boolean {
        val itemStack = inventory.getItem(position)
        if (itemStack != null) {
            val id = itemStack.getItemId()
            when (id) {
                CraftventureKeys.ID_ITEM_MENU_PREVIOUS -> {
                    page--
                    return true
                }

                CraftventureKeys.ID_ITEM_MENU_NEXT -> {
                    page++
                    return true
                }
            }
        }
        return super.handleNavigationButtonsClick(inventory, position, player, action)
    }

    override fun addNavigationButtons(inventory: Inventory) {
        super.addNavigationButtons(inventory)
        inventory.setItem(previousButtonIndex.index, InventoryConstants.getPreviousPageButton(page, pageCount))
        inventory.setItem(nextButtonIndex.index, InventoryConstants.getNextPageButton(page, pageCount))
    }

    fun generateCenteredPagedTitle(title: String, loading: Boolean = false) = if (loading) {
        centeredTitle("$title (loading)")
    } else if (maxPages > 1) {
        centeredTitle("$title ${page + 1}/${maxPages}")
    } else centeredTitle(title)
}