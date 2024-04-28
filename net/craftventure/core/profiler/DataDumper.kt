package net.craftventure.core.profiler

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.getCvMetaData
import net.craftventure.core.manager.PlayerStateManager.gameState
import net.craftventure.core.ride.trackedride.FlatrideManager
import net.craftventure.core.ride.trackedride.TrackedRideManager
import net.craftventure.core.utils.HeapDumper
import net.craftventure.core.utils.MapUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.CommandBlock
import org.bukkit.event.HandlerList
import java.io.File
import java.io.PrintWriter
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

object DataDumper {
    private fun writeHeader(printWriter: PrintWriter, name: String) {
        printWriter.apply {
            println()
            println()
            println("===========================")
            println("   $name")
            println("===========================")
        }
    }

    fun dumpGenerics(printWriter: PrintWriter) {
        printWriter.apply {
            writeHeader(printWriter, "Generics")
            BackgroundService.writeDebugTo(printWriter)
        }
    }

    fun dumpThreads(printWriter: PrintWriter) {
        printWriter.apply {
            writeHeader(printWriter, "Threads")
            val traces = Thread.getAllStackTraces()
            for (threadData in traces) {
                val thread = threadData.key
                if (thread.state != Thread.State.WAITING) {
                    val trace = threadData.value
                    println("Thread name=${thread.name} id=${thread.id} interrupted=${thread.isInterrupted} alive=${thread.isAlive} state=${thread.state}")
                    println(trace.joinToString("\n").trim().prependIndent())
                } else {
                    println("Thread name=${thread.name} id=${thread.id} interrupted=${thread.isInterrupted} alive=${thread.isAlive} state=${thread.state}")
                }
                println("=========================================")
            }
        }
    }

    fun dumpPlayers(printWriter: PrintWriter) {
        printWriter.apply {
            writeHeader(printWriter, "Players")
            for (player in Bukkit.getOnlinePlayers()) {
                println(player.name)
                val meta = player.getCvMetaData()
                val state = player.gameState()
                if (meta != null) {
                    println("   - AFK ${meta.afkStatus?.isAfk}")
                    println("   - Ride ${state?.ride?.id}")
                    println("   - Location: ${player.location}")
                    println(
                        "   - Resourcepack: loading/has=${player.resourcePackStatus?.name}"
                    )
                } else {
                    println("Player has no CVMetaData attached!")
                }
            }
        }
    }

    fun dumpWorlds(printWriter: PrintWriter) {
        writeHeader(printWriter, "Worlds")
        for (world in Bukkit.getServer().worlds) {
            printWriter.apply {
                println("World ${world.name}")
                println("   - Loaded chunks ${world.loadedChunks.size}")
                println("   - Loaded entites ${world.entities.size}")
                println("   - Loaded tileentities ${world.loadedChunks.sumOf { it.tileEntities.size }}")
                println("   - Players ${world.players.size}")
                println()

//                println("   Tile entities")
//                for (chunk in world.loadedChunks) {
//                    val tileEntities = chunk.tileEntities
//                    if (tileEntities.isNotEmpty()) {
//                        println("       - Tile entities in chunk x=${chunk.x} z=${chunk.z}")
//                        for (tileEntity in tileEntities) {
//                            println("           - ${tileEntity.type}")
//                        }
//                    }
//                }

                val commandBlocks: List<CommandBlock> = world
                    .loadedChunks
                    .map { it.tileEntities.toList() }
                    .flatten()
                    .filter { it is CommandBlock }
                    .map { it as CommandBlock }
                println("   - Loaded CommandBlocks ${commandBlocks.size}")
                for (commandBlock in commandBlocks) {
                    println("       - Command x=${commandBlock.x} y=${commandBlock.y} z=${commandBlock.z}: ${commandBlock.command}")
                }
            }
        }
    }

