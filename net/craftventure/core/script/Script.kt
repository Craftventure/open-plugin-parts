package net.craftventure.core.script

abstract class Script {
    var scriptController: ScriptController? = null

    var isRunning: Boolean = false
        protected set

    abstract val repeats: Boolean
    abstract val isValid: Boolean

    open fun reset() {}

    @Throws(ScriptControllerException::class)
    open fun onLoad() {
    }

    @Throws(ScriptControllerException::class)
    open fun onStart() {
        isRunning = true
    }

    @Throws(ScriptControllerException::class)
    open fun onUpdate() {
    }

    @Throws(ScriptControllerException::class)
    open fun onStop() {
        isRunning = false
    }

    @Throws(ScriptControllerException::class)
    open fun onUnload() {
    }
}