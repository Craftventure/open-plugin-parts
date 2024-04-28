package net.craftventure.core.map.renderer

interface MapTask<T : MapEntryRenderer> {
    fun isRunning(): Boolean

    @Throws(IllegalStateException::class)
    fun start(renderer: T)
}