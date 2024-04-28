package net.craftventure.core.utils

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.displayNamePlain
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.bukkit.ktx.util.ItemStackUtils2
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeSync
import net.craftventure.core.database.ItemStackLoader
import net.craftventure.core.database.model.ownable.PredecessorAchievement
import net.craftventure.core.feature.kart.KartManager
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.Permissions
import net.craftventure.core.manager.CoinBoostManager
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.getItemStack
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.bukkit.extensions.update
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.craftventure.database.type.*
import net.craftventure.temporary.create
import net.craftventure.temporary.getOwnableItemMetadata
import net.craftventure.temporary.getPredecessorsMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent.runCommand
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

object OwnableItemUtils {
    fun toItem(
        ownableItem: OwnableItem,
        shopItemStackData: ItemStackData,
        player: Player?,
        forceVisible: Boolean,
        multiplyAmount: Int = 1,
        ownedItems: List<PlayerOwnedItem> = if (player != null) MainRepositoryProvider.playerOwnedItemRepository.get(
            player.uniqueId,
            ownableItem.id!!
        ) else emptyList(),
        addActions: Boolean = true,
    ): ItemStack? {
        val accountType = ownableItem.bankAccountType
        val itemStack = shopItemStackData.getItemStack(ownableItem.type!!)
        if (itemStack != null) {
            ItemStackLoader.update(itemStack, ownableItem, ownedItems.firstOrNull())
        }
        if (shopItemStackData.overridenTitle == null)
            itemStack?.displayName(CVTextColor.serverNotice + ownableItem.id)

        val loreBuilder = ComponentBuilder.LoreBuilder()
        loreBuilder.text(accountType!!.emoji, color = NamedTextColor.WHITE)
        loreBuilder.accented("${ownableItem.price!! * multiplyAmount}")
        loreBuilder.accented(
            ", ${ownableItem.type!!.nameForAmount(multiplyAmount * ownableItem.buyAmount!!)}",
            addSpace = false
        )
        if (ownableItem.consumable!!.allowOwningMultiple && multiplyAmount > 1)
            loreBuilder.accented("${ownableItem.buyAmount!! * multiplyAmount}x")

        if (ownableItem.consumable!!.consumeUponUsage) {
            loreBuilder.moveToBlankLine()
            loreBuilder.accented("This item is consumed upon usage")
        }

        if (shopItemStackData.overridenLore != null) {
            loreBuilder.emptyLines(1)
            loreBuilder.text(shopItemStackData.overridenLore!!)
        }


        if (ownableItem.permission != null) {
            loreBuilder.apply {
                emptyLines(1)
                val allowed = player != null && player.hasPermission(ownableItem.permission!!)
                val color = if (allowed) NamedTextColor.GREEN else NamedTextColor.RED
                val prefix = if (allowed) "✅ " else "❎ "
                when (ownableItem.permission) {
                    Permissions.CREW -> text("${prefix}Requires crew", color)
                    Permissions.DRAGONCLAN -> text("${prefix}Requires DragonClan", color)
                    Permissions.VIP -> text("${prefix}Requires VIP", color)
                    "craftventure.rank.vipkart.buy" -> text("${prefix}Requires VIP", color)
                    else -> text("${prefix}Requires a specific permission to buy (${ownableItem.permission})", color)
                }
            }
        }

        loreBuilder.emptyLines()
        ownableItem.getOwnableItemMetadata()?.describe(loreBuilder)
//        description?.let { ownableItemMetadata ->
//            loreBuilder.apply {
//                emptyLines(1)
//                description.forEach {
//                    component(it)
//                    moveToBlankLine()
//                }
//            }
//        }

        val predecessors = ownableItem.getPredecessorsMeta()
        if (player != null && predecessors?.hasRequirements() == true) {
            if (!predecessors.shouldBeVisibleTo(player) && !forceVisible) return null
            val ownableItemDatabase = MainRepositoryProvider.ownableItemRepository
            val itemstackDatabase = MainRepositoryProvider.itemStackDataRepository
            val playedOwnedItemDatabase = MainRepositoryProvider.playerOwnedItemRepository

            val predecessorNames: List<Component> = predecessors
                .items
//                            .filter { id ->
//                                !playedOwnedItemDatabase.owns(player.uniqueId, id.name)
//                            }
                .map { item ->
                    val owned = playedOwnedItemDatabase.owns(player.uniqueId, item.name)
                    val precedessorOwnableItem = ownableItemDatabase.findSilent(item.name)
                    val itemStackData = itemstackDatabase
                        .findSilent(precedessorOwnableItem?.guiItemStackDataId ?: item.name)

                    if (owned && item.onlyShowIfMissing) return@map null

                    Component.text(
                        (if (owned) "✅ " else "❎ ") + precedessorOwnableItem?.type?.displayName + ": " +
                                (itemStackData?.itemStack?.displayNamePlain() ?: item.name) +
                                (if (owned) " (owned)" else ""),
                        if (owned) NamedTextColor.GREEN else NamedTextColor.RED,
                    )
                }
                .filterNotNull()

            val predecessorAchievements: List<Component> = predecessors
                .achievements
                .map { item ->
                    val achievement = MainRepositoryProvider.achievementRepository.findCached(item.name)
                    val hasCompletedResult = item.hasCompleted(player)
                    val hasCompleted = hasCompletedResult == PredecessorAchievement.Result.Yes

                    if (hasCompleted && item.onlyShowIfMissing) return@map null

                    val achievementDisplayName = item.overrideName ?: achievement?.displayName
                    val count = item.count ?: 1
                    Component.text(
                        (if (hasCompleted) "✅ " else "❎ ") +
                                (if (count == 1) "" else "${count}x ") +
                                (if (achievement?.type == AchievementType.SECRET && !item.forceShow && !hasCompleted) "(secret)"
                                else achievementDisplayName) +
                                (if (hasCompleted) " (rewarded)" else ""),
                        if (hasCompleted) NamedTextColor.GREEN else NamedTextColor.RED,
                    )
                }
                .filterNotNull()

            val predecessorRidecounts: List<Component> = predecessors
                .rideCounts
                .map { item ->
                    val ride = MainRepositoryProvider.rideRepository.getByName(item.name)
                    val hasCompleted = item.hasCompleted(player)

                    if (hasCompleted && item.onlyShowIfMissing) return@map null

                    val count = item.count ?: 1
                    Component.text(
                        (if (hasCompleted) "✅ " else "❎ ") +
                                (if (count == 1) "" else "${count}x ") +
                                ride?.displayName + (if (hasCompleted) " (rewarded)" else ""),
                        if (hasCompleted) NamedTextColor.GREEN else NamedTextColor.RED,
                    )
                }
                .filterNotNull()

            val predecessorChecks: List<Component> = predecessors
                .checks
                .map { item ->
                    val hasCompleted = item.hasCompleted(player)
                    Component.text(
                        (if (hasCompleted) "✅ " else "❎ ") + item.displayName() + (if (hasCompleted) " (rewarded)" else ""),
                        if (hasCompleted) NamedTextColor.GREEN else NamedTextColor.RED,
                    )
                }

            val predecessorStyle = Style.style().decoration(TextDecoration.ITALIC, false).build()
            if (predecessorRidecounts.isNotEmpty())
                loreBuilder.apply {
                    emptyLines(1)
                    action("Ride counter requirement(s):")
                    predecessorRidecounts.forEach {
                        moveToBlankLine()
                        component(Component.empty().style(predecessorStyle) + it)
                    }
                }
            if (predecessorNames.isNotEmpty())
                loreBuilder.apply {
                    emptyLines(1)
                    action("Required predecessor item(s):")
                    predecessorNames.forEach {
                        moveToBlankLine()
                        component(Component.empty().style(predecessorStyle) + it)
                    }
                }
            if (predecessorAchievements.isNotEmpty())
                loreBuilder.apply {
                    emptyLines(1)
                    action("Required predecessor achievement(s):")
                    predecessorAchievements.forEach {
                        moveToBlankLine()
                        component(Component.empty().style(predecessorStyle) + it)
                    }
                }
            if (predecessorChecks.isNotEmpty())
                loreBuilder.apply {
                    emptyLines(1)
                    action("Required checks:")
                    predecessorChecks.forEach {
                        moveToBlankLine()
                        component(Component.empty().style(predecessorStyle) + it)
                    }
                }
        }

        if (player != null && ownableItem.consumable!!.allowOwningMultiple) {
            loreBuilder.apply {
                emptyLines(1)
                accented(
                    "You own ${
                        MainRepositoryProvider.playerOwnedItemRepository
                            .ownsCount(player.uniqueId, ownableItem.id!!)
                    } of this"
                )
            }
        }

//                if (player.isCrew()) {
//                    if (lore.isNotEmpty())
//                        lore += "\n\n"
//                    lore += CVChatColor.MENU_DEFAULT_LORE_ACCENT + "[Crew] Owned by ${MainRepositoryProvider.playerOwnedItemRepository.totalOwnCount(
//                        ownableItem.id
//                    )}"
//                }

