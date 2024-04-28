package net.craftventure.core.manager

import kotlinx.coroutines.*
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.CvDispatchers
import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.extension.isMarkedAsWornItem
import net.craftventure.core.extension.markAsWornItem
import net.craftventure.core.extension.setItemId
import net.craftventure.core.feature.balloon.BalloonManager
import net.craftventure.core.inventory.InventoryConstants
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.metadata.ClientSettingsMetadata
import net.craftventure.core.metadata.EquippedItemsMeta
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.core.metadata.InventoryTrackingMeta
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.getItemStack
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.generated.cvdata.tables.pojos.*
import net.craftventure.database.type.EquippedItemSlot
import net.craftventure.database.type.ItemType
import net.craftventure.temporary.getOwnableItemMetadata
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType

object EquipmentManager {
    const val SLOT_CONSUMPTION = 3
    const val SLOT_WEAPON = 4
    const val SLOT_EVENT = 5

    private val scope = CoroutineScope(SupervisorJob() + CvDispatchers.mainThreadDispatcher)
    private val playerEquippedItemRepository = MainRepositoryProvider.playerEquippedItemRepository
    private val ownableItemRepository = MainRepositoryProvider.ownableItemRepository
    private val itemStackDataRepository = MainRepositoryProvider.itemStackDataRepository
    private val playerOwnedItemRepository = MainRepositoryProvider.playerOwnedItemRepository
    private val titleRepository = MainRepositoryProvider.titleRepository

    fun destroy() {
        scope.cancel()
    }

    fun invalidatePlayerEquippedItems(player: Player) {
        scope.launch(Dispatchers.IO) {
            val items = playerEquippedItemRepository.getAllByPlayer(player.uniqueId)
            val resolved = items.associate { it.slot!! to EquippedItemData.of(it, player) }
            player.equippedItemsMeta()?.equippedItems = resolved

//            logcat(LogPriority.WARN) { "Applying equipped items" }
            reapply(player)
        }
    }

