package net.craftventure.core.effect

import net.craftventure.core.CraftventureCore
import org.bukkit.Bukkit


abstract class BaseEffect(override val name: String) : SimpleEffect, Runnable {
    private var isPlaying = false
    private var task = -1
    protected var tick: Int = 0
    protected var autoStopTicks = 20 * 60 * 5

    override fun play() {
        if (!isPlaying) {
            reset()
            isPlaying = true
            task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), this, 1L, 1L)
            onStarted()
        }
    }

    override fun isPlaying(): Boolean {
        return isPlaying
    }

    override fun isStoppable(): Boolean {
        return false
    }

    override fun stop() {
        if (isPlaying) {
            isPlaying = false
            Bukkit.getScheduler().cancelTask(task)
            reset()
            onStopped()
        }
    }

    open fun onStarted() {

    }

    open fun onStopped() {

    }

    protected open fun reset() {
        tick = 0
    }

    override fun run() {
        tick++
        update(tick)
        if (autoStopTicks in 1..(tick - 1)) {
            stop()
        }
    }

    abstract fun update(tick: Int)
}
