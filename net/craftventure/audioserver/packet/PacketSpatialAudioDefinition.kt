package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketSpatialAudioDefinition(
    @Json(name = "audio_id")
    val audioId: String,
    @Json(name = "sound_url")
    val soundUrl: String,
    @Json(name = "distance_model")
    val distanceModel: DistanceModel = DistanceModel.inverse,
    @Json(name = "panning_model")
    val panningModel: PanningModel = PanningModel.HRTF,
    val state: PacketSpatialAudioUpdate
) : BasePacket(PacketID.SPATIAL_AUDIO_DEFINITION) {
    enum class DistanceModel {
        inverse,
        exponential,
        linear
    }

    enum class PanningModel {
        HRTF
    }
}
