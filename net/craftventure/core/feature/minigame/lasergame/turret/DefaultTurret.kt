package net.craftventure.core.feature.minigame.lasergame.turret

import net.craftventure.bukkit.ktx.extension.coloredItem
import net.craftventure.bukkit.ktx.extension.disableManipulations
import net.craftventure.bukkit.ktx.extension.setYawPitchDegrees
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.spawn
import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity
import net.craftventure.core.feature.minigame.lasergame.LaserGamePlayer
import net.craftventure.core.feature.minigame.lasergame.projectile.DirectionalPlasmaProjectile
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.InterpolationUtils
import net.craftventure.core.utils.LookAtUtil
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import kotlin.math.abs

class DefaultTurret(
    location: Location,
    game: LaserGame,
    val shootLocation: Location = location.clone().add(0.0, 0.80, 0.0),
    owner: LaserGameEntity? = null
) : BaseSingleTargetTurret(
    game = game,
    currentLocation = location,
    boundingBoxes = arrayOf(
        BoundingBox(
            location.x - 0.5,
            location.y,
            location.z - 0.5,
            location.x + 0.5,
            location.y + 1.2,
            location.z + 0.5
        )
    ),
    owner = owner,
    hp = 2,
    spawnSounds = defaultSpawnSounds,
    spottedSounds = defaultSpottedSounds,
    spottedLostSounds = defaultSpottedLostSounds,
    moveSounds = defaultMoveSounds,
    destroySounds = defaultDestroySounds,
    shootSounds = defaultShootSounds
) {
    private var standEntity: ArmorStand? = null
    private var gunEntity: ArmorStand? = null

    private var lastShot = 0L
    private var lastScanSound = 0L

    private var currentYaw = spawnYaw
    private var currentPitch = 0.0

    private var targetYaw = currentYaw
    private var targetPitch = currentPitch
    override var kills: Int = 0
    private val currentTargetYawPitch = LookAtUtil.YawPitch()
    private var lastParticle = 0L

    override fun isPartOfTarget(entity: Entity): Boolean =
        entity.entityId == gunEntity?.entityId || entity.entityId == standEntity?.entityId

    override fun onStateChanged(newState: State) {
//        Logger.debug("State $newState for turret $name")
        if (newState != State.BUILDING && newState != State.DESTROYED) {
            setupTurret(1.0)
        }
    }

    override fun destroy(silent: Boolean) {
        super.destroy(silent)
        standEntity?.remove()
        standEntity = null
        gunEntity?.remove()
        gunEntity = null
    }

    private fun getColor() = (owner as? LaserGamePlayer)?.team?.color ?: Color.RED

    private fun requireStandEntity(progress: Double): ArmorStand {
        standEntity?.let { existing ->
            if (existing.isValid) {
                return existing
            }
        }
        standEntity?.remove()
        val spawnLoc = currentLocation.clone().add(0.0, -EntityConstants.ArmorStandHeadOffset, 0.0)
        val newEntity = spawnLoc.spawn<ArmorStand>()
        newEntity.isVisible = false
        newEntity.setGravity(false)
        newEntity.equipment.helmet = coloredItem(Material.FIREWORK_STAR, getColor(), 3)
        newEntity.disableManipulations()
        standEntity = newEntity
        return newEntity
    }

    private fun requireGunEntity(progress: Double): ArmorStand {
        gunEntity?.let { existing ->
            if (existing.isValid) {
                return existing
            }
        }
        standEntity?.remove()
        val spawnLoc = currentLocation.clone()
            .add(0.0, (progress * (shootLocation.y - currentLocation.y)) - EntityConstants.ArmorStandHeadOffset, 0.0)
        spawnLoc.yaw = 0f
        val newEntity = spawnLoc.spawn<ArmorStand>()
        newEntity.isVisible = false
        newEntity.setGravity(false)
        newEntity.equipment.helmet = coloredItem(Material.FIREWORK_STAR, getColor(), 4)
        newEntity.disableManipulations()
        gunEntity = newEntity
        return newEntity
    }

    private fun setupTurret(progress: Double) {
        if (isDestroyed) return
        val standProgress = InterpolationUtils.getMu(0.0, 0.5, progress).clamp(0.0, 1.0)
        val standEntity = requireStandEntity(standProgress)
        EntityUtils.teleport(
            standEntity,
            standEntity.location.apply {
                y = currentLocation.y - EntityConstants.ArmorStandHeadOffset
            })
        standEntity.headPose =
            EulerAngle(Math.toRadians(InterpolationUtils.linearInterpolate(-90.0, 0.0, standProgress)), 0.0, 0.0)

        val gunProgress = InterpolationUtils.getMu(0.5, 1.0, progress).clamp(-1.0, 1.0)
        if (gunProgress >= 0) {
            val gunEntity = requireGunEntity(gunProgress)
            EntityUtils.teleport(
                gunEntity,
                gunEntity.location.apply {
                    y =
                        currentLocation.y + (gunProgress * (shootLocation.y - currentLocation.y)) - EntityConstants.ArmorStandHeadOffset
                })
            gunEntity.headPose = EulerAngle(Math.toRadians(currentPitch), Math.toRadians(currentYaw), 0.0)
        }
    }

    private fun updateTurretHeading() {
        val gunEntity = requireGunEntity(1.0)

        currentYaw = AngleUtils.smallestMoveTo(currentYaw, targetYaw, movementSpeedInDegreesPerTick)
        currentPitch = AngleUtils.smallestMoveTo(currentPitch, targetPitch, movementSpeedInDegreesPerTick)

        currentPitch = currentPitch.clamp(minPitch, maxPitch)
//        Logger.debug("Pitch is ${currentPitch.format(2)} ${targetPitch.format(2)}")

        gunEntity.headPose = EulerAngle(Math.toRadians(currentPitch), Math.toRadians(currentYaw), 0.0)
    }

    override fun update(game: LaserGame) {
//        Logger.debug("Updating turret $name")
        when (state) {
            State.BUILDING -> {
                val progress = (millisInCurrentState / 2500.0).clamp(0.0, 1.0)
                if (progress >= 1.0) {
                    state = State.SCANNING
                } else {
                    setupTurret(progress)
                }
            }
            State.UPGRADING -> {
                state = State.SCANNING
            }
            State.SCANNING -> {
                doScan()
            }
            State.DESTROYED -> {
            }
            State.INITIALISING -> {}
        }

        val isLowHp = hp <= 1
        if (isLowHp) {
            val now = System.currentTimeMillis()
            if (lastParticle < now - 500) {
                lastParticle = now
                currentLocation.spawnParticleX(
                    particle = Particle.CAMPFIRE_COSY_SMOKE,
                    count = 0,
                    offsetX = 0.0, offsetY = 0.1, offsetZ = 0.0,
                    extra = 0.5
                )
            }
        }
        standEntity?.apply {
            fireTicks = if (isLowHp) 20 else 0
            setCanTick(isLowHp)
        }
        gunEntity?.apply {
            fireTicks = if (isLowHp) 20 else 0
            setCanTick(isLowHp)
        }
    }

    private fun doScan() {
        val now = System.currentTimeMillis()
        val target = acquireTarget()
        if (target != null) {
            val targetLocation = target.mainHitTarget
            LookAtUtil.getYawPitchFromRadian(
                shootLocation.x,
                shootLocation.y,
                shootLocation.z,
                targetLocation.x,
                targetLocation.y,
                targetLocation.z,
                currentTargetYawPitch
            )

            targetYaw = Math.toDegrees(currentTargetYawPitch.yaw) + 90
            targetPitch = -Math.toDegrees(currentTargetYawPitch.pitch) + 90

//            val canShoot = currentTargetYawPitch.yaw
            updateTurretHeading()
        } else if (lastScanSound + 3000 < now && lastTargetChange + 1500 < now) {
            lastScanSound = now
            playMoveSound()
            targetYaw = CraftventureCore.getRandom().nextDouble() * 360
            targetPitch = minPitch + (CraftventureCore.getRandom().nextDouble() * (abs(minPitch) + abs(maxPitch)))
        }

        updateTurretHeading()

        if (target != null) {
            val canShoot = abs(AngleUtils.distance(currentYaw, targetYaw)) < 3.0 &&
                    abs(AngleUtils.distance(currentPitch, targetPitch)) < 3.0
            if (canShoot && lastShot + 1500 < now) {
                lastShot = now
                playShootSound()
                game.shootProjectile(
                    DirectionalPlasmaProjectile(
                        shooter = this,
                        location = shootLocation,
                        direction = Vector().setYawPitchDegrees(targetYaw, targetPitch),
                        length = maxReach.toInt(),
                        game = game,
                        damageType = LaserGameEntity.DamageType.turret
                    )
                )
            }
        }
    }
}