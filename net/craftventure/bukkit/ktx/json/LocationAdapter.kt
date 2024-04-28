package net.craftventure.bukkit.ktx.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import org.bukkit.Bukkit
import org.bukkit.Location


class LocationAdapter {
    @FromJson
    fun fromJson(json: Json) = Location(
        Bukkit.getWorld(json.world)!!,
        json.x,
        json.y,
        json.z,
        json.yaw ?: 0f,
        json.pitch ?: 0f
    )

    @ToJson
    fun toJson(instance: Location) = Json(
        world = instance.world.name,
        x = instance.x,
        y = instance.y,
        z = instance.z,
        yaw = instance.yaw.takeIf { it != 0f },
        pitch = instance.pitch.takeIf { it != 0f }
    )

    @JsonClass(generateAdapter = true)
    data class Json(
        val world: String = "world",
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float?,
        val pitch: Float?
    )
}