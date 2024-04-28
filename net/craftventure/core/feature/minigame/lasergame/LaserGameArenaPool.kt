package net.craftventure.core.feature.minigame.lasergame

object LaserGameArenaPool {
    private val arenas = hashSetOf<LaserGameArena>()
    private val claimed = hashSetOf<LaserGameArena>()

    fun register(arena: LaserGameArena) {
        arenas.add(arena)
    }

    fun claim(arenaMode: LaserGameArenaMode?, teamMode: LaserGameTeamMode?): LaserGameArena? {
        val potentialArenas = arenas
            .filter { it !in claimed && (arenaMode == null || arenaMode == it.arenaMode) && (teamMode == null || teamMode == it.teamMode) }
        if (potentialArenas.isNotEmpty()) {
            val arena = potentialArenas.random()
            claimed.add(arena)
            return arena
        }
        return null
    }

    fun release(arena: LaserGameArena) {
        if (!arena.validate()) throw IllegalStateException("A non-valid arena can't be added to the pool")
        claimed.remove(arena)
    }
}