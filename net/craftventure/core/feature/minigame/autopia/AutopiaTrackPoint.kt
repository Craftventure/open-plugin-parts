package net.craftventure.core.feature.minigame.autopia

import com.squareup.moshi.JsonClass
import org.bukkit.Location

@JsonClass(generateAdapter = true)
data class AutopiaTrackPoint(
    val location: Location,
    val size: Double
)