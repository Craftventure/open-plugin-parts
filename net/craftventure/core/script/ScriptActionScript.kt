package net.craftventure.core.script

import java.util.*

class ScriptActionScript : Script() {
    private val particlePlaybackList: MutableList<ScriptActionPlayback> = LinkedList()
    override var repeats: Boolean = false

    override val isValid: Boolean
        get() = particlePlaybackList.size > 0

    fun add(particlePlayback: ScriptActionPlayback) {
        particlePlaybackList.add(particlePlayback)
        var length: Long = 0
        for (particlePlayback1 in particlePlaybackList) {
            length = Math.max(length, particlePlayback1.animationLength)
        }
        for (particlePlayback1 in particlePlaybackList) {
            particlePlayback1.animationLength = length
        }
        repeats = particlePlaybackList.any { it.repeat }
    }

    override fun reset() {
        super.reset()
        for (playback in particlePlaybackList) {
            playback.reset()
        }
    }

    @Throws(ScriptControllerException::class)
    override fun onStart() {
        super.onStart()
        //        Logger.console("Play particle");
        for (playback in particlePlaybackList) {
            playback.play()
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        if (!repeats && !particlePlaybackList.any { it.shouldKeepPlaying() }) {
            onStop()
        }
    }

    @Throws(ScriptControllerException::class)
    override fun onStop() {
        super.onStop()
        //        Logger.console("Stop particle");
        for (playback in particlePlaybackList) {
            playback.stop()
        }
    }
}