package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketAreaRemove(
    @Json(name = "area_id")
    val areaId: String
) : BasePacket(PacketID.AREA_REMOVE)
