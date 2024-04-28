package net.craftventure.core.ktx.util

import net.craftventure.core.ktx.concurrency.CvExecutors.scheduledExecutor
import net.craftventure.core.ktx.extension.broadcastAsDebugTimings
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object BackgroundService {
    private val animatables = ConcurrentHashMap.newKeySet<Animatable>()//LinkedList<Animatable>()
    private var scheduledFuture: ScheduledFuture<*>? = null
    internal var lastUpdateTime: Long = 0L
        private set

    fun writeDebugTo(printWriter: PrintWriter) {
        printWriter.apply {
            println("Animatables: ${animatables.size}")
            println("Last update: $lastUpdateTime (${Date(lastUpdateTime)})")
            try {
                var i = 0
                for (animatable in animatables) {
                    println("Animatable ${animatable.javaClass.simpleName}")
                    try {
                        animatable.onAnimationUpdate()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        Logger.severe("Failed to update animatable[$i]: ${e.message} of class ${animatable.javaClass.simpleName}")
                    }
                    i++
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                println("Failed to execute all animatables: ${e.message}")
            }
        }
    }

    fun init() {
        scheduledFuture = scheduledExecutor.scheduleAtFixedRate({
            try {
                update()
            } catch (e: Exception) {
                Logger.capture(e)
                Logger.severe("Failed to update background service: ${e.message}")
            }
        }, 0, 50, TimeUnit.MILLISECONDS)
    }

    fun destroy() {
        scheduledFuture!!.cancel(true)
    }

    private fun update() {
        val timeA = System.currentTimeMillis()
        for (animatable in animatables) {
            try {
                animatable.onAnimationUpdate()
            } catch (e: Throwable) {
                Logger.capture(e)
                Logger.severe("Failed to update animatables: ${e.message}")
            }
        }
        lastUpdateTime = System.currentTimeMillis()
        val timeB = System.currentTimeMillis()
        val ms = timeB - timeA
        if (ms > 20) {
            String.format("Updating backgroundservice took %1\$s ms", ms).broadcastAsDebugTimings()
        }
    }

    fun add(animatable: Animatable) {
        val result = animatables.add(animatable)
//        Logger.debug("Added ${animatable.javaClass.simpleName}:$result via ${Logger.miniTrace(8)}")
    }

    fun remove(animatable: Animatable) {
        val result = animatables.remove(animatable)
//        Logger.debug("Removed ${animatable.javaClass.simpleName}:$result via ${Logger.miniTrace(8)}")
    }

    interface Animatable {
        fun onAnimationUpdate()
    }
}
