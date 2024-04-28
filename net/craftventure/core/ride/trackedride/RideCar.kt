package net.craftventure.core.ride.trackedride

import net.craftventure.audioserver.api.AudioServerApi.addAndSync
import net.craftventure.audioserver.api.AudioServerApi.remove
import net.craftventure.bukkit.ktx.extension.isCrew
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

abstract class RideCar /*implements FpsTicker.Animatable*/(protected var name: String) {
    var trackSegment: TrackSegment? = null
        protected set(trackSegment) {
            if (trackSegment !== this.trackSegment) {
                onTrackSegmentChanged(this.trackSegment, trackSegment)
                field = trackSegment
            }
        }

    lateinit var attachedTrain: RideTrain
    var distance = 0.0
    var location: Vector = Vector()
        protected set
    var trackedRide: TrackedRide? = null

    @JvmField
    var length = 0.0

    @JvmField
    var yawRadian = 0.0

    @JvmField
    var pitchRadian = 0.0
    var velocity = 1.0 / 20.0
    var acceleration = 0.0

    @JvmField
    var carFrontBogieDistance = 0.0

    @JvmField
    var carRearBogieDistance = 0.0
    val carId: Int
    var audioName: String? = null
        private set
    var sync: Long? = null
        private set

    //    @Override
    //    public void onAnimationUpdate() {
    //        for (Player passenger : getPassengers()) {
    //            PlayerExtensionKt.followDirection(
    //                    passenger,
    //                    yawFreedom,
    //                    pitchFreedom,
    //                    Math.toDegrees(yawRadian + (Math.PI * 0.5)),
    //                    180 - Math.toDegrees(pitchRadian - (Math.PI * 0.5)),
    //                    0.05
    //            );
    ////            passenger.sendMessage(Math.toDegrees(pitchRadian) + " = " + (Math.toDegrees(-(pitchRadian + (Math.PI * 0.5)))));
    //        }
    //    }
    private var hasTrainSound = false
    var meta: MutableMap<String, Any> = hashMapOf()

    open fun addPassenger(player: Player, entity: Entity): Boolean {
        if ((attachedTrain.canEnter || player.isCrew()) && entity.passengers.isEmpty()) {
            return entity.addPassenger(player)
        }
        return false
    }

    open fun enteredCar(player: Player?, entity: Entity?) {
//        Logger.debug("enteredCar")
        trackSegment?.onPlayerEnteredCarOnSegment(this, player)
    }

    open fun exitedCar(player: Player?, entity: Entity?) {
//        Logger.debug("exitedCar")
        trackSegment?.onPlayerExitedCarOnSegment(this, player)
    }

    abstract fun putPassenger(player: Player?): Boolean

    fun setHasTrainSound(hasTrainSound: Boolean): RideCar {
        this.hasTrainSound = hasTrainSound
        return this
    }

    fun stopOnboardSynchronizedAudio() {
        audioName = null
        sync = null
    }

    fun pauzeOnboardSynchronizedAudio() {
        for (player in getPassengers()) {
            remove(audioName!!, player!!)
        }
    }

    fun setOnboardSynchronizedAudio(audioName: String?, sync: Long) {
        this.audioName = audioName
        this.sync = sync
        for (player in getPassengers()) {
            addAndSync(audioName!!, player!!, sync) //now - 5000);
        }
    }

    fun triggerResync(syncTarget: Long, margin: Long) {
        val currentSync = System.currentTimeMillis() - sync!!
        if (currentSync > 0) {
            if (currentSync > syncTarget + margin || currentSync < syncTarget - margin) {
//                Logger.info("Triggering a resync of onboard audio of %s", false, getTrackedRide().getName());
                for (player in getPassengers()) {
                    addAndSync(audioName!!, player!!, System.currentTimeMillis() - syncTarget)
                }
            }
        }
    }

    fun attachTrain(train: RideTrain) {
//        check(attachedTrain == null) { "A RideCar can not be attached to multiple trains, all RideCar's must be separate instances" }
        attachedTrain = train
    }

    abstract fun getMaxifotoPassengerList(): List<Entity?>
    abstract fun getPassengers(): List<Player?>
    fun hasPassengers(): Boolean {
        return getPassengerCount() > 0
    }

    abstract fun getPassengerCount(): Int
    abstract fun isEntitySeatOfCar(entityId: Int): Boolean
    abstract fun mount(entityId: Int, player: Player?): Boolean
    abstract fun containsPlayer(player: Player?): Boolean
    abstract fun canUnmount(player: Player?): Boolean
    abstract fun unmount(player: Player?): Boolean

    abstract fun eject()

    abstract fun spawn(spawnLocation: Location?)
    abstract fun despawn()

    /**
     * @param location
     * @param trackYawRadian   Yaw in radian
     * @param trackPitchRadian Pitch in radian
     * @param bankingDegrees   Banking in degree
     */
    open fun move(
        location: Vector?,
        trackYawRadian: Double,
        trackPitchRadian: Double,
        bankingDegrees: Double
    ) {
    }

    open fun onTrackSegmentChanged(oldSegment: TrackSegment?, newSegment: TrackSegment?) {
        attachedTrain.onTrackSegmentChangedForRideCar(this, oldSegment, newSegment)
    }

    fun setTrackSegmentAndDistance(segment: TrackSegment, distance: Double) {
        this.distance = distance
        trackSegment = segment
    }

    abstract fun toJson(): Json

    open fun <T : Json> toJson(source: T): T {
        source.carFrontBogieDistance = carFrontBogieDistance
        source.carRearBogieDistance = carRearBogieDistance
        source.length = length
        return source
    }

    open fun <T : Json> restore(source: T) {
        carFrontBogieDistance = source.carFrontBogieDistance
        carRearBogieDistance = source.carRearBogieDistance
        length = source.length
    }

    companion object {
        private var CAR_ID = 0
        const val KEY_CAR = "trackedRideCar"
    }

    //    private double yawFreedom = 20.0;
    //    private double pitchFreedom = 50.0;
    init {
        carId = CAR_ID++
        //        FpsTicker.add(this);
    }

    abstract class Json {
        var carFrontBogieDistance = 0.0
        var carRearBogieDistance = 0.0
        var length = 0.0

        abstract fun create(ride: TrackedRide): RideCar
    }
}