package net.craftventure.core.feature.minigame.lasergame

enum class LaserGameArenaMode(val compatibleWith: Array<LaserGameTeamMode>) {
    DEATHMATCH(arrayOf(LaserGameTeamMode.FFA, LaserGameTeamMode.TEAMS)),
    PAYLOAD(arrayOf(LaserGameTeamMode.TEAMS)),
    CTF(arrayOf(LaserGameTeamMode.TEAMS))
}