package net.craftventure.core.task

import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.core.CraftventureCore
import net.craftventure.core.script.ScriptManager
import org.bukkit.Bukkit

object EurosatBotTask {
    fun init() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            AudioServerApi.enable("spacemountain_eurosatbot")
            AudioServerApi.sync("spacemountain_eurosatbot", System.currentTimeMillis())
            ScriptManager.stop("spacemountain", "robot")
            ScriptManager.start("spacemountain", "robot")
        }, 20L, 20 * 63)
    }
}
