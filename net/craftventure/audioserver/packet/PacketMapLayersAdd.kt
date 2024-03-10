package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass
import net.craftventure.audioserver.config.AudioMapOverlay

@JsonClass(generateAdapter = true)
class PacketMapLayersAdd(
    val mode: Mode = Mode.ADD,
    val group: String?,
    val layers: Set<MapLayer>
) : BasePacket(PacketID.ADD_MAP_LAYERS) {

    companion object {
        fun createInitialPacket(overlays: List<AudioMapOverlay>, mode: Mode = Mode.SET, group: String? = null) =
            PacketMapLayersAdd(
                mode = mode,
                group = group,
                layers = overlays.map {
                    MapLayer(
                        id = it.id,
                        group = it.group,
                        base64 = it.base64,
                        url = it.url,
                        x = it.x,
                        z = it.z,
                        width = it.width,
                        height = it.height,
                        zIndex = it.zIndex,
                        visibilityAreas = it.visibilityAreas
                    )
                }.toHashSet()
            )
    }

    enum class Mode {
        ADD,
        SET
    }

    @JsonClass(generateAdapter = true)
    data class MapLayer(
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
}
