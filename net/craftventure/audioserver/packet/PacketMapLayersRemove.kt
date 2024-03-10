package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketMapLayersRemove(
    // ids
    val remove: Set<String>?,
    val group: String?
) : BasePacket(PacketID.REMOVE_MAP_LAYERS)