        if (addActions) {
            if (ownedItems.isNotEmpty() && !ownableItem.consumable!!.consumeUponUsage) {
                if (ownableItem.consumable!!.allowOwningMultiple)
                    loreBuilder.apply {
                        emptyLines(1)
                        action("Click to buy more of this item")
                    }
                else
                    loreBuilder.apply {
                        emptyLines(1)
                        action("Click to equip owned item")
                    }
            } else {
                loreBuilder.apply {
                    emptyLines(1)
                    action("Click to buy this item")
                }
            }
        }
        itemStack!!.lore(loreBuilder.buildLineComponents())

        itemStack.amount = ownableItem.buyAmount!! * multiplyAmount
//                if (ownableItem.type != ItemType.WEAPON) {
        itemStack.updateMeta<ItemMeta> {
            addItemFlags(*ItemFlag.values())
        }
        ItemStackUtils2.hideAttributes(itemStack)
        ItemStackUtils2.hideEnchants(itemStack)
//                }

        return itemStack
    }

    fun canEquip(
        player: Player,
        ownableItem: OwnableItem,
        ownedItems: List<PlayerOwnedItem>
    ): Boolean =
        ownedItems.isNotEmpty() && !ownableItem.consumable!!.consumeUponUsage && !ownableItem.consumable!!.allowOwningMultiple

    fun buy(ownableItem: OwnableItem, player: Player, multiplyAmount: Int = 1): Boolean {
        val accountType = ownableItem.bankAccountType
        if (ownableItem.permission != null && !player.hasPermission(ownableItem.permission!!)) {
            val errorMessage = "You don't have the permission to buy this item!"
            player.sendMessage(CVTextColor.serverNotice + errorMessage)
            player.sendTitleWithTicks(stay = 20 * 3, subtitleColor = NamedTextColor.RED, subtitle = errorMessage)
            return false
        }
        val predecessors = ownableItem.getPredecessorsMeta()
        if (predecessors != null) {
            if (!predecessors.hasCompleted(player)) {
                val errorMessage = "You don't own the required predecessors for this item"
                player.sendMessage(CVTextColor.serverError + errorMessage)
                player.sendTitleWithTicks(stay = 20 * 3, subtitleColor = NamedTextColor.RED, subtitle = errorMessage)
                return false
            }
        }
        val ownableItemId = ownableItem.id
        val itemStackData =
            MainRepositoryProvider.itemStackDataRepository.findCached(ownableItem.guiItemStackDataId!!)
        val itemStack = itemStackData?.getItemStack(ownableItem.type!!)
        // TODO: Make it so that you can buy multiple items of the item allows for that
        if (!ownableItem.consumable!!.allowOwningMultiple) {
            val ownedItems = MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId, ownableItemId!!)
            if (ownedItems.isNotEmpty()) {
                if (itemStack != null) {
                    equip(
                        player,
                        ownedItems.first(),
                        ownableItem,
                        EquippedItemSlot.find(ownableItem.type!!).firstOrNull()
                    )
                }
                return false
            }
        }

