package net.craftventure.core.feature.jumppuzzle

interface JumpPuzzleCheckPoint {
    fun getOrder(): Int

    fun shouldUnlock(player: JumpPuzzlePlayer): Boolean

    fun onUsed(player: JumpPuzzlePlayer)
}