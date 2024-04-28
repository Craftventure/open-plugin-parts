package net.craftventure.core.feature.kart.actions

import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.animation.armature.WrappedJoint
import net.craftventure.core.extension.collidingCheck
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.kart.KartAction
import net.craftventure.core.feature.kart.addon.KartAddon
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import net.craftventure.core.utils.BoundingBox
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import kotlin.math.roundToInt

class AtAtAction : KartAddon(), KartAction {
    private var lastUse = -1L
    private var lastGunLeft = false

    private var gunLeftLastShotAt = 0L
    private var gunRightLastShotAt = 0L

    private var gunLeft: Kart.VisualFakeSeat? = null
    private var gunRight: Kart.VisualFakeSeat? = null

    private var gunLeftJoint: WrappedJoint? = null
    private var gunRightJoint: WrappedJoint? = null

    private val shootMatrix = Matrix4x4()
    private var projectiles = mutableListOf<LaserProjectile>()

    override fun execute(kart: Kart, type: KartAction.Type, target: Player?) {
        if (lastUse < System.currentTimeMillis() - 3000 /*|| CraftventureCore.isTestServer()*/) {
            lastUse = System.currentTimeMillis()

            val gunLeft =
                gunLeft ?: kart.npcs.firstOrNull { it.settings.id == "gunleft" }?.also { gunLeft = it } ?: return
            val gunRight =
                gunRight ?: kart.npcs.firstOrNull { it.settings.id == "gunright" }?.also { gunRight = it } ?: return

            val gunToShoot = if (lastGunLeft) {
                gunRightLastShotAt = System.currentTimeMillis()
                gunRight
            } else {
                gunLeftLastShotAt = System.currentTimeMillis()
                gunLeft
            }
            lastGunLeft = !lastGunLeft

            shootMatrix.set(gunToShoot.currentMatrix)
            val location = shootMatrix.toVector().toLocation(kart.startLocation.world!!)
            val quat = shootMatrix.rotation
            quat.rotateX(-90.0)

            location.yaw = quat.yaw.toFloat()
            location.pitch = quat.pitch.toFloat()
            val direction = location.direction.normalize()
            location.add(direction.clone().multiply(1.1))

//            for (i in 0..5) {
//                location.spawnParticleX(
//                    Particle.CAMPFIRE_COSY_SMOKE,
//                    count = 0,
//                    offsetX = direction.x,
//                    offsetY = direction.y,
//                    offsetZ = direction.z,
//                    extra = 0.5
//                )
//            }
            location.world!!.playSound(location, "minecraft:minigame.laser.laser.4", SoundCategory.AMBIENT, 0.2f, 0.1f)

            projectiles.add(
                LaserProjectile(
                    location,
                    direction.clone(),
                    speed = CoasterMathUtils.kmhToBpt(80.0),
                )
            )
        }
    }

    override fun onPostUpdate(kart: Kart) {
        super.onPostUpdate(kart)
        projectiles.removeAll { !it.update() }
    }

    override fun onPreArmatureUpdate(kart: Kart, armature: Armature, interactor: Kart.PhysicsInteractor) {
        super.onPreArmatureUpdate(kart, armature, interactor)

        val now = System.currentTimeMillis()
        val gunLeftJoint = gunLeftJoint ?: armature.allJoints().first { it.name == "GunLeft" }
            .let { WrappedJoint(it) }
            .also { gunLeftJoint = it }
        gunLeftJoint.reset()
        animateGun(gunLeftJoint, now - gunLeftLastShotAt)

        val gunRightJoint = gunRightJoint ?: armature.allJoints().first { it.name == "GunRight" }
            .let { WrappedJoint(it) }
            .also { gunRightJoint = it }
        gunRightJoint.reset()
        animateGun(gunRightJoint, now - gunRightLastShotAt)
    }

    private fun animateGun(joint: WrappedJoint, time: Long) {
        if (time > 500) {
            return
        }
//        Logger.debug("${joint.joint.name}: ${time}")
        if (time < 100) {
            val t = time / 100.0
            joint.joint.transform.translate(0.0, -0.5 * t, 0.0)
        } else {
            val t = (time - 100.0) / 400.0
            joint.joint.transform.translate(0.0, -0.5 + (0.5 * t), 0.0)
        }
    }

    class LaserProjectile(
        private val location: Location,
        private val direction: Vector,
        private val speed: Double,
        private val maxDistance: Double = 30.0,
        private val color: Color = Color.RED,
        private val boundingBox: BoundingBox = BoundingBox(
            -0.05,
            -0.05,
            -0.05,
            0.05,
            0.05,
            0.05
        ),
        private val particleSpawner: (effectLoc: Location, i: Double, steps: Int) -> Unit = { effectLoc, i, steps ->
            if (i % 2.0 == 0.0) {
                effectLoc.spawnParticleX(
                    Particle.REDSTONE,
                    count = 1,
                    offsetX = 0.0,
                    offsetY = 0.0,
                    offsetZ = 0.0,
                    data = Particle.DustOptions(color, 0.5f),
                    longDistance = true
                )
            }
        },
        private val explosionHandler: ((source: LaserProjectile, location: Location) -> Unit)? = null
    ) {
        private var currentDistance = 0.0
        private val calcBoundingBox = BoundingBox()

        fun update(): Boolean {
            val effectLoc = location.clone().add(direction.clone().normalize().multiply(currentDistance))
            val steps = (speed * 20 * 6).roundToInt()
            val stepDistance = 1.0 / steps.toDouble()
            val stepDirection = direction.clone().normalize().multiply(stepDistance)
//            Logger.debug("===============================")
//            Logger.debug(
//                "at=${
//                    effectLoc.toVector().asString(2)
//                } stepDistance=${stepDistance.format(2)} direction=${direction.asString(2)}"
//            )

//        Logger.debug("Handling hits with ${targets.size} targets")

            iLoop@ for (i in generateSequence(0.0, { it + stepDistance }).takeWhile { it <= speed }) {
//                Logger.debug("i=${i.format(2)}")
                effectLoc.add(stepDirection)
                currentDistance += stepDistance

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
                        doExplosion(effectLoc)
                        return false
                    }
                }

                particleSpawner(effectLoc, i, steps)
            }

            if (currentDistance > maxDistance) {
                return false
            }

            return true
        }

        private fun doExplosion(location: Location) {
            explosionHandler?.invoke(this, location)
        }
    }

    companion object {
        fun createVisualProjectile(
            location: Location,
            direction: Vector,
            speed: Double = CoasterMathUtils.kmhToBpt(50.0),
            maxDistance: Double = 15.0
        ) {
            location.world!!.playSound(location, "minecraft:minigame.laser.laser.4", SoundCategory.AMBIENT, 0.2f, 0.1f)

            val projectile = LaserProjectile(
                location,
                direction.clone(),
                speed = speed,
                maxDistance = maxDistance,
            )
            val task = object : BukkitRunnable() {
                override fun run() {
                    if (!projectile.update()) {
                        this.cancel()
                    }
                }
            }
            task.runTaskTimer(PluginProvider.plugin, 1, 1)
        }
    }
}