    fun reapply(player: Player) {
        val meta = player.equippedItemsMeta() ?: return

        val job = scope.launch(Dispatchers.IO) {
//            logcat { "Reapplying for ${player.name}" }
//
////        logcat { "Updating items on thread ${Thread.currentThread().name}" }
//            meta.equippedItems.forEach { (slot, data) ->
//                logcat { "  - ${slot.name}: ${data.id}" }
//            }

            val appliedEquippedItems = AppliedEquippedItems()
            val hasInventoryOpen = player.getMetadata<InventoryTrackingMeta>()?.isMenuOpen ?: false
//            logcat { "hasInventoryOpen=$hasInventoryOpen" }

//            logcat { "Inventory open $hasInventoryOpen" }

//            for (i in 0 until appliedEquippedItems.hotBar.size) {
//                appliedEquippedItems.hotBar[i] = null
//            }
            if (!hasInventoryOpen) {
                appliedEquippedItems.hotBar[0] =
                    ItemStack(Material.NETHER_STAR)
                        .markAsWornItem()
                        .displayName(InventoryConstants.NAME_ITEM_CRAFTVENTURE_MENU)
                        .updateMeta<ItemMeta> {
                            this.setPlaceableKeys(this.placeableKeys + Material.AIR.key)
                        }
                        .hidePlacedOn()
                        .setItemId(CraftventureKeys.ID_ITEM_CRAFTVENTURE_MENU)
                        .toEquippedItemData(CraftventureKeys.ID_ITEM_CRAFTVENTURE_MENU)

                appliedEquippedItems.hotBar[1] =
                    ItemStack(Material.MINECART)
                        .markAsWornItem()
                        .displayName(InventoryConstants.NAME_ITEM_ATTRACTIONS_MENU)
                        .updateMeta<ItemMeta> {
                            this.setPlaceableKeys(this.placeableKeys + Material.AIR.key)
                        }
                        .hidePlacedOn()
                        .setItemId(CraftventureKeys.ID_ITEM_ATTRACTIONS_MENU)
                        .toEquippedItemData(CraftventureKeys.ID_ITEM_ATTRACTIONS_MENU)

                appliedEquippedItems.hotBar[2] =
                    player.playerProfile
                        .toSkullItem()
                        .markAsWornItem()
                        .displayName(InventoryConstants.NAME_ITEM_PROFILE_MENU)
                        .updateMeta<ItemMeta> {
                            this.setPlaceableKeys(this.placeableKeys + Material.AIR.key)
                        }
                        .hidePlacedOn()
                        .setItemId(CraftventureKeys.ID_ITEM_PROFILE_MENU)
                        .toEquippedItemData(CraftventureKeys.ID_ITEM_PROFILE_MENU)

                if (player.isCrew())
                    appliedEquippedItems.hotBar[8] =
                        ItemStack(Material.COMPASS)
                            .markAsWornItem()
                            .displayName(CVTextColor.MENU_DEFAULT_TITLE + "Ze Great Navigatoer")
                            .toEquippedItemData("cv:compass")

            }

            val costumeItems = meta.equippedItems[EquippedItemSlot.COSTUME]?.costumeItems
            val clothingSource = costumeItems ?: meta.equippedItems

            if (!player.isInvisible && player.getPotionEffect(PotionEffectType.INVISIBILITY) == null) {
                appliedEquippedItems.hairstyleItem = meta.equippedItems[EquippedItemSlot.HAIRSTYLE]?.takeIfOwned()
                appliedEquippedItems.helmetItem = clothingSource[EquippedItemSlot.HELMET]?.takeIfOwned()
                appliedEquippedItems.chestplateItem = clothingSource[EquippedItemSlot.CHESTPLATE]?.takeIfOwned()
                appliedEquippedItems.leggingsItem = clothingSource[EquippedItemSlot.LEGGINGS]?.takeIfOwned()
                appliedEquippedItems.bootsItem = clothingSource[EquippedItemSlot.BOOTS]?.takeIfOwned()
            }

            appliedEquippedItems.title = meta.equippedItems[EquippedItemSlot.TITLE]?.takeIfOwned()?.title

            appliedEquippedItems.costumeItem = meta.equippedItems[EquippedItemSlot.COSTUME]?.takeIfOwned()

            if (!hasInventoryOpen) {
                appliedEquippedItems.weaponItem = meta.equippedItems[EquippedItemSlot.HANDHELD]?.takeIfOwned()
                appliedEquippedItems.consumptionItem = meta.equippedItems[EquippedItemSlot.CONSUMPTION]?.takeIfOwned()
            }
            appliedEquippedItems.balloonItem = meta.equippedItems[EquippedItemSlot.BALLOON]?.takeIfOwned()

            appliedEquippedItems.shoulderPetLeft = meta.equippedItems[EquippedItemSlot.SHOULDER_PET_LEFT]?.takeIfOwned()
            appliedEquippedItems.shoulderPetRight =
                meta.equippedItems[EquippedItemSlot.SHOULDER_PET_RIGHT]?.takeIfOwned()

            if (!isActive) return@launch

            launch(CvDispatchers.mainThreadDispatcher) {
                val event = PlayerEquippedItemsUpdateEvent(player, meta, appliedEquippedItems)
                Bukkit.getPluginManager().callEvent(event)

                appliedEquippedItems.applyTo(player, scope = this)
                BalloonManager.invalidate(player)
            }
        }
        meta.replaceUpdateJob(job)
    }

    @Deprecated("Objectively crap, doesn't check the actual item being held")
    fun getSelectedItemSlotType(player: Player, slot: Int = player.inventory.heldItemSlot): ItemType? {
        return when (slot) {
            SLOT_WEAPON -> ItemType.WEAPON
            else -> null
        }
    }

    class AppliedEquippedItems {
        val hotBar: Array<EquippedItemData?> = arrayOfNulls(9)

        var costumeItem: EquippedItemData? = null
        var offhandItem: EquippedItemData? = null
        var shoulderPetLeft: EquippedItemData? = null
        var shoulderPetRight: EquippedItemData? = null

        var hairstyleItem: EquippedItemData? = null
        var helmetItem: EquippedItemData? = null
        var chestplateItem: EquippedItemData? = null
        var leggingsItem: EquippedItemData? = null
        var bootsItem: EquippedItemData? = null

        var title: Title? = null

        var balloonItem: EquippedItemData? = null

        var consumptionItem: EquippedItemData?
            get() = hotBar[SLOT_CONSUMPTION]
            set(value) {
                hotBar[SLOT_CONSUMPTION] = value
            }

        var weaponItem: EquippedItemData?
            get() = hotBar[SLOT_WEAPON]
            set(value) {
                hotBar[SLOT_WEAPON] = value
            }

