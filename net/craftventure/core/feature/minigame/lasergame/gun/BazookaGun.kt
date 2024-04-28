package net.craftventure.core.feature.minigame.lasergame.gun

import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity
import net.craftventure.core.feature.minigame.lasergame.LaserGamePlayer
import net.craftventure.core.feature.minigame.lasergame.projectile.DirectionalPlasmaProjectile
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.ceil


class BazookaGun : BaseLaserGun() {
    override var lastUse: Long = 0
    override var cooldownFinish: Long = 0

    override fun use(game: LaserGame, player: LaserGamePlayer, leftClick: Boolean): Boolean {
        if (cooldownFinish > System.currentTimeMillis()) return false
        lastUse = System.currentTimeMillis()
        cooldownFinish = System.currentTimeMillis() + if (player.player.isSneaking) 3000 else 2000

        val shootLocation = player.player.eyeLocation
//        shootLocation.y += player.player.getEyeHeight(false) * 0.8
        val lookDirection = shootLocation.direction

        val DAMAGE = 2
        val REACH = 5.0

        doSound(shootLocation)
        val shot = game.shootProjectile(
            DirectionalPlasmaProjectile(
                shooter = player,
                location = shootLocation,
                direction = lookDirection,
                length = if (player.player.isSneaking) 60 else 30,
                damage = 2,
                game = game,
                damageType = LaserGameEntity.DamageType.gun,
                type = DirectionalPlasmaProjectile.ProjectileType.EXPLOSION_ON_IMPACT,
                particleSpawner = { effectLoc: Location, i: Int, steps: Int ->
                    if (i % steps * 2 == 0) {
                        effectLoc.spawnParticleX(
                            Particle.EXPLOSION_NORMAL,
                            count = 2,
                            offsetX = 0.1,
                            offsetY = 0.1,
                            offsetZ = 0.1,
                            longDistance = true
                        )
                    }
                },
                explosionHandler = { source: DirectionalPlasmaProjectile, location: Location ->
                    location.spawnParticleX(
                        Particle.EXPLOSION_HUGE,
                        count = 1,
                        offsetX = 0.1,
                        offsetY = 0.1,
                        offsetZ = 0.1,
                        longDistance = true
                    )
                    source.game.targets.forEach { target ->
                        if (target.isEnemy(source.shooter) && target.canBeHit()) {
                            val distance = target.currentLocation.distance(location)
                            if (distance < REACH) {
                                val damage = ceil((1.0 - (distance / REACH)) * DAMAGE).toInt()
                                if (damage > 0) {
                                    val result = target.hitBy(source = source.shooter, damage = damage)
                                    if (result == LaserGameEntity.HitResult.DESTROYED)
                                        game.handleKill(
                                            target,
                                            source.shooter,
                                            LaserGameEntity.HitType.NORMAL,
                                            LaserGameEntity.DamageType.gun
                                        )
                                    else {
                                        val targetLocation = target.currentLocation
                                        val velocity = Vector(
                                            location.x - targetLocation.x,
                                            location.y - targetLocation.y,
                                            location.z - targetLocation.z
                                        ).normalize().apply {
                                            y *= 4
                                        }.multiply(((REACH - distance) * 0.6).clamp(0.5, 2.0) * -1)
                                        velocity.y = abs(velocity.y).clamp(0.3, 1.0)
                                        target.applyVelocity(velocity)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        )

        if (shot) {
            val velocity = player.player.velocity
            velocity.add(player.player.location.direction.normalize().multiply(-2.25))
            player.player.velocity = velocity
        }

        return true
    }
}