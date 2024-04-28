package net.craftventure.core.ride.trackedride.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class SceneItem {
    var at: Double = 0.0

    @Json(name = "segment_id")
    var segmentId: String? = null

    var type: Type? = null

    @Json(name = "group_id")
    var groupId: String? = null
    var name: String? = null
    var debug: Boolean = false

    @Json(name = "dont_warn_not_stopping")
    var dontWarnNotStopping: Boolean = false

    @Json(name = "play_without_players")
    val playWithoutPlayers = false
        get() = field || type == Type.STOP

    enum class Type(val starts: Boolean, val stops: Boolean) {
        @Json(name = "start")
        START(true, false),

        @Json(name = "stop")
        STOP(false, true),

        @Json(name = "restart")
        RESTART(true, false)
    }
}