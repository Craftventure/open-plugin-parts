package net.craftventure.core.ride.tracklessride.programpart.data

import net.craftventure.core.ride.tracklessride.programpart.ProgramPart
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene

abstract class ProgramPartData<S> {
//    lateinit var type: String

    abstract fun toPart(scene: TracklessRideScene): ProgramPart<S>
}