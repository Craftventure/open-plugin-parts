package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
class AudioPacketArea(
    var xMin: Double,
    var yMin: Double,
    var zMin: Double,
    var xMax: Double,
    var yMax: Double,
    var zMax: Double,
)