package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketKick(
    var message: String? = null
) : BasePacket(PacketID.KICK)
