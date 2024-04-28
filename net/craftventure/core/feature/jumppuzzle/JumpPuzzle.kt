package net.craftventure.core.feature.jumppuzzle

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.event.PlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.markAsWornItem
import net.craftventure.core.feature.kart.KartManager
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.manager.PlayerStateManager.gameState
import net.craftventure.core.metadata.ConsumptionEffectTracker
import net.craftventure.core.serverevent.KartStartEvent
import net.craftventure.core.serverevent.OwnedItemConsumeEvent
import net.craftventure.core.serverevent.OwnedItemUseEvent
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.type.MinigameScoreType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

open class JumpPuzzle(
    val gameId: String? = null,
    val displayName: String,
    val description: String,
    val playArea: Area,
    val startingArea: Area,
    val rewardArea: Area,
    val rewardCallback: RewardCallback,
//        val checkPoints: List<JumpPuzzleCheckPoint> = emptyList()
    val listener: PuzzleListener? = null,
    val publicAfter: Date? = null,
    val enterCallback: ((Player) -> Unit)? = null,
    val exitCallback: ((Player) -> Unit)? = null
) : Listener {
    //    private val hasStarted = false
    val players = CopyOnWriteArrayList<JumpPuzzlePlayer>()
    private var task = -1

    operator fun contains(player: Player): Boolean = players.any { it.player === player }

    fun start() {
        if (task != -1)
            return

        Bukkit.getServer().pluginManager.registerEvents(this, CraftventureCore.getInstance())
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), this::update, 1L, 1L)
    }

    fun stop() {
        if (task == -1)
            return

        HandlerList.unregisterAll(this)
        Bukkit.getScheduler().cancelTask(task)

        task = -1
    }

    fun getData(player: Player) = players.firstOrNull { it.player === player }
    fun isPlaying(player: Player) = getData(player) != null

    private fun blockFlying(player: Player) {
        if (!PermissionChecker.isCrew(player)) {
            val wasFlying = player.isFlying || player.allowFlight || player.isGliding
            player.isFlying = false
            player.allowFlight = false
            if (wasFlying) {
                player.sendMessage(Translation.NOFLYZONE_BLOCKED.getTranslation(player)!!)
                player.teleport(player.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
            }
        }
    }

    private var tick = 0
    private fun update() {
        tick++
        if (tick > 10)
            tick = 0

        val removes = mutableListOf<JumpPuzzlePlayer>()
        for (player in players) {
            listener?.onPlayerUpdate(player)
            if (player.player.isGliding)
                player.player.isGliding = false
            if (player.player.isFlying || player.player.allowFlight) {
                blockFlying(player.player)
            }
            if (player.player.isInsideVehicle) {
                if (player.qualified) {
                    player.player.sendTitleWithTicks(
                        10, 20 * 4, 10,
                        NamedTextColor.DARK_RED, "Disqualified",
                        NamedTextColor.RED, "You entered a vehicle"
                    )
                    player.qualified = false
                    removes.add(player)
                }
            }
            if (tick == 0) {
                if (player.qualified) {
                    display(
                        player.player,
                        Message(
                            id = ChatUtils.ID_MINIGAME,
                            text = Component.text(
                                "Current try ${
                                    DateUtils.format(
                                        System.currentTimeMillis() - player.startTime,
                                        "00s"
                                    )
                                }", CVTextColor.serverNotice
                            ),
                            type = MessageBarManager.Type.NOTICE,
                            untilMillis = TimeUtils.secondsFromNow(1.0),
                        ),
                        replace = true,
                    )
                } else {
                    display(
                        player.player,
                        Message(
                            id = ChatUtils.ID_MINIGAME,
                            text = Component.text(
                                "Explorer mode, return to start to start over",
                                CVTextColor.serverNotice
                            ),
                            type = MessageBarManager.Type.WARNING,
                            untilMillis = TimeUtils.secondsFromNow(1.0),
                        ),
                        replace = true,
                    )
                }
            }

            if (rewardArea.isInArea(player.player)) {
                player.qualified = false
                reward(player)
                removes.add(player)
            }
        }
        for (remove in removes) {
            remove(remove.player)
            players.remove(remove)
        }

        if (tick == 0) {
            // check players
        }
        onUpdate()
    }

    private fun reward(player: JumpPuzzlePlayer) {
//        info("${player.player.name} finished jumppuzzle $displayName", logToCrew = false)

        val time = System.currentTimeMillis() - player.startTime
        val score = if (gameId != null) MinigameScore(
            UUID.randomUUID(),
            player.player.uniqueId,
            gameId,
            time.toInt(),
            LocalDateTime.now(),
            MinigameScoreType.TOTAL,
            null,
            player.player.isCrew()
        )
        else null
        listener?.onStop(player)
        rewardCallback.onReward(this, player, score)

        player.player.sendTitleWithTicks(
            10, 20 * 4, 10,
            NamedTextColor.GOLD, "$displayName finished!",
            NamedTextColor.YELLOW, "With a time of ${DateUtils.formatWithMillis(time, "?")}"
        )

        if (score != null) {
            executeAsync {
                if (!MainRepositoryProvider.minigameScoreRepository.createSilent(score)) {
                    Logger.severe(
                        "Failed to save $displayName score for player ${player.player.name} with score ${score.score}ms",
                        logToCrew = false
                    )
                }
            }
        }
    }

    open fun onUpdate() {}

    @EventHandler(ignoreCancelled = true)
    fun onStartKarting(event: KartStartEvent) {
        if (players.any { it.player === event.player }) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        players.firstOrNull {
            it.player === event.player
        }?.apply {
            if (!player.isCrew()) {
                event.isCancelled = true
                player.isFlying = false
                player.allowFlight = false
                player.teleport(player.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
            } else {
                player.sendMessage(CVTextColor.serverNotice + "Remember, flight is not allowed during this jumping puzzle, but as crew you can still fly!")
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerToggleGliding(event: EntityToggleGlideEvent) {
        if (!event.isGliding) return

        val eventPlayer = event.entity as? Player ?: return
        players.firstOrNull {
            it.player === eventPlayer
        }?.apply {
            if (!player.isCrew()) {
                event.isCancelled = true

                player.isGliding = false
                player.teleport(player.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
            } else {
                player.sendMessage(CVTextColor.serverNotice + "Remember, gliding is not allowed during this jumping puzzle, but as crew you can still fly!")
            }
        }
    }

    private fun remove(player: Player) {
        players.firstOrNull { it.player == player }?.apply {
            this.cleanUp()
            players.remove(this)
            listener?.onStop(this)
//            Logger.console("${player.name} exited $displayName")

            if (qualified) {
                player.sendTitleWithTicks(
                    10, 20 * 4, 10,
                    NamedTextColor.DARK_RED, "You failed",
                    NamedTextColor.RED, "You left $displayName"
                )
            }
            GameModeManager.setDefaultFly(player)
            exitCallback?.invoke(player)
            player.gameState()?.jumpPuzzle = null
            EquipmentManager.reapply(player)
        }
    }

    private fun add(player: Player) {
        if (!players.any { it.player == player }) {
            player.isFlying = false
            player.allowFlight = false
            player.isGliding = false

            KartManager.kartForPlayer(player)?.destroy()

            player.getMetadata<ConsumptionEffectTracker>()?.clearAll()
            player.clearActivePotionEffects()

            val puzzlePlayer = JumpPuzzlePlayer(player, qualified = true)
            players.add(puzzlePlayer)
            listener?.onStart(puzzlePlayer)
//            Logger.console("${player.name} entered $displayName")

            player.displayTitle(
                TitleManager.TitleData.ofTicks(
                    id = "jumppuzzle",
                    title = CVTextColor.serverNoticeAccent + displayName,
                    subtitle = CVTextColor.serverNotice + description,
                    fadeInTicks = 10,
                    stayTicks = 20 * 4,
                    fadeOutTicks = 10,
                ),
                replace = true,
            )
            enterCallback?.invoke(player)
            player.gameState()?.jumpPuzzle = this
            EquipmentManager.reapply(player)
        }
    }

    private fun check(player: Player) {
        val isJoinEnabled = FeatureManager.isFeatureEnabled(FeatureManager.Feature.JUMP_PUZZLE_JOIN)
        if (!isJoinEnabled && players.isEmpty()) return
        val inPuzzle = players.any { it.player == player }
        if (inPuzzle) {
            if (!playArea.isInArea(player)) {
                remove(player)
            }
        } else {
            if (!isJoinEnabled) return
            if (startingArea.isInArea(player)) {
                if (publicAfter != null) {
                    val now = Date()
                    if (now.before(publicAfter) && !player.isCrew()) {
                        return
                    }
                }
                add(player)
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        remove(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = players.firstOrNull { it.player === event.player }
        if (player != null) {
            if (player.qualified) {
                val distance = event.from.distance(event.to)
//                Logger.debug("Distance ${distance.format(2)}")

                if (distance >= 4) {
                    event.player.displayTitle(
                        TitleManager.TitleData.ofTicks(
                            id = "jumppuzzle",
                            title = NamedTextColor.DARK_RED + "Disqualified",
                            subtitle = NamedTextColor.RED + "You (were) teleported",
                            fadeInTicks = 10,
                            stayTicks = 20 * 4,
                            fadeOutTicks = 10,
                        ),
                        replace = true,
                    )
                    player.qualified = false
                    remove(player = player.player)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerLocationChangedEvent) {
        if (event.locationChanged)
            check(event.player)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player) {
            val ingame = players.any { it.player === entity }
            if (ingame) {
                if (event.cause == EntityDamageEvent.DamageCause.FALL) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onOwnedItemConsume(event: OwnedItemConsumeEvent) {
        if (players.any { it.player === event.player }) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    fun onOwnedItemUse(event: OwnedItemUseEvent) {
        if (players.any { it.player === event.player }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerWornItemsChanged(event: PlayerEquippedItemsUpdateEvent) {
        players.firstOrNull { it.player === event.player }?.let { player ->
            if (event.appliedEquippedItems.chestplateItem?.itemStack?.type == Material.ELYTRA)
                event.appliedEquippedItems.chestplateItem =
                    ItemStack(Material.AIR).markAsWornItem().toEquippedItemData("elytra_replacement")
        }
    }

    interface RewardCallback {
        fun onReward(puzzle: JumpPuzzle, player: JumpPuzzlePlayer, score: MinigameScore? = null)
    }

    @JsonClass(generateAdapter = true)
    class Json(
        val game_id: String,
        val display_name: String,
        val description: String,
        val play_area: Area.Json,
        val starting_area: Area.Json,
        val reward_area: Area.Json,
        val achievements: List<String>?,
        val items: List<String>?
    ) {
        fun create() = JumpPuzzle(
            gameId = game_id,
            displayName = display_name,
            description = description,
            playArea = play_area.create(),
            startingArea = starting_area.create(),
            rewardArea = reward_area.create(),
            rewardCallback = object : RewardCallback {
                override fun onReward(puzzle: JumpPuzzle, player: JumpPuzzlePlayer, score: MinigameScore?) {
                    val hasAchievements = achievements?.isNotEmpty() ?: false
                    val hasItems = items?.isNotEmpty() ?: false
                    if (hasAchievements || hasItems)
                        executeAsync {
                            achievements?.forEach { achievement ->
                                MainRepositoryProvider.achievementProgressRepository
                                    .reward(player.player.uniqueId, achievement)
                            }
                            items?.forEach { item ->
                                MainRepositoryProvider.playerOwnedItemRepository
                                    .createOneLimited(player.player.uniqueId, item, -1)
                            }
                        }
                }
            }
        )
    }

    interface PuzzleListener {
        fun onPlayerUpdate(puzzlePlayer: JumpPuzzlePlayer) {}
        fun onStart(puzzlePlayer: JumpPuzzlePlayer) {}
        fun onStop(puzzlePlayer: JumpPuzzlePlayer) {}
    }
}
