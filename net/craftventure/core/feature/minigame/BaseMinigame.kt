package net.craftventure.core.feature.minigame

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.removeAllPotionEffects
import net.craftventure.bukkit.ktx.extension.sendTo
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.isAfk
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.manager.visibility.VisibilityManager
import net.craftventure.core.metadata.ConsumptionEffectTracker
import net.craftventure.core.serverevent.KartStartEvent
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.serverevent.ProvideLeaveInfoEvent
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.TimeUnit


abstract class BaseMinigame<META>(
    override val internalName: String,
    override val displayName: String,
    override val minRequiredPlayers: Int,
    val exitLocation: Location,
    val preparingTicks: Int = 20 * 15,
    val minKeepPlayingRequiredPlayers: Int = minRequiredPlayers,
    val subType: Minigame.SubType = Minigame.SubType.MINIGAME,
    val baseStoppingTicks: Int = 20 * 5,
    val description: String,
    val warpName: String?,
    val representationItem: ItemStack,
    val saveScores: Boolean = true,
) : Listener, Minigame {
    override var isFromConfig: Boolean = false
    internal var players = hashSetOf<MinigamePlayer<META>>()
        private set
    protected var state: Minigame.State = Minigame.State.IDLE
        protected set(value) {
            if (this.state != value) {
//                Logger.debug("State for $internalName set to $value")
                val oldState = this.state
                field = value
                onStateChanged(oldState, value)
            }
        }

    override fun represent(): ItemStack = representationItem

    override fun provideWarp(): Warp? = warpName?.let { MainRepositoryProvider.warpRepository.findCachedByName(it) }

    override fun describeGameplay(): String = description

    /**
     * In seconds
     */
    protected var overTime = 0L
    private var stoppingTicks = 0
    protected var startingTicks = 0
        private set
    var playStartTime = System.currentTimeMillis()
        private set
    protected var endAt: Long? = null
    protected var lastUpdate = System.currentTimeMillis()
        private set
    protected var displayTimeLeft = false
    override val timeLeft: Long?
        get() = maxGameTimeLength() - playTime
    override val isRunning: Boolean
        get() = state == Minigame.State.RUNNING || state == Minigame.State.STOPPING_GAME || state == Minigame.State.PREPARING_GAME
    override val playTime: Long
        get() = System.currentTimeMillis() - playStartTime

    init {
        Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance())
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), this::updateTick, 1L, 1L)
    }

    protected fun getPlayerByMeta(meta: META) = players.firstOrNull { it.metadata === meta }

    protected fun getMetaPlayer(player: Player) = players.firstOrNull { it.player === player }

    fun maxGameTimeLength() = levelBaseTimeLimit + TimeUnit.SECONDS.toMillis(overTime)

    open fun sortedPlayers(): List<MinigamePlayer<META>> = players.toList()

    override fun isPlaying(player: Player) = players.any { it.player === player }
    override fun isOnStandby(player: Player) = players.any { it.player === player && it.standby }

    protected open fun update(timePassed: Long) {}

    protected open fun updateTick() {
        for (p in players.toList()) {
            if (p.player.isAfk())
                leave(p.player, Minigame.LeaveReason.LEAVE)
        }

        if (state == Minigame.State.IDLE) {
            // Nothing
        } else if (state == Minigame.State.PREPARING_GAME) {
            overTime = 0
            if (startingTicks > 0)
                if (startingTicks % (5 * 20) == 0 || (startingTicks % 20 == 0 && startingTicks <= 5 * 20))
                    players.forEach { it.player.sendMessage(CVTextColor.serverNotice + "${startingTicks / 20} seconds to start..") }

            if (startingTicks >= 0)
                startingTicks--
            if (startingTicks <= 0) {
                state = Minigame.State.RUNNING
            }

        } else if (state == Minigame.State.RUNNING) {
            for (player in players) {
                if (!player.standby && (player.player.isFlying || player.player.allowFlight)) {
                    GameModeManager.setDefaultFly(player.player)
                    player.allowNextTeleport()
                    player.player.teleport(player.player.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
                }
            }

            val playTime = playTime
            val maxGameTimeLength = maxGameTimeLength()
            if (maxGameTimeLength in 1..playTime) {
                stop(Minigame.StopReason.OUT_OF_TIME)
            } else if (maxGameTimeLength > 0 && displayTimeLeft) {
                val timeLeft = maxGameTimeLength - playTime
                for (player in players) {
                    display(
                        player.player,
                        Message(
                            id = ChatUtils.ID_MINIGAME,
                            text = Component.text(
                                "Game ending in ${DateUtils.format(timeLeft, "?")}",
                                CVTextColor.serverNotice
                            ),
                            type = MessageBarManager.Type.MINIGAME,
                            untilMillis = TimeUtils.secondsFromNow(1.0),
                        ),
                        replace = true,
                    )
                }
            }

        } else if (state == Minigame.State.STOPPING_GAME) {
            stoppingTicks--
            if (stoppingTicks <= 0) {
                state = Minigame.State.IDLE
            }
        }

        val now = System.currentTimeMillis() - lastUpdate
        update(now)
    }

    protected open fun onStateChanged(oldState: Minigame.State, newState: Minigame.State) {
//        Logger.console("State of $internalName changed to $newState (was $oldState)")

        when (newState) {
            Minigame.State.IDLE -> {
                players.forEach {
                    onPlayerLeft(it, Minigame.LeaveReason.GAME_STOPPING)
                }
                players = hashSetOf()
            }

            Minigame.State.STOPPING_GAME -> stoppingTicks = baseStoppingTicks
            Minigame.State.PREPARING_GAME -> {
                startingTicks = preparingTicks
            }

            Minigame.State.RUNNING -> playStartTime = System.currentTimeMillis()
        }

        for (player in players) {
            EquipmentManager.reapply(player.player)
        }
    }

    protected open fun onUpdatePlayerWornItems(player: MinigamePlayer<META>, event: PlayerEquippedItemsUpdateEvent) {}

//    @EventHandler(priority = EventPriority.NORMAL)
//    fun onPlayerRespawn(event: PlayerRespawnEvent) {
//        players.firstOrNull { it.player == event.player }?.let {
//            if (isRunning()) {
//            } else {
//                event.respawnLocation = exitLocation
//            }
//        }
//    }

    private fun onPlayersUpdated() {
//        Logger.debug("Players updated: ${players.size} of $minKeepPlayingRequiredPlayers")
        if (players.size < minKeepPlayingRequiredPlayers) {
//            Logger.debug("Stopping game")
            stop(Minigame.StopReason.TOO_FEW_PLAYERS)
        }
    }

    protected open fun onPlayerJoined(player: MinigamePlayer<META>) {
        player.player.isFlying = false
        player.player.allowFlight = false
        player.player.isGliding = false

        VisibilityManager.broadcastChangesFrom(player.player)
    }

    protected open fun onPlayerLeft(minigamePlayer: MinigamePlayer<META>, reason: Minigame.LeaveReason) {
        if (!minigamePlayer.standby) {
            minigamePlayer.player.leaveVehicle()
            executeSync {
//                Logger.debug("Teleporting ${minigamePlayer.player.name} to minigame exit")
                minigamePlayer.allowNextTeleport()
                minigamePlayer.player.teleport(exitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                EquipmentManager.reapply(minigamePlayer.player)
                GameModeManager.setDefaults(minigamePlayer.player)
                VisibilityManager.broadcastChangesFrom(minigamePlayer.player)
            }
        }
    }

    override fun leave(player: Player, reason: Minigame.LeaveReason): Boolean {
        val minigamePlayer = players.firstOrNull { it.player === player } ?: return false
//        Logger.debug("Players: ${players.size}: ${players.joinToString(", ") { "${it.player.name}/${it.player === player}" }}")
        players = players.filter { it.player !== player }.toHashSet()
        Logger.debug("Players: ${players.size} after ${player.name} left $displayName", logToCrew = false)
        players.forEach {
            Translation.MINIGAME_PLAYER_LEFT.getTranslation(minigamePlayer.player, player.name, displayName)
                ?.sendTo(it.player)
        }

        onPlayerLeft(minigamePlayer, reason)
        onPlayersUpdated()

        return true
    }

    override fun canStart(): Boolean = !isRunning

    override fun start(players: List<Player>): Boolean {
        if (players.size < minKeepPlayingRequiredPlayers || players.size < minRequiredPlayers) return false
        if (!canStart()) return false
        val result = players.shuffled().map { join(it, false) }.any { it }
        if (result) {
            state = Minigame.State.PREPARING_GAME
        }
        return result
    }

    override fun canJoin(player: Player): Boolean = state == Minigame.State.IDLE

    override fun join(player: Player, announce: Boolean): Boolean {
        if (players.any { it.player === player }) return false
        if (!canJoin(player)) return false
        player.getMetadata<ConsumptionEffectTracker>()?.clearAll()
        player.removeAllPotionEffects()
        player.leaveVehicle()
        player.spigot().respawn()
        onPreJoin(player)
        val meta = provideMeta(player)
        val minigamePlayer = MinigamePlayer(player, meta)
        players = (players + minigamePlayer).toHashSet()
        EquipmentManager.reapply(player)
        onPlayerJoined(minigamePlayer)

        if (announce) {
            val message = CVTextColor.serverNotice + "${player.name} has joined"
            players.forEach { it.player.sendMessage(message) }
        }

        return true
    }

    protected abstract fun provideMeta(player: Player): META
    protected open fun onPreJoin(player: Player) {}
    protected open fun onPreStop(stopReason: Minigame.StopReason) {}
    protected open fun onStopped(stopReason: Minigame.StopReason) {}

    override fun stop(reason: Minigame.StopReason): Boolean {
        if (state == Minigame.State.RUNNING || state == Minigame.State.PREPARING_GAME) {
            onPreStop(reason)

            state = if (reason == Minigame.StopReason.ALL_PLAYERS_FINISHED ||
                reason == Minigame.StopReason.ALL_PLAYERS_FINISHED ||
                reason == Minigame.StopReason.OUT_OF_TIME ||
                reason == Minigame.StopReason.FAILURE
            )
                Minigame.State.STOPPING_GAME
            else
                Minigame.State.IDLE
            Logger.debug("Stopping with reason $reason")
            onStopped(reason)
            return true
        }
        return false
    }

    override fun destroy() {
        stop(Minigame.StopReason.SERVER_STOP)
        players = hashSetOf()
    }

    @EventHandler(ignoreCancelled = true)
    fun onStartKarting(event: KartStartEvent) {
        if (players.any { it.player === event.player && !it.standby }) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (event.cause == PlayerTeleportEvent.TeleportCause.UNKNOWN) return
//        Logger.console("${isRunning()} $state")
        if (!isRunning) return
//        Logger.console("Teleporting player")

        val player = players.firstOrNull { it.player === event.player && !it.standby }
        if (player != null) {
            if (!player.allowNextTeleport) {
//                Logger.debug("Denying teleport for ${event.player.name} ${event.cause}")
                event.isCancelled = true
            } else {
//                Logger.debug("Allowing teleport for ${event.player.name} ${event.cause}")
            }
            player.teleported()
        }
    }

    @EventHandler
    fun onPlayerWornItemsChanged(event: PlayerEquippedItemsUpdateEvent) {
        players.firstOrNull {
            it.player === event.player && !it.standby
        }?.let {
            onUpdatePlayerWornItems(it, event)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        leave(event.player, Minigame.LeaveReason.LEAVE)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        leave(event.player, Minigame.LeaveReason.LEAVE)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        players.firstOrNull {
            it.player === event.player && !it.standby
        }?.apply {
            event.isCancelled = true
//            Logger.info("Flying disabled for ${event.player.name}")
            player.isFlying = false
            player.allowFlight = false
//                allowNextTeleport()
            allowNextTeleport()
            player.teleport(player.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerToggleGlide(event: EntityToggleGlideEvent) {
        if (event.isGliding) return

        val eventPlayer = event.entity as? Player ?: return
        players.firstOrNull {
            it.player === eventPlayer && !it.standby
        }?.apply {
            event.isCancelled = true

//                allowNextTeleport()
            allowNextTeleport()
            player.teleport(player.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    open fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (players.any { it.player === event.entity && !it.standby }) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    open fun onRequestLeaveCommand(event: ProvideLeaveInfoEvent) {
        if (players.none { it.player === event.player }) return

        event.data.add(
            ProvideLeaveInfoEvent.Entry(
                ProvideLeaveInfoEvent.Category.Minigame,
                "Leave minigame ${displayName}",
                representation = represent(),
            ) {
                leave(event.player, Minigame.LeaveReason.LEAVE)
            })
    }

    abstract fun toJson(): Json

    open fun <T : Json> toJson(source: T): T {
        return source
    }

    open fun <T : Json> restore(source: T) {
    }

    internal class Counter {
        private var startAt: Long = System.currentTimeMillis()
        private var previousUpdateAt = 0L

        fun range(): LongRange {
            val range = LongRange(previousUpdateAt, System.currentTimeMillis() - startAt)
            previousUpdateAt = range.endInclusive
            return range
        }

        fun reset() {
            startAt = System.currentTimeMillis()
            previousUpdateAt = 0
        }
    }

    abstract class Json : Minigame.Json() {
        var preparingTicks: Int = 20 * 15
        var minKeepPlayingRequiredPlayers: Int = minRequiredPlayers
        var subType: Minigame.SubType = Minigame.SubType.MINIGAME
        var baseStoppingTicks: Int = 20 * 5
    }
}