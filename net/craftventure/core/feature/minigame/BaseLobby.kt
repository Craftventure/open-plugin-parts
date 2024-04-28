package net.craftventure.core.feature.minigame

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.entitymeta.requireMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.extension.sendTo
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.isAfk
import net.craftventure.core.feature.kart.KartManager
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.Deny
import net.craftventure.core.manager.PlayerStateManager.gameState
import net.craftventure.core.manager.PlayerStateManager.isAllowedToJoinMinigameLobby
import net.craftventure.core.manager.visibility.VisibilityManager
import net.craftventure.core.metadata.GenericPlayerMeta
import net.craftventure.core.serverevent.KartStartEvent
import net.craftventure.core.serverevent.ProvideLeaveInfoEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.time.OffsetDateTime
import java.util.*
import kotlin.math.ceil

open class BaseLobby(
    val nonVipAfter: Date? = null,
    val nonCrewAfter: Date? = null,
    val allowPreQueueing: Boolean = true,
    val crewOnly: Boolean = false,
    override val minigame: Minigame,
    override val id: String
) : Listener, Lobby {
    private var countdownTicks: Int = 0
    override var state: Lobby.State = Lobby.State.IDLE
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onStateChanged(value, old)
            }
        }
    override val queuedCount: Int
        get() = players.size
    private var players = hashSetOf<Player>()
    private val listeners = hashSetOf<Lobby.Listener>()
    private val task: Int

    init {
        Bukkit.getServer().pluginManager.registerEvents(this, CraftventureCore.getInstance())
        task = executeSync(1, 1, this::update)
    }

    override fun destroy() {
        players.forEach {
            it.sendMessage(CVTextColor.serverError + "The lobby for ${minigame.displayName} has been destroyed")
        }
        HandlerList.unregisterAll(this)
        Bukkit.getScheduler().cancelTask(task)
    }

    override fun isQueued(player: Player): Boolean = players.contains(player)
    override fun addListener(listener: Lobby.Listener): Boolean = listeners.add(listener)
    override fun removeListener(listener: Lobby.Listener): Boolean = listeners.remove(listener)

    private fun triggerListenerUpdate() {
        listeners.forEach { it.onUpdated(this) }
    }

    private fun onStateChanged(new: Lobby.State, old: Lobby.State) {
        triggerListenerUpdate()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        tryLeave(event.player)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        tryLeave(event.player)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onRequestLeaveCommand(event: ProvideLeaveInfoEvent) {
        if (players.none { it === event.player }) return
        event.data.add(
            ProvideLeaveInfoEvent.Entry(
                ProvideLeaveInfoEvent.Category.Minigame,
                "Leave lobby for minigame ${minigame.displayName}",
                representation = minigame.represent(),
            ) {
                tryLeave(event.player)
            })
    }

    @EventHandler(ignoreCancelled = true)
    fun onStartKarting(event: KartStartEvent) {
        if (players.any { it === event.player }) {
            event.isCancelled = true
        }
    }

    override fun tryJoin(player: Player): Boolean {
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.MINIGAME_JOIN)) {
            player.sendMessage(CVTextColor.serverNotice + "Minigames have been temporarily disabled")
            return false
        }
        if (crewOnly && !player.isCrew()) {
            player.sendMessage(CVTextColor.serverNotice + "This game is still in development")
            return false
        }
