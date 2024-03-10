package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketReload : BasePacket(PacketID.RELOAD)