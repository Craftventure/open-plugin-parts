package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketPing(
    @Json(name = "send_time")
    val sendTime: Long
) : BasePacket(PacketID.PING)
