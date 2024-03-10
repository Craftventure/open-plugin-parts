package net.craftventure.core.ride.tracklessride.scene

import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import java.util.concurrent.TimeUnit

abstract class SceneData {
    lateinit var type: String
    lateinit var exitsTo: List<String>
    lateinit var program: Map<Int, List<ProgramPartData<Any>>>
    val limitEntryValue: Int? = null
    val limitEntryUnit: TimeUnit? = null

    abstract fun toScene(tracklessRide: TracklessRide, id: String): TracklessRideScene
}