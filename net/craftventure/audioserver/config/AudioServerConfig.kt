package net.craftventure.audioserver.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AudioServerConfig(
    val `$schema`: String = "https://jsonschema.craftventure.net/model/net.craftventure.audioserver.config.AudioServerConfig.json",
    val areas: List<AudioArea> = emptyList(),
    @Json(name = "area_parts")
    val areaParts: List<AreaPart> = emptyList(),
    val overlays: List<AudioMapOverlay> = emptyList(),
    val markers: List<AudioMarker> = emptyList(),
) {
    fun getAudioAreaByName(name: String?): AudioArea? {
        if (name == null)
            return null
        for (audioArea in areas) {
            if (name == audioArea.name) {
                return audioArea
            }
        }
        return null
    }

    fun mergeWith(overlay: AudioServerConfig): AudioServerConfig {
        val overlayAreaIds = overlay.areas.map { it.name }
        val overlayAreaPartsIds = overlay.areaParts.map { it.name }
        val overlayOverlaysIds = overlay.overlays.map { it.id }
        val overlayMarkersIds = overlay.markers.map { it.id }

        areas.forEach {
            if (it.name in overlayAreaIds)
                throw IllegalStateException("Duplicate area ${it.name} in audio config")
        }

        areaParts.forEach {
            if (it.name in overlayAreaPartsIds)
                throw IllegalStateException("Duplicate area part ${it.name} in audio config")
        }

        overlays.forEach {
            if (it.id in overlayOverlaysIds)
                throw IllegalStateException("Duplicate overlay ${it.id} in audio config")
        }

        markers.forEach {
            if (it.id in overlayMarkersIds)
                throw IllegalStateException("Duplicate marker ${it.id} in audio config")
        }

        return AudioServerConfig(
            this.`$schema`,
            this.areas + overlay.areas,
            this.areaParts + overlay.areaParts,
            this.overlays + overlay.overlays,
            this.markers + overlay.markers,
        )
    }
}
