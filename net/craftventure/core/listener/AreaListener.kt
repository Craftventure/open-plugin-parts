package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.area.AreaTracker
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.extension.removeAllPotionEffects
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.config.AreaConfigManager
import net.craftventure.core.config.CraftventureAreasReloadedEvent
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.manager.visibility.VisibilityManager
import net.craftventure.core.metadata.OwnedItemCache
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.teleport
import net.craftventure.database.bukkit.extensions.teleportIfPermissioned
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


class AreaListener private constructor() : Listener {
    private val areaTrackers = ArrayList<AreaTracker>()

    init {
        updateAreas()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onAreasReloaded(event: CraftventureAreasReloadedEvent) {
        updateAreas()
    }

    private fun updateAreas() {
        for (areaTracker in areaTrackers) {
            areaTracker.stop()
        }
        areaTrackers.clear()
        val areaConfigList = AreaConfigManager.getInstance().getAreaConfigList()
        Logger.info("Updating " + areaConfigList.size + " areas for AreaListener")
        for (areaConfig in areaConfigList) {
//            Logger.debug(" Area ${areaConfig.name} enabled=${areaConfig.enabled} ${areaConfig.shouldCreateTracker()} ${areaConfig}")
            if (areaConfig.enabled && areaConfig.shouldCreateTracker()) {
                val areaTracker = AreaTracker(areaConfig.area)
                areaTracker.addListener(object : AreaTracker.StateListener {
                    override fun onPoseChanged(areaTracker: AreaTracker, player: Player, pose: Pose) {
                        super.onPoseChanged(areaTracker, player, pose)

                        if (pose == Pose.SWIMMING && areaConfig.swimBlocked) {
                            executeSync { player.isSwimming = false }
                        }
                    }

                    override fun onEnter(areaTracker: AreaTracker, player: Player) {
                        if (PluginProvider.isNonProductionServer())
                            player.sendMessage(NamedTextColor.GRAY + "[DEBUG] Entered ${areaConfig.name}")

                        if (areaConfig.blockFlying) {
                            if (player.isCrew() && player.gameMode == GameMode.CREATIVE) {
                                // Ignore
                            } else
                                player.removeAllPotionEffects {
                                    it.type == PotionEffectType.SPEED ||
                                            it.type == PotionEffectType.SLOW_FALLING ||
                                            it.type == PotionEffectType.SLOW ||
                                            it.type == PotionEffectType.LEVITATION
                                }
                        }

                        if (areaConfig.joinMessage != null && areaConfig.isActive) {
                            player.sendMessage(areaConfig.joinMessage)
                        }
                        //                        Logger.console(player.getName() + " entered " + areaConfig.getName());
                        if (areaConfig.linkedAchievement != null ||
                            areaConfig.enterFlag != null ||
                            areaConfig.reward != null
                        ) {
                            executeAsync {
                                if (areaConfig.isActive && areaConfig.enterFlag != null)
                                    MainRepositoryProvider.playerKeyValueRepository.create(
                                        player.uniqueId,
                                        areaConfig.enterFlag,
                                        "1"
                                    )
                                if (areaConfig.isActive && areaConfig.linkedAchievement != null) {
                                    if (PluginProvider.isNonProductionServer())
                                        player.sendMessage(NamedTextColor.GRAY + "[DEBUG] Rewarding achievement ${areaConfig.linkedAchievement}")
                                    MainRepositoryProvider.achievementProgressRepository.reward(
                                        player.uniqueId,
                                        areaConfig.linkedAchievement
                                    )
                                }
                                if (areaConfig.isActive && areaConfig.reward != null)
                                    CommandReward.reward(player, areaConfig.reward)
                            }
                        }

                        areaConfig.enterWarp?.let { warpName ->
                            val warp = MainRepositoryProvider.warpRepository.findCachedByName(warpName) ?: return@let
                            if (warp.teleportIfPermissioned(player, force = areaConfig.forceWarp)) {
                                return
                            }
                        }

                        areaConfig.requiredItems?.let { requiredItems ->
                            val warpName = areaConfig.warpForRequiredItems ?: return@let
                            val warp = MainRepositoryProvider.warpRepository.findCachedByName(warpName) ?: return@let
                            val items = player.getMetadata<OwnedItemCache>() ?: return@let
                            if (items.ownedItemIds.containsAll(requiredItems)) {
                                warp.teleport(player)
                                areaConfig.requiredItemWarpMessage?.let {
                                    player.sendMessage(it.parseWithCvMessage())
                                }
                            } else {
                                if (areaConfig.requiredItemMissingEffect) {
                                    player.addPotionEffect(
                                        PotionEffect(
                                            PotionEffectType.CONFUSION,
                                            20 * 5,
                                            1,
                                            true,
                                            false
                                        )
                                    )
                                    player.addPotionEffect(
                                        PotionEffect(
                                            PotionEffectType.BLINDNESS,
                                            Int.MAX_VALUE,
                                            1,
                                            true,
                                            false
                                        )
                                    )
                                    player.playSound(player.location, SoundUtils.DRAGON_EGG, 1f, 1f)
                                }
                                areaConfig.requiredItemsMissingMessage?.let {
                                    player.sendMessage(it.parseWithCvMessage())
                                }
                            }
                        }

                        if (areaConfig.forceVanish)
                            VisibilityManager.broadcastChangesFrom(player)

                        if (areaConfig.blockFlying) {
                            if (player.isVIP()) {
                                display(
                                    player,
                                    Message(
                                        id = ChatUtils.ID_GENERAL_NOTICE,
                                        text = Component.text(
                                            "Your flight is temporarily disabled in this area",
                                            CVTextColor.serverError
                                        ),
                                        type = MessageBarManager.Type.NOTICE,
                                        untilMillis = TimeUtils.secondsFromNow(4.0),
                                    ),
                                    replace = true,
                                )
                            }

                            if (player.isGliding)
                                player.isGliding = false

                            if (player.isFlying) {
                                blockFlying(player)
                            }
                            if (PermissionChecker.isCrew(player)) {
                                player.sendMessage(CVTextColor.serverNotice + ("No-fly zone " + areaConfig.name + " entered"))
                            }
                        }
                    }

                    override fun onStartGliding(areaTracker: AreaTracker, player: Player, cancellable: Cancellable) {
                        if (areaConfig.blockFlying) {
                            cancellable.isCancelled = true
                        }
                    }

                    override fun onLeave(areaTracker: AreaTracker, player: Player) {
                        if (PluginProvider.isNonProductionServer())
                            player.sendMessage(NamedTextColor.GRAY + "[DEBUG] Left ${areaConfig.name}")
                        if (areaConfig.forceVanish)
                            VisibilityManager.broadcastChangesFrom(player)
                        if (areaConfig.leaveMessage != null && areaConfig.isActive) {
                            player.sendMessage(areaConfig.leaveMessage)
                        }
                        //                        Logger.console(player.getName() + " left " + areaConfig.getName());
                        if (areaConfig.blockFlying) {
                            if (PermissionChecker.isCrew(player)) {
                                player.sendMessage(CVTextColor.serverNotice + ("No-fly zone " + areaConfig.name + " exited"))
                            }
                            GameModeManager.setDefaultFly(player)
                        }

                        areaConfig.requiredItems?.let { requiredItems ->
                            player.removePotionEffect(PotionEffectType.BLINDNESS)
                        }
                    }

                    override fun onMove(areaTracker: AreaTracker, player: Player) {
                        if (areaConfig.blockFlying && player.isFlying) {
                            blockFlying(player)
                        }
                    }

                    override fun onFlightToggled(areaTracker: AreaTracker, player: Player, flying: Boolean) {
                        //                        Logger.console(player.getName() + " flight toggled " + areaConfig.getName());
                        if (flying && areaConfig.blockFlying) {
                            blockFlying(player)
                        }
                    }

                    private fun blockFlying(player: Player) {
                        if (!PermissionChecker.isCrew(player)) {
                            val wasFlying = player.isFlying || player.allowFlight
                            player.isFlying = false
                            player.allowFlight = false
                            if (wasFlying) {
                                display(
                                    player,
                                    Message(
                                        id = ChatUtils.ID_GENERAL_NOTICE,
                                        text = Translation.NOFLYZONE_BLOCKED.getTranslation(player)!!,
                                        type = MessageBarManager.Type.NOTICE,
                                        untilMillis = TimeUtils.secondsFromNow(4.0),
                                    ),
                                    replace = true,
                                )
                                if (!player.isInsideVehicle)
                                    player.teleport(player.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
                            }
                        }
                    }
                })
                areaTracker.start()
                areaTrackers.add(areaTracker)
            }
        }
    }

    companion object {
        private var instance = AreaListener()
        fun getInstance() = instance
    }
}
