package net.craftventure.audioserver.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AreaFilter(
    val areas: List<String>,
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
class AreaValuePair<T>(
    val active: T,
    val inactive: T
)
