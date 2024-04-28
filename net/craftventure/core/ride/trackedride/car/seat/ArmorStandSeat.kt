package net.craftventure.core.ride.trackedride.car.seat

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.entitymeta.Meta
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.ride.RotationFixer
import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.RideTrain
import net.craftventure.core.ride.trackedride.TrackedRide
import net.craftventure.core.ride.trackedride.TrackedRideHelper
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.utils.EntityUtils.setInstantUpdate
import net.craftventure.core.utils.ItemStackUtils
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle


class ArmorStandSeat @JvmOverloads constructor(
    rightOffset: Double,
    upOffset: Double,
    forwardOffset: Double,
    passengerCar: Boolean,
    entityName: String,
    permanentYawOffset: Float = 0f
) : Seat<ArmorStand>(
    rightOffset,
    upOffset,
    forwardOffset,
    passengerCar,
    entityName,
    permanentYawOffset
) {
    //    private var modelRotation: Double = 0.0
    private var model: ItemStack? = null
    private val rotationFixer = RotationFixer()
    private val quaternion = Quaternion()
    private val quaternion2 = Quaternion()

    fun hasModel() = model != null

    fun setModel(model: ItemStack): ArmorStandSeat {
        this.model = model
        this.setShouldAlwaysTeleport(true)
        return this
    }

    override fun spawn(
        world: World,
        x: Double,
        y: Double,
        z: Double,
        yaw: Double,
        pitch: Double,
        car: RideCar
    ): ArmorStand {
        val armorStand = world.spawn(Location(world, x, y, z, yaw.toFloat(), pitch.toFloat()), ArmorStand::class.java)
        armorStand.addDisabledSlots(*EquipmentSlot.values())
        Meta.setEntityMeta(armorStand, Meta.createTempKey(RideTrain.KEY_TRAIN), car.attachedTrain)
        Meta.setEntityMeta(armorStand, Meta.createTempKey(RideCar.KEY_CAR), car)
        Meta.setEntityMeta(armorStand, Meta.createTempKey(KEY_SEAT), this)
        armorStand.setOwner(car.trackedRide!!)
//        if (model != null)
        armorStand.setInstantUpdate()
        if (isPassengerCar)
            TrackedRideHelper.setCarEntity(armorStand, car)
        else
            TrackedRideHelper.setCarModelEntity(armorStand, car)
        armorStand.isSilent = true
        armorStand.isInvulnerable = true
        armorStand.setCanTick(false)
//        EntityUtils.noClip(armorStand, true)
//        EntityUtils.invisible(armorStand, true)
        armorStand.setGravity(false)
        armorStand.setBasePlate(false)
        armorStand.isVisible = false
        armorStand.customName = entityName
        if (model != null) {
            armorStand.setHelmet(model!!.clone())
        }
        return armorStand
    }

    val useQuat: Boolean
        get() = model != null// && !isPassengerCar

    override fun move(
        x: Double,
        y: Double,
        z: Double,
        trackYawRadian: Double,
        trackPitchRadian: Double,
        bankingDegree: Double,
        forceUpdate: Boolean,
        car: RideCar
    ) {
        super.move(
            x,
            y - 1.5,
            z,
            /*if (useQuat) 0.0 else*/ trackYawRadian,
            /*if (useQuat) 0.0 else*/ trackPitchRadian,
            /*if (useQuat) 0.0 else*/ bankingDegree,
            forceUpdate,
            car
        )
    }

    //    override fun applyYaw(): Boolean = !useQuat
    override fun applyPitch(): Boolean = !useQuat
    override fun applyRoll(): Boolean = !useQuat

    override fun move(
        entity: ArmorStand,
        x: Double,
        y: Double,
        z: Double,
        trackYawRadian: Double,
        trackPitchRadian: Double,
        bankingDegree: Double,
        forceUpdate: Boolean,
        rideCar: RideCar
    ) {

//        val passengers = entity.passengers
        if (model != null/* || passengers.isNotEmpty()*/) {
//            if (useQuat) {
            val x = Math.toRadians(bankingDegree) + modelEulerOffset.x
            val z = (trackPitchRadian + Math.PI * 0.5) + modelEulerOffset.z

            quaternion.setIdentity()
            quaternion.rotateYawPitchRoll(
                Math.toDegrees(-z),
                0.0,
                -bankingDegree + Math.toDegrees(modelEulerOffset.x)
            )// Math.toRadians(bankingDegree))
//                quaternion.setFromYawPitchRoll(-Math.toRadians(bankingDegree), 0.0, -z)// Math.toRadians(bankingDegree))
//                quaternion.rotateY(Math.toDegrees(-trackYawRadian))
//                quaternion.rotateX(Math.toDegrees(-z))
//                quaternion.rotateZ(bankingDegree)
//                quaternion.rotateYawPitchRoll(0.0, Math.toDegrees(trackYawRadian), 0.0)//modelEulerOffset.y, Math.toDegrees(x))

//            if (passengers.isNotEmpty()) {
//                quaternion2.setTo(quaternion)
//                quaternion2.setIdentity()
//                quaternion2.rotateY(-Math.toDegrees(trackYawRadian))
//                quaternion2.multiply(quaternion)
//
//                passengers.forEach { passenger ->
//                    if (passenger is Player) {
//                        logcat { "Applying quat2 to ${passenger.name}" }
//                        SmoothCoastersHelper.api.setRotation(
//                            null,
//                            passenger,
//                            quaternion2.x.toFloat(),
//                            quaternion2.y.toFloat(),
//                            quaternion2.z.toFloat(),
//                            quaternion2.w.toFloat(),
//                            1
//                        )
//                    }
//                }
//            }
            if (model != null) {
                rotationFixer.setNextRotation(quaternion)

                val rotation = rotationFixer.getCurrentRotation()
                entity.headPose = EulerAngle(
                    Math.toRadians(rotation.x),
                    Math.toRadians(rotation.y),
                    Math.toRadians(rotation.z),
                )
            }
//            poseUpdater.setHeadPoseWithPacket(
//                entity,
//                Math.toRadians(rotation.x),
//                Math.toRadians(rotation.y),
//                Math.toRadians(rotation.z)
//            )
//                entity.isCustomNameVisible = true
//                entity.customName =
//                    "trackYawRadian=${trackYawRadian.format(2)} trackPitchRadian=${trackPitchRadian.format(2)} x=${rotation.x.format(
//                        2
//                    )} y=${rotation.y.format(2)} z=${rotation.z.format(2)}"
//            } else {
//                val x = Math.toRadians(bankingDegree) + modelEulerOffset.x
//                val z = (trackPitchRadian + Math.PI * 0.5) + modelEulerOffset.z
//
//                rotationFixer.setNextRotation(-z, 0.0 + modelEulerOffset.y, x)
//
//                val rotation = rotationFixer.getCurrentRotation()
//
//                poseUpdater.setHeadPoseWithPacket(entity, rotation.x, rotation.y, rotation.z)
//            }
        }
    }

    @JsonClass(generateAdapter = true)
    class Json : Seat.Json() {
        var model: String? = null
        var isMiniArmorStand: Boolean = false

        override fun create(ride: TrackedRide, car: DynamicSeatedRideCar) = ArmorStandSeat(
            rightOffset,
            upOffset,
            forwardOffset,
            isPassengerCar,
            ride.name,
            permanentYawOffset,
        ).apply {
            permission = this@Json.permission
            model = ItemStackUtils.fromString(this@Json.model)
            this.isMiniArmorStand = this@Json.isMiniArmorStand
        }
    }
}
