package net.craftventure.bukkit.ktx.area

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.asString
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector

class SimpleArea(loc1: Location, loc2: Location) : Area() {

    @Deprecated("")
    var loc1: Location

    @Deprecated("")
    var loc2: Location
    override val min: Vector
    override val max: Vector

    override val world: World
        get() = loc1.world!!

    constructor(world: String, x: Double, y: Double, z: Double, x2: Double, y2: Double, z2: Double) : this(
        Location(Bukkit.getWorld(world), x, y, z),
        Location(Bukkit.getWorld(world), x2, y2, z2)
    )

    constructor(world: World, x: Double, y: Double, z: Double, x2: Double, y2: Double, z2: Double) : this(
        Location(world, x, y, z),
        Location(world, x2, y2, z2)
    )

    constructor(
        world: String,
        x: Double,
        y: Double,
        z: Double,
        x2: Double,
        y2: Double,
        z2: Double,
        offset: Vector
    ) : this(
        Location(
            Bukkit.getWorld(world),
            x + offset.x,
            y + offset.y,
            z + offset.z
        ),
        Location(
            Bukkit.getWorld(world),
            x2 + offset.x,
            y2 + offset.y,
            z2 + offset.z
        )
    )

    init {
        val newLoc1 = loc1.clone()
        val newLoc2 = loc2.clone()

        if (loc1.x > loc2.x) {
            newLoc1.x = loc2.x
            newLoc2.x = loc1.x
        }
        if (loc1.y > loc2.y) {
            newLoc1.y = loc2.y
            newLoc2.y = loc1.y
        }
        if (loc1.z > loc2.z) {
            newLoc1.z = loc2.z
            newLoc2.z = loc1.z
        }

        this.loc1 = newLoc1
        this.loc2 = newLoc2
        this.min = newLoc1.toVector()
        this.max = newLoc2.toVector()
    }

    override fun isInArea(x: Double, y: Double, z: Double): Boolean {
        if (x >= loc1.x && x <= loc2.x)
            if (y >= loc1.y && y <= loc2.y)
                if (z >= loc1.z && z <= loc2.z)
                    return true
        return false
    }

    override fun getMinChunk(): Chunk {
        return loc1.chunk
    }

    override fun getMaxChunk(): Chunk {
        return loc2.chunk
    }

    fun getSize(): Vector {
        return Vector(max.x - min.x, max.y - min.y, max.z - min.z)
    }

    override fun toString(): String {
        return "SimpleArea(min=${min.asString()}, max=${max.asString()})"
    }

    fun toJson() = Json(world.name, min, max)

    @JsonClass(generateAdapter = true)
    data class Json(
        val world: String,
        val min: Vector,
        val max: Vector
    ) : Area.Json() {
        override fun create(): SimpleArea {
            val world = Bukkit.getWorld(world)!!
            return SimpleArea(min.toLocation(world), max.toLocation(world))
        }
    }

    @JsonClass(generateAdapter = true)
    data class JsonLegacy(
        val world: String = "world",
        val x_min: Double,
        val x_max: Double,
        val y_min: Double,
        val y_max: Double,
        val z_min: Double,
        val z_max: Double,
    ) : Area.Json() {
        override fun create(): SimpleArea {
            val world = Bukkit.getWorld(world)!!
            return SimpleArea(world, x_min, y_min, z_min, x_max, y_max, z_max)
        }
    }
}
