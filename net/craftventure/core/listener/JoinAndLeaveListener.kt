package net.craftventure.core.listener

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import net.craftventure.bukkit.ktx.entitymeta.EntityEvents
import net.craftventure.bukkit.ktx.entitymeta.MetaAnnotations
import net.craftventure.bukkit.ktx.entitymeta.MetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.extension.sendTo
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.plugin.Environment
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.feature.finalevent.FinaleTimer
import net.craftventure.core.feature.maxifoto.MaxiFoto
import net.craftventure.core.ktx.extension.broadcastAsDebugTimings
import net.craftventure.core.ktx.extension.utcMillis
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.*
import net.craftventure.core.metadata.AfkStatus
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.requireEquippedItemsMeta
import net.craftventure.core.utils.EmojiHelper
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.getNearestWarpFromLocation
import net.craftventure.database.bukkit.extensions.teleport
import net.craftventure.database.generated.cvdata.tables.pojos.UuidName
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.*
import java.time.LocalDateTime


class JoinAndLeaveListener : Listener {

    private fun makeRoomForNewPlayers() {
        if (Bukkit.getOnlinePlayers().size >= CraftventureCore.getSettings().maxGuestCount - CraftventureCore.getSettings().afkFreeSlots -
            openVipSlots - openCrewSlots
        ) {
            for (player in Bukkit.getOnlinePlayers()) {
                val afkStatus = player.getMetadata<AfkStatus>()
                if (afkStatus != null && afkStatus.isAfk) {
                    player.kick(
                        Component.text(
                            "You were kicked because you were AFK and the server was full, feel free to join back another time!",
                            CVTextColor.serverNoticeAccent
                        )
                    )
                    makeRoomForNewPlayers()
                    return
                }
            }
        }
    }

    @EventHandler
    fun onAsyncLogin(event: AsyncPlayerPreLoginEvent) {
        if (Bukkit.getOnlinePlayers().size >= CraftventureCore.getSettings().maxGuestCount) {
            executeSync { this.makeRoomForNewPlayers() }
        }
        MainRepositoryProvider.uuidNameRepository.createOrUpdateSilent(
            UuidName(
                event.uniqueId,
                event.name,
                LocalDateTime.now()
            )
        )
    }

    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player
        val canAlwaysJoin = player.hasPermission("craftventure.join.always")//player.isCrew()

