package net.craftventure.core.feature.minigame.lasergame.gun

import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity
import net.craftventure.core.feature.minigame.lasergame.LaserGamePlayer
import net.craftventure.core.feature.minigame.lasergame.projectile.DirectionalPlasmaProjectile


class DefaultGun : BaseLaserGun() {
    override var lastUse: Long = 0
    override var cooldownFinish: Long = 0

    override fun use(game: LaserGame, player: LaserGamePlayer, leftClick: Boolean): Boolean {
        if (cooldownFinish > System.currentTimeMillis()) return false
        lastUse = System.currentTimeMillis()
        cooldownFinish = System.currentTimeMillis() + if (player.player.isSneaking) 1500 else 750

        val shootLocation = player.player.eyeLocation
//        shootLocation.y += player.player.getEyeHeight(false) * 0.8
        val lookDirection = shootLocation.direction

        doSound(shootLocation)
        game.shootProjectile(
            DirectionalPlasmaProjectile(
                shooter = player,
                location = shootLocation,
                direction = lookDirection,
                length = if (player.player.isSneaking) 50 else 20,
                game = game,
                damageType = LaserGameEntity.DamageType.gun
            )
        )
        return true
    }
}