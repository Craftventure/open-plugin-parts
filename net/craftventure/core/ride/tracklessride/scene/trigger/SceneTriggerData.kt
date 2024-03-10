package net.craftventure.core.ride.tracklessride.scene.trigger

import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene

abstract class SceneTriggerData {
    lateinit var type: String

    abstract fun toTrigger(scene: TracklessRideScene): SceneTrigger
}