    fun dumpWorldsExtended(printWriter: PrintWriter) {
        writeHeader(printWriter, "Worlds (Extended)")
        for (world in Bukkit.getServer().worlds) {
            printWriter.apply {

                for (world in Bukkit.getWorlds()) {
                    printWriter.println("   World " + world.name)
                    var chunkCoordIntegerMap: MutableMap<ChunkCoord, Int> = java.util.HashMap()
                    for (chunk in world.loadedChunks) {
                        chunkCoordIntegerMap[ChunkCoord(chunk.x, chunk.z)] = chunk.tileEntities.size
                    }
                    chunkCoordIntegerMap = MapUtil.sortByValueReversed(chunkCoordIntegerMap)

                    for ((chunkCoord, value) in chunkCoordIntegerMap) {
                        if (value != null && value > 0) {
                            val chunk = world.getChunkAt(chunkCoord.x, chunkCoord.z)

                            val start = System.currentTimeMillis()
                            for (blockState in chunk.tileEntities) {
                                try {
                                    blockState.update()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            val end = System.currentTimeMillis()

                            val block = chunk.getBlock(8, 100, 8)
                            printWriter.println("       x" + chunkCoord.x + " z" + chunkCoord.z + ": " + value + " (updating: " + (end - start) + "ms) /minecraft:tp " + block.x + " " + block.y + " " + block.z)
                        }
                    }
                    printWriter.println()
                }

                for (world in Bukkit.getWorlds()) {
                    printWriter.println("   World " + world.name)
                    var entityCount: MutableMap<Material, Int> = java.util.HashMap()
                    for (chunk in world.loadedChunks) {
                        for (blockState in chunk.tileEntities) {
                            val block = blockState.block
                            var count: Int? = entityCount[block.type]
                            if (count == null)
                                count = 1
                            else
                                count++
                            entityCount[block.type] = count
                        }
                    }

                    entityCount = MapUtil.sortByValueReversed(entityCount)

                    for ((material, value) in entityCount) {
                        printWriter.println("       " + material.name + ": " + value)
                    }

                    printWriter.println()
                }


                for (world in Bukkit.getWorlds()) {
                    for (entity in world.entities) {
                        printWriter.println(
                            "   - ${entity::class.java.name} ${entity.customName} " +
                                    "x=${entity.location.x.toInt()} y=${entity.location.y.toInt()} z=${entity.location.z.toInt()}"
                        )
                    }
                }
            }
        }
    }

    fun dumpListeners(printWriter: PrintWriter) {
        writeHeader(printWriter, "Listeners")
        val handlers = HandlerList.getHandlerLists()
        val counts = HashMap<String, Int>()
        for (handler in handlers) {
            for (listener in handler.registeredListeners) {
                if (listener.plugin is CraftventureCore) {
                    val key = listener.listener.javaClass.name
                    val count = counts.getOrDefault(key, 0) + 1
                    counts[key] = count
                }
            }
        }
        val entries = counts.entries.sortedByDescending { it.value }
        for (entry in entries) {
            printWriter.println("   - ${entry.key}: ${entry.value}")
        }
    }

    fun dumpSystem(printWriter: PrintWriter) {
        printWriter.apply {
            writeHeader(printWriter, "System")
            println("Available processors (cores): " + Runtime.getRuntime().availableProcessors())
            println("Total thread active: ${Thread.activeCount()}")

            /* Total amount of free memory available to the JVM */
            println("Free memory (bytes): " + Runtime.getRuntime().freeMemory())

            /* This will return Long.MAX_VALUE if there is no preset limit */
            val maxMemory = Runtime.getRuntime().maxMemory()
            /* Maximum amount of memory the JVM will attempt to use */
            println("Maximum memory (bytes): " + if (maxMemory == java.lang.Long.MAX_VALUE) "no limit" else maxMemory)

            /* Total memory currently available to the JVM */
            println("Total memory available to JVM (bytes): " + Runtime.getRuntime().totalMemory())

            /* Get a list of all filesystem roots on this system */
            val roots = File.listRoots()

            /* For each filesystem root, print some info */
            for (root in roots) {
                println("\nFile system root: " + root.canonicalPath)
                println("  - Total space (bytes): " + root.totalSpace)
                println("  - Free space (bytes): " + root.freeSpace)
                println("  - Usable space (bytes): " + root.usableSpace)
            }

            val nameOS = "os.name"
            val versionOS = "os.version"
            val architectureOS = "os.arch"
            println("Name of the OS: " + System.getProperty(nameOS))
            println("Version of the OS: " + System.getProperty(versionOS))
            println("Architecture of the OS: " + System.getProperty(architectureOS))
            flush()
            val bean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
            println(bean.systemCpuLoad)
        }
    }

    @Throws()
    fun dumpHeap(file: File) {
        Bukkit.broadcast(CVTextColor.serverNotice + "Server diagnosis started, the server will freeze for a moment")
        HeapDumper.dumpHeap(file.canonicalPath, true)
        Bukkit.broadcast(CVTextColor.serverNotice + "Server diagnosis done")
    }

    fun dumpRides(printWriter: PrintWriter) {
        printWriter.apply {
            writeHeader(printWriter, "Rides")
            for (trackedRide in TrackedRideManager.getTrackedRideList()) {
                println("TrackedRide ${trackedRide.name} (${trackedRide.ride?.displayName})")
                println(" - BlockSections")
                val section = 1
                val numberFormat = DecimalFormat("###")
                for (trackSegment in trackedRide.getTrackSegments()) {
                    if (trackSegment.isBlockSection) {
                        println("     #${numberFormat.format(section.toLong())} ${trackSegment.id}, active? ${trackSegment.blockReservedTrain != null}")
                    }
                }

                for (trackSegment in trackedRide.getTrackSegments()) {
                    println(" - TrackSegment")
                    println("     Id ${trackSegment.id}")
                    var debugData: String? = null
                    try {
                        debugData = trackSegment.debugData()
                    } catch (e: Exception) {
                        debugData = e.message
                        e.printStackTrace()
                    }

                    if (debugData != null) {
                        println("     Debug $debugData")
                    }
                    println("     Length ${trackSegment.length}")
                    println("     BlockSection? ${trackSegment.isBlockSection}")
                    println("     Any trains on segment? ${trackSegment.anyRideTrainOnSegment != null}")
                    println("     BlockingTrain? ${trackSegment.blockReservedTrain != null}")
                }

                for (rideTrain in trackedRide.getRideTrains()) {
                    println(" - RideTrain")
                    println("     Id ${rideTrain.trainId}")
                    println("     Velocity ${rideTrain.velocity}")
                    println("     Passenger count ${rideTrain.passengerCount}")
                    println("     Length ${rideTrain.length}")
                    println("     Front car segment ${rideTrain.frontCarTrackSegment.id}")
                    println("     Last car segment ${rideTrain.lastCarTrackSegment.id}")
                    println("     Front car distance ${rideTrain.frontCarDistance}")
                    println("     Front car segment length ${rideTrain.frontCarTrackSegment.length}")
                    println("     Front distance valid? ${rideTrain.frontCarDistance <= rideTrain.frontCarTrackSegment.length}")
                    for (rideCar in rideTrain.cars) {
                        println("     - RideCar")
                        println("         SegmentId ${rideCar.trackSegment?.id}")
                        println("         Velocity ${rideCar.velocity}")
                        println("         Passenger count ${rideCar.getPassengerCount()}")
                        println("         Length ${rideCar.length}")
                        println("         Distance ${rideCar.distance}")
                        println("         Segment length ${rideCar.trackSegment?.length}")
                        println("         Distance valid? ${rideCar.distance <= (rideCar.trackSegment?.length ?: 0.0)}")
                        println("         Yaw ${Math.toDegrees(rideCar.yawRadian)}")
                        println("         Pitch ${Math.toDegrees(rideCar.pitchRadian)}")
                        for (passenger in rideCar.getPassengers()) {
                            println("             - Passenger ${passenger?.name}")
                        }
                    }
                }
                println()
            }

            for (flatRide in FlatrideManager.getFlatrideList()) {
                println("FlatRide ${flatRide.rideName} (${flatRide?.ride?.displayName})")
                for (passenger in flatRide.passengers) {
                    println(" - Passenger ${passenger.name}")
                }
                println()
            }
        }
    }

    data class ChunkCoord(val x: Int, val z: Int)

    interface CompletionListener {
        fun onDumpCompleted()
    }
}