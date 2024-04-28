package net.craftventure.core.animation

import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.utils.SimpleInterpolator
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.EntityType
import org.bukkit.scheduler.BukkitRunnable

class VerticalDoor @JvmOverloads constructor(
    private val blockData: BlockData,
    private val moveHeight: Double,
    private val sourceLocations: Array<Location>,
    private val simpleInterpolator: SimpleInterpolator,
    private val npcEntityTracker: NpcEntityTracker,
    private val yClamp: (Location, Double) -> Double = { location, y -> y }
) : Door {
    private var fallingBlocks: MutableList<NpcEntity> = mutableListOf()
    private var runnable: BukkitRunnable? = null
    override fun open(ticks: Int) {
        if (runnable != null) {
            runnable!!.cancel()
            runnable = null
        }
        requireEntities()
        npcEntityTracker.startTracking()
        //        Logger.console("Open");
//        animate(ticks, true);
        runnable = object : BukkitRunnable() {
            var currentTick = 0
            override fun run() {
                currentTick++
//                logcat { "Tick open=$currentTick for ${javaClass.simpleName}" }
                val percentage = simpleInterpolator.interpolate(currentTick.toDouble(), 0.0, 1.0, ticks.toDouble())
                //                Logger.console("%" + percentage + " Y= " + sourceLocations[0].getY() + ", delta" + (moveHeight * percentage));
//                Logger.console("Y" + (sourceLocations[0].getY() + (moveHeight * percentage)));
                if (currentTick > ticks) {
                    for (location in sourceLocations) {
                        val newLocation = location.clone()
                        newLocation.y = yClamp(location, newLocation.y + moveHeight)
                        newLocation.block.blockData = blockData
                    }
                    executeSync {
                        npcEntityTracker.stopTracking()
                    }
                    cancel()
                } else {
                    for (i in fallingBlocks.indices) {
                        fallingBlocks[i].move(
                            sourceLocations[i].x,
                            yClamp(sourceLocations[i], sourceLocations[i].y + moveHeight * percentage),
                            sourceLocations[i].z
                        )
                    }
                }

                if (currentTick == 1) {
                    for (location in sourceLocations) {
                        location.block.type = Material.AIR
                    }
                }
            }
        }
        runnable!!.runTaskTimer(CraftventureCore.getInstance(), 1L, 1L)
    }

    val isRunning: Boolean
        get() = runnable != null && !runnable!!.isCancelled

    override fun close(ticks: Int) {
        if (runnable != null) {
            runnable!!.cancel()
            runnable = null
        }
        requireEntities()
        npcEntityTracker.startTracking()
        //        Logger.console("Close");
//        animate(ticks, false);
        runnable = object : BukkitRunnable() {
            var currentTick = 0
            override fun run() {
                currentTick++
//                logcat { "Tick close=$currentTick for ${javaClass.simpleName}" }
                val percentage = 1 - simpleInterpolator.interpolate(currentTick.toDouble(), 0.0, 1.0, ticks.toDouble())
                //                Logger.console("%" + percentage + " Y= " + sourceLocations[0].getY() + ", delta" + (moveHeight * percentage));
//                Logger.console("Y" + (sourceLocations[0].getY() + (moveHeight * percentage)));
                if (currentTick > ticks) {
                    for (location in sourceLocations) {
                        location.block.blockData = blockData
                    }
                    executeSync {
                        npcEntityTracker.stopTracking()
                    }
                    cancel()
                } else {
                    for (i in fallingBlocks.indices) {
                        fallingBlocks[i].move(
                            sourceLocations[i].x,
                            yClamp(sourceLocations[i], sourceLocations[i].y + moveHeight * percentage),
                            sourceLocations[i].z
                        )
                    }
                }

                if (currentTick == 1) {
                    for (location in sourceLocations) {
                        val newLocation = location.clone()
                        newLocation.y = newLocation.y + moveHeight
                        newLocation.block.type = Material.AIR
                    }
                }
            }
        }
        runnable!!.runTaskTimer(CraftventureCore.getInstance(), 1L, 1L)
    }

    private fun requireEntities() {
        if (fallingBlocks.size == 0) {
            for (i in sourceLocations.indices) {
                val location = sourceLocations[i].clone()
                location.y = yClamp(location, location.y + moveHeight)
                val entity = NpcEntity("verticalDoor", EntityType.FALLING_BLOCK, location)
                entity.noGravity(true)
                entity.setBlockData(blockData)
                fallingBlocks += entity
                npcEntityTracker.addEntity(fallingBlocks[i])
            }
        }
    }
}