//                player.sendMessage(Translation.MENU_SHOP_ATTEMPTING_BUY.getTranslation(player)!!)

        val price = ownableItem.price!! * multiplyAmount
        val bankAccount = MainRepositoryProvider.bankAccountRepository.getOrCreate(player.uniqueId, accountType!!)
        if (!(bankAccount != null && bankAccount.balance!! >= price)) {
            if (bankAccount != null)
                player.sendMessage(
                    Translation.MENU_SHOP_NOT_ENOUGH_VENTURECOINS.getTranslation(
                        player, price - bankAccount.balance!!,
                        accountType.nameForAmount(price - bankAccount.balance!!),
                        accountType.abbreviation
                    )!!
                )
        } else {
            //                        CraftventureCore.getPl//offer.getOwnableItem()
            if (!MainRepositoryProvider.bankAccountRepository.delta(
                    player.uniqueId,
                    accountType,
                    (-price).toLong(),
                    TransactionType.SHOP
                )
            ) {
                Logger.severe(player.name + " bought item " + ownableItem.id + " for " + price + " but changing their balance appears to have failed")
                player.sendMessage(Translation.MENU_SHOP_BUY_ERROR.getTranslation(player)!!)
            } else {
                val amount = ownableItem.buyAmount!! * multiplyAmount
                val result = MainRepositoryProvider.playerOwnedItemRepository.create(
                    uuid = player.uniqueId,
                    ownableItemId = ownableItem.id!!,
                    paidPrice = ownableItem.price!! / ownableItem.buyAmount!!,
                    times = amount,
                    showReceiveMessage = true,
                    updateWornItems = true,
//                        disableListeners = !isLastItem,
                )

                if (!result) {
                    player.sendMessage(Translation.MENU_SHOP_BUY_FAILED.getTranslation(player)!!)
                } else {
                    player.displayTitle(
                        TitleManager.TitleData.ofTicks(
                            id = "item_received",
                            title = Translation.ITEM_RECEIVED_TITLE.getTranslation(player),
                            subtitle = if (ownableItem.type!!.equippable) Translation.ITEM_RECEIVED_TITLE_SUBTITLE.getTranslation(
                                player
                            ) else null,
                            fadeInTicks = 20,
                            stayTicks = 5 * 20,
                            fadeOutTicks = 20,
                        )
                    )

                    if (itemStackData != null && itemStack != null) {
                        if (ownableItem.type!!.equippable) {
                            val message = Component.text(
                                "Click here to equip your newly obtained item '${itemStack.displayNamePlain()}'",
                                CVTextColor.serverNotice
                            )
                                .clickEvent(
                                    runCommand(
                                        "/equip ${
                                            EquippedItemSlot.find(ownableItem.type!!)
                                                .firstOrNull()?.displayName?.lowercase(Locale.getDefault())
                                        } ${ownableItem.id}"
                                    )
                                )
                            player.sendMessage(message)
                        } else if (ownableItem.type == ItemType.KART) {
                            val message =
                                Component.text("Click here to start driving your new kart", CVTextColor.serverNotice)
                                    .clickEvent(runCommand(String.format("/karts ${ownableItem.id}", ownableItem.id)))
                            player.sendMessage(message)
                        }
                    }
                    //                                player.sendMessage(Translation.MENU_SHOP_EQUIP.getTranslation(player));
                    player.location.world!!.playSound(player.location, SoundUtils.MONEY, 1f, 1f)
//                            SoundUtils.playToPlayer(player, player.location, SoundUtils.MONEY, 10f, 1f)

                    MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "buy_first_item")
                    return true
                }
                //                            createdItem = itemStack;
            }
        }
        return false
    }

    fun equip(ownableItem: OwnableItem, player: Player, multiplyAmount: Int = 1): Boolean {
        val accountType = ownableItem.bankAccountType
        if (ownableItem.permission != null && !player.hasPermission(ownableItem.permission!!)) {
            val errorMessage = "You don't have the permission to buy this item!"
            player.sendMessage(CVTextColor.serverNotice + errorMessage)
            player.sendTitleWithTicks(stay = 20 * 3, subtitleColor = NamedTextColor.RED, subtitle = errorMessage)
            return false
        }
        val predecessors = ownableItem.getPredecessorsMeta()
        if (predecessors != null) {
            if (!predecessors.hasCompleted(player)) {
                val errorMessage = "You don't own the required predecessors for this item"
                player.sendMessage(CVTextColor.serverError + errorMessage)
                player.sendTitleWithTicks(stay = 20 * 3, subtitleColor = NamedTextColor.RED, subtitle = errorMessage)
                return false
            }
        }
        val ownableItemId = ownableItem.id
        val itemStackData =
            MainRepositoryProvider.itemStackDataRepository.findCached(ownableItem.guiItemStackDataId!!)
        val itemStack = itemStackData?.getItemStack(ownableItem.type!!)
        // TODO: Make it so that you can buy multiple items of the item allows for that
        if (!ownableItem.consumable!!.allowOwningMultiple) {
            val ownedItems = MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId, ownableItemId!!)
            if (ownedItems.isNotEmpty()) {
                if (itemStack != null) {
                    equip(
                        player,
                        ownedItems.first(),
                        ownableItem,
                        EquippedItemSlot.find(ownableItem.type!!).firstOrNull()
                    )
                }
                return true
            }
        }
        return false
    }

    fun equip(
        player: Player,
        playerOwnedItem: PlayerOwnedItem?,
        ownableItem: OwnableItem,
        slot: EquippedItemSlot?
    ): EquipResult {
        Logger.debug("${player.name} is equipping ${ownableItem.id} of type ${ownableItem.type} into slot $slot")
        if (slot != null && ownableItem.type !in slot.supportedItemTypes) {
            player.sendMessage(CVTextColor.serverError + "That item doesn't fit in this slot!")
            Logger.warn("${player.name} tried to equip ${ownableItem.id} of type ${ownableItem.type} into slot $slot")
            return EquipResult.INVALID
        }
        if (ownableItem.type == ItemType.SERVER_COIN_BOOSTER) {
            val coinBooster = MainRepositoryProvider.coinBoosterRepository.findCached(ownableItem.id!!)
            if (coinBooster == null) {
                player.sendMessage(CVTextColor.serverError + "That coinbooster does not appear to exist, please contact the crew to fix this!")
                return EquipResult.INVALID
            }
            val result = MainRepositoryProvider.playerOwnedItemRepository
                .delete(player.uniqueId, ownableItem.id!!, 1)
            if (result != 1) {
                val message =
                    "Failed to remove coin booster after activating for ${player.name} (${player.uniqueId}) for booster ${coinBooster.id} with result $result"
                player.sendMessage(CVTextColor.serverError + "This coinbooster was already enabled")
                Logger.warn(message)
                return EquipResult.FAILED
            } else if (MainRepositoryProvider.activeServerCoinBoosterRepository.activate(
                    player.uniqueId,
                    coinBooster.id!!
                )
            ) {
                player.sendMessage(Translation.MENU_COINBOOSTER_ACTIVATED.getTranslation(player)!!)
                return EquipResult.USED
            } else {
                player.sendMessage(Translation.MENU_COINBOOSTER_ACTIVATION_FAILED.getTranslation(player)!!)
                return EquipResult.FAILED
            }
        } else if (ownableItem.type == ItemType.COIN_BOOSTER) {
            val activeCoinBoosters = MainRepositoryProvider.activeCoinBoosterRepository.getByPlayer(player.uniqueId)
            if (activeCoinBoosters.isEmpty()) {
                val coinBooster = MainRepositoryProvider.coinBoosterRepository.findCached(ownableItem.id!!)
                if (coinBooster == null) {
                    player.sendMessage(CVTextColor.serverError + "That coinbooster does not appear to exist, please contact the crew to fix this!")
                    return EquipResult.INVALID
                }
                val result = MainRepositoryProvider.playerOwnedItemRepository.delete(
                    player.uniqueId, ownableItem.id!!, 1
                )

                if (result != 1) {
                    val message =
                        "Failed to remove coin booster after activating for ${player.name} (${player.uniqueId}) for booster ${coinBooster.id} with result $result"
                    player.sendMessage(CVTextColor.serverError + "This coinbooster was already enabled")
                    Logger.warn(message)
                    return EquipResult.INVALID
                } else if (MainRepositoryProvider.activeCoinBoosterRepository.activate(
                        player.uniqueId,
                        coinBooster
                    )
                ) {
                    player.sendMessage(Translation.MENU_COINBOOSTER_ACTIVATED.getTranslation(player)!!)
                    val rewardPerMinute = CoinBoostManager.getCoinRewardPerMinute(player).toLong()
                    player.sendMessage(
                        CVTextColor.serverNotice + "Your current reward is $rewardPerMinute ${
                            BankAccountType.VC.nameForAmount(
                                rewardPerMinute
                            )
                        }"
                    )
                    return EquipResult.USED
                } else {
                    player.sendMessage(Translation.MENU_COINBOOSTER_ACTIVATION_FAILED.getTranslation(player)!!)
                    return EquipResult.FAILED
                }
            } else {
                player.sendMessage(Translation.MENU_COINBOOSTER_ACTIVE_BOOSTER_LIMITED.getTranslation(player)!!)
                return EquipResult.INVALID
            }
        } else if (slot == EquippedItemSlot.TITLE) {
            if (MainRepositoryProvider.playerEquippedItemRepository.update(
                    player,
                    slot,
                    ownableItem,
                    playerOwnedItem?.id
                )
            ) {
                player.sendMessage(Translation.MENU_ITEMS_EQUIPPED.getTranslation(player)!!)
                return EquipResult.EQUIPPED
            } else {
                player.sendMessage(Translation.MENU_ITEMS_EQUIPMENT_FAILED.getTranslation(player)!!)
                return EquipResult.FAILED
            }
        } else if (ownableItem.type == ItemType.KART) {
            if (ownableItem.type == ItemType.KART) {
//                val itemStackData =
//                    if (playerOwnedItem != null)
//                        MainRepositoryProvider.ownableItemRepository.findCached(playerOwnedItem.ownedItemId!!)?.let {
//                            MainRepositoryProvider.itemStackDataRepository.findCached(it.guiItemStackDataId!!)
//                        }
//                    else null

                val kartId = ownableItem.getOwnableItemMetadata()?.kartId?.orElse()
                val kartProperties =
                    kartId?.let { KartManager.kartPropertiesFromConfig(it, ownableItem.getOwnableItemMetadata()) }
//                val kartProperties = KartManager.getKartProperties(
//                    ownableItem.id,
//                    itemStackData?.itemStack?.let { ItemStackLoader.update(it, ownableItem, playerOwnedItem) }
//                )
                if (kartProperties != null) {
                    executeSync {
                        if (!KartManager.isSafeLocation(player)) {
                            player.sendMessage(CVTextColor.serverNotice + "This space doesn't seem suitable for spawning a kart")
                            return@executeSync
                        }
                        KartManager.cleanupParkedKart(player)
                        if (!KartManager.isKarting(player))
                            try {
                                KartManager.startKarting(
                                    player = player,
                                    kartProperties = kartProperties,
                                    location = player.location,
                                    applySafetyCheck = true,
                                    parkable = true,
                                    spawnType = KartManager.SpawnType.User,
                                )
                            } catch (e: Exception) {
                                player.sendMessage(CVTextColor.serverError + (e.message ?: "Failed to spawn kart"))
                            }
                    }
                    return EquipResult.USED
                } else {
                    Logger.debug("Failed to spawn kart ${ownableItem.id}")
                    player.sendMessage(CVTextColor.serverError + "Failed to spawn that kart")
                }
                return EquipResult.FAILED
            }
        } else if (slot != null) {
            if (MainRepositoryProvider.playerEquippedItemRepository.update(
                    player,
                    slot,
                    ownableItem,
                    playerOwnedItem?.id
                )
            ) {
                player.sendMessage(Translation.MENU_ITEMS_EQUIPPED.getTranslation(player)!!)
                return EquipResult.EQUIPPED
            } else {
                player.sendMessage(Translation.MENU_ITEMS_EQUIPMENT_FAILED.getTranslation(player)!!)
                return EquipResult.FAILED
            }
        }

        player.sendMessage(CVTextColor.serverError + "You can't equip that item this way, try one of the following:")
        val slots = EquippedItemSlot.find(ownableItem.type!!)
        slots.forEach {
            val command = "/equip ${it.displayName.lowercase(Locale.getDefault())} ${ownableItem.id}"
            player.sendMessage(
                Component.text(command, CVTextColor.serverError)
                    .hoverEvent(Component.text("Click to run", CVTextColor.CHAT_HOVER).asHoverEvent())
                    .clickEvent(runCommand(command))
            )
        }

        Logger.warn("Failed to equip ${playerOwnedItem?.ownedItemId}")
        return EquipResult.FAILED
//        player.sendMessage(Translation.MENU_ITEMS_EQUIPMENT_FAILED.getTranslation(player)!!)
    }

    enum class EquipResult {
        INVALID,
        EQUIPPED,
        USED,
        FAILED
    }
}