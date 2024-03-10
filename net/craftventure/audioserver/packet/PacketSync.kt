package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketSync(
    @Json(name = "area_id")
    val areaId: Long,
    val sync: Long
) : BasePacket(PacketID.SYNC)
