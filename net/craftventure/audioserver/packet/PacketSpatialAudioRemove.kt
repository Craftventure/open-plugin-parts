package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketSpatialAudioRemove(
    @Json(name = "audio_id")
    val audioId: String
) : BasePacket(PacketID.SPATIAL_AUDIO_REMOVE)
