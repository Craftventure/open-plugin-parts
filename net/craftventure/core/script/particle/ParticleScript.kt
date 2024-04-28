package net.craftventure.core.script.particle

import net.craftventure.core.script.Script
import net.craftventure.core.script.ScriptControllerException
import java.util.*

class ParticleScript : Script() {
    private val particlePlaybackList: MutableList<ParticlePlayback> = LinkedList()
    override var repeats: Boolean = false

    override val isValid: Boolean
        get() = particlePlaybackList.size > 0

    fun add(particlePlayback: ParticlePlayback) {
        particlePlaybackList.add(particlePlayback)
        repeats = particlePlaybackList.any { it.repeat }
    }

    override fun reset() {
        super.reset()
        for (particlePlayback in particlePlaybackList) {
            particlePlayback.reset()
        }
    }

    @Throws(ScriptControllerException::class)
    override fun onStart() { //        Logger.console("Play particle");
        super.onStart()
        for (particlePlayback in particlePlaybackList) {
            particlePlayback.play()
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        if (!repeats && !particlePlaybackList.any { it.shouldKeepPlaying() }) {
            onStop()
        }
    }

    @Throws(ScriptControllerException::class)
    override fun onStop() { //        Logger.console("Stop particle");
        super.onStop()
        for (particlePlayback in particlePlaybackList) {
            particlePlayback.stop()
        }
    }
}