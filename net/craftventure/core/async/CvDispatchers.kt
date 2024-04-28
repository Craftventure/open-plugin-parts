package net.craftventure.core.async

import kotlinx.coroutines.asCoroutineDispatcher
import net.craftventure.core.CraftventureCore
import org.bukkit.Bukkit
import java.util.concurrent.Executor

object CvDispatchers {
    val asyncMainThreadExecutor = Executor { command ->
        val task = Bukkit.getScheduler().callSyncMethod(CraftventureCore.getInstance()) {
            command.run()
        }
        while (!task.isDone) {
        }
    }
    val mainThreadDispatcher = asyncMainThreadExecutor.asCoroutineDispatcher()
}