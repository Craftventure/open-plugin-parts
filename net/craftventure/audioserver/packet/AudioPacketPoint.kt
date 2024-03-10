package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class AudioPacketPoint(
    var x: Double,
    var y: Double,
)