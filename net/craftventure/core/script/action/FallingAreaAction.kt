package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.core.async.executeSync
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.utils.LocationUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.util.Vector

@JsonClass(generateAdapter = true)
data class FallingAreaAction(
    @Expose val world: String = "world",
    @Expose val xMin: Double = 0.0,
    @Expose val yMin: Double = 0.0,
    @Expose val zMin: Double = 0.0,
    @Expose val xMax: Double = 0.0,
    @Expose val yMax: Double = 0.0,
    @Expose val zMax: Double = 0.0,
    @Expose val velocityX: Double = 0.0,
    @Expose val velocityY: Double = 0.0,
    @Expose val velocityZ: Double = 0.0,
) : ScriptAction() {

    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
        val locationMin = Location(Bukkit.getWorld(world), xMin, yMin, zMin)
        val locationMax = Location(Bukkit.getWorld(world), xMax, yMax, zMax)
        executeSync {
            if (!LocationUtil.isZero(locationMin) && !LocationUtil.isZero(locationMax)) {
                val vector = Vector(velocityX, velocityY, velocityZ)
                val area = SimpleArea(locationMin, locationMax)
                val loc = area.min.toLocation(Bukkit.getWorld(world)!!).clone()
                val delta = area.getSize()
                val startingBlock = loc.block

                for (x in 0..delta.blockX) {
                    for (z in 0..delta.blockZ) {
                        for (y in 0..delta.blockY) {
                            val block = startingBlock.getRelative(x, y, z)
                            val data = block.blockData
                            block.type = Material.AIR
                            if (data.material.isBlock) {
//                                block.location.spawnParticleX(
//                                    Particle.REDSTONE,
//                                    data = Particle.DustOptions(Color.RED, 4f)
//                                )
                                val fblock =
                                    block.world.spawnFallingBlock(block.location.clone().add(0.5, 0.5, 0.5), data)
                                fblock.velocity = vector
                                fblock.dropItem = false
                            }
                        }
                    }
                }
            }
        }
    }

    override val actionTypeId: Int
        get() = Type.FALLING_AREA
}
