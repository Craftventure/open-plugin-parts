package net.craftventure.core.script

import net.craftventure.core.npc.actor.ActorPlayback
import java.util.*

class ActorScript : Script() {
    private val actorPlaybackList: MutableList<ActorPlayback> = LinkedList()
    override var repeats: Boolean = false
    fun add(actorPlayback: ActorPlayback) {
        actorPlaybackList.add(actorPlayback)
        repeats = actorPlaybackList.any { it.repeat }
    }

    override val isValid: Boolean
        get() = actorPlaybackList.size > 0

    override fun reset() {
        super.reset()
        for (actorPlayback in actorPlaybackList) {
            actorPlayback.reset()
        }
    }

    @Throws(ScriptControllerException::class)
    override fun onStart() { //        Logger.console("Play actor");
        super.onStart()
        for (actorPlayback in actorPlaybackList) {
            actorPlayback.play()
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        if (!repeats && !actorPlaybackList.any { it.shouldKeepPlaying() }) {
            onStop()
        }
    }

    @Throws(ScriptControllerException::class)
    override fun onStop() { //        Logger.console("Stop actor");
        super.onStop()
        for (actorPlayback in actorPlaybackList) {
            actorPlayback.stop()
        }
    }
}