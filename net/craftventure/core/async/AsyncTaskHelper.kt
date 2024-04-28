package net.craftventure.core.async

import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

// Use coroutines instead of this crap if you can!

//val mainDispatcher = (Executor { command ->
//    Bukkit.getScheduler().runTask(CraftventureCore.getInstance(), command)
//}).asCoroutineDispatcher()

fun executeAsync(action: () -> Unit) =
    CraftventureCore.getExecutorService().execute {
        try {
            action.invoke()
        } catch (e: Exception) {
            Logger.capture(e)
            throw e
        }
    }

fun <T> executeAsync(async: () -> T, sync: (T) -> Unit) =
    executeAsync {
        val result = async()
        executeSync {
            sync(result)
        }
    }

fun executeAsync(after: Long, action: () -> Unit): Int =
    Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
        executeAsync { action.invoke() }
    }, after)

fun executeMain(action: () -> Unit) {
    if (PluginProvider.isOnMainThread()) action() else executeSync(action)
}

fun executeAnyAsync(action: () -> Unit) {
    if (PluginProvider.isOnMainThread()) executeAsync(action) else action()
}

fun executeSync(action: () -> Unit): Int =
    Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance()) {
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
    }.runTaskLater(CraftventureCore.getInstance(), after).taskId

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
    }.runTaskTimer(CraftventureCore.getInstance(), after, repeat).taskId

fun executeSyncCancellable(after: Long, repeat: Long, action: BukkitRunnable.() -> Unit): Int =
    object : BukkitRunnable() {
        override fun run() {
            try {
                action.invoke(this)
            } catch (e: Exception) {
                Logger.capture(e)
                throw e
            }
        }
    }.runTaskTimer(CraftventureCore.getInstance(), after, repeat).taskId