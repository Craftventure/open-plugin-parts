package net.craftventure.core.ride.trackedride.car.effect

import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import kotlin.math.cos
import kotlin.math.sin

class FloatingMovementHandler(
    currentValue: Double,
    floatingSpeed: Double,
    floatingMultiplier: Double,
    timeOffset: Long,
    private val applyChecker: (car: RideCar) -> Boolean
) : CoasterRideTrain.MovementHandler {
    var currentValue = 0.0
        private set
    var floatingSpeed = 0.0
    var floatingMultiplier = 0.0
    private var time = System.currentTimeMillis()
    private var timeOffset: Long = 0

    init {
        this.currentValue = currentValue
        this.floatingSpeed = floatingSpeed
        this.floatingMultiplier = floatingMultiplier
        this.timeOffset = timeOffset
    }

    override fun handlePitchRadian(pitch: Double, rideCar: RideCar): Double {
        if (!applyChecker(rideCar)) return pitch
        time = System.currentTimeMillis()
        return pitch + cos((timeOffset + time) * floatingSpeed) * floatingMultiplier
    }

    override fun handleYawRadian(yaw: Double, rideCar: RideCar): Double {
        return yaw
    }

    override fun handleBanking(bankingDegrees: Double, rideCar: RideCar): Double {
        if (!applyChecker(rideCar)) return bankingDegrees
        time = System.currentTimeMillis()
        return bankingDegrees + sin((timeOffset + time) * floatingSpeed) * floatingMultiplier
    }
}