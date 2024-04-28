package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.chat.bungee.extension.asPlainText
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.UiElement
import net.craftventure.core.async.executeAsync
import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.feature.dressingroom.DressingRoomPlayerState
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.ktx.json.toJson
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.core.metadata.OwnedItemCache
import net.craftventure.core.utils.OwnableItemUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.update
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.EquippedItemSlot
import net.craftventure.database.type.TransactionType
import net.craftventure.temporary.getOwnableItemMetadata
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

class BarberMainMenu(
    private val player: Player,
    itemsIds: Set<String>,
    private val state: DressingRoomPlayerState,
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 2,
    maxItemRows = 3,
    maxItemColumns = 9,
    owner = player,
) {
    private val INDEX_ROTATE_CLOCKWISE = calculateZeroBasedIndex(0, 3)
    private val INDEX_ROTATE_COUNTER_CLOCKWISE = calculateZeroBasedIndex(0, 5)
    private val INDEX_ITEM_REMOVE = calculateZeroBasedIndex(1, 3)
    private val INDEX_ITEM_PREVIEW_AND_APPLY = calculateZeroBasedIndex(1, 4)
    private val INDEX_CHANGE_COLOR = calculateZeroBasedIndex(1, 5)

    private var items by invalidatingUnequality(emptyList<InventoryItem>())
    private var loading: Boolean by invalidatingUnequality(false)

    private var pickedItem: ConfiguringHairstyle? = state.getEquipment(EquippedItemSlot.HAIRSTYLE)
        ?.let {
            val item = it.ownableItem ?: return@let null
            val stack = it.itemStack ?: return@let null
            val meta = it.playerOwnedItemMeta
            ConfiguringHairstyle(
                InventoryItem(item, stack, null),
                meta?.parsedMainColor ?: stack.getColor() ?: Color.WHITE
            )
        }
        set(value) {
            if (field !== value) {
                state.setEquipment(
                    EquippedItemSlot.HAIRSTYLE,
                    if (value == null) null
                    else EquipmentManager.EquippedItemData.of(
                        "hairstyle",
                        value.item.item.clone().setColor(value.color)
                    ).copy(ownableItem = value.item.ownableItem)
                )
            }
            field = value
            scheduleLayout()
        }

    init {
        titleComponent = generateCenteredPagedTitle("Hairstyles")

        underlay = CvComponent.resettingInventoryOverlay(
            UiElement("\uE0E5", 256)
        )

        executeAsync {
            loading = true
            val ownedItems = MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId)
            val items = mutableListOf<InventoryItem>()
            itemsIds.forEach { itemId ->
                val ownableItem =
                    MainRepositoryProvider.ownableItemRepository.findCached(itemId, loadIfCacheInvalid = true)
                if (ownableItem != null && (ownableItem.price ?: -1) >= 0) {
                    val itemStackData = ownableItem.guiItemStackDataId?.let {
                        MainRepositoryProvider.itemStackDataRepository.findCached(
                            it,
                            loadIfCacheInvalid = true
                        )
                    }

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
                            color = ownedItems.find { it.ownedItemId == itemId }
                                ?.getOwnableItemMetadata()?.parsedMainColor
                        )
                    }
                }
            }

            this.items = items.sortedBy { it.item.displayName().asPlainText() }

