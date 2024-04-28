package net.craftventure.core.async

import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit

@Deprecated("Use executeAsync and executeSync")
abstract class AsyncResultTask : Runnable {
    private var cancelled = false

    override fun run() {
        try {
            doInBackground()
        } catch (e: Exception) {
            Logger.capture(IllegalStateException("AsyncResultTask failed to execute its background task", e))
        }

        if (!cancelled)
            Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance()) { this.onPostExecute() }
    }

    fun cancel() {
        cancelled = true
    }

    fun executeNow() {
        CraftventureCore.getExecutorService().execute(this)
    }

    abstract fun doInBackground()

    abstract fun onPostExecute()
}
