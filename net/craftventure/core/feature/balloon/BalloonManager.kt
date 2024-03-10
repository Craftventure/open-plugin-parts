package net.craftventure.core.feature.balloon

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.extension.isDisconnected
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.core.async.executeAsync
import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.feature.balloon.extensions.EntityExtension
import net.craftventure.core.feature.balloon.extensions.LeashExtension
import net.craftventure.core.feature.balloon.extensions.PopExtension
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.holders.PlayerHolder
import net.craftventure.core.feature.balloon.types.Balloon
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.extension.toOptional
import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.BalloonTrackerMeta
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.temporary.getOwnableItemMetadata
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap


class BalloonManager : BackgroundService.Animatable {

    override fun onAnimationUpdate() {
        synchronized(holders) {
            val iterator = holders.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!entry.key.isValid) {
                    entry.value.despawn(true)
                    iterator.remove()
                    continue
                }
                try {
                    entry.value.update()
                } catch (e: Exception) {
                    Logger.capture(e)
                }
            }
        }
    }

    companion object {
        private var balloonManager: BalloonManager? = null
        private val holders = ConcurrentHashMap<BalloonHolder, Balloon>()

        fun init() {
            balloonManager = BalloonManager()
            BackgroundService.add(balloonManager!!)

            for (player in Bukkit.getOnlinePlayers()) {
                invalidate(player)
            }
        }

        fun destroy() {
            if (balloonManager != null)
                BackgroundService.remove(balloonManager!!)
            holders.values.forEach { it.despawn(false) }
        }

        fun create(holder: BalloonHolder, balloon: Balloon) {
            remove(holder)
            balloon.spawn(holder)
            holders[holder] = balloon
        }

        fun create(player: Player, balloon: Balloon) {
            remove(player)
            if (!player.isInsideVehicle) {
                val playerHolder = PlayerHolder(player)
                create(playerHolder, balloon)
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "equip_balloon")
                }
            }
        }

        fun isAllowedToHoldBalloon(player: Player): Boolean {
//            val vehicle = player.vehicle
//            val ownerType = vehicle?.getInstanceOwnerType()
            return player.isConnected() && !player.isInsideVehicle && player.gameMode != GameMode.SPECTATOR && player.spectatorTarget == null && FeatureManager.isFeatureEnabled(
                FeatureManager.Feature.BALLOON_ACTIVATE
            )
        }

        fun invalidate(player: Player) {
            if (player.isDisconnected()) return
            val allow = isAllowedToHoldBalloon(player)
            val balloonTrackerMeta = player.getMetadata<BalloonTrackerMeta>()
//            logcat { "Invalidating allow=$allow" }
            if (!allow) {
                remove(player)
                balloonTrackerMeta?.clear()
                return
            }

            if (balloonTrackerMeta == null) {
//                logcat { "No meta" }
                remove(player)
                return
            }
            val equippedMeta = player.equippedItemsMeta()
            val equippedBalloonData = equippedMeta?.appliedEquippedItems?.balloonItem

            if (balloonTrackerMeta.update(equippedBalloonData)) {
                if (equippedBalloonData == null) {
//                    logcat { "Removing" }
                    remove(player)
                } else {
                    val balloon = toBalloon(equippedBalloonData)
                    if (balloon != null) {
//                        logcat { "Creating" }
                        create(player, balloon)
                    } else {
//                        logcat { "Invalid balloon" }
                        balloonTrackerMeta.clear()
                    }
                }
            }
        }

        private fun defaultBalloon() = OwnableItemMetadata(
            extensibleBalloon = ExtensibleBalloon.Json(
                extensions = listOf(
                    PopExtension.Json(),
                    LeashExtension.Json(),
                    EntityExtension.Json(
                        helmet = "self"
                    ),
                )
            ).toOptional()
        )

        fun toBalloon(equippedBalloonData: EquipmentManager.EquippedItemData): Balloon? {
            val balloonId = equippedBalloonData.id
            if (balloonId.startsWith("balloon_")) {
                val ownableItem = equippedBalloonData.ownableItem
                try {
                    val balloonMeta = ownableItem?.getOwnableItemMetadata()?.extensibleBalloon?.orElse()
                        ?: defaultBalloon().extensibleBalloon?.orElse()
                    if (balloonMeta != null) {
                        return balloonMeta.toBalloon(balloonId, equippedBalloonData.ownableItem)
                    }
                } catch (e: Exception) {
                    Logger.capture(e)
                }
                return null

//                val itemStackData =
//                    equippedBalloonData.balloonItemStack ?: equippedBalloonData.itemStackData?.itemStack
//
//                if (itemStackData == null) {
//                    Logger.severe("Failed to locate balloondata for %s", false, balloonId)
//                } else {
//                    val stack = ItemStackLoader.update(itemStackData, ownableItem, null)
//                    return SimpleBalloon(balloonId, stack)
//                }
            }
            return null
        }

        fun toBalloon(ownableItem: OwnableItem): Balloon? {
            val balloonId = ownableItem.id ?: return null
            if (balloonId.startsWith("balloon_")) {
                try {
                    val balloonMeta = ownableItem.getOwnableItemMetadata()?.extensibleBalloon?.orElse()
                        ?: defaultBalloon().extensibleBalloon?.orElse()
                    if (balloonMeta != null) {
                        return balloonMeta.toBalloon(balloonId, ownableItem)
                    }
                } catch (e: Exception) {
                    Logger.capture(e)
                }
                return null

//                val itemStackData = ownableItem.guiItemStackDataId
//                    ?.let { MainRepositoryProvider.itemStackDataRepository.findCached(it, loadIfCacheInvalid = true) }
//                    ?.itemStack
//
//                if (itemStackData == null) {
//                    Logger.severe("Failed to locate balloondata for %s", false, balloonId)
//                } else {
//                    val stack = ItemStackLoader.update(itemStackData, ownableItem, null)
//                    return SimpleBalloon(balloonId, stack)
//                }
            }
            return null
        }

        fun remove(holder: BalloonHolder) {
            synchronized(holders) {
                val iterator = holders.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.key === holder) {
                        entry.value.despawn(true)
                        iterator.remove()
                    }
                }
            }
        }

        fun remove(player: Player) {
            //        Logger.console("Remove balloon for " + player.getName());
            synchronized(holders) {
                val iterator = holders.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val key = entry.key
                    if (key is PlayerHolder && key.holder === player) {
                        entry.value.despawn(true)
                        iterator.remove()
                        EquipmentManager.reapply(player)
                    }
                }
            }
        }
    }
}
