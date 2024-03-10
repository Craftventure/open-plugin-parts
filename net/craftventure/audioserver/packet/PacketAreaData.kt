package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.audioserver.AudioServer
import net.craftventure.audioserver.config.AreaPartCorner
import net.craftventure.audioserver.config.AreaValuePair
import net.craftventure.audioserver.config.AudioArea
import net.craftventure.audioserver.config.Resource

@JsonClass(generateAdapter = true)
class PacketAreaData(
    @Json(name = "repeat_type")
    val repeatType: String,
    val volume: Double,
    val duration: Long,
    val sync: Long,
    @Json(name = "fade_time")
    val fadeTime: Long,
    val preloads: List<String>,
    val overrides: List<String>,
    val resources: List<Resource>,
    val name: String,
    @Json(name = "display_name")
    val displayName: String?,
    @Json(name = "area_id")
    val areaId: Long,
    val filters: List<AreaFilterDto>
) : BasePacket(PacketID.AREA_DEFINITION) {

    constructor(audioArea: AudioArea) : this(
        areaId = audioArea.areaId,
        resources = audioArea.resources,
        fadeTime = audioArea.fadeTime,
        sync = audioArea.loadTime,
        volume = audioArea.volume,
        overrides = audioArea.overrides,
        repeatType = audioArea.repeatType!!.name,
        duration = audioArea.duration,
        name = audioArea.name!!,
        displayName = audioArea.displayName,
        preloads = emptyList(),
        filters = audioArea.filters.map { filter ->
            AreaFilterDto(
                AudioServer.instance.audioServerConfig!!.areaParts
                    .filter { it.name in filter.areas }
                    .map { areaPart ->
                        AreaDto(
                            areaPart.corner1!!, areaPart.corner2!!
                        )
                    },
                filter.feather,
                filter.easing,
                filter.kind,
                filter.type,
                filter.frequency,
                filter.detune,
                filter.gain,
                filter.q
            )
        }
    )

    @JsonClass(generateAdapter = true)
    class AreaFilterDto(
        val areas: List<AreaDto>,
        val feather: Double,
        val easing: String?,
        val kind: String,
        val type: String?,
        val frequency: AreaValuePair<Double>?,
        val detune: AreaValuePair<Double>?,
        val gain: AreaValuePair<Double>?,
        val q: AreaValuePair<Double>?
    )

    @JsonClass(generateAdapter = true)
    class AreaDto(
        val min: AreaPartCorner,
        val max: AreaPartCorner
    )
}
