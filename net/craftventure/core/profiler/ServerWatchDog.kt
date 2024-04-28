package net.craftventure.core.profiler

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.craftventure.bukkit.ktx.extension.asString
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

class ServerWatchDog : BackgroundService.Animatable, Listener {
    private var laggTicks = 0
    private var laggFreeTicks = 0
    private var lastTick = System.currentTimeMillis()
    private var laggStart = -1L
    private var isLagging = false

    @EventHandler
    fun onServerTick(event: ServerTickEndEvent) {
        onTick()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (event.player.isCrew()) {
            bossBar.addPlayer(event.player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (event.player.isCrew())
            bossBar.removePlayer(event.player)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        if (event.player.isCrew())
            bossBar.removePlayer(event.player)
    }

    private fun onTick() {
        lastTick = System.currentTimeMillis()
    }

    override fun onAnimationUpdate() {
        val lastTick = System.currentTimeMillis() - lastTick
        if (lastTick > 1000) {
            laggTicks++
            onStartLagging()

            if (laggTicks % 20 == 0) {
                if (laggTicks > 60)
                    printDebugTrace()
            }

            if (lastTick > 1100)
                if (!bossBar.isVisible)
                    bossBar.isVisible = true
            bossBar.setTitle(
                "Server is hanging ${((System.currentTimeMillis() - laggStart) / 1000f).format(1)} seconds"
            )
        } else {
            laggFreeTicks++
            if (laggFreeTicks > 80) {
                if (bossBar.isVisible) {
                    bossBar.isVisible = false
                    bossBar.setTitle("Server is OK")
                }
            }
            onStopLagging()
        }
    }

    private fun printDebugTrace() {
        Logger.warn("Server is hanging:\n${PluginProvider.getMainThread().stackTrace.joinToString("\n    at ")}")
        Logger.warn(
            "[WatchDog] Players: ${
                Bukkit.getOnlinePlayers().joinToString(", ") {
                    it.name + "/" + it.location.toVector().asString(2)
                }
            }"
        )
    }

    private fun onStartLagging() {
        if (isLagging) return
        if (laggTicks < 2) return

        laggStart = lastTick//System.currentTimeMillis()
        isLagging = true
        laggFreeTicks = 0
        printDebugTrace()
        val message = "[WatchDog] Server quit ticking (hangs?). See console for trace"//.broadcastAsDebugTimings()
        Logger.info(message)
    }

    private fun onStopLagging() {
        if (!isLagging) return

//        bossBar.isVisible = false
        val message =
            "[WatchDog] Server is ticking again (hung for ${
                ((System.currentTimeMillis() - laggStart) / 1000f).format(
                    2
                )
            } seconds)"//.broadcastAsDebugTimings()
        Logger.warn(message)
        bossBar.setTitle("Server hung for ${((System.currentTimeMillis() - laggStart) / 1000f).format(2)} seconds")
        laggStart = 0
        laggTicks = 0
        laggFreeTicks = 0
        isLagging = false
    }

    companion object {
        private var serverWatchDog: ServerWatchDog? = null
        private val bossBar by lazy {
            Bukkit.createBossBar("Active", BarColor.WHITE, BarStyle.SOLID).apply {
                progress = 1.0
                isVisible = false
            }
        }

        fun init() {
            destroy() // To be sure the previous one is cleaned

            serverWatchDog = ServerWatchDog()
            BackgroundService.add(serverWatchDog!!)
            Bukkit.getPluginManager().registerEvents(serverWatchDog!!, CraftventureCore.getInstance())

            for (player in Bukkit.getOnlinePlayers()) {
                if (player.isCrew())
                    bossBar.addPlayer(player)
            }
        }

        fun destroy() {
            serverWatchDog?.let { hangListener ->
                HandlerList.unregisterAll(hangListener)
                BackgroundService.remove(hangListener)
            }
            serverWatchDog = null
        }
    }
}