package net.craftventure.audioserver.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AreaPartCorner(
    val x: Double,
    val y: Double,
    val z: Double
)
