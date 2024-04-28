package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.bukkit.ktx.util.CraftventureKeys.isSeat
import net.craftventure.bukkit.ktx.util.ItemStackUtils2
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemRepresentation
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.ConsumableType
import net.craftventure.database.type.ItemType
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.conversations.Conversation
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class ArmorStandEditorMenu(
    player: Player,
    private val armorStand: ArmorStand
) : InventoryMenu(
    owner = player,
) {
    private var equipment = EquipmentData.fromArmorStand(armorStand)
    private val placeholderHelmet = ItemStack(MaterialConfig.GUI_HELMET)
        .unbreakable()
        .hideAttributes()
        .apply { displayName("§7Helmet Placeholder") }
    private val placeholderChestplate = ItemStack(MaterialConfig.GUI_CHESTPLATE)
        .unbreakable()
        .hideAttributes()
        .apply { displayName("§7Chestplate Placeholder") }
    private val placeholderLeggings = ItemStack(MaterialConfig.GUI_LEGGINGS)
        .unbreakable()
        .hideAttributes()
        .apply { displayName("§7Leggings Placeholder") }
    private val placeholderBoots = ItemStack(MaterialConfig.GUI_BOOTS)
        .unbreakable()
        .hideAttributes()
        .apply { displayName("§7Boots Placeholder") }
    private val placeholderItemInMainHand = ItemStack(MaterialConfig.GUI_HAND)
        .unbreakable()
        .hideAttributes()
        .apply { displayName("§7Main Hand Placeholder") }
    private val placeholderItemInOffHand = ItemStack(MaterialConfig.GUI_HAND)
        .unbreakable()
        .hideAttributes()
        .apply { displayName("§7Off Hand Placeholder") }

    //    private val placeholderHelmet = ItemStack()
    init {
        rowsCount = 5
        titleComponent = centeredTitle("ArmorStand Editor")
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        val offset = 9 * 2
        val air = ItemStack(Material.AIR)
        val newItem =
            if (action == InventoryAction.PICKUP_ALL) player.equipment.itemInMainHand else ItemStack(Material.AIR)
        when (position) {
            0 -> {
                armorStand.remove()
                executeSync { player.closeInventoryStack() }
            }

            1 -> {
                equipment.apply {
                    helmet = null
                    chestplate = null
                    leggings = null
                    boots = null
                    itemInMainHand = null
                    itemInOffHand = null
                }
                invalidate()
            }

            2 -> {
                val equipment = armorStand.equipment
                equipment.helmet = (inventory.getItem(offset + 0)?.isPlaceHolder(placeholderHelmet) ?: air)
                equipment.chestplate = (inventory.getItem(offset + 1)?.isPlaceHolder(placeholderChestplate) ?: air)
                equipment.leggings = (inventory.getItem(offset + 2)?.isPlaceHolder(placeholderLeggings) ?: air)
                equipment.boots = (inventory.getItem(offset + 3)?.isPlaceHolder(placeholderBoots) ?: air)
                equipment.setItemInMainHand(
                    inventory.getItem(offset + 4)?.isPlaceHolder(placeholderItemInMainHand)
                        ?: air
                )
                equipment.setItemInOffHand(
                    inventory.getItem(offset + 5)?.isPlaceHolder(placeholderItemInOffHand)
                        ?: air
                )
                executeSync { player.closeInventoryStack() }
            }

            3 -> {
                armorStand.isVisible = !armorStand.isVisible
                invalidate()
            }

            4 -> {
                armorStand.isSmall = !armorStand.isSmall
                invalidate()
            }

            5 -> {
                armorStand.isMarker = !armorStand.isMarker
                invalidate()
            }

            6 -> {
                armorStand.setArms(!armorStand.hasArms())
                invalidate()
            }

            7 -> {
                armorStand.setGravity(!armorStand.hasGravity())
                invalidate()
            }

            8 -> {
                if (PluginProvider.isTestServer() && player.isJoey()) {
                    player.beginConversation(
                        Conversation(
                            PluginProvider.getInstance(),
                            player,
                            object : StringPrompt() {
                                override fun getPromptText(p0: ConversationContext): String =
                                    " \n \n \n§eEnter costume name (ex. costume_swagger)"

                                override fun acceptInput(p0: ConversationContext, p1: String?): Prompt? {
//                                    Logger.debug("$p1 $p0")
                                    val id = p1
                                    if (id?.startsWith("costume_") == false || id == null) {
                                        return this
                                    } else {
                                        val helmet = equipment.helmet
                                        val chestplate = equipment.chestplate
                                        val leggings = equipment.leggings
                                        val boots = equipment.boots
                                        executeAsync {
                                            val itemStackDatabase = MainRepositoryProvider.itemStackDataRepository
                                            val ownableItemDatabase = MainRepositoryProvider.ownableItemRepository
                                            ownableItemDatabase.create(
                                                OwnableItem(
                                                    id = id,
                                                    price = 999999,
                                                    buyAmount = 1,
                                                    permission = id + "_helmet",
                                                    guiItemStackDataId = id + "_helmet",
                                                    enabled = true,
                                                    type = ItemType.COSTUME,
                                                    bankAccountType = BankAccountType.VC,
                                                    predecessors = null,
                                                    consumable = ConsumableType.SINGLE,
                                                    metadata = null
                                                )
                                            )
                                            if (helmet != null)
                                                itemStackDatabase.create(
                                                    ItemStackData(
                                                        id + "_helmet",
                                                        helmet.serializeAsBytes(),
                                                        null,
                                                        null
                                                    )
                                                )
                                            if (chestplate != null)
                                                itemStackDatabase.create(
                                                    ItemStackData(
                                                        id + "_chestplate",
                                                        chestplate.serializeAsBytes(),
                                                        null,
                                                        null
                                                    )
                                                )
                                            if (leggings != null)
                                                itemStackDatabase.create(
                                                    ItemStackData(
                                                        id + "_leggings",
                                                        leggings.serializeAsBytes(),
                                                        null,
                                                        null
                                                    )
                                                )
                                            if (boots != null)
                                                itemStackDatabase.create(
                                                    ItemStackData(
                                                        id + "_boots",
                                                        boots.serializeAsBytes(),
                                                        null,
                                                        null
                                                    )
                                                )
                                            player.sendMessage(CVTextColor.serverNotice + "Costume $id created")
                                        }
                                    }
                                    return null
                                }

                            })
                    )
                }
                invalidate()
            }

            9 -> {
                armorStand.setCanMove(!armorStand.canMove())
                invalidate()
            }

            10 -> {
                armorStand.setCanTick(!armorStand.canTick())
                invalidate()
            }

            11 -> {
                executeSync { player.closeInventoryStack() }
                armorStand.addPassenger(player)
            }

            12 -> {
                executeSync { player.closeInventoryStack() }
                armorStand.isSeat = !armorStand.isSeat
            }

            offset + 0 -> {
                equipment.helmet = newItem
                invalidate()
            }

            offset + 1 -> {
                equipment.chestplate = newItem
                invalidate()
            }

            offset + 2 -> {
                equipment.leggings = newItem
                invalidate()
            }

            offset + 3 -> {
                equipment.boots = newItem
                invalidate()
            }

            offset + 4 -> {
                equipment.itemInMainHand = newItem
                invalidate()
            }

            offset + 5 -> {
                equipment.itemInOffHand = newItem
                invalidate()
            }
        }
    }

    override fun onLayout(inventory: Inventory) {
        ItemStack(Material.TNT).apply {
            displayName(NamedTextColor.DARK_RED + "[DANGEROUS] Delete ArmorStand")
            inventory.setItem(0, this)
        }
        ItemStack(Material.CAULDRON).apply {
            displayName(NamedTextColor.RED + "Clear equipment")
            inventory.setItem(1, this)
        }
        BankAccountType.VC.itemRepresentation.apply {
            displayName(NamedTextColor.GREEN + "Save equipment")
            inventory.setItem(2, this)
        }
        ItemStack(Material.POTION).apply {
            displayName(NamedTextColor.GREEN + "Make ${if (armorStand.isVisible) "invisible" else "visible"} ")
            inventory.setItem(3, this)
        }
        ItemStack(Material.BROWN_MUSHROOM).apply {
            displayName(NamedTextColor.GREEN + "Make ${if (armorStand.isSmall) "large" else "small"} ")
            inventory.setItem(4, this)
        }
        ItemStack(Material.OAK_SIGN).apply {
            displayName(NamedTextColor.YELLOW + "[WARNING] Make ${if (armorStand.isMarker) "non-marker" else "marker"} ")
            loreWithBuilder { text("You can't easily undo this as you can no longer interact with the ArmorStand if it's a marker.") }
            inventory.setItem(5, this)
        }
        ItemStack(MaterialConfig.ARM).apply {
            displayName(NamedTextColor.GREEN + "Has arms: ${armorStand.hasArms()}")
            inventory.setItem(6, this)
        }
        ItemStack(Material.ANVIL).apply {
            displayName(NamedTextColor.GREEN + "Has gravity: ${armorStand.hasGravity()}")
            inventory.setItem(7, this)
        }
        if (PluginProvider.isTestServer() && owner!!.isJoey())
            ItemStack(Material.COMMAND_BLOCK).apply {
                displayName(NamedTextColor.GREEN + "Create costume")
                inventory.setItem(8, this)
            }

        ItemStack(Material.ANVIL).apply {
            displayName(NamedTextColor.GREEN + "Can move: ${armorStand.canMove()}")
            loreWithBuilder { text("Only works when the ArmorStand can tick") }
            inventory.setItem(9, this)
        }
        ItemStack(Material.ANVIL).apply {
            displayName(NamedTextColor.GREEN + "Can tick: ${armorStand.canTick()}")
            loreWithBuilder { text("Should not be used, as ticking eats up server performance. You turn this on, and I will look for you.. I will find you.. and I will kill you...") }
            inventory.setItem(10, this)
        }
        ItemStack(Material.MINECART).apply {
            displayName(NamedTextColor.GREEN + "Ride this entity")
            inventory.setItem(11, this)
        }
        ItemStack(Material.ACACIA_BOAT).apply {
            val isSeat = armorStand.isSeat
            displayName(NamedTextColor.GREEN + "${if (isSeat) "Unmark" else "Mark"} as seat")
            inventory.setItem(12, this)
        }

        val offset = 9 * 2
        inventory.setItem(
            offset + 0, (equipment.helmet?.nullIfAir()
                ?: placeholderHelmet).loreWithBuilder { action("Click to replace with item in your main hand") }
        )
        inventory.setItem(
            offset + 1, (equipment.chestplate?.nullIfAir()
                ?: placeholderChestplate).loreWithBuilder { action("Click to replace with item in your main hand") }
        )
        inventory.setItem(
            offset + 2, (equipment.leggings?.nullIfAir()
                ?: placeholderLeggings).loreWithBuilder { action("Click to replace with item in your main hand") }
        )
        inventory.setItem(
            offset + 3, (equipment.boots?.nullIfAir()
                ?: placeholderBoots).loreWithBuilder { action("Click to replace with item in your main hand") }
        )
        inventory.setItem(
            offset + 4, (equipment.itemInMainHand?.nullIfAir()
                ?: placeholderItemInMainHand).loreWithBuilder { action("Click to replace with item in your main hand") }
        )
        inventory.setItem(
            offset + 5, (equipment.itemInOffHand?.nullIfAir()
                ?: placeholderItemInOffHand).loreWithBuilder { action("Click to replace with item in your main hand") }
        )

        var index = 9 * 3
        for (passenger in armorStand.passengers) {
            if (index + 1 < 9 * 5) {
                inventory.setItem(
                    index++,
                    if (passenger is Player) {
                        passenger.uniqueId.toSkullItem()
                    } else
                        ItemStackUtils2.create(
                            Material.STONE,
                            "Passenger ${passenger.type} ${passenger.name}"
                        )
                )
            }
        }
    }

    private fun ItemStack.nullIfAir() = if (type == Material.AIR) null else this
    private fun ItemStack.isPlaceHolder(itemStack: ItemStack) =
        if (this === itemStack || this == itemStack) null else this

    class EquipmentData(
        var helmet: ItemStack?,
        var chestplate: ItemStack?,
        var leggings: ItemStack?,
        var boots: ItemStack?,
        var itemInMainHand: ItemStack?,
        var itemInOffHand: ItemStack?
    ) {
        companion object {
            fun fromArmorStand(armorStand: ArmorStand): EquipmentData {
                val equipment = armorStand.equipment
                return EquipmentData(
                    equipment.helmet,
                    equipment.chestplate,
                    equipment.leggings,
                    equipment.boots,
                    equipment.itemInMainHand,
                    equipment.itemInOffHand
                )
            }
        }
    }
}