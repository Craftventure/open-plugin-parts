package net.craftventure.audioserver.config

import com.squareup.moshi.JsonClass
import net.craftventure.audioserver.packet.AudioPacketArea

@JsonClass(generateAdapter = true)
class AudioMapOverlay(
    val id: String,
    val group: String?,
    val base64: String?,
    val url: String?,
    val x: Int,
    val z: Int,
    val width: Int,
    val height: Int,
    val zIndex: Int,
    val visibilityAreas: List<AudioPacketArea>,
)
