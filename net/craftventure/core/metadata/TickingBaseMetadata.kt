package net.craftventure.core.metadata

import net.craftventure.bukkit.ktx.entitymeta.BaseMetadata
import net.craftventure.core.CraftventureCore
import org.bukkit.Bukkit

abstract class TickingBaseMetadata : BaseMetadata() {
    private val task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
        update()
    }, 1L, 1L)

    protected open fun update() {}

    override fun onDestroy() {
        super.onDestroy()
        Bukkit.getScheduler().cancelTask(task)
    }
}