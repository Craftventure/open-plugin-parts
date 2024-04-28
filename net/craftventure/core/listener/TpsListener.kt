package net.craftventure.core.listener

import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


object TpsListener {
    private val TPS_THRESSHOLD = 19.9
    private var lastTpsAboveThresshold = true
    private var scheduledFuture: ScheduledFuture<*>? = null

    fun init() {
        scheduledFuture = CraftventureCore.getScheduledExecutorService().scheduleAtFixedRate({
            try {
                update()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, 2, TimeUnit.SECONDS)
    }

    fun destroy() {
        if (scheduledFuture != null)
            scheduledFuture!!.cancel(true)
    }

    fun setLastTpsAboveThresshold(lastTpsAboveThresshold: Boolean) {
        if (lastTpsAboveThresshold != TpsListener.lastTpsAboveThresshold) {
            TpsListener.lastTpsAboveThresshold = lastTpsAboveThresshold
            if (lastTpsAboveThresshold) {
                val message =
                    String.format("TPS appears to be stable again above the thresshold of %1\$s", TPS_THRESSHOLD)
                Logger.info(message, logToCrew = true)
//                Logger.info("AutoNerf enabled all automatically disabled features again // TODO", logToCrew = true)
            } else {
                val message = String.format("TPS has dropped below the thresshold %1\$s", TPS_THRESSHOLD)
                Logger.warn(message, logToCrew = true)
//                Logger.warn(
//                    "AutoNerf may automatically disable features if the TPS doesn't stabilize, it will notify in chat // TODO",
//                    logToCrew = true
//                )
            }
        }
    }

    private fun setTps(tps: Double) {
        setLastTpsAboveThresshold(TPS_THRESSHOLD <= tps)
    }

    fun update() {
        setTps(Bukkit.getServer().tps[0])
    }
}
