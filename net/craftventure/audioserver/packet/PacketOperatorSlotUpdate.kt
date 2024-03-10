package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketOperatorSlotUpdate(
    val data: PacketOperatorDefinition.OperatorSlot
) : BasePacket(PacketID.OPERATOR_SLOT_UPDATE)