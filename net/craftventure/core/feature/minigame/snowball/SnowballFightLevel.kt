package net.craftventure.core.feature.minigame.snowball

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.core.feature.minigame.BaseMinigameLevel
import net.craftventure.core.feature.minigame.SpawnLocation
import org.bukkit.Location

class SnowballFightLevel(
    id: String,
    maxPlayers: Int = 12,
    playTimeInSeconds: Int,
    val blueSpawns: Array<SpawnLocation>,
    val redSpawns: Array<SpawnLocation>,
    area: Area
) : BaseMinigameLevel(
    id,
    maxPlayers,
    playTimeInSeconds,
    area
) {
    fun getSpawn(player: SnowballPlayer) = if (player.team == SnowballPlayer.Team.RED)
        redSpawns.random()
    else
        blueSpawns.random()

    override fun toJson(): BaseMinigameLevel.Json {
        TODO("Not yet implemented")
    }

    @JsonClass(generateAdapter = true)
    class Json : BaseMinigameLevel.Json() {
        lateinit var redSpawns: Set<Location>
        lateinit var blueSpawns: Set<Location>

        override fun create() = SnowballFightLevel(
            id,
            maxPlayers,
            playTimeInSeconds,
            blueSpawns.map { SpawnLocation(it) }.toTypedArray(),
            redSpawns.map { SpawnLocation(it) }.toTypedArray(),
            area.create(),
        )
    }
}
