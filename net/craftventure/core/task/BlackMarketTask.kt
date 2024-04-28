package net.craftventure.core.task

import net.craftventure.core.CraftventureCore
import net.craftventure.core.script.action.PlaceSchematicAction
import org.bukkit.Bukkit


object BlackMarketTask {
    private var isActive = false
    private var isNight = true

    fun init() {
        if (isActive)
            return
        isActive = true

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            val ticks = Bukkit.getWorld("world")!!.time
            val isNowNight = ticks in 14001..21999
            if (isNowNight != isNight) {
                isNight = isNowNight
                //                Logger.console("Night? " + isNowNight);
                if (isNowNight)
                    PlaceSchematicAction("singapore", "code").withName("blackmarket_night").noAir(false).execute(null)
                else
                    PlaceSchematicAction("singapore", "code").withName("blackmarket_day").noAir(false).execute(null)
            }
        }, 1L, (20 * 5).toLong())
    }
}
