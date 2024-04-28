package net.craftventure.core.feature.minigame.lasergame.projectile

import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.core.extension.collidingCheck
import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity
import net.craftventure.core.utils.BoundingBox
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.util.Vector


class DirectionalPlasmaProjectile(
    override val shooter: LaserGameEntity,
    private val location: Location,
    private val direction: Vector,
    private val length: Int,
    private val color: Color = Color.RED,
    val damage: Int = 1,
    val piercePower: Int = 1,
    private val boundingBox: BoundingBox = BoundingBox(
        -0.05,
        -0.05,
        -0.05,
        0.05,
        0.05,
        0.05
    ),
    val friendlyFire: Boolean = false,
    val game: LaserGame,
    override val damageType: LaserGameEntity.DamageType,
    private val particleSpawner: (effectLoc: Location, i: Int, steps: Int) -> Unit = { effectLoc, i, steps ->
        if (i % steps * 2 == 0) {
            effectLoc.spawnParticleX(
                Particle.REDSTONE,
                count = 3,
                offsetX = 0.1,
                offsetY = 0.1,
                offsetZ = 0.1,
                data = Particle.DustOptions(color, 0.5f),
                range = length * 1.2,
                longDistance = true
            )
        }
    },
    private val type: ProjectileType = ProjectileType.RAY,
    private val explosionHandler: ((source: DirectionalPlasmaProjectile, location: Location) -> Unit)? = null
) : LaserGameProjectile {
    private val calcBoundingBox = BoundingBox()
    override val isValid: Boolean
        get() = false

    private data class HitLocation(
        val what: LaserGameEntity,
        var location: Location,
        var hitType: LaserGameEntity.HitType
    )

    override fun update(game: LaserGame) {
        val effectLoc = location.clone()
        val steps = 3
        direction.normalize().multiply(1.0 / (steps.toDouble() * 2))

        val targets = game.targets.filter { it !== shooter && it !== shooter.rootOwner }

//        Logger.debug("Handling hits with ${targets.size} targets")

        val targetHits = mutableListOf<HitLocation>()
        iLoop@ for (i in 0..length * steps * 2) {
            effectLoc.add(direction)

            var hitTarget = false
            if (i % 2 == 0) {
                targetLoop@ for (target in targets) {
                    if (target === shooter) continue
                    val damageKind = target.isHit(effectLoc) ?: continue
                    val existingDamage = targetHits.firstOrNull { it.what === target }

//                Logger.debug("Damage $damageKind ${target.name} (existing=${existingDamage?.damageKind})")// to $target, existing=$existingDamage")

                    if (damageKind.dealsDamage && existingDamage != null && existingDamage.hitType.dealsDamage) {
                        val hitCount = targetHits.count { it.hitType.dealsDamage }
//                        Logger.debug("Already dealt damage, check if we should continue: hitCount=$hitCount for piercePower=$piercePower")
                        if (hitCount >= piercePower) {
//                            Logger.debug("Bullet has used all its piercing power and can hit no more targets")
                            break@iLoop
                        }
                    }

                    if (type == ProjectileType.EXPLOSION_ON_IMPACT) {
                        doExplosion(effectLoc)
                        return
                    }

                    if (existingDamage == null) {
                        hitTarget = true
                        targetHits.add(HitLocation(target, effectLoc.clone(), damageKind))
                    } else if (existingDamage.hitType.priority < damageKind.priority) {
                        hitTarget = true
                        existingDamage.hitType = damageKind
                        existingDamage.location = effectLoc.clone()
                    }
                }

                if (!hitTarget) {
                    val damageCount = targetHits.count { it.hitType.dealsDamage }
                    if (damageCount >= piercePower) {
//                        Logger.debug("Bullet has used all its piercing power")
                        break@iLoop
                    }
                }
            }

            val block = effectLoc.block
            if (block.type != Material.AIR) {
                val collides = effectLoc.world!!
                    .collidingCheck(
                        boundingBox,
                        calcBoundingBox,
                        effectLoc.x,
                        effectLoc.y,
                        effectLoc.z
                    )
                if (collides) {
                    effectLoc.spawnParticleX(Particle.EXPLOSION_NORMAL, offsetX = 0.5, offsetY = 0.5, offsetZ = 0.5)
//                    Logger.debug("Block hit")
                    if (type == ProjectileType.EXPLOSION_ON_IMPACT) {
                        doExplosion(effectLoc)
                        return
                    }
                    break@iLoop
                }
//                val boundingBox =
            }
//            if (effectLoc.block.type != Material.AIR && effectLoc.block.type.isSolid) {
//                effectLoc.spawnParticleX(Particle.FIREWORKS_SPARK, offsetX = 0.5, offsetY = 0.5, offsetZ = 0.5)
//                Logger.debug("Block hit")
//                spawnExplosion(effectLoc)
//                break@iLoop
//            }

            particleSpawner(effectLoc, i, steps)
        }
        if (targetHits.isNotEmpty()) {
            dealDamage(targetHits)
            return
        }
//        Logger.debug("Nothing was hit")
        return
    }

    private fun doExplosion(location: Location) {
        explosionHandler?.invoke(this, location)
    }

    private fun dealDamage(hits: List<HitLocation>) {
        hits.forEach { hit ->
            val damageKind = hit.hitType
            val location = hit.location
            val target = hit.what
            if (target === shooter) return@forEach

            if (!friendlyFire && !target.isEnemy(shooter)) return@forEach

            if (damageKind == LaserGameEntity.HitType.NEAR_HIT) {
                location.world!!.playSound(
                    location,
                    "${SoundUtils.SOUND_PREFIX}:minigame.laser.laser.travel",
                    1.0f,
                    1.0f
                )
            } else if (target.canBeHit()) {
                val totalDamageDealt = damage + (if (damageKind.critical) 1 else 0)
//                Logger.debug("Hit $damageKind ${target} with totalDamageDealt=$totalDamageDealt")
                val result = target.hitBy(source = shooter, damage = totalDamageDealt)
                if (result == LaserGameEntity.HitResult.DESTROYED)
                    game.handleKill(target, shooter, damageKind, damageType)
            } else {
//                Logger.debug("Invincible")
                target.currentLocation.add(0.0, 1.0, 0.0).spawnParticleX(
                    Particle.VILLAGER_ANGRY,
                    offsetX = 0.5,
                    offsetY = 0.5,
                    offsetZ = 0.5,
                    count = 5,
                    extra = 0.1,
                    range = length * 1.2
                )
            }
        }
    }

    enum class ProjectileType {
        RAY,
        EXPLOSION_ON_IMPACT
    }
}