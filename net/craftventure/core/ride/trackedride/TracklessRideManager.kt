package net.craftventure.core.ride.trackedride

import net.craftventure.core.CraftventureCore
import net.craftventure.core.api.CvApi
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.npc.actor.RecordingData
import net.craftventure.core.ride.shooter.ShooterEntityFactory
import net.craftventure.core.ride.shooter.ShooterParticlePathFactory
import net.craftventure.core.ride.shooter.ShooterRideContext
import net.craftventure.core.ride.shooter.config.ShooterConfig
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.config.GraphConfig
import net.craftventure.core.ride.tracklessride.config.TracklessRideConfig
import net.craftventure.core.ride.tracklessride.navigation.GraphRouter
import net.craftventure.core.ride.tracklessride.navigation.NavigationGraph
import net.craftventure.core.script.particle.ParticleMap
import net.craftventure.core.utils.GsonUtils
import org.bukkit.Bukkit
import java.io.File

object TracklessRideManager {
    private val rides = mutableSetOf<TracklessRide>()
    private var initialised = false
    private fun initialise() {
        if (!initialised) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
//                long start = System.currentTimeMillis();
                for (ride in rides) {
                    ride.update()
                }

                executeAsync {
                    for (trackedRide in rides) {
                        trackedRide.updateAsync()
                    }
                }
            }, 1, 1)
            initialised = true
        }
    }

    @JvmStatic
    fun add(ride: TracklessRide) {
        initialise()
        rides.add(ride)
    }

    @JvmStatic
    fun remove(ride: TracklessRide) {
        rides.remove(ride)
    }

    fun getRide(internalName: String?): TracklessRide? {
        for (trackedRide in rides) {
            if (internalName != null && internalName.equals(trackedRide.id, ignoreCase = true)) {
                return trackedRide
            }
        }
        return null
    }

    fun getRideList(): Set<TracklessRide> = rides

    fun reload() {
        val configRides = rides.filter { it.isFromConfig }
        configRides.forEach { it.destroy() }
        rides.removeAll(configRides)

        val directory = File(CraftventureCore.getInstance().dataFolder, "data/ride")
        directory.listFiles()?.filter { it.isDirectory }?.forEach { directory ->
            val tracklessRideFile = File(directory, "trackless.json")
            val splinesFile = File(directory, "graph.json")
//            Logger.debug("directory ${directory.absolutePath} ${trackedRideFile.exists()} ${splinesFile.exists()}")

            if (tracklessRideFile.exists() && splinesFile.exists()) {
                try {
                    val ride = loadTracklessRide(directory)
                    try {
                        ride.isFromConfig = true
                        ride.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
//                        Logger.capture(e)
                        Logger.severe(
                            "Failed to properly initialize the ride from config ${tracklessRideFile.path} (${e.message}), it's advised to restart the server as it may be corrupted in it's current state. See console for full message",
                            logToCrew = true
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
//                    Logger.capture(e)
                    Logger.severe(
                        "Failed to load trackless ride ${tracklessRideFile.path}: ${e.message}. See console for full message",
                        logToCrew = true
                    )
                }
            }
        }
    }

    private fun loadTracklessRide(directory: File): TracklessRide {
        val configFile = File(directory, "trackless.json")
        val splinesFile = File(directory, "graph.json")
        val shooterConfigFile = File(directory, "shooterconfig.json")

        val navigation = NavigationGraph()
        val trackContent = splinesFile.readText()
        val trackAdapter = CvMoshi.adapter(GraphConfig::class.java)!!
        val track = trackAdapter.fromJson(trackContent)!!

        track.nodes.forEach { navigation.createNode(it.location, it.id, it.dimensions) }
        track.parts.forEach {
            navigation.addPathPart(it.toPathPart().apply {
//                this as SplinedPathPart
//                Logger.debug("Adding $id with ${this.length.format(2)} length and ${this.trackPieceCount}")
            })
        }

        navigation.requirePrepared()

        val configContent = configFile.readText()
        val configAdapter = CvMoshi.adapter(TracklessRideConfig::class.java)!!
        val config = configAdapter.fromJson(configContent)!!

        val shooterConfig = if (shooterConfigFile.exists()) {
            val shooterConfigContent = shooterConfigFile.readText()
            val shooterConfigAdapter = CvMoshi.adapter(ShooterConfig::class.java)!!
            shooterConfigAdapter.fromJson(shooterConfigContent)!!
        } else {
            null
        }

//        Logger.debug("Nodes")
//        navigation.nodesList.sortedBy { it.id }.forEach { node ->
//            Logger.debug("Node ${node.id} with ${node.links.size} links and ${node.connections.size} connections")
//            node.links.sortedBy { it.part.id }.forEach {
//                Logger.debug("  - linked with ${it.part.id} at ${it.at.format(2)}/${it.location.asString(2)}")
//            }
//            node.connections.sortedBy { it.to.id }.forEach { connection ->
//                Logger.debug("  - connected with ${connection.to.id} over ${connection.by.id} with a length of ${connection.length}")
//            }
//        }

        val shooterRideContext = if (shooterConfig != null) {
            ShooterRideContext(shooterConfig).also { shooterRideContext ->
                val npcDirectory = File(directory, "npc")
                val npcFiles = npcDirectory.walkTopDown().filter { it.isFile && it.name.endsWith(".json") }
                val npcFactories = npcFiles.mapNotNull { npcFile ->
                    try {
                        val id = npcDirectory.toPath().relativize(npcFile.parentFile!!.toPath()).toString()
                            .replace("\\", "/") + "/${npcFile.nameWithoutExtension}"
                        val recordingData = CvApi.gsonActor.fromJson(npcFile.readText(), RecordingData::class.java)
//                        Logger.debug("Loaded entity with id $id")
                        ShooterEntityFactory(
                            id,
                            shooterRideContext,
                            recordingData
                        )
                    } catch (e: Exception) {
                        Logger.capture(e)
                        null
                    }
                }.toList()

                val particleDirectory = File(directory, "particle")
                val particleFiles = particleDirectory.walkTopDown().filter { it.isFile && it.name.endsWith(".json") }
                val particleFactories = particleFiles.mapNotNull { particleFile ->
                    try {
                        val id = particleDirectory.toPath().relativize(particleFile.parentFile!!.toPath()).toString()
                            .replace("\\", "/") + "/${particleFile.nameWithoutExtension}"

                        val particlePath =
                            GsonUtils.read(CvApi.gsonActor, particleFile, ParticleMap::class.java)
//                        Logger.debug("Loaded entity with id $id")
                        ShooterParticlePathFactory(
                            id,
                            shooterRideContext,
                            particlePath
                        )
                    } catch (e: Exception) {
                        Logger.capture(e)
                        null
                    }
                }.toList()

                shooterRideContext.setEntityFactories(npcFactories)
                shooterRideContext.setParticleFactories(particleFactories)
                shooterRideContext.init()
            }
        } else null

        val ride = TracklessRide(
            id = config.id,
            graph = navigation,
            area = config.area.create(),
            exitLocationProvider = { config.exitLocation },
            router = GraphRouter(navigation),
            config = config,
            carFactoryProvider = { group, car ->
                val config = config.carConfig["$group/$car"] ?: config.carConfig["$car"] ?: config.carConfig["*"]!!
                config.createFactory(directory)
            },
            shooterRideContext = shooterRideContext
        )

        config.queues?.map { it.create(ride) }?.forEach { queue ->
            queue.start()
            ride.addQueue(queue)
            ride.addDestroyListener {
                queue.stop()
                ride.removeQueue(queue)
            }
        }

        return ride
    }
}