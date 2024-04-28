package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.displayNamePlain
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.config.AreaConfigManager
import net.craftventure.core.extension.getItemId
import net.craftventure.core.extension.isAfk
import net.craftventure.core.metadata.CooldownTrackerMeta
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.core.serverevent.IdentifiedItemUseEvent
import net.craftventure.core.serverevent.OwnedItemConsumeEvent
import net.craftventure.core.serverevent.OwnedItemUseEvent
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.teleport
import net.craftventure.database.repository.PlayerKeyValueRepository
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.BoundingBox

class ItemListener : Listener {
    companion object {
        val vogelrokEggLocation = Location(Bukkit.getWorld("world"), -122.50, 43.5, -843.50)
        val hitboxSize = 1.0
        val eggBoundingBox = BoundingBox.of(vogelrokEggLocation, hitboxSize, hitboxSize, hitboxSize)

        fun applyVogelRokWarp(player: Player, playEffect: Boolean = false): Boolean {
            val warp = MainRepositoryProvider.warpRepository.cachedItems.firstOrNull { it.id == "vogelrok_egg_gate" }
            if (warp != null) {
                if (playEffect) {
                    player.location.clone().add(0.0, player.eyeHeight * 0.5, 0.0).spawnParticleX(
                        Particle.CAMPFIRE_COSY_SMOKE,
                        count = 20,
                        offsetX = 0.3,
                        offsetY = 1.0,
                        offsetZ = 0.3
                    )
                    player.world.playSound(player.location, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.AMBIENT, 1f, 1f)
                }
                warp.teleport(player)
                return true
            } else {
                player.sendMessage(CVTextColor.serverNotice + "Looks like this feature is broken, please report it to the crew and don't waste your eggs")
                return false
            }
        }
    }

    private fun handleVogelRokEggHit(location: Location, player: Player) {
//        Logger.debug("Handling hit at ${location.toVector().asString()} with box ${eggBoundingBox}")
        if (eggBoundingBox.contains(location.toVector())) {
            if (applyVogelRokWarp(player, true)) {
                executeAsync {
                    val database = MainRepositoryProvider.playerKeyValueRepository
                    var value = database.getValue(
                        player.uniqueId,
                        PlayerKeyValueRepository.ROK_EGG_UNUSED_COUNT
                    )?.toIntOrNull() ?: 0
                    value++
                    database.createOrUpdate(
                        player.uniqueId,
                        PlayerKeyValueRepository.ROK_EGG_UNUSED_COUNT,
                        value.toString()
                    )
                    player.sendMessage(CVTextColor.serverNotice + "If you fail to experience Vogel Rok, use /myrokeggandi to warp back here")
                }
            }
        }
    }

    @EventHandler
    fun onHit(event: ProjectileHitEvent) {
        val item = (event.entity as? Snowball)?.item ?: return
        if (item.itemMeta.hasCustomModelData() && item.itemMeta.customModelData != 5) return
        val player = event.entity.shooter as? Player ?: return

        val block = event.hitBlock
        if (block != null) {
//            Logger.debug("Hit block ${block.type} at ${block.location.toVector().asString()}")
            handleVogelRokEggHit(block.location, player)
            return
        }

        val entity = event.hitEntity
        if (entity != null) {
//            Logger.debug("Hit entity ${entity.type} at ${entity.location.toVector().asString()}")
            handleVogelRokEggHit(entity.location, player)
            return
        }
    }

//    @EventHandler
//    fun onCollide(event: ProjectileCollideEvent) {
//        val player = event.entity.shooter as? Player ?: return
//        handleVogelRokEggHit(event.entity.location, player)
//    }

