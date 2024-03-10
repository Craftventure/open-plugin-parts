package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketAreaState(
    @Json(name = "area_id")
    val areaId: Long,
    val playing: Boolean
) : BasePacket(PacketID.AREA_STATE)
