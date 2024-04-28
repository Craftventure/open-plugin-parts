package net.craftventure.bukkit.ktx.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import org.bukkit.Location
import org.bukkit.util.Vector


class VectorAdapter {
    @FromJson
    fun fromJson(json: Json) = Vector(json.x, json.y, json.z)

    @ToJson
    fun toJson(instance: Location) = Json(instance.x, instance.y, instance.z)

    @JsonClass(generateAdapter = true)
    data class Json(
        val x: Double = 0.0,
        val y: Double = 0.0,
        val z: Double = 0.0,
    )
}