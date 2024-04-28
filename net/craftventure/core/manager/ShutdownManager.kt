package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.manager.BossBarManager
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.PlayerStateManager.gameState
import net.craftventure.core.manager.PlayerStateManager.isInMiniGame
import net.craftventure.core.manager.PlayerStateManager.isInRide
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.Ride
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import java.time.Duration
import java.util.concurrent.TimeUnit

object ShutdownManager : Listener {
    private var taskId: Int? = null
    private var currentStateTicks = 0
    private var ticksSinceLastMessage = 0
    private var state: State? = null
    private var shutdownTime: Long = 0

    private var shutdownSeconds = 0
    var shuttingDown = false
        private set(value) {
            if (field != value) {
                field = value
                Bukkit.getOnlinePlayers().forEach { trigger(it) }
            }
            force = false
        }

    var force = false

    fun startShuttingDown(force: Boolean) {
        if (taskId == null) {
            ticksSinceLastMessage = 0
            state = State.WAITING_FOR_RIDES
            shuttingDown = true

            Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance())

            sendTranslation(Translation.SHUTDOWN_EMPTYING_RIDES)
            sendTitle(subtitle = Translation.SHUTDOWN_START_TITLE)

            taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
                update()
            }, 1L, 1L)
        }
        this.force = force
    }

    fun trigger(player: Player) {
        if (shuttingDown && state == State.WAITING_FOR_RIDES)
            BossBarManager.display(
                player,
                BossBarManager.Message(
                    "shutdown",
                    arrayOf(
                        Component.text(
                            "Craftventure is going to restart or into maintenance mode",
                            CVTextColor.serverError
                        ),
//                        if (state == State.COUNTDOWN)
//                            Component.text(
//                                "Restarting in ${
//                                    TimeUnit.MILLISECONDS.toSeconds(shutdownTime - System.currentTimeMillis()).toInt()
//                                } seconds",
//                                CVTextColor.serverError
//                            )
//                        else
                        Component.text(
                            "Waiting for ${getRideCount()} ride(s) and ${getMinigameCount()} minigame(s) to finish",
                            CVTextColor.serverError
                        ),
                    ),
                    priority = BossBarManager.Priority.shutdown,
                    untilMillis = Long.MAX_VALUE,
                ),
                replace = true
            )
        else
            BossBarManager.remove(player, "shutdown")
    }

    private fun getMinigameCount() = Bukkit.getOnlinePlayers().count { it.isInMiniGame() }
    private fun getRideCount() = Bukkit.getOnlinePlayers().count { it.isInRide() }

    private fun update() {
        if (!shuttingDown) {
            cancelShuttingDown()
            return
        }

        if (state == State.WAITING_FOR_RIDES) {
            currentStateTicks++
            ticksSinceLastMessage++

            if (ticksSinceLastMessage % 60 == 0) {
                val players = Bukkit.getOnlinePlayers()
                players.forEach { trigger(it) }
            }

            if (ticksSinceLastMessage > 20 * 30) {
                ticksSinceLastMessage = 0
                Logger.info("Restart is still scheduled, waiting for empty rides", logToCrew = false)
                for (player in Bukkit.getOnlinePlayers()) {
                    player.sendMessage(Translation.SHUTDOWN_EMPTYING_RIDES.getTranslation(player)!!)
                    sendTitle(subtitle = Translation.SHUTDOWN_START_TITLE)
                }
            }

            val players = Bukkit.getOnlinePlayers().map { it to it.gameState() }
            val count = players.count { it.second?.ride != null || it.second?.minigame != null }

            if (count == 0 || force) {
                val message =
                    CVTextColor.serverNotice + "You were ejected from your current ride as the server is shutting down/restarting. Your ride counter has been increased though!"
                if (force) {
                    val counters = mutableListOf<Pair<Ride, Player>>()
                    players.forEach {
                        val ride = it.second?.ride
                        if (ride != null) {
                            counters.add(ride.ride!! to it.first)
                            it.first.leaveVehicle()
                        }
                    }
                    executeAsync {
                        val database = MainRepositoryProvider.rideCounterRepository
                        counters.forEach { (ride, player) ->
                            database.increaseCounter(player.uniqueId, ride.name!!)
                            player.sendMessage(message)
                        }
                    }
                }
                moveToState(State.COUNTDOWN)
            }
        } else if (state == State.COUNTDOWN) {
            currentStateTicks++
            ticksSinceLastMessage++
            val millisUntilShutdown = shutdownTime - System.currentTimeMillis()

            val secondsLeft = TimeUnit.MILLISECONDS.toSeconds(millisUntilShutdown).toInt()
            if (shutdownSeconds != secondsLeft) {
                shutdownSeconds = secondsLeft
                ticksSinceLastMessage = 0

                val players = Bukkit.getOnlinePlayers()
                players.forEach { trigger(it) }

                if (shutdownSeconds == 5) {
                    Bukkit.getOnlinePlayers().forEach {
                        TitleManager.remove(it, "shutdown")
                        it.displayTitle(
                            TitleManager.TitleData.ofFade(
                                id = "shutdown",
                                type = TitleManager.Type.Shutdown,
                                fadeIn = Duration.ofSeconds(5),
                                stay = Duration.ofSeconds(3),
                                fadeOut = Duration.ofSeconds(5),
                            ),
                            replace = true,
                        )
                    }
                }

                if (shutdownSeconds % 5 == 0 || shutdownSeconds <= 5) {
                    Bukkit.getConsoleSender().sendMessage(
                        Translation.SHUTDOWN_IN_X_SECONDS.getTranslation(
                            null,
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilShutdown)
                        )!!
                    )
                }

                for (player in Bukkit.getOnlinePlayers()) {
                    val translation = Translation.SHUTDOWN_IN_X_SECONDS.getTranslation(
                        player,
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilShutdown)
                    )
                    if (shutdownSeconds % 5 == 0 || shutdownSeconds <= 5)
                        player.sendMessage(translation!!)
//                    player.displayTitle(
//                        TitleManager.TitleData.ofTicks(
//                            title = null,
//                            subtitle = translation,
//                            fadeInTicks = 0,
//                            stayTicks = 5 * 20,
//                            fadeOutTicks = 20,
//                        )
//                    )
                }
            }

            if (millisUntilShutdown < 0) {
                for (player in Bukkit.getOnlinePlayers()) {
                    player.kick(Translation.SHUTDOWN_KICKED.getTranslation(player))
                }
                Bukkit.getServer().spigot().restart()
            }
        }
    }

    private fun sendTitle(title: Translation? = null, subtitle: Translation? = null) {
        for (player in Bukkit.getOnlinePlayers()) {
            player.displayTitle(
                TitleManager.TitleData.ofTicks(
                    id = "shutdown",
                    type = TitleManager.Type.Shutdown,
                    title = title?.getTranslation(player),
                    subtitle = subtitle?.getTranslation(player),
                    fadeInTicks = 20,
                    stayTicks = 5 * 20,
                    fadeOutTicks = 20,
                )
            )
        }
    }

    private fun sendTranslation(translation: Translation) {
        for (player in Bukkit.getOnlinePlayers())
            player.sendMessage(translation.getTranslation(player)!!)
    }

    private fun moveToState(state: State?) {
        this.state = state
        this.ticksSinceLastMessage = 0
        this.currentStateTicks = 0
        this.shutdownTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10)
    }

    fun cancelShuttingDown() {
        val taskId = taskId ?: return
        shuttingDown = false
        HandlerList.unregisterAll(this)
        Bukkit.getScheduler().cancelTask(taskId)
        sendTitle(subtitle = Translation.SHUTDOWN_STOP_TITLE)

        this.taskId = null
    }

    private enum class State {
        WAITING_FOR_RIDES, COUNTDOWN
    }
}