//            (state.getEquipment(EquippedItemSlot.HAIRSTYLE)
//                ?: state.getActualEquipment(EquippedItemSlot.HAIRSTYLE))?.let { equippedItemData ->
//                equippedItemData.ownableItem?.id?.let { ownableItemId ->
//                    items.find { it.ownableItem.id == ownableItemId }?.let {
//                        pickedItem = ConfiguringHairstyle(
//                            it,
//                            equippedItemData.itemStack?.getColor() ?: DyeColor.values().random().color
//                        )
//                    }
//                }
//            }

            loading = false
        }
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

        items.getOrNull(index)?.let { item ->
            logcat { "Setting picked item" }
            pickedItem = ConfiguringHairstyle(item, DyeColor.values().random().color)
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

//        logcat { "Position $position" }
        when (position) {
            INDEX_ROTATE_CLOCKWISE -> state.rotateSelfYawByDegrees(45.0)
            INDEX_ROTATE_COUNTER_CLOCKWISE -> state.rotateSelfYawByDegrees(-45.0)
            INDEX_ITEM_REMOVE -> {
                pickedItem = null
            }

            INDEX_ITEM_PREVIEW_AND_APPLY -> {
                player.closeInventoryStack()

                executeAsync {
                    val pickedItem = pickedItem
                    if (pickedItem == null) {
                        val updatedMeta = MainRepositoryProvider.playerEquippedItemRepository.update(
                            player,
                            EquippedItemSlot.HAIRSTYLE,
                            null,
                            null,
                        )
                        if (!updatedMeta) {
                            logcat { "Failed to update meta" }
                            return@executeAsync
                        }
                        if (MainRepositoryProvider.bankAccountRepository.delta(
                                player.uniqueId,
                                BankAccountType.VC,
                                -5L,
                                TransactionType.BARBER,
                            )
                        ) {
                            player.location.world!!.playSound(player.location, SoundUtils.MONEY, 1f, 1f)
                        }
                        state.requestDestroy()
                        return@executeAsync
                    }
                    val ownableItem = pickedItem.item.ownableItem
                    val itemId = ownableItem.id ?: return@executeAsync
                    MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(
                        player.uniqueId,
                        itemId,
                        -2
                    )
                    val ownedItem = MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId, itemId)
                    if (ownedItem.isEmpty()) {
                        logcat { "No owned items" }
                        return@executeAsync
                    }

                    val usingItem = ownedItem.first()
                    val meta = (usingItem.getOwnableItemMetadata() ?: OwnableItemMetadata()).copy(
                        mainColor = Optional.of(pickedItem.color.toHexColor())
                    )
                    val updatedItem = MainRepositoryProvider.playerOwnedItemRepository.updateOwnableItemMetadata(
                        usingItem.id!!,
                        meta.toJson()
                    )
//                    logcat { "MetaId ${usingItem.id}" }
//                    logcat { "MetaRaw ${meta}" }
//                    logcat { "MetaJson ${meta.toJson()}" }

                    if (updatedItem != 1) {
                        logcat { "Updateditem = $updatedItem" }
                        return@executeAsync
                    }

                    val updatedMeta = MainRepositoryProvider.playerEquippedItemRepository.update(
                        player,
                        EquippedItemSlot.HAIRSTYLE,
                        pickedItem.item.ownableItem,
                        usingItem.id,
                    )
                    if (!updatedMeta) {
                        logcat { "Failed to update meta" }
                        return@executeAsync
                    }

                    MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "get_a_haircut")

                    val price = ownableItem.price
                    if (price != null && price > 0 && MainRepositoryProvider.bankAccountRepository.delta(
                            player.uniqueId,
                            ownableItem.bankAccountType!!,
                            -price.toLong(),
                            TransactionType.BARBER,
                        )
                    ) {
                        player.location.world!!.playSound(player.location, SoundUtils.MONEY, 1f, 1f)
                    }

                    state.requestDestroy()
                }
            }

            INDEX_CHANGE_COLOR -> {
                if (pickedItem == null) return

                val menu = ColorPickerMenu(
                    player,
                    listener = object : ColorPickerMenu.ResultListener {
                        override fun onPicked(color: Color) {
                            pickedItem = pickedItem?.copy(color = color)
                        }
                    },
                    itemstackRepresenter = { color -> pickedItem?.item?.item?.clone()?.setColor(color) },
                    startColor = inventory.getItem(INDEX_ITEM_PREVIEW_AND_APPLY)?.getColor() ?: DyeColor.WHITE.color
                )
                menu.openAsMenu(player)
            }
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)

        val ownableItem = pickedItem?.item?.ownableItem
        val pickedItem = pickedItem

        inventory[INDEX_ROTATE_CLOCKWISE] = dataItem(Material.STICK, 11).apply {
            displayName(CVTextColor.MENU_DEFAULT_TITLE + "Rotate player 45° clockwise")

        }
        inventory[INDEX_ROTATE_COUNTER_CLOCKWISE] = dataItem(Material.STICK, 13).apply {
            displayName(CVTextColor.MENU_DEFAULT_TITLE + "Rotate player 45° counter clockwise")
        }

        if (pickedItem != null) {
            inventory[INDEX_ITEM_REMOVE] = dataItem(Material.STICK, 17).applyAllHideItemFlags().apply {
                displayName(CVTextColor.MENU_DEFAULT_TITLE + "Remove hairstyle")
                loreWithBuilder {
                    action("Click to remove hairstyle preview")
                }
            }
        }

        val previewItem = pickedItem?.item?.item?.clone() ?: player.playerProfile.toSkullItem().applyAllHideItemFlags()
        inventory[INDEX_ITEM_PREVIEW_AND_APPLY] = previewItem.apply {
            if (pickedItem != null)
                setColor(pickedItem.color)
            displayName(CVTextColor.MENU_DEFAULT_TITLE + "Apply this style")
            if (ownableItem != null)
                loreWithBuilder {
                    accented("Price ${ownableItem.price!!}")
                    text(ownableItem.bankAccountType!!.emoji, color = NamedTextColor.WHITE)
                    emptyLines()
                    action("Click to apply this hairstyle")
                }
            else
                loreWithBuilder {
                    accented("Price 15")
                    text(BankAccountType.VC.emoji, color = NamedTextColor.WHITE)
                    emptyLines()
                    action("Click to apply removing your hairstyle")
                }
        }

        if (pickedItem != null)
            inventory[INDEX_CHANGE_COLOR] = dataItem(Material.STICK, 16).apply {
                displayNameWithBuilder { text("Change color") }
            }
    }

    data class ConfiguringHairstyle(
        val item: InventoryItem,
        val color: Color,
    )

    data class InventoryItem(
        val ownableItem: OwnableItem,
        val item: ItemStack,
        val color: Color?,
    )
}