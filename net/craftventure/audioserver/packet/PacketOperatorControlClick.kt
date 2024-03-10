package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketOperatorControlClick(
    @Json(name = "ride_id")
    val rideId: String,
    @Json(name = "control_id")
    val controlId: String
) : BasePacket(PacketID.OPERATOR_CONTROL_CLICK)