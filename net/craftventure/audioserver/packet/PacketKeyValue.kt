package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketKeyValue(
    val key: String,
    val value: Any
) : BasePacket(PacketID.KEY_VALUE)
