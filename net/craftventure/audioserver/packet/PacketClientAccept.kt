package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketClientAccept @JvmOverloads constructor(
    val servername: String,
    val uuid: String,
    @Json(name = "send_time")
    val sendTime: Long = System.currentTimeMillis()
) : BasePacket(PacketID.CLIENT_ACCEPTED) {
}
