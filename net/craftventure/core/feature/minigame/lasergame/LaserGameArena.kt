package net.craftventure.core.feature.minigame.lasergame

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import org.bukkit.Location

@JsonClass(generateAdapter = true)
class LaserGameArena(
    val arenaId: String,
    val area: Area,
    val arenaMode: LaserGameArenaMode,
    val teamMode: LaserGameTeamMode,
    val teamModeTeamCount: Int = 0,
    val spawns: Array<Location>,
    val teamSpawns: Array<Array<Location>> = emptyArray(),
    val extensions: Array<LaserGameArenaExtension> = emptyArray()
//    val maxPlayers: Int,
//    val levelBaseTimeLimit: Long
) {
    fun validate(): Boolean {
        if (teamMode !in arenaMode.compatibleWith) return false
        if (teamMode == LaserGameTeamMode.TEAMS && teamModeTeamCount < 1) return false
        return true
    }
}