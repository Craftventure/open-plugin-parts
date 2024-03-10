package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketOperatorRideUpdate(
    val ride: RideUpdate
) : BasePacket(PacketID.OPERATOR_RIDE_UPDATE) {
    @JsonClass(generateAdapter = true)
    class RideUpdate(
        val id: String,
        val name: String? = null,
        @Json(name = "force_display")
        val forceDisplay: Boolean
    )
}