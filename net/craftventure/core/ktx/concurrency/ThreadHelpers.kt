package net.craftventure.core.ktx.coroutine

import net.craftventure.core.ktx.concurrency.CvExecutors
import net.craftventure.core.ktx.util.Logger

//val mainDispatcher = (Executor { command ->
//    Bukkit.getScheduler().runTask(CraftventureCore.getInstance(), command)
//}).asCoroutineDispatcher()

fun executeAsync(action: () -> Unit) =
    CvExecutors.executor.execute {
        try {
            action.invoke()
        } catch (e: Exception) {
            Logger.capture(e)
            throw e
        }
    }