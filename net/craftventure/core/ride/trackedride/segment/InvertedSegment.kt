package net.craftventure.core.ride.trackedride.segment

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.trackedride.SplinedTrackSegmentJson
import net.craftventure.core.ride.trackedride.TrackSegment
import net.craftventure.core.ride.trackedride.TrackedRide

class InvertedSegment(id: String, displayName: String, trackedRide: TrackedRide) :
    SplinedTrackSegment(id, displayName, trackedRide) {
    constructor(id: String, trackedRide: TrackedRide) : this(id, id, trackedRide)

    override fun transformPitch(pitch: Double): Double {
        return super.transformPitch(-pitch)
    }

    override fun transformYaw(yaw: Double): Double {
        return super.transformYaw(yaw + Math.PI)
    }

    override fun toJson(): Json {
        val json = Json()
        return toJson(json)
    }

    @JsonClass(generateAdapter = true)
    open class Json : SplinedTrackSegmentJson() {
        override fun create(trackedRide: TrackedRide): TrackSegment =
            InvertedSegment(id, displayName, trackedRide).apply {
                this.restore(this@Json)
            }
    }
}
