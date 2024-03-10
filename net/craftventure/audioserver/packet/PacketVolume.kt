package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketVolume(
    val volume: Double,
    val type: String?
) : BasePacket(PacketID.VOLUME) {

    enum class VolumeType {
        master,
        music,
        effect,
        ambient
    }
}
