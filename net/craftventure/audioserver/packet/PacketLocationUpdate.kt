package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketLocationUpdate(
    val x: Double?,
    val y: Double?,
    val z: Double?,
    val yaw: Float?,
    val pitch: Float?
) : BasePacket(PacketID.LOCATION_UPDATE)
