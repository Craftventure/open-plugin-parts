package net.craftventure.core.feature.balloon.physics

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.add
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.CraftventureCore
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.utils.InterpolationUtils
import org.bukkit.Location
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.abs

class DefaultBalloonPhysics(
    private val data: Json = Json(),
    private val random: Random = CraftventureCore.getRandom()
) : BalloonPhysics() {
    private val inertia = Vector(0.0, 0.2, 0.0)
    private val temp = Vector(0, 0, 0)
    private val yInertia = 0.05
    private var rotationSpeed = CraftventureCore.getRandom().nextFloat()

    override val currentRotationalForces: Double
        get() = rotationSpeed.toDouble()

    override fun update(
        balloon: ExtensibleBalloon,
        ownerLocation: Location,
        balloonLocation: Location,
        oldBalloonLocation: Location
    ) {
//        val balloonHolder = balloon.balloonHolder ?: return
        val leashLength = (data.maxLeashLength ?: balloon.leashLength)
            .coerceAtMost(balloon.balloonHolder?.maxLeashLength ?: Double.MAX_VALUE)
        rotationSpeed *= 0.97f
        if (rotationSpeed < 1 && rotationSpeed > -1) {
            if (rotationSpeed >= 0)
                rotationSpeed = 1f
            else
                rotationSpeed = -1f
        }
        balloon.angle += rotationSpeed

        temp.set(balloonLocation.x, balloonLocation.y, balloonLocation.z)
        inertia.multiply(0.95)
        if (inertia.y < yInertia) {
            inertia.y = Math.min(yInertia, inertia.y + yInertia)
        }
        //        Logger.console("%s", inertia);
        if (abs(inertia.x) < 0.02 && abs(inertia.y) < 0.06 && abs(inertia.z) < 0.02)
            inertia.add(
                (random.nextDouble() * 0.016) - 0.008,
                (random.nextDouble() * 0.016) - 0.008,
                (random.nextDouble() * 0.016) - 0.008
            )
        balloonLocation.add(inertia.x, inertia.y, inertia.z)

        //        ownerLocation.getWorld().spawnParticle(Particle.END_ROD, ownerLocation, 1, 0, 0, 0, 0);
        val distance = ownerLocation.distance(balloonLocation)
        if (distance > leashLength) {
            val percentage = leashLength / distance
            balloonLocation.x = InterpolationUtils.linearInterpolate(ownerLocation.x, balloonLocation.x, percentage)
            balloonLocation.y = InterpolationUtils.linearInterpolate(ownerLocation.y, balloonLocation.y, percentage)
            balloonLocation.z = InterpolationUtils.linearInterpolate(ownerLocation.z, balloonLocation.z, percentage)
        }
        inertia.set(balloonLocation.x - temp.x, balloonLocation.y - temp.y, balloonLocation.z - temp.z)
        rotationSpeed += (inertia.x - inertia.z).toFloat() * 2.5f
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
        val maxLeashLength: Double? = null
    ) : BalloonPhysics.Json() {
        override fun toPhysics(): BalloonPhysics = DefaultBalloonPhysics(this)

        companion object {
            const val type = "default"
        }
    }
}