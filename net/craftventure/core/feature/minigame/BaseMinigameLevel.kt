package net.craftventure.core.feature.minigame

import net.craftventure.bukkit.ktx.area.Area

abstract class BaseMinigameLevel(
    val id: String,
    val maxPlayers: Int,
    val playTimeInSeconds: Int,
    val area: Area
) {
    abstract fun toJson(): Json

    open fun <T : Json> toJson(source: T): T {
        return source
    }

    open fun <T : Json> restore(source: T) {
    }

    abstract class Json {
        lateinit var id: String
        var maxPlayers: Int = 2
        var playTimeInSeconds: Int = 60
        lateinit var area: Area.Json

        abstract fun create(): BaseMinigameLevel
    }
}