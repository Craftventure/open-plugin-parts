package net.craftventure.core.ride.trackedride.car

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.rotateY
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.forEachAllocationless
import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.TrackedRide
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.car.seat.Seat
import net.craftventure.core.utils.MathUtil
import org.bukkit.util.Vector

class SpinningCar(name: String, length: Double) : DynamicSeatedRideCar(name, length) {
    private var angle = 0.0
    private val base = Vector(0.0, length / 2.0, 0.0)
    private val calculation = Vector()
    private var angularVelocity = 0.0

    private var lastYaw: Double? = null

    private var spinMode = SpinMode.LOCK_STRAIGHT
    private var targetSpeed: Double = 3.0
    private var brakeForce: Double = 0.996
    private val SPIN_LIMIT = 5.0
    var interactor: SpinningCarInteractor? = null

    fun triggerRotation() {
        if (angularVelocity >= 0)
            angle += 0.01
        else
            angle -= 0.01
    }

    fun triggerRandomRotation(max: Double) {
        angularVelocity += max - (CraftventureCore.getRandom().nextDouble() * max * 2)
    }

    fun setSpinMode(spinMode: SpinMode, targetSpeed: Double = 2.0, brakeForce: Double = 0.98) {
        this.spinMode = spinMode
        this.targetSpeed = targetSpeed
        this.brakeForce = Math.abs(brakeForce)
    }

    override fun move(location: Vector?, trackYawRadian: Double, trackPitchRadian: Double, banking: Double) {
        val previousYaw = (lastYaw ?: trackYawRadian)
        val yawDiff = MathUtil.getRelativeRadianAngleDifference(trackYawRadian, previousYaw)

//        Logger.info("diff=${yawDiff.format(2)} ${trackYawRadian.format(2)}<=>${previousYaw.format(2)}")

        when (spinMode) {
            SpinMode.FREE -> {
                angularVelocity += yawDiff * (velocity + acceleration) * -30 * (CraftventureCore.getRandom()
                    .nextDouble() + 0.3)
                angularVelocity *= brakeForce
                if (angularVelocity > 0) {
                    angularVelocity -= 0.005
                    if (angularVelocity < 0)
                        angularVelocity = 0.0
                } else if (angularVelocity < 0) {
                    angularVelocity += 0.005
                    if (angularVelocity > 0)
                        angularVelocity = 0.0
                }
//                Logger.info("angularVelocity=${angularVelocity.format(2)} yawDiff=${yawDiff.format(2)}")
            }
            SpinMode.CONTROLLED -> {
                angularVelocity *= brakeForce
                if (angularVelocity > 0 && targetSpeed < 0) {
                    angularVelocity -= 0.05

                } else if (angularVelocity < 0 && targetSpeed > 0) {
                    angularVelocity += 0.05

                } else if (angularVelocity >= 0 && targetSpeed > 0) {
                    if (angularVelocity < targetSpeed)
                        angularVelocity = Math.min(angularVelocity + 0.1, targetSpeed)

                } else if (angularVelocity <= 0 && targetSpeed < 0) {
                    if (angularVelocity > targetSpeed)
                        angularVelocity = Math.max(angularVelocity - 0.1, targetSpeed)
                }
            }
            SpinMode.LOCK_STRAIGHT -> {
                if (angularVelocity > SPIN_LIMIT)
                    angularVelocity = SPIN_LIMIT
                if (angularVelocity < -SPIN_LIMIT)
                    angularVelocity = -SPIN_LIMIT

                angularVelocity *= 0.92

                if (angularVelocity > 0) {
                    angularVelocity -= 0.01
                    if (angularVelocity < 0)
                        angularVelocity = 0.0
                } else if (angularVelocity < 0) {
                    angularVelocity += 0.01
                    if (angularVelocity > 0)
                        angularVelocity = 0.0
                }

                val targetSpeed = Math.abs(targetSpeed)

//                Logger.info("targetSpeed=${targetSpeed.format(2)} angularVelocity=${angularVelocity.format(2)} angle=${angle.format(2)}")

                if (angularVelocity >= 0) {
                    if (angularVelocity < targetSpeed)
                        angularVelocity = targetSpeed

                    val targetAngle = if (angle == 0.0) 0.0 else if (angle > 180) 360.0 else 180.0
//                    Logger.info("targetAngleA=${targetAngle.format(2)} +=${angle + angularVelocity}")
                    if (angle + angularVelocity >= targetAngle) {
                        angularVelocity = +0.0
                        angle = targetAngle
//                        Logger.info("angle set to $targetAngle")
                    }
                } else {
                    if (angularVelocity > -targetSpeed)
                        angularVelocity = -targetSpeed

                    val targetAngle = if (angle == 0.0) 0.0 else if (angle < 180) 0.0 else 180.0
//                    Logger.info("targetAngleB=${targetAngle.format(2)} +=${angle + angularVelocity}")
                    if (angle + angularVelocity <= targetAngle) {
                        angularVelocity = -0.0
                        angle = targetAngle
//                        Logger.info("angle set to $targetAngle")
                    }
                }
            }
        }

        lastYaw = trackYawRadian
        angle += angularVelocity.clamp(-SPIN_LIMIT, SPIN_LIMIT)
        while (angle >= 360)
            angle -= 360.0
        while (angle < 0)
            angle += 360.0

        val angleRadian = Math.toRadians(angle)

        seats.forEachAllocationless {
            calculation.set(it.rightOffset, it.upOffset, it.forwardOffset)
            base.set(0.0, 0.0, length * -0.5)
            calculation.rotateY(angleRadian, base)

            if (interactor == null || interactor?.isSpinningSeat(it) == true) {
                if (it is ArmorStandSeat && it.hasModel())
                    it.modelEulerOffset.y = angleRadian
                else
                    it.yawOffset = Math.toDegrees(angleRadian).toFloat()
                it.invertBanking = angle == 180.0
            }
            it.seatPosition.set(calculation)
        }

        super.move(location, trackYawRadian, trackPitchRadian, banking)
    }

    interface SpinningCarInteractor {
        fun isSpinningSeat(seat: Seat<*>): Boolean
    }

    override fun toJson(): Json = toJson(Json())

    @JsonClass(generateAdapter = true)
    class Json : RideCar.Json() {
        override fun create(ride: TrackedRide): RideCar = SpinningCar(ride.name, length)
    }

    enum class SpinMode {
        FREE,
        LOCK_STRAIGHT,
        CONTROLLED
    }
}