        var eventItem: EquippedItemData?
            get() = hotBar[SLOT_EVENT]
            set(value) {
                hotBar[SLOT_EVENT] = value
            }

        fun applyTo(
            player: Player,
            equippedItemsMeta: EquippedItemsMeta? = player.equippedItemsMeta(),
            scope: CoroutineScope
        ) {
            if (!PluginProvider.plugin.isOnMainThread()) {
                scope.launch(CvDispatchers.mainThreadDispatcher) {
                    applyTo(
                        player,
                        equippedItemsMeta = equippedItemsMeta,
                        scope = scope
                    )
                }
                return
            }
            if (!scope.isActive) return

            equippedItemsMeta?.appliedEquippedItems = this

            val playerEquipment = player.equipment
            playerEquipment.setHelmet(helmetItem?.itemStack ?: hairstyleItem?.itemStack, true)
            playerEquipment.setChestplate(chestplateItem?.itemStack, true)
            playerEquipment.setLeggings(leggingsItem?.itemStack, true)
            playerEquipment.setBoots(bootsItem?.itemStack, true)

            val playerInventory = player.inventory
            hotBar.forEachIndexed { slot, equippedItemData ->
                if (player.gameMode != GameMode.CREATIVE ||
                    playerInventory.getItem(slot).let { it == null || it.isMarkedAsWornItem(true) }
                ) {
                    playerInventory.setItem(slot, equippedItemData?.itemStack)
                }
            }

            equippedItemsMeta?.applySpawnPacketsTo(player)
            equippedItemsMeta?.applySpawnPacketsTo(*player.trackedPlayers.toTypedArray())

            val clientSettingsMetadata = player.getMetadata<ClientSettingsMetadata>()
            clientSettingsMetadata?.resend()

            CraftventureCore.getWornTitleManager().setTitle(player, title)
        }

        fun clearAll() {
            clearArmor()
            clearItems()
            clearSpecials()
        }

        fun clearSpecials() {
            title = null
            hairstyleItem = null
            offhandItem = null
            weaponItem = null
            consumptionItem = null
            balloonItem = null
            shoulderPetLeft = null
            shoulderPetRight = null
        }

        fun clearItems() {
            hotBar.forEachIndexed { index, _ ->
                hotBar[index] = null
            }
        }

        fun clearArmor() {
            helmetItem = null
            chestplateItem = null
            leggingsItem = null
            bootsItem = null
        }
    }

