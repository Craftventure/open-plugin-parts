package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayNamePlain
import net.craftventure.bukkit.ktx.extension.hideAttributes
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.getItemStack
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.bukkit.extensions.update
import net.craftventure.database.extension.firstOfSlot
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerEquippedItem
import net.craftventure.database.type.EquippedItemSlot
import net.craftventure.database.type.ItemType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class EquipmentMenu(
    player: Player
) : InventoryMenu(
    owner = player,
), Listener {
    private var equippedItems: List<PlayerEquippedItem> = emptyList()
    private var loading: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    init {
        rowsCount = 5
        updateItems()
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
        )
//        Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance())
    }

    //    override fun onDestroy() {
//        super.onDestroy()
//        HandlerList.unregisterAll(this)
//    }
    override fun onResumed() {
        super.onResumed()
        updateItems()
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return

        val itemStack = inventory.getItem(position)
        if (itemStack != null) {
            val type = when (position) {
                titleSlot -> EquippedItemSlot.TITLE
                helmetSlot -> EquippedItemSlot.HELMET
                chestplateSlot -> EquippedItemSlot.CHESTPLATE
                leggingsSlot -> EquippedItemSlot.LEGGINGS
                bootsSlot -> EquippedItemSlot.BOOTS
                costumeSlot -> EquippedItemSlot.COSTUME
                balloonSlot -> EquippedItemSlot.BALLOON
                weaponSlot -> EquippedItemSlot.HANDHELD
                consumptionSlot -> EquippedItemSlot.CONSUMPTION
                laserGameASlot -> EquippedItemSlot.LASER_GAME_A
                laserGameBSlot -> EquippedItemSlot.LASER_GAME_B
                shoulderPetLeftSlot -> EquippedItemSlot.SHOULDER_PET_LEFT
                shoulderPetRightSlot -> EquippedItemSlot.SHOULDER_PET_RIGHT
                else -> null
            } ?: return

            val delete = equippedItems.firstOfSlot(type) != null && action != InventoryAction.PICKUP_HALF
            if (delete) {
                setNull(type)
            } else {
                executeSync {
                    val menu = OwnedItemsMenu(
                        player = player,
                        itemFilter = OwnedItemsMenu.SimpleItemFilter(type.supportedItemTypes, type.displayName),
                        slot = type
                    )
                    player.openMenu(menu)
                }
                return
            }

            when (itemStack.displayName()) {
                NAME_ITEM_REMOVE_HELMET -> setNull(EquippedItemSlot.HELMET)
                NAME_ITEM_REMOVE_CHESTPLATE -> setNull(EquippedItemSlot.CHESTPLATE)
                NAME_ITEM_REMOVE_LEGGINGS -> setNull(EquippedItemSlot.LEGGINGS)
                NAME_ITEM_REMOVE_BOOTS -> setNull(EquippedItemSlot.BOOTS)
                NAME_ITEM_REMOVE_COSTUME -> setNull(EquippedItemSlot.COSTUME)
                NAME_ITEM_REMOVE_BALLOON -> setNull(EquippedItemSlot.BALLOON)
                NAME_ITEM_REMOVE_TITLE -> setNull(EquippedItemSlot.TITLE)
                NAME_ITEM_REMOVE_WEAPON -> setNull(EquippedItemSlot.HANDHELD)
                NAME_ITEM_REMOVE_CONSUMPTION -> setNull(EquippedItemSlot.CONSUMPTION)
                NAME_ITEM_REMOVE_LASERGAME_ITEM_A -> setNull(EquippedItemSlot.LASER_GAME_A)
                NAME_ITEM_REMOVE_LASERGAME_ITEM_B -> setNull(EquippedItemSlot.LASER_GAME_B)
                NAME_ITEM_REMOVE_SHOULDER_PET_LEFT -> setNull(EquippedItemSlot.SHOULDER_PET_LEFT)
                NAME_ITEM_REMOVE_SHOULDER_PET_RIGHT -> setNull(EquippedItemSlot.SHOULDER_PET_RIGHT)
            }
        }
    }

    private var titleItemStack: ItemStack? = null
    private var helmetItemStack: ItemStack? = null
    private var chestplateItemStack: ItemStack? = null
    private var leggingsItemStack: ItemStack? = null
    private var bootsItemStack: ItemStack? = null
    private var costumeItemStack: ItemStack? = null
    private var balloonItemStack: ItemStack? = null
    private var weaponItemStack: ItemStack? = null
    private var consumptionItemStack: ItemStack? = null
    private var laserGameAStack: ItemStack? = null
    private var laserGameBStack: ItemStack? = null
    private var shoulderPetLeftStack: ItemStack? = null
    private var shoulderPetRightStack: ItemStack? = null

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)

        inventory.setItem(titleSlot, titleItemStack)
        inventory.setItem(helmetSlot, helmetItemStack)
        inventory.setItem(chestplateSlot, chestplateItemStack)
        inventory.setItem(leggingsSlot, leggingsItemStack)
        inventory.setItem(bootsSlot, bootsItemStack)
        inventory.setItem(costumeSlot, costumeItemStack)
        inventory.setItem(balloonSlot, balloonItemStack)
        inventory.setItem(weaponSlot, weaponItemStack)
        inventory.setItem(consumptionSlot, consumptionItemStack)
        inventory.setItem(laserGameASlot, laserGameAStack)
        inventory.setItem(laserGameBSlot, laserGameBStack)

        inventory.setItem(shoulderPetLeftSlot, shoulderPetLeftStack)
        inventory.setItem(shoulderPetRightSlot, shoulderPetRightStack)
    }

    private fun getItemStackForWornItem(slot: EquippedItemSlot): ItemStack? {
        val equippedItem = equippedItems.firstOfSlot(slot)
        if (equippedItem != null) {
            val ownableItem = MainRepositoryProvider.ownableItemRepository.findCached(equippedItem.item!!)
            if (ownableItem != null) {
                val itemStackData =
                    MainRepositoryProvider.itemStackDataRepository.findCached(ownableItem.guiItemStackDataId!!)
                if (itemStackData != null) {
                    if (ownableItem.type == ItemType.TITLE) {
                        val title =
                            MainRepositoryProvider.titleRepository.findCached(ownableItem.id!!.replace("title_", ""))
                        if (title != null) {
                            return itemStackData.getItemStack(ownableItem.type!!)?.hideAttributes()
                                ?.displayNameWithBuilder {
                                    text("Title ${title.displayName}")
                                }
                                ?.loreWithBuilder {
                                    accented("Currently displaying above your head:")
                                    moveToBlankLine()
                                    component(title.title?.parseWithCvMessage() ?: Component.empty())
                                    action("Leftclick to unequip")
                                    action("Rightclick to change")
                                }
                        }
                    }
                    return itemStackData.getItemStack(slot)?.hideAttributes()
                        ?.displayNameWithBuilder {
                            text(itemStackData.getItemStack(ownableItem.type!!)?.displayNamePlain() ?: "Unknown")
                        }
                        ?.loreWithBuilder {
                            action("Leftclick to unequip")
                            action("Rightclick to change")
                        }
                }
            }
        }
        return slot.itemStack()
            .displayNameWithBuilder {
                text("Equip to ${slot.displayName}")
            }
            .hideAttributes()
    }


    private fun setNull(slot: EquippedItemSlot) {
        val player = requireOwner()
//        player.sendMessage(Translation.MENU_ITEMS_EQUIPPING.getTranslation(player))
        executeAsync {
            if (MainRepositoryProvider.playerEquippedItemRepository.update(player, slot, null, null)) {
//                    player.sendMessage(Translation.MENU_ITEMS_EQUIPPED.getTranslation(player))
            } else {
                player.sendMessage(Translation.MENU_ITEMS_EQUIPMENT_FAILED.getTranslation(player)!!)
            }
            updateItems()
        }
    }

    private fun updateItems() {
        loading = true
        loading = false

        titleComponent = centeredTitle("Equipment (loading)")

        executeAsync {
            equippedItems = MainRepositoryProvider.playerEquippedItemRepository.getAllByPlayer(requireOwner().uniqueId)
            titleItemStack = getItemStackForWornItem(EquippedItemSlot.TITLE)
            helmetItemStack = getItemStackForWornItem(EquippedItemSlot.HELMET)
            chestplateItemStack = getItemStackForWornItem(EquippedItemSlot.CHESTPLATE)
            leggingsItemStack = getItemStackForWornItem(EquippedItemSlot.LEGGINGS)
            bootsItemStack = getItemStackForWornItem(EquippedItemSlot.BOOTS)
            costumeItemStack = getItemStackForWornItem(EquippedItemSlot.COSTUME)
            balloonItemStack = getItemStackForWornItem(EquippedItemSlot.BALLOON)
            weaponItemStack = getItemStackForWornItem(EquippedItemSlot.HANDHELD)
            consumptionItemStack = getItemStackForWornItem(EquippedItemSlot.CONSUMPTION)
            laserGameAStack = getItemStackForWornItem(EquippedItemSlot.LASER_GAME_A)
            laserGameBStack = getItemStackForWornItem(EquippedItemSlot.LASER_GAME_B)
            shoulderPetLeftStack = getItemStackForWornItem(EquippedItemSlot.SHOULDER_PET_LEFT)
            shoulderPetRightStack = getItemStackForWornItem(EquippedItemSlot.SHOULDER_PET_RIGHT)
            titleComponent = centeredTitle("Equipment")
            scheduleLayout()
        }
    }

    private val titleSlot = calculateZeroBasedIndex(1, 1)
    private val shoulderPetLeftSlot = calculateZeroBasedIndex(1, 3)
    private val helmetSlot = calculateZeroBasedIndex(1, 4)
    private val shoulderPetRightSlot = calculateZeroBasedIndex(1, 5)
    private val chestplateSlot = calculateZeroBasedIndex(2, 4)
    private val leggingsSlot = calculateZeroBasedIndex(3, 4)
    private val bootsSlot = calculateZeroBasedIndex(4, 4)
    private val costumeSlot = calculateZeroBasedIndex(3, 1)
    private val balloonSlot = calculateZeroBasedIndex(2, 1)
    private val laserGameASlot = calculateZeroBasedIndex(1, 0)
    private val laserGameBSlot = calculateZeroBasedIndex(2, 0)
    private val weaponSlot = calculateZeroBasedIndex(2, 5)
    private val consumptionSlot = calculateZeroBasedIndex(2, 3)

    companion object {
        val NAME_ITEM_REMOVE_HELMET = CVTextColor.MENU_DEFAULT_TITLE + "Unequip helmet"
        val NAME_ITEM_REMOVE_CHESTPLATE = CVTextColor.MENU_DEFAULT_TITLE + "Unequip chestplate"
        val NAME_ITEM_REMOVE_LEGGINGS = CVTextColor.MENU_DEFAULT_TITLE + "Unequip leggings"
        val NAME_ITEM_REMOVE_BOOTS = CVTextColor.MENU_DEFAULT_TITLE + "Unequip boots"
        val NAME_ITEM_REMOVE_COSTUME = CVTextColor.MENU_DEFAULT_TITLE + "Unequip costume"
        val NAME_ITEM_REMOVE_BALLOON = CVTextColor.MENU_DEFAULT_TITLE + "Unequip balloon"
        val NAME_ITEM_REMOVE_TITLE = CVTextColor.MENU_DEFAULT_TITLE + "Unequip title"
        val NAME_ITEM_REMOVE_WEAPON = CVTextColor.MENU_DEFAULT_TITLE + "Unequip weapon"
        val NAME_ITEM_REMOVE_CONSUMPTION = CVTextColor.MENU_DEFAULT_TITLE + "Unequip consumption"
        val NAME_ITEM_REMOVE_LASERGAME_ITEM_A = CVTextColor.MENU_DEFAULT_TITLE + "Unequip item (A)"
        val NAME_ITEM_REMOVE_LASERGAME_ITEM_B = CVTextColor.MENU_DEFAULT_TITLE + "Unequip item (B)"
        val NAME_ITEM_REMOVE_SHOULDER_PET_LEFT = CVTextColor.MENU_DEFAULT_TITLE + "Stow away left shoulder pet"
        val NAME_ITEM_REMOVE_SHOULDER_PET_RIGHT = CVTextColor.MENU_DEFAULT_TITLE + "Stow away right shoulder pet"
    }
}