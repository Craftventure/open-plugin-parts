package net.craftventure.audioserver.config

import com.squareup.moshi.JsonClass
import net.craftventure.audioserver.packet.AudioPacketPoint

@JsonClass(generateAdapter = true)
class AudioMarker(
    val id: String,
    val type: String? = null,
    val className: String? = null,
    val group: String? = null,
    val popupName: String? = null,
    val base64: String? = null,
    val url: String? = null,
    val x: Double,
    val z: Double,
    val zIndex: Int? = null,
    val size: AudioPacketPoint? = null,
    val anchor: AudioPacketPoint? = null,
    val popupAnchor: AudioPacketPoint? = null,
)
