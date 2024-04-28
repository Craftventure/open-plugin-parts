package net.craftventure.bukkit.ktx.coroutine

import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.coroutine.executeAsync
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

// This code is shit, don't use it please. Coroutines didn't exist yet back when I made this

fun <T> executeAsync(async: () -> T, sync: (T) -> Unit) =
    executeAsync {
        val result = async()
        executeSync {
            sync(result)
        }
    }

fun executeAsync(after: Long, action: () -> Unit): Int =
    Bukkit.getScheduler().scheduleSyncDelayedTask(PluginProvider.getInstance(), {
        executeAsync { action.invoke() }
    }, after)

fun executeMain(action: () -> Unit) {
    if (PluginProvider.isOnMainThread()) action() else executeSync(action)
}

fun executeAnyAsync(action: () -> Unit) {
    if (PluginProvider.isOnMainThread()) executeAsync(action) else action()
}

fun executeSync(action: () -> Unit): Int =
    Bukkit.getScheduler().scheduleSyncDelayedTask(PluginProvider.getInstance()) {
        try {
            action.invoke()
        } catch (e: Exception) {
            Logger.capture(e)
            throw e
        }
    }

fun executeSync(after: Long, action: () -> Unit): Int =
    object : BukkitRunnable() {
        override fun run() {
            try {
                action.invoke()
            } catch (e: Exception) {
                Logger.capture(e)
                throw e
            }
        }
    }.runTaskLater(PluginProvider.getInstance(), after).taskId

fun executeSync(after: Long, repeat: Long, action: () -> Unit): Int =
    object : BukkitRunnable() {
        override fun run() {
            try {
                action.invoke()
            } catch (e: Exception) {
                Logger.capture(e)
                throw e
            }
        }
    }.runTaskTimer(PluginProvider.getInstance(), after, repeat).taskId