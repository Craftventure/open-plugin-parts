package net.craftventure.bukkit.ktx.entitymeta

import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

abstract class BasePlayerMetadata(player: Player) : BaseMetadata() {
    private var player: Player? = player
    private val lock = Any()
    override fun onDestroy() {
        super.onDestroy()
        player = null
        removeTask?.cancel()
    }

    protected fun player() = player!!

    private var removeTask: BukkitRunnable? = null

    fun removeAfter(millis: Long) {
        removeAt(System.currentTimeMillis() + millis)
    }

    fun removeAt(millis: Long) {
        synchronized(lock) {
            removeTask?.cancel()
            val removeTask = object : BukkitRunnable() {
                override fun run() {
                    player?.removeMetadata(this@BasePlayerMetadata)
                }
            }

            val target = millis - System.currentTimeMillis()
            if (target <= 0) {
                removeTask.runTask(PluginProvider.getInstance())
            } else {
                removeTask.runTaskLater(PluginProvider.getInstance(), ((target / 1000.0) * 20.0).toLong())
            }
            this.removeTask = removeTask
        }
    }

    override fun isValid(target: Any): Boolean =
        (target as? Player)?.isConnected() == true || player?.isConnected() == true
}