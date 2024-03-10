package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketMarkerRemove(
    // ids
    val remove: Set<String>?,
    val group: String?
) : BasePacket(PacketID.MARKER_REMOVE)
