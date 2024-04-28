package net.craftventure.core.script

import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.serverevent.ScriptStartEvent
import net.craftventure.core.serverevent.ScriptStopEvent
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class ScriptController(
    val npcEntityTracker: NpcEntityTracker?,
    val groupId: String,
    val name: String
) {
    var isPlaying = false
        private set
    private var hasLoaded = false
    private var hasStarted = false
    private var taskedRunnable: BukkitRunnable? = null
    private val scriptList: MutableList<Script> = LinkedList()
    val scriptCount: Int
        get() = scriptList.size

    val scripts: List<Script>
        get() = scriptList

    fun init() {
        onLoad()
    }

    fun addScript(script: Script) {
        if (!isPlaying) {
            script.scriptController = this
            scriptList.add(script)
        }
    }

    fun restart(): Boolean {
        stop()
        return start()
    }

    fun start(): Boolean {
//        Logger.debug("Start %s", false, !isPlaying);
        if (!isPlaying) {
            isPlaying = true
            taskedRunnable = object : BukkitRunnable() {
                override fun run() {
                    onUpdate()
                }
            }
            taskedRunnable!!.runTaskTimer(CraftventureCore.getInstance(), 1, 1)
            onStart()
            return true
        }
        return false
    }

    fun stop(): Boolean {
//        Logger.debug("Stop %s", false, isPlaying);
        if (isPlaying) {
            isPlaying = false
            if (taskedRunnable != null) taskedRunnable!!.cancel()
            onStop()
            return true
        }
        return false
    }

    fun onLoad() {
        if (hasLoaded) return
        for (script in scriptList) {
            try {
                script.onLoad()
            } catch (e: ScriptControllerException) {
                capture(e)
                return
            }
        }
        hasLoaded = true
        ScriptManager.broadcastLoad(this)
    }

    fun onStart() {
        if (hasLoaded) {
            npcEntityTracker?.startTracking()
            for (script in scriptList) {
//                Logger.debug("Starting " + script.getClass().getSimpleName());
                try {
                    script.reset()
                    script.onStart()
                } catch (e: ScriptControllerException) {
                    capture(e)
                    return
                }
            }
            hasStarted = true
            Bukkit.getPluginManager().callEvent(ScriptStartEvent(this, this.groupId, this.name))
        }
    }

    fun onUpdate() {
        if (hasLoaded) {
            var shouldStop = true
            for (i in scriptList.indices) {
                val script = scriptList[i]
                try {
                    script.onUpdate()
                } catch (e: ScriptControllerException) {
                    capture(e)
                    return
                }
                if (script.isRunning) shouldStop = false
            }
            if (shouldStop) {
//                debug("Should stop $groupId/$name", false)
                stop()
            }
        }
    }

    fun onStop() {
        if (hasStarted) {
            hasStarted = true
            for (script in scriptList) {
                try {
//                    Logger.debug("Stopping " + script.getClass().getSimpleName());
                    script.onStop()
                } catch (e: ScriptControllerException) {
                    capture(e)
                    return
                }
            }
            npcEntityTracker!!.stopTracking()
            Bukkit.getPluginManager().callEvent(ScriptStopEvent(this, this.groupId, this.name))
        }
    }

    fun onUnload() {
        if (hasLoaded) {
            for (script in scriptList) {
                try {
                    script.onUnload()
                } catch (e: ScriptControllerException) {
                    capture(e)
                    return
                }
                script.scriptController = null
            }
            npcEntityTracker!!.release()
            ScriptManager.broadcastUnloaded(this)
        }
    }

}