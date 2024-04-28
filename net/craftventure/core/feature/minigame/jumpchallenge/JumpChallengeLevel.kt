package net.craftventure.core.feature.minigame.jumpchallenge

import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.core.feature.minigame.BaseMinigameLevel
import net.craftventure.core.feature.minigame.SpawnLocation

class JumpChallengeLevel(
    id: String,
    maxPlayers: Int = 12,
    playTimeInSeconds: Int,
    val spawnLocation: SpawnLocation,
    val finish: Area,
    area: Area
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