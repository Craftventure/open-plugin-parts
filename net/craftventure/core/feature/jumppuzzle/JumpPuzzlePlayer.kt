package net.craftventure.core.feature.jumppuzzle

import org.bukkit.entity.Player

class JumpPuzzlePlayer(
    val player: Player,
    var qualified: Boolean = true,
    val startTime: Long = System.currentTimeMillis()
) {
    fun cleanUp() {}
    fun timePlayed() = System.currentTimeMillis() - startTime
}