package net.craftventure.core.feature.balloon.physics

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.add
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.CraftventureCore
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.utils.InterpolationUtils
import net.craftventure.core.utils.LookAtUtil
import org.bukkit.Location
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.abs

class BabyYodaBalloonPhysics(
    private val data: Json = Json(),
    private val random: Random = CraftventureCore.getRandom()
) : BalloonPhysics() {
    private val inertia = Vector(0.0, 0.2, 0.0)
    private val temp = Vector(0, 0, 0)

    //    private var rotationSpeed = CraftventureCore.getRandom().nextFloat()
    private val yawPitch = LookAtUtil.YawPitch()

    override val currentRotationalForces: Double
        get() = 0.0//rotationSpeed.toDouble()

    override fun update(
        balloon: ExtensibleBalloon,
        ownerLocation: Location,
        balloonLocation: Location,
        oldBalloonLocation: Location
    ) {
        val balloonHolder = balloon.balloonHolder ?: return
        val leashLength = (data.maxLeashLength ?: balloon.leashLength)
            .coerceAtMost(balloon.balloonHolder?.maxLeashLength ?: Double.MAX_VALUE)
//        rotationSpeed *= 0.97f
//        if (rotationSpeed < 1 && rotationSpeed > -1) {
//            if (rotationSpeed >= 0)
//                rotationSpeed = 1f
//            else
//                rotationSpeed = -1f
//        }
//        balloon.angle += rotationSpeed * 0.3f
        LookAtUtil.getYawPitchFromRadian(oldBalloonLocation.toVector(), balloonLocation.toVector(), yawPitch)
        balloon.angle =
            AngleUtils.smallestMoveTo(
                balloon.angle.toDouble(),
                Math.toDegrees(yawPitch.yaw) + 90.0,
                maxOf(
                    oldBalloonLocation.distance(balloonLocation) * 50,
                    20.0 / 2.0
                ).coerceIn(-180.0, 180.0)
            )
                .toFloat()//Math.toDegrees(yawPitch.yaw).toFloat()

//        Logger.debug(
//            "target=${
//                Math.toDegrees(yawPitch.yaw).format(2)
//            } actual=${balloon.angle.format(2)} from ${
//                oldBalloonLocation.toVector().asString(2)
//            } ${balloonLocation.toVector().asString(2)}"
//        )

        temp.set(balloonLocation.x, balloonLocation.y, balloonLocation.z)
        inertia.multiply(0.95)
        //        Logger.console("%s", inertia);
        if (abs(inertia.x) < 0.02 && abs(inertia.y) < 0.06 && abs(inertia.z) < 0.02)
            inertia.add(
                (random.nextDouble() * 0.016) - 0.008,
                0.0,
                (random.nextDouble() * 0.016) - 0.008
            )
        balloonLocation.add(inertia.x, inertia.y, inertia.z)

        val ownerCenterLocation = balloonHolder.ownerCenterLocation
        //        ownerLocation.getWorld().spawnParticle(Particle.END_ROD, ownerLocation, 1, 0, 0, 0, 0);
        val distance = ownerCenterLocation.distance(balloonLocation.clone().apply { y = ownerCenterLocation.y })
        val avoidCloseDistance = data.minLeashLength.coerceAtMost(leashLength)
        if (distance > leashLength) {
//            Logger.debug("Too far away (${distance.format(2)})")
            val percentage = leashLength / distance
            balloonLocation.x =
                InterpolationUtils.linearInterpolate(ownerCenterLocation.x, balloonLocation.x, percentage)
            balloonLocation.z =
                InterpolationUtils.linearInterpolate(ownerCenterLocation.z, balloonLocation.z, percentage)
        } else if (distance != 0.0 && distance < avoidCloseDistance) {
//            Logger.debug("Too close! (${distance.format(2)})")
            balloonLocation.x = InterpolationUtils.linearInterpolate(
                ownerCenterLocation.x,
                balloonLocation.x,
                avoidCloseDistance / distance
            )
            balloonLocation.z = InterpolationUtils.linearInterpolate(
                ownerCenterLocation.z,
                balloonLocation.z,
                avoidCloseDistance / distance
            )
        }
        inertia.x = balloonLocation.x - temp.x
        inertia.y = (ownerCenterLocation.y - balloonLocation.y) * 0.8 * (1.0 / 20.0)
        inertia.z = balloonLocation.z - temp.z
//        rotationSpeed += (inertia.x - inertia.z).toFloat() * 2.5f
//        if (oldLocation!!.distanceSquared(location!!) > 25 * 25) {
//            Bukkit.getScheduler().scheduleSyncDelayedTask(
//                CraftventureCore.getInstance(),
//                { tracker!!.forceRespawn(player) },
//                5
//            )
//        }
    }

    @JsonClass(generateAdapter = true)
    class Json(
        val minLeashLength: Double = 1.0,
        val maxLeashLength: Double? = null
    ) : BalloonPhysics.Json() {
        override fun toPhysics(): BalloonPhysics = BabyYodaBalloonPhysics(this)

        companion object {
            const val type = "babyyodafloataround"
        }
    }
}