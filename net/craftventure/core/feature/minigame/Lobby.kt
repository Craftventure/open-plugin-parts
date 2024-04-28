package net.craftventure.core.feature.minigame

import org.bukkit.entity.Player

interface Lobby {
    val minigame: Minigame
    val queuedCount: Int
    val maxPlayers: Int
        get() = minigame.maxPlayers
    val state: State
    val id: String

    fun tryJoin(player: Player): Boolean
    fun tryLeave(player: Player): Boolean
    fun addListener(listener: Listener): Boolean
    fun removeListener(listener: Listener): Boolean
    fun isQueued(player: Player): Boolean
    fun destroy()

    interface Listener {
        fun onUpdated(lobby: Lobby)
    }

    enum class State(val lobbyDescription: String) {
        IDLE("Queueing"),
        COUNTDOWN_TO_START("Counting down…"),
        RUNNING("Pre-queueing")
    }

    abstract class Json {
        abstract fun create(minigame: Minigame): Lobby
    }
}