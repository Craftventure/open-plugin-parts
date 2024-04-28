package net.craftventure.core.feature.minigame.lasergame.turret

import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameItem
import net.craftventure.core.feature.minigame.lasergame.LaserGamePlayer

class TurretItem(val type: LaserGameTurretType) : LaserGameItem {
    override var lastUse: Long = 0
    override var cooldownFinish: Long = 0

    override fun use(game: LaserGame, player: LaserGamePlayer, leftClick: Boolean): Boolean {
        if (cooldownFinish > System.currentTimeMillis()) return false
        val hasTurret = game.hasTurret(player)
        if (!leftClick && hasTurret) {
            lastUse = System.currentTimeMillis()
            cooldownFinish = System.currentTimeMillis() + 1000

            game.removeTurret(player, false)
            return true
        }
        if (hasTurret) return false
        lastUse = System.currentTimeMillis()
        cooldownFinish = System.currentTimeMillis() + 3000

        val turret = type.factory.invoke(player.currentLocation, game)
        turret.owner = player
        game.spawnTurret(turret)
        return false
    }
}