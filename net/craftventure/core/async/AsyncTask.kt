package net.craftventure.core.async

import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger

@Deprecated("Use executeAsync and executeSync")
abstract class AsyncTask : Runnable {
    override fun run() {
        try {
            doInBackground()
        } catch (e: Exception) {
            Logger.capture(IllegalStateException("AsyncTask failed to execute its background task", e))
        }

    }

    fun executeNow() {
        CraftventureCore.getExecutorService().execute(this)
    }

    abstract fun doInBackground()
}