//        Logger.info("${player.name} trying to join ${lobbyListener?.minigame?.name}")
        if (!allowPreQueueing && minigame.isRunning) {
            Translation.MINIGAME_RUNNING.getTranslation(player)?.sendTo(player)
            return false
        }
        nonVipAfter?.let {
            val now = Date()
            if (!player.isVIP() && now.before(nonVipAfter)) {
                val between = nonVipAfter.time - now.time

                player.sendMessage(
                    CVTextColor.vip + "This minigame is currently being tested by VIP's, come back after ${
                        DateUtils.format(
                            between,
                            "?"
                        )
                    }"
                )
                return false
            }
        }
        nonCrewAfter?.let {
            val now = Date()
            if (!player.isCrew() && now.before(nonCrewAfter)) {
                val between = nonCrewAfter.time - now.time

                player.sendMessage(
                    CVTextColor.serverError + "This minigame is currently not opened yet, come back after ${
                        DateUtils.format(
                            between,
                            "?"
                        )
                    }"
                )
                return false
            }
        }
        if (CraftventureCore.getInstance().isShuttingDown) {
            Translation.SHUTDOWN_PREPARING.getTranslation(player)?.sendTo(player)
            return false
        }
        val meta = player.requireMetadata<GenericPlayerMeta>()
        if (meta.lastGameJoinTime > System.currentTimeMillis() - 2000) {
            player.sendMessage(CVTextColor.serverError + "You can only join a game every 2 seconds")
            return false
        }
        val result = player.isAllowedToJoinMinigameLobby()
        if (result is Deny) {
            player.sendMessage(result.errorComponent)
            return false
        }
        if (player.isInsideVehicle) {
            Translation.MINIGAME_LOBBY_VEHICLE_BANNED.getTranslation(player)?.sendTo(player)
            return false
        }
        if (players.size >= maxPlayers) {
            Translation.MINIGAME_LOBBY_FULL.getTranslation(player, minigame.displayName)?.sendTo(player)
            return false
        }
        if (players.contains(player)) {
            Translation.MINIGAME_LOBBY_ALREADY_JOINED.getTranslation(player)?.sendTo(player)
            return false
        }
        val otherGame = MinigameManager.getParticipatingGame(player)
        if (otherGame != null) {
            Translation.MINIGAME_LOBBY_ALREADY_JOINED_OTHER.getTranslation(player, otherGame.displayName)
                ?.sendTo(player)
            return false
        }
        if (player.isAfk()) {
            player.sendMessage(CVTextColor.serverNotice + "You think it's a good idea to join a minigame while being AFK?")
            return false
        }

        if (minigame.isRunning && minigame.canJoin(player)) {
            if (minigame.join(player, true)) {
                return true
            }
        }

        meta.lastGameJoinTime = System.currentTimeMillis()
        players.add(player)

        KartManager.cleanupParkedKart(player)
        val joinTranslation = Translation.MINIGAME_LOBBY_PLAYER_JOINED
        players.forEach { joinTranslation.getTranslation(it, player.name, minigame.displayName)?.sendTo(it) }

        triggerListenerUpdate()
        VisibilityManager.broadcastChangesFrom(player)

        player.gameState()?.minigameLobby = this

        return true
    }

    override fun tryLeave(player: Player): Boolean {
        val result = players.remove(player)

        if (result) {
            val leaveTranslation = Translation.MINIGAME_LOBBY_PLAYER_LEFT
            players.forEach {
                leaveTranslation.getTranslation(it, player.name, minigame.displayName)?.sendTo(it)
            }
            leaveTranslation.getTranslation(player, player.name, minigame.displayName)?.sendTo(player)

            triggerListenerUpdate()
            VisibilityManager.broadcastChangesFrom(player)

            player.gameState()?.minigameLobby = null
        }

        return result
    }

    fun isReadyForStart(): Boolean {
        val size = players.size
        return size in minigame.minRequiredPlayers..minigame.maxPlayers
    }

    private fun update() {
        for (p in players) {
            if (p.isAfk())
                tryLeave(p)
        }

        if (minigame.isRunning) {
            if (state != Lobby.State.RUNNING)
                state = Lobby.State.RUNNING
        } else {
            if (state == Lobby.State.RUNNING) {
                state = Lobby.State.IDLE
            }
            if (state == Lobby.State.IDLE) {
                if (isReadyForStart()) {
                    restartCountdown()
                }
            }
        }

        if (state == Lobby.State.COUNTDOWN_TO_START) {
            if (!isReadyForStart()) {
                state = Lobby.State.IDLE
            } else {
                if (queuedCount >= maxPlayers)
                    countdownTicks = 0
                if (countdownTicks > 0)
                    if (countdownTicks % (5 * 20) == 0 || (countdownTicks % 20 == 0 && countdownTicks <= 5 * 20))
                        players.take(maxPlayers).forEach {
                            Translation.MINIGAME_LOBBY_STARTING_IN.getTranslation(
                                it,
                                minigame.displayName,
                                ceil(countdownTicks / 20.0).toInt()
                            )?.sendTo(it)
                        }
                if (countdownTicks <= 0) {
                    tryStart()
                }
                countdownTicks--
            }
        }
    }

    private fun tryStart() {
        if (minigame.canStart()) {
            val startingPlayers = players.take(maxPlayers)
            if (minigame.start(startingPlayers)) {
                Logger.debug("Triggered minigame start")
                startingPlayers.forEach { it.gameState()?.minigameLobby = null }
                players.removeAll(startingPlayers)
                triggerListenerUpdate()
            } else {
                Logger.debug("Failed to trigger minigame start")
            }
        } else {
            restartCountdown()
        }
    }

    private fun restartCountdown() {
        state = Lobby.State.COUNTDOWN_TO_START
        countdownTicks = 20 * 20
    }

    fun onGameStarted() {
        players.clear()
        update()
    }

    fun serverLeave(player: Player) {
        players.remove(player)
        update()
    }

    open fun toJson(): Json {
        return toJson(Json())
    }

    open fun <T : Json> toJson(source: T): T {
        return source
    }

    open fun <T : Json> restore(source: T) {
    }

    @JsonClass(generateAdapter = true)
    open class Json : Lobby.Json() {
        var nonVipAfter: OffsetDateTime? = null
        var nonCrewAfter: OffsetDateTime? = null
        var allowPreQueueing: Boolean = true
        var crewOnly: Boolean = false
        lateinit var id: String

        override fun create(minigame: Minigame): Lobby = BaseLobby(
            nonVipAfter = nonVipAfter?.let { Date(it.toEpochSecond() * 1000) },
            nonCrewAfter = nonCrewAfter?.let { Date(it.toEpochSecond() * 1000) },
            allowPreQueueing = allowPreQueueing,
            crewOnly = crewOnly,
            minigame = minigame,
            id = id
        )
    }
}