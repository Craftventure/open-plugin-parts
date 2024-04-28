package net.craftventure.bukkit.ktx.area

import com.squareup.moshi.JsonClass
import org.bukkit.World
import org.bukkit.util.Vector

class CombinedArea(vararg val areas: Area) : Area() {
    override val world: World = areas.first().world

    override val min: Vector = Vector(
        areas.minByOrNull { it.min.x }!!.min.x,
        areas.minByOrNull { it.min.y }!!.min.y,
        areas.minByOrNull { it.min.z }!!.min.z
    )
    override val max: Vector = Vector(
        areas.maxByOrNull { it.max.x }!!.max.x,
        areas.maxByOrNull { it.max.y }!!.max.y,
        areas.maxByOrNull { it.max.z }!!.max.z
    )

    override fun isInArea(x: Double, y: Double, z: Double): Boolean {
        return areas.any { it.isInArea(x, y, z) }
    }

    @JsonClass(generateAdapter = true)
    data class Json(
        val areas: List<Area.Json>
    ) : Area.Json() {
        override fun create(): CombinedArea = CombinedArea(*areas.map { it.create() }.toTypedArray())
    }
}
