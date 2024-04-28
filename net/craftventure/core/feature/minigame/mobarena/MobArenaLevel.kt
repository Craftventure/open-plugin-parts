package net.craftventure.core.feature.minigame.mobarena

import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.core.feature.minigame.BaseMinigameLevel
import org.bukkit.Location
import org.bukkit.util.Vector

class MobArenaLevel(
    id: String,
    maxPlayers: Int = 5,
    playTimeInSeconds: Int = 60 * 15,
    area: SimpleArea,
    val offset: Vector,
    val limboLocation: Location,
    val waves: Array<MobArena.Wave>
) : BaseMinigameLevel(
    id,
    maxPlayers,
    playTimeInSeconds,
    area
) {
    override fun toJson(): Json {
        TODO("Not yet implemented")
    }
}