    @EventHandler//(ignoreCancelled = true)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
//        Logger.debug("InteractAt cancelled=${event.isCancelled}")
        if (onHandleClick(event.player, ClickType.LEFT_CLICK, event.hand, event.rightClicked.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler//(ignoreCancelled = true)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
//        Logger.debug("InteractE cancelled=${event.isCancelled}")
        if (onHandleClick(event.player, ClickType.LEFT_CLICK, event.hand, event.rightClicked.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler//(ignoreCancelled = true)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
//        val shouldIgnore = /*event.isCancelled && */event.useItemInHand() == Event.Result.DENY
//        Logger.debug("Interact shouldIgnore=$shouldIgnore cancelled=${event.isCancelled} itemInHand=${event.useItemInHand()} block=${event.useInteractedBlock()} ${event.hand}")
//        if (shouldIgnore) return
        val clickLocation = event.clickedBlock?.getRelative(event.blockFace)?.location?.clone()?.add(0.5, 0.5, 0.5)
            ?: event.player.location
        when (event.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK ->
                if (onHandleClick(event.player, ClickType.LEFT_CLICK, event.hand!!, clickLocation)) {
                    event.isCancelled = true
                }

            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK ->
                if (onHandleClick(event.player, ClickType.RIGHT_CLICK, event.hand!!, clickLocation)) {
                    event.isCancelled = true
                }

            else -> {}
        }
    }

    private fun onHandleClick(
        player: Player,
        clicktype: ClickType,
        hand: EquipmentSlot,
        clickLocation: Location
    ): Boolean {
        val itemStack = player.equipment.getItem(hand)
        val itemId = itemStack.getItemId()

//        logcat { "Using $itemId from hand=$hand ($clicktype)" }

        if (itemId != null) {
            val useEvent = IdentifiedItemUseEvent(player, clicktype, itemId, hand, clickLocation)
            Bukkit.getPluginManager().callEvent(useEvent)
            if (useEvent.isCancelled) return true
        }

        val meta = player.equippedItemsMeta() ?: return false

        val item = meta.equippedItems.values.find { it.id == itemId } ?: return false

        val ownableItem = item.ownableItem
        val itemUseMeta = item.ownableItemMeta?.item ?: return false/*.apply {
            logcat { "No meta" }
        }*/
        val timeout = itemUseMeta.useTimeout

        val trackerMeta = player.getOrCreateMetadata { CooldownTrackerMeta(player) }
        val canUse = trackerMeta.use(itemUseMeta.category.name, timeout)
        if (!canUse) {
//            logcat { "Can't use, return" }
            return true
        }

        if (clicktype == ClickType.LEFT_CLICK && !itemUseMeta.activateWithLeftClick) return false
        if (clicktype == ClickType.RIGHT_CLICK && !itemUseMeta.activateWithRightClick) return false

        if (!itemUseMeta.allowWhileAfk && player.isAfk()) return false
        if (!itemUseMeta.allowWhileInVehicle && player.isInsideVehicle) return false
        if (player.gameMode == GameMode.SPECTATOR && itemUseMeta.requireNonSpectator) return false

        val event = OwnedItemUseEvent(player, clicktype, item, hand, clickLocation)
        Bukkit.getPluginManager().callEvent(event)

        if (!event.isCancelled) {
            val shouldBlock = itemUseMeta.effects.map { it.shouldBlockConsumption(player) }.firstOrNull()
            if (shouldBlock != null) {
                event.isCancelled = true
                player.sendMessage(
                    Component.text(
                        "Item ${item.itemStack?.displayNamePlain()} was not used: ",
                        CVTextColor.serverError
                    ) + shouldBlock
                )
                return true
            }
//            logcat { "Applying ${itemUseMeta.effects.size} effects" }
            itemUseMeta.effects.forEach { it.apply(player, clickLocation, item) }

            if (ownableItem?.consumable?.consumeUponUsage == true && itemUseMeta.deleteOnUseIfConsumable) {
                executeAsync {
                    MainRepositoryProvider.playerOwnedItemRepository.delete(player.uniqueId, item.id, 1)
                }
            }
            return true
        }

        return event.isCancelled
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onPlayerItemConsumeEvent(event: PlayerItemConsumeEvent) {
        event.isCancelled = true
//        logcat { "Consume" }
        val itemId = event.item.getItemId() ?: return
//        logcat { "Item $itemId" }
        val player = event.player
        val wornData = player.equippedItemsMeta()?.equippedItems
        val equippedItem = wornData?.values?.find { it.id == itemId }

        if (equippedItem?.id != itemId) return
//        logcat { "Handle item $itemId" }
//        val equippedItemStack = equippedItem?.itemStack

        val meta = equippedItem.ownableItemMeta ?: return
        val consumptionMeta = meta.consumptionMeta ?: return

        val shouldBlock = consumptionMeta.effects.firstNotNullOfOrNull { it.shouldBlockConsumption(player) }
        if (shouldBlock != null) {
            player.sendMessage(
                Component.text(
                    "Consuming ${equippedItem.itemStack?.displayNamePlain()} is currently not possible: ",
                    CVTextColor.serverError
                ) + shouldBlock
            )
            return
        }

        val bukkitEvent = OwnedItemConsumeEvent(player, equippedItem)
        Bukkit.getPluginManager().callEvent(bukkitEvent)
        if (bukkitEvent.isCancelled || AreaConfigManager.getInstance()
                .isConsumptionBlocked(player.location.toVector())
        ) {
            player.sendMessage(
                Component.text(
                    "Consuming ${equippedItem.itemStack?.displayNamePlain()} is currently not allowed",
                    CVTextColor.serverError
                )
            )
            return
        }

        player.foodLevel += consumptionMeta.foodLevelDelta
        player.saturation += consumptionMeta.saturationLevelDelta

//        logcat { "Handle item consume $itemId" }
        consumptionMeta.effects.forEach { it.apply(player, player.location, equippedItem) }
        executeAsync {
            MainRepositoryProvider.playerOwnedItemRepository
                .delete(player.uniqueId, itemId, 1)
        }
    }

    enum class ClickType {
        LEFT_CLICK,
        RIGHT_CLICK
    }
}