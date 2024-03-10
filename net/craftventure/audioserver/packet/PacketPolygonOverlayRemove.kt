package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketPolygonOverlayRemove(
    val remove: Set<String>?,
    val group: String?
) : BasePacket(PacketID.POLYGON_OVERLAY_REMOVE)
