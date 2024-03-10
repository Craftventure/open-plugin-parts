package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketSpatialAudioUpdate(
    @Json(name = "audio_id")
    var audioId: String?,
    var x: Double? = null,
    var y: Double? = null,
    var z: Double? = null,
    @Json(name = "orientation_x")
    var orientationX: Double? = null,
    @Json(name = "orientation_y")
    var orientationY: Double? = null,
    @Json(name = "orientation_z")
    var orientationZ: Double? = null,
    @Json(name = "cone_inner_angle")
    var coneInnerAngle: Double? = null,
    @Json(name = "cone_outer_angle")
    var coneOuterAngle: Double? = null,
    @Json(name = "cone_outer_gain")
    var coneOuterGain: Double? = null,
    @Json(name = "max_distance")
    var maxDistance: Double? = null,
    @Json(name = "ref_distance")
    var refDistance: Double? = null,
    @Json(name = "rolloff_factor")
    var rolloffFactor: Double? = null,
    var volume: Double? = null,
    var rate: Double? = null,
    var playing: Boolean? = null,
    var loop: Boolean? = null,
    var sync: Long? = null,
    @Json(name = "fade_out_start_distance")
    var fadeOutStartDistance: Double? = null,
    @Json(name = "fade_out_end_distance")
    var fadeOutEndDistance: Double? = null
) : BasePacket(PacketID.SPATIAL_AUDIO_UPDATE)
