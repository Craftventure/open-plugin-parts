package net.craftventure.core.feature.balloon.physics

import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import org.bukkit.Location

abstract class BalloonPhysics {
    open val currentRotationalForces: Double
        get() = 0.0

    abstract fun update(
        balloon: ExtensibleBalloon,
        ownerLocation: Location,
        balloonLocation: Location,
        oldBalloonLocation: Location
    )

    abstract class Json {
        abstract fun toPhysics(): BalloonPhysics
    }
}