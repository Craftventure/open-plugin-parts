package net.craftventure.core.effect


interface SimpleEffect {
    val name: String

    fun isPlaying(): Boolean

    fun isStoppable(): Boolean

    fun play()

    fun stop()
}
