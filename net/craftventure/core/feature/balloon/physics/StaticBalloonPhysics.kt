package net.craftventure.core.feature.balloon.physics

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import org.bukkit.Location

class StaticBalloonPhysics(
    private val data: Json,
) : BalloonPhysics() {
    override val currentRotationalForces: Double
        get() = 0.0

    override fun update(
        balloon: ExtensibleBalloon,
        ownerLocation: Location,
        balloonLocation: Location,
        oldBalloonLocation: Location
    ) {
    }

    @JsonClass(generateAdapter = true)
    class Json : BalloonPhysics.Json() {
        override fun toPhysics(): BalloonPhysics = StaticBalloonPhysics(this)

        companion object {
            const val type = "static"
        }
    }
}