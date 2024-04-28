package net.craftventure.core.feature.minigame.autopia

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.core.feature.minigame.BaseMinigameLevel
import net.craftventure.core.feature.minigame.SpawnLocation
import org.bukkit.Location

class AutopiaLevel(
    id: String,
    maxPlayers: Int = 12,
    playTimeInSeconds: Int,
    val track: AutopiaTrack,
    val laps: Int = 3,
    val spawnLocations: Array<SpawnLocation>,
    area: Area
) : BaseMinigameLevel(
    id,
    maxPlayers,
    playTimeInSeconds,
    area
) {
    fun spawnLocation(index: Int): SpawnLocation = spawnLocations[index % spawnLocations.size]

    override fun toJson(): BaseMinigameLevel.Json {
        TODO("Not yet implemented")
    }

    @JsonClass(generateAdapter = true)
    class Json : BaseMinigameLevel.Json() {
        var laps: Int = 3
        lateinit var spawns: Set<Location>
        lateinit var trackPoints: List<AutopiaTrackPoint>

        override fun create() = AutopiaLevel(
            id,
            maxPlayers,
            playTimeInSeconds,
            AutopiaTrack(trackPoints),
            laps,
            spawns.map { SpawnLocation(it) }.toTypedArray(),
            area.create(),
        )
    }
}

