package net.craftventure.core.ride.trackedride.config

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.trackedride.*
import org.bukkit.Location

@JsonClass(generateAdapter = true)
class TrackedRideConfig {
    lateinit var id: String
    var overlays: List<String>? = null
    var pukeRate: Double? = null
    var finishAchievement: String? = null
    var allowCoasterDay: Boolean = true
    var syncStations: Map<String, List<String>>? = null
    lateinit var exitLocation: Location
    var queues: List<RideQueue.TrackedRideJson>? = null
    lateinit var area: Area.Json
    var operatorArea: Area.Json? = null
    lateinit var carTemplate: Map<String, RideCar.Json>
    lateinit var trains: List<TrainConfig>
    lateinit var track: List<TrackSegmentConfig>
    var addons: List<TrackedRideAddOn>? = null
    var preTrainUpdateListeners: List<PreTrainUpdateListenerJson>? = null
}

@JsonClass(generateAdapter = true)
class TrainConfig {
    lateinit var spawnSegmentId: String
    var spawnDistance: Double = 0.0
    var sounds: MutableSet<TrackSegment.TrackType> = mutableSetOf()
    lateinit var cars: List<String>
}

@JsonClass(generateAdapter = true)
class TrackSegmentConfig {
    lateinit var data: TrackSegmentJson
    lateinit var splineId: String
    lateinit var previousSegmentId: String
    lateinit var nextSegmentId: String
}

@JsonClass(generateAdapter = true)
class SplinesJson {
    lateinit var parts: List<SplinePart>
}

@JsonClass(generateAdapter = true)
class SplinePart {
    lateinit var type: String
    lateinit var id: String
    lateinit var nodes: List<SplineNode.Json>
}

abstract class PreTrainUpdateListenerJson {
    abstract fun create(): TrackedRide.PreTrainUpdateListener
}

@JsonClass(generateAdapter = true)
class SwitchAtDistance : PreTrainUpdateListenerJson() {
    lateinit var segmentId: String
    var distance: Double = 0.0
    lateinit var newSegmentId: String
    var newDistance: Double = 0.0

    override fun create(): TrackedRide.PreTrainUpdateListener =
        TrackedRide.PreTrainUpdateListener { rideCar ->
            try {
                if (rideCar.trackSegment?.id == segmentId && rideCar.distance > distance) {
                    val segment = rideCar.trackedRide!!.getSegmentById(newSegmentId)!!
                    rideCar.attachedTrain.move(segment, newDistance)
                }
            } catch (e: Exception) {
                Logger.warn(
                    "Error at PreTrainUpdateListener for ride ${rideCar.trackedRide?.name}: ${e.message}",
                    logToCrew = true
                )
                Logger.capture(e)
            }
        }
}

abstract class TrackedRideAddOn {
    abstract fun installIn(trackedRide: TrackedRide)
}