package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass
import net.craftventure.audioserver.config.AudioMarker

@JsonClass(generateAdapter = true)
class PacketMarkerAdd(
    val mode: Mode = Mode.ADD,
    val group: String?,
    val markers: Set<MapMarker>
) : BasePacket(PacketID.MARKER_ADD) {

    companion object {
        fun createInitialPacket(overlays: List<AudioMarker>, mode: Mode = Mode.SET, group: String? = null) =
            PacketMarkerAdd(
                mode = mode,
                group = group,
                markers = overlays.map {
                    MapMarker(
                        id = it.id,
                        type = it.type,
                        className = it.className,
                        group = it.group,
                        popupName = it.popupName,
                        base64 = it.base64,
                        url = it.url,
                        x = it.x,
                        z = it.z,
                        zIndex = it.zIndex ?: 1,
                        size = it.size,
                        anchor = it.anchor,
                        popupAnchor = it.popupAnchor,
                    )
                }.toHashSet()
            )
    }

    enum class Mode {
        ADD,
        SET
    }

    @JsonClass(generateAdapter = true)
    data class MapMarker(
        val id: String,
        val type: String? = null,
        val className: String? = null,
        val group: String? = null,
        val popupName: String? = null,
        val base64: String? = null,
        val url: String? = null,
        val x: Double,
        val z: Double,
        val zIndex: Int,
        val size: AudioPacketPoint? = null,
        val anchor: AudioPacketPoint? = null,
        val popupAnchor: AudioPacketPoint? = null,
    )
}
