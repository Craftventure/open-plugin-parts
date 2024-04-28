package net.craftventure.bukkit.ktx.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import net.craftventure.bukkit.ktx.area.SimpleArea


class SimpleAreaAdapter {
    @FromJson
    fun fromJson(json: Json) = SimpleArea(
        json.world,
        json.x_min,
        json.y_min,
        json.z_min,
        json.x_max,
        json.y_max,
        json.z_max
    )

    @ToJson
    fun toJson(instance: SimpleArea) =
        Json(
            instance.world.name,
            instance.min.x,
            instance.min.y,
            instance.min.z,
            instance.max.x,
            instance.max.y,
            instance.max.z
        )

    @JsonClass(generateAdapter = true)
    data class Json(
        val world: String = "world",
        val x_min: Double,
        val x_max: Double,
        val y_min: Double,
        val y_max: Double,
        val z_min: Double,
        val z_max: Double
    )
}