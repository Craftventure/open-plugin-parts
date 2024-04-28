package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.bukkit.ktx.util.SlotBackgroundManager
import net.craftventure.chat.bungee.extension.asPlainText
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.feature.dressingroom.DressingRoomPlayerState
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.OwnedItemCache
import net.craftventure.core.utils.OwnableItemUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.craftventure.database.type.EquippedItemSlot
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class DressingRoomPreviewPickerMenu(
    player: Player,
    itemsIds: Set<String>,
    private val allowedTypes: Set<EquippedItemSlot>,
    private val state: DressingRoomPlayerState,
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
    private val INDEX_ROTATE_CLOCKWISE = calculateZeroBasedIndex(0, 3)
    private val INDEX_ROTATE_COUNTER_CLOCKWISE = calculateZeroBasedIndex(0, 5)

    private var items by invalidatingUnequality(emptyList<InventoryItem>())
    private var loading: Boolean by invalidatingUnequality(false)

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()

        executeAsync {
            val items = mutableListOf<InventoryItem>()
            itemsIds.forEach { itemId ->
                val ownableItem =
                    MainRepositoryProvider.ownableItemRepository.findCached(itemId, loadIfCacheInvalid = true)
                if (ownableItem != null) {
                    val itemStackData = ownableItem.guiItemStackDataId?.let {
                        MainRepositoryProvider.itemStackDataRepository.findCached(
                            it,
                            loadIfCacheInvalid = true
                        )
                    }
                    if (allowedTypes.none { ownableItem.type in it.supportedItemTypes }) return@forEach

                    if (itemStackData != null) {
                        val itemStack = OwnableItemUtils.toItem(
                            ownableItem,
                            itemStackData,
                            player,
                            forceVisible = true,
                            ownedItems = player.getMetadata<OwnedItemCache>()?.ownedItems ?: emptyList(),
                            addActions = false,
                        )

                        items += InventoryItem(
                            ownableItem,
                            itemStack ?: ItemStack(Material.AIR),
                            MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId, itemId)
                        )
                    }
                }
            }

            this.items = items.sortedBy { it.item.displayName().asPlainText() }
            rebuildIndications()

            loading = false
        }
    }

    private fun rebuildIndications() {
        slotBackgroundManager.clearTag("selected")

        val itemRange = page * itemsPerPage until (page + 1) * itemsPerPage
        val initialSlot = (itemStartRowOffset * 9) + itemStartColumnOffset
        val equipmentItems = state.getAllEquipment().map { it.value.id }

        items.drop(page * itemsPerPage).take((page + 1) * itemsPerPage).forEachIndexed { index, inventoryItem ->
            if (inventoryItem.ownableItem.id in equipmentItems) {
//                logcat { "Setting slot ${initialSlot + index}" }
                slotBackgroundManager.setSlot(
                    SlotBackgroundManager.slotIndex(initialSlot + index),
                    FontCodes.Slot.underlay20,
                    NamedTextColor.DARK_GREEN,
                    tag = "selected"
                )
            }
        }

        triggerSlotChanges()
    }

    private fun updateTitle() {
        rebuildIndications()
        titleComponent = generateCenteredPagedTitle("Preview", loading)
    }

    override fun onPageChanged() {
        super.onPageChanged()
        rebuildIndications()
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

        val item = items.getOrNull(index) ?: return

        val types = allowedTypes.filter { item.ownableItem.type in it.supportedItemTypes }

        if (types.isEmpty()) {

        } else if (types.size == 1) {
            val slot = types[0]
            state.setEquipment(
                slot,
                EquipmentManager.EquippedItemData.of(item.ownableItem, player, slot, ownCountOverride = 1)
            )
            rebuildIndications()
        } else {
            val menu = DressingRoomPreviewPickerSlotSelectorMenu(
                player,
                item,
                types,
                state
            )
            player.openMenu(menu)
            rebuildIndications()
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

        when (position) {
            INDEX_ROTATE_CLOCKWISE -> state.rotateSelfYawByDegrees(45.0)
            INDEX_ROTATE_COUNTER_CLOCKWISE -> state.rotateSelfYawByDegrees(-45.0)
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)

        inventory[INDEX_ROTATE_CLOCKWISE] = MaterialConfig.dataItem(Material.STICK, 11).apply {
            displayName(CVTextColor.MENU_DEFAULT_TITLE + "Rotate player 45° clockwise")

        }
        inventory[INDEX_ROTATE_COUNTER_CLOCKWISE] = MaterialConfig.dataItem(Material.STICK, 13).apply {
            displayName(CVTextColor.MENU_DEFAULT_TITLE + "Rotate player 45° counter clockwise")
        }
    }

    data class InventoryItem(
        val ownableItem: OwnableItem,
        val item: ItemStack,
        val ownedItems: List<PlayerOwnedItem>
    )
}
