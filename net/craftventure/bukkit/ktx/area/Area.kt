package net.craftventure.bukkit.ktx.area

import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.jsontools.PolymorphicHint
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.util.Vector

/**
 * Created by joey_ on 14-2-2017.
 */
abstract class Area {
    fun isInArea(entity: Entity): Boolean = isInArea(entity.location)
    fun isInArea(location: Location): Boolean = isInArea(location.x, location.y, location.z)
    fun isInArea(vector: Vector) = isInArea(vector.x, vector.y, vector.z)
    abstract fun isInArea(x: Double, y: Double, z: Double): Boolean

    abstract val world: World
    abstract val min: Vector
    abstract val max: Vector

    val xSize: Double
        get() = max.x - min.x
    val ySize: Double
        get() = max.y - min.y
    val zSize: Double
        get() = max.z - min.z

//    fun randomLocation(random: Random = PluginProvider.getRandom()) = Location(
//        world,
//        min.x + xSize * random.nextDouble(),
//        min.y + ySize * random.nextDouble(),
//        min.z + zSize * random.nextDouble()
//    )

    open fun overlaps(chunk: Chunk): Boolean {
        val min = getMinChunk()
        val max = getMaxChunk()

        return chunk.x >= min.x && chunk.x <= max.x || chunk.z >= min.z && chunk.z <= max.z
    }

    open fun getMinChunk(): Chunk {
        return min.toLocation(world).chunk
    }

    open fun getMaxChunk(): Chunk {
        return max.toLocation(world).chunk
    }

    open fun loadChunks(keepLoaded: Boolean) {
        val min = getMinChunk()
        val max = getMaxChunk()

        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                world.loadChunk(x, z)
                if (keepLoaded)
                    world.getChunkAt(x, z).apply {
                        addPluginChunkTicket(PluginProvider.getInstance())
                    }
            }
        }
    }

    open fun getSpannedChunks(): Sequence<Chunk> {
        val min = getMinChunk()
        val max = getMaxChunk()

        return sequence {
            for (x in min.x..max.x) {
                for (z in min.z..max.z) {
                    yield(world.getChunkAt(x, z, false))
                }
            }
        }
    }

    open fun getAllLocations(): Sequence<Location> {
        val min = min
        val max = max

        return sequence {
            for (x in min.blockX..max.blockX) {
                for (y in min.blockY..max.blockY) {
                    for (z in min.blockZ..max.blockZ) {
                        yield(Location(world, x.toDouble(), y.toDouble(), z.toDouble()))
                    }
                }
            }
        }
    }

    operator fun contains(location: Location) = isInArea(location)
    operator fun contains(entity: Entity) = isInArea(entity)
    operator fun contains(vector: Vector) = isInArea(vector)

    @PolymorphicHint(
        types = [
            PolymorphicHint.PolymorphicHintType(
                "simple",
                "net.craftventure.bukkit.ktx.area.SimpleArea"
            )
        ]
    )
    abstract class Json {
        abstract fun create(): Area
    }

    companion object {
        data class ChunkDifference(
            val removed: Set<Long>,
            val added: Set<Long>,
            val kept: Set<Long>,
        )

        fun chunkDifference(old: Area, new: Area): ChunkDifference {
            if (old.getMinChunk() == new.getMinChunk() && old.getMaxChunk() == new.getMaxChunk())
                return ChunkDifference(
                    emptySet(),
                    emptySet(),
                    old.getSpannedChunks().map { it.chunkKey }.toSet(),
                )

            val oldChunks = old.getSpannedChunks().toMutableList()
            val newChunks = new.getSpannedChunks().toMutableList()

            val removed = mutableSetOf<Long>()
            val added = mutableSetOf<Long>()
            val kept = mutableSetOf<Long>()

            oldChunks.forEach {
                if (it !in newChunks) removed += it.chunkKey
                else kept += it.chunkKey
                newChunks.remove(it)
            }

            newChunks.forEach {
                if (it !in oldChunks) added += it.chunkKey
//                else kept += it.chunkKey
            }

            return ChunkDifference(removed, added, kept)
        }
    }
}

operator fun Iterable<Area>.contains(location: Location) = any { it.isInArea(location) }