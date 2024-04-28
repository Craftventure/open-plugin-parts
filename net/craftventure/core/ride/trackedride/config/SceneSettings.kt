package net.craftventure.core.ride.trackedride.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class SceneSettings {
    var items: List<SceneItem> = ArrayList()
}