        if (!canAlwaysJoin && Bukkit.getOnlinePlayers().size >= CraftventureCore.getSettings().maxGuestCount) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_FULL,
                Component.text(
                    "Craftventure is currently too crowded, you might try it again later!",
                    CVTextColor.serverError
                )
            )
            executeSync { this.makeRoomForNewPlayers() }
        }

        if (canAlwaysJoin) {
//            if (Bukkit.getOnlinePlayers().size >= CraftventureCore.getSettings().maxGuestCount) {
//                val kickPlayer = Bukkit.getOnlinePlayers().filter { !it.isCrew() && !it.isVIP() }.random()
//                    ?: Bukkit.getOnlinePlayers().filter { !it.isCrew() }.random()
//                if (kickPlayer != null) {
//                    kickPlayer.kickPlayer("A crewmember took your spot :'(")
//                }
//            }
        } else if (player.isVIP()) {
            if (!canVipsJoin()) {
                Logger.info("Join denied: ${event.player.name} is a VIP but VIP slots are full")
                event.disallow(
                    PlayerLoginEvent.Result.KICK_FULL,
                    Component.text(
                        "Craftventure is currently too crowded, you might try it again later!",
                        CVTextColor.serverError
                    )
                )
                executeSync { this.makeRoomForNewPlayers() }
            }
        } else if (!canGuestsJoin()) {
            Logger.info("Join denied: ${event.player.name} is a guest but guest slots are full")
            event.disallow(
                PlayerLoginEvent.Result.KICK_FULL, Component.text(
                    "Craftventure is currently too crowded, you might try it again later!",
                    CVTextColor.serverError
                )
            )
            executeSync { this.makeRoomForNewPlayers() }
        }

        if (LocalDateTime.now() > FinaleTimer.endingDate)
            executeSync(20 * 3) {
                val offset = FinaleTimer.nextPlay!!.utcMillis - LocalDateTime.now().utcMillis
                if (offset > 5_000) {
                    player.sendMessage(
                        CVTextColor.serverNoticeAccent + "Next finale cinematic starting in ${
                            DateUtils.format(
                                offset,
                                "?"
                            )
                        }"
                    )
                }
            }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.scoreboard = Bukkit.getScoreboardManager().newScoreboard
        if (player.vehicle != null) {
            player.vehicle?.remove()
        }
        event.joinMessage(null)
        login(player)

        player.level = 0
        player.exp = 0f
        player.totalExperience = 0
        player.absorptionAmount = 0.0
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.apply {
            baseValue = defaultValue
        }

        EmojiHelper.updateEmojis(player)

        if (PluginProvider.environment == Environment.STAGING) {
            CrewFtp.requestStart()
        }
    }

    @EventHandler
    fun onPlayerDisconnect(event: PlayerConnectionCloseEvent) {
        if (Bukkit.getServer().onlinePlayers.isEmpty())
            CrewFtp.stop()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage(Component.empty())
        logout(event.player)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        logout(event.player)
    }

    private fun login(player: Player) {
        val start = System.currentTimeMillis()
        if (player.vehicle != null) {
            player.vehicle?.remove()
        }
        for (potionEffect in player.activePotionEffects) {
            player.removePotionEffect(potionEffect.type)
        }
        TabHeaderFooterManager.update(player)
        TeamsManager.update(player)
        MessageBarManager.require(player)
        TitleManager.require(player)
        MetaFactory.handleLogin(player)

        GameModeManager.setDefaults(player)
        player.requireEquippedItemsMeta()
        EquipmentManager.invalidatePlayerEquippedItems(player)

        ShutdownManager.trigger(player)

        player.isInvisible = false
        player.isInvulnerable = false

        val shouldWarp = !player.isCrew()
        val goToSpawn = !player.isVIP() && player.lastSeen < System.currentTimeMillis() - (15 * 60 * 1000)

        if (shouldWarp) {
            if (goToSpawn) {
                executeAsync {
                    val warp = MainRepositoryProvider.warpRepository.findCachedByName("spawn")
                    warp?.let {
                        executeSync {
                            warp.teleport(player)
                        }
                    }
                }
            } else {
                executeAsync {
                    val warp =
                        MainRepositoryProvider.warpRepository.getNearestWarpFromLocation(player.location, player)
                            ?: MainRepositoryProvider.warpRepository.findCachedByName("spawn")

                    warp?.let {
                        executeSync {
                            warp.teleport(player)
                        }
                    }
                }
            }
        }

        executeAsync(20 * 5) {
            val serverCoinboosters = MainRepositoryProvider.activeServerCoinBoosterRepository.count()
            val personalBoosters = MainRepositoryProvider.activeCoinBoosterRepository.getByPlayer(player.uniqueId).size
            if (serverCoinboosters > 0 || personalBoosters > 0)
                Translation.COINBOOSTER_SERVER_REMINDER_MESSAGE.getTranslation(
                    player,
                    serverCoinboosters,
                    personalBoosters
                )?.sendTo(player)

            CommandVipTrial.sendVipInvites(player)
        }

        if (System.currentTimeMillis() - start >= 5) {
            val ms = System.currentTimeMillis() - start
            ("Login for ${player.name} took ${ms}ms").broadcastAsDebugTimings()
        }

        MaxiFoto.cacheSkin(player)
    }

    private fun logout(player: Player) {
        val start = System.currentTimeMillis()
        player.leaveVehicle()
        try {
            CraftventureCore.getWornTitleManager().remove(player)
        } catch (e: Exception) {
            Logger.capture(e)
        }

        //        Logger.console("1 " + (System.currentTimeMillis() - start));
        try {
            TeamsManager.remove(player)
        } catch (e: Exception) {
            Logger.capture(e)
        }

        //        Logger.console("2 " + (System.currentTimeMillis() - start));
//        try {
//            CvMetadata.remove(player, true)
//        } catch (e: Exception) {
//            Logger.capture(e)
//        }

        MetaAnnotations.cleanup(player)
        EntityEvents.cleanup(player)

        //        Logger.console("3 " + (System.currentTimeMillis() - start));
        if (System.currentTimeMillis() - start > 5) {
            Logger.warn("Logout for ${player.name} took ${System.currentTimeMillis() - start}ms")
        }
    }

    companion object {

        val onlineVipCount: Int
            get() {
                var count = 0
                for (player in Bukkit.getOnlinePlayers()) {
                    if (PermissionChecker.isVIP(player) && !PermissionChecker.isCrew(player)) {
                        count++
                    }
                }
                return count
            }

        val onlineCrewCount: Int
            get() {
                var count = 0
                for (player in Bukkit.getOnlinePlayers()) {
                    if (PermissionChecker.isCrew(player)) {
                        count++
                    }
                }
                return count
            }

        val openCrewSlots: Int
            get() = Math.max(0, CraftventureCore.getSettings().crewReservedSlots - onlineCrewCount)

        val openVipSlots: Int
            get() = Math.max(0, CraftventureCore.getSettings().vipReservedSlots - onlineVipCount)

        fun canVipsJoin(): Boolean {
            return Bukkit.getOnlinePlayers().size < CraftventureCore.getSettings().maxGuestCount - openCrewSlots
        }

        fun canGuestsJoin(): Boolean {
            return Bukkit.getOnlinePlayers().size < CraftventureCore.getSettings().maxGuestCount -
                    openCrewSlots - openVipSlots
        }
    }
}
