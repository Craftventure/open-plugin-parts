package net.craftventure.core.ride.tracklessride.scene

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide

class TracklessDefaultScene(
    id: String,
    tracklessRide: TracklessRide,
    private val data: Data,
) : TracklessRideScene(id, tracklessRide, data) {

    companion object {
        const val type = "scene"
    }

    @JsonClass(generateAdapter = true)
    class Data : SceneData() {
        override fun toScene(tracklessRide: TracklessRide, id: String): TracklessRideScene =
            TracklessDefaultScene(id, tracklessRide, this)
    }
}