package net.craftventure.core.ride.trackedride

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.trackedride.TrackSegment.*
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment
import org.bukkit.Location


abstract class TrackSegmentJson {
    lateinit var id: String
    lateinit var displayName: String
    var leaveMode: LeaveMode = LeaveMode.LEAVE_TO_EXIT
    var ejectType: EjectType = EjectType.EJECT_TO_EXIT
    var friction: Double = 0.9985
    var gravitationalInfluence: Double = 0.05
    var isBlockSection: Boolean = false
    var blockType: BlockType = BlockType.BLOCK_SECTION
    var disableHaltCheck: Boolean = false
    var nameOnMap: String? = null
    var trackType: TrackType = TrackType.DEFAULT
    var active: Boolean = true
    var shouldAutomaticallyReroutePreviousSegment: Boolean = false
    var offsetFromNextSection: Double = 0.0
    var exitLocationOverride: Location? = null
    var distanceListeners: List<DistanceListenerJson>? = null

    abstract fun create(trackedRide: TrackedRide): TrackSegment
}


@JsonClass(generateAdapter = true)
open class SplinedTrackSegmentJson : TrackSegmentJson() {
    override fun create(trackedRide: TrackedRide): TrackSegment =
        SplinedTrackSegment(id, displayName, trackedRide).apply {
            this.restore(this@SplinedTrackSegmentJson)
        }
}

abstract class DistanceListenerJson {
    var targetDistance: Double = 0.0
    var filterFirstCar: Boolean = false
    var filterLastCar: Boolean = false

    abstract fun create(): DistanceListener
}

@JsonClass(generateAdapter = true)
class EjectAtDistance : DistanceListenerJson() {
    override fun create() = object : DistanceListener(targetDistance, filterFirstCar, filterLastCar) {
        override fun onTargetHit(rideCar: RideCar) {
            rideCar.attachedTrain.eject()
        }
    }
}
