package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketDisplayAreaEnter(
    val name: String?
) : BasePacket(PacketID.DISPLAY_AREA_ENTER)
