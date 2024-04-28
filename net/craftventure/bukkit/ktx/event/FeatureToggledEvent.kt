package net.craftventure.bukkit.ktx.event

import net.craftventure.bukkit.ktx.manager.FeatureManager
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class FeatureToggledEvent(
    val feature: FeatureManager.Feature,
    val enabled: Boolean
) : Event(!Bukkit.isPrimaryThread()) {

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
