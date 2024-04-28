package net.craftventure.core.feature.minigame.lasergame.gun

import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity
import net.craftventure.core.feature.minigame.lasergame.LaserGamePlayer
import net.craftventure.core.feature.minigame.lasergame.projectile.DirectionalPlasmaProjectile
import kotlin.math.ceil


class ShotGun(
    val config: Config = Config()
) : BaseLaserGun() {
    override var lastUse: Long = 0
    override var cooldownFinish: Long = 0

    override fun use(game: LaserGame, player: LaserGamePlayer, leftClick: Boolean): Boolean {
        if (cooldownFinish > System.currentTimeMillis()) return false
        val sneaking = player.player.isSneaking
        lastUse = System.currentTimeMillis()
        cooldownFinish = System.currentTimeMillis() + if (sneaking) config.cooldownSneaking else config.cooldown

        val shootLocation = player.player.eyeLocation
//        shootLocation.y += player.player.getEyeHeight(false) * 0.8

        doSound(shootLocation)
        val lookDirection = shootLocation.direction

        val anglePerRay = if (sneaking) config.rayOffsetSneaking else config.rayOffset
        val rayCount = if (sneaking) config.raysSneaking else config.rays
        var currentAngle = (rayCount.toDouble() / 2.0) * -anglePerRay + (anglePerRay * 0.5)

        for (i in 0 until rayCount) {
//            Logger.debug("$i > ${currentAngle.format(2)} > ${anglePerRay.format(2)}")
            val direction = lookDirection.clone()
                .rotateAroundY(Math.toRadians(currentAngle))
            game.shootProjectile(
                DirectionalPlasmaProjectile(
                    shooter = player,
                    location = shootLocation,
                    direction = direction,
                    length = ceil(if (sneaking) config.lengthSneaking else config.length).toInt(),
                    game = game,
                    damageType = LaserGameEntity.DamageType.shotgun
                )
            )
            currentAngle += anglePerRay
        }
        return true
    }

    data class Config(
        val cooldown: Long = 1000,
        val cooldownSneaking: Long = 1750,
        val rayOffset: Double = 8.0,
        val rayOffsetSneaking: Double = 10.0,
        val length: Double = 8.0,
        val lengthSneaking: Double = 18.0,
        val rays: Int = 5,
        val raysSneaking: Int = 3
    )
}