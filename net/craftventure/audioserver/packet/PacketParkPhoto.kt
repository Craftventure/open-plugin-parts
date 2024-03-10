package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketParkPhoto(
    val data: String,
    val persons: List<PhotoPerson>,
    val name: String,
    val time: Long,
    val type: Type
) : BasePacket(PacketID.PARK_PHOTO) {

    @JsonClass(generateAdapter = true)
    class PhotoPerson(
        val name: String?,
        val uuid: String
    )

    enum class Type {
        ONRIDE_PICTURE
    }
}