    data class EquippedItemData(
        val id: String,
        val ownCount: Int,
        val itemStack: ItemStack? = null,
        val equippedItem: PlayerEquippedItem? = null,
        val ownableItem: OwnableItem? = null,
        val itemStackData: ItemStackData? = null,
        val playerOwnedItem: PlayerOwnedItem? = null,
        val title: Title? = null,
        val costumeItems: Map<EquippedItemSlot, EquippedItemData?>? = null,
        val balloonItemStack: ItemStack? = null,
    ) {
        val ownableItemMeta: OwnableItemMetadata? = ownableItem?.getOwnableItemMetadata()
        val playerOwnedItemMeta: OwnableItemMetadata? = playerOwnedItem?.getOwnableItemMetadata()

        init {
            id.let { itemStack?.setItemId(it) }
        }

        fun takeIfOwned() = if (ownCount > 0) this else null

        companion object {
            fun ItemStack.toEquippedItemData(id: String = "generated_item") = of(id, this)

            fun of(id: String, itemStack: ItemStack?) = EquippedItemData(
                id = id,
                ownCount = 1,
                itemStack = itemStack,
            )

            fun of(
                ownableItem: OwnableItem,
                player: Player,
                slot: EquippedItemSlot,
                ownCountOverride: Int? = null,
            ): EquippedItemData {
                val itemId = ownableItem.id!!
                val ownCount = ownCountOverride ?: playerOwnedItemRepository.ownsCount(player.uniqueId, itemId)

                val ownableItem = ownableItemRepository.findCached(itemId, loadIfCacheInvalid = true)

                val itemStackData = ownableItem?.guiItemStackDataId
                    ?.let { itemStackDataRepository.findCached(it, loadIfCacheInvalid = true) }

                val itemStack = itemStackData?.getItemStack(ownableItem)
                itemStack?.setItemId(itemId)
                itemStack?.amount = ownCount.takeIf { it < 100 } ?: 99


                val title = if (slot == EquippedItemSlot.TITLE) {
                    titleRepository.findCached(itemId.replace("title_", ""))
                } else null

                val costumeItems = if (slot == EquippedItemSlot.COSTUME) {
                    mapOf<EquippedItemSlot, EquippedItemData?>(
                        EquippedItemSlot.HELMET to EquippedItemData(
                            id = itemId,
                            itemStack = itemStackDataRepository.cachedItems.firstOrNull { it.id == itemId + "_helmet" }?.itemStack,
                            ownCount = 1,
                        ),
                        EquippedItemSlot.CHESTPLATE to EquippedItemData(
                            id = itemId,
                            itemStack = itemStackDataRepository.cachedItems.firstOrNull { it.id == itemId + "_chestplate" }?.itemStack,
                            ownCount = 1,
                        ),
                        EquippedItemSlot.LEGGINGS to EquippedItemData(
                            id = itemId,
                            itemStack = itemStackDataRepository.cachedItems.firstOrNull { it.id == itemId + "_leggings" }?.itemStack,
                            ownCount = 1,
                        ),
                        EquippedItemSlot.BOOTS to EquippedItemData(
                            id = itemId,
                            itemStack = itemStackDataRepository.cachedItems.firstOrNull { it.id == itemId + "_boots" }?.itemStack,
                            ownCount = 1,
                        ),
                    )
                } else null

                return EquippedItemData(
                    id = itemId,
                    ownCount = ownCount,
                    itemStack = itemStack,
                    equippedItem = null,
                    ownableItem = ownableItem,
                    itemStackData = itemStackData,
                    playerOwnedItem = null,
                    title = title,
                    costumeItems = costumeItems,
                )
            }

            fun of(item: PlayerEquippedItem, player: Player): EquippedItemData {
                val itemId = item.item!!
                val ownCount = playerOwnedItemRepository.ownsCount(player.uniqueId, itemId)

                val ownableItem = ownableItemRepository.findCached(itemId, loadIfCacheInvalid = true)

                val itemStackData = ownableItem?.guiItemStackDataId
                    ?.let { itemStackDataRepository.findCached(it, loadIfCacheInvalid = true) }

                val itemStack = itemStackData?.getItemStack(ownableItem)
                    ?.markAsWornItem()
                    ?.setItemId(itemId)
                itemStack?.amount = ownCount.takeIf { it < 100 } ?: 99

                val playerOwnedItem =
                    item.source?.let { playerOwnedItemRepository.find(it) }

                val color = playerOwnedItem?.getOwnableItemMetadata()?.parsedMainColor
                    ?: ownableItem?.getOwnableItemMetadata()?.parsedMainColor
                if (color != null) {
                    itemStack?.setColor(color)
                }

                val title = if (item.slot == EquippedItemSlot.TITLE) {
                    titleRepository.findCached(itemId.replace("title_", ""))
                } else null

                val costumeItems = if (item.slot == EquippedItemSlot.COSTUME) {
                    mapOf<EquippedItemSlot, EquippedItemData?>(
                        EquippedItemSlot.HELMET to EquippedItemData(
                            id = itemId,
                            itemStack = itemStackDataRepository.cachedItems.firstOrNull { it.id == itemId + "_helmet" }?.itemStack,
                            ownCount = 1,
                        ),
                        EquippedItemSlot.CHESTPLATE to EquippedItemData(
                            id = itemId,
                            itemStack = itemStackDataRepository.cachedItems.firstOrNull { it.id == itemId + "_chestplate" }?.itemStack,
                            ownCount = 1,
                        ),
                        EquippedItemSlot.LEGGINGS to EquippedItemData(
                            id = itemId,
                            itemStack = itemStackDataRepository.cachedItems.firstOrNull { it.id == itemId + "_leggings" }?.itemStack,
                            ownCount = 1,
                        ),
                        EquippedItemSlot.BOOTS to EquippedItemData(
                            id = itemId,
                            itemStack = itemStackDataRepository.cachedItems.firstOrNull { it.id == itemId + "_boots" }?.itemStack,
                            ownCount = 1,
                        ),
                    )
                } else null

                return EquippedItemData(
                    id = itemId,
                    ownCount = ownCount,
                    itemStack = itemStack,
                    equippedItem = item,
                    ownableItem = ownableItem,
                    itemStackData = itemStackData,
                    playerOwnedItem = playerOwnedItem,
                    title = title,
                    costumeItems = costumeItems,
                )
            }
        }
    }
}