package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketOperatorControlUpdate(
    @Json(name = "control_model")
    val controlModel: PacketOperatorDefinition.OperatorControlModel
) : BasePacket(PacketID.OPERATOR_CONTROL_UPDATE)