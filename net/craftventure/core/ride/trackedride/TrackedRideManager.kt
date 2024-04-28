package net.craftventure.core.ride.trackedride

import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ride.trackedride.car.effect.FloatingMovementHandler
import net.craftventure.core.ride.trackedride.config.SplinesJson
import net.craftventure.core.ride.trackedride.config.TrackedRideConfig
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.ForkRouterSegment
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import org.bukkit.Bukkit
import org.bukkit.util.Vector
import java.io.File

object TrackedRideManager {
    private val rides = mutableSetOf<TrackedRide>()
    private var initialised = false
    private fun initialise() {
        if (!initialised) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
//                long start = System.currentTimeMillis();
                for (trackedRide in rides) {
                    trackedRide.update()
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
    fun removeTrackedRide(trackedRide: TrackedRide) {
        rides.remove(trackedRide)
    }

    @JvmStatic
    fun addTrackedRide(trackedRide: TrackedRide) {
        initialise()
        rides.add(trackedRide)
    }

    fun getTrackedRide(internalName: String?): TrackedRide? {
        for (trackedRide in rides) {
            if (internalName != null && internalName.equals(trackedRide.name, ignoreCase = true)) {
                return trackedRide
            }
        }
        return null
    }

    fun getTrackedRideList(): Set<TrackedRide> {
        return rides
    }

    fun reload() {
        val configRides = rides.filter { it.isFromConfig }
        configRides.forEach { it.destroy() }
        rides.removeAll(configRides)

        val directory = File(CraftventureCore.getInstance().dataFolder, "data/ride")
        directory.listFiles()?.filter { it.isDirectory }?.forEach { directory ->
            val trackedRideFile = File(directory, "trackedride.json")
            val splinesFile = File(directory, "splines.json")
//            Logger.debug("directory ${directory.absolutePath} ${trackedRideFile.exists()} ${splinesFile.exists()}")

            if (trackedRideFile.exists() && splinesFile.exists()) {
                try {
                    val ride = loadTrackedRide(trackedRideFile, splinesFile)
                    try {
                        ride.isFromConfig = true
                        ride.initialize()
                    } catch (e: Exception) {
                        e.printStackTrace()
//                        Logger.capture(e)
                        Logger.severe(
                            "Failed to properly initialize the ride from config ${trackedRideFile.path} (${e.message}), it's advised to restart the server as it may be corrupted in it's current state. See console for full message",
                            logToCrew = true
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
//                    Logger.capture(e)
                    Logger.severe(
                        "Failed to load tracked ride ${trackedRideFile.path}: ${e.message}. See console for full message",
                        logToCrew = true
                    )
                }
            }
        }
    }

    private fun loadTrackedRide(configFile: File, splinesFile: File): TrackedRide {
        val configContent = configFile.readText()
        val configAdapter = CvMoshi.adapter(TrackedRideConfig::class.java)
        val config = configAdapter.fromJson(configContent)!!

        val splinesContent = splinesFile.readText()
        val splines = CvMoshi.adapter(SplinesJson::class.java).fromJson(splinesContent)!!

        val ride = OperableCoasterTrackedRide(
            config.id,
            config.area.create(),
            config.exitLocation,
            config.finishAchievement,
            config.id
        )
        if (config.operatorArea != null)
            ride.setOperatorArea(config.operatorArea!!.create())

        config.track.forEach { trackConfig ->
            val segment = trackConfig.data.create(ride) as SplinedTrackSegment
//            Logger.debug("Finding ${trackConfig.splineId}")
            val spline = splines.parts.firstOrNull { it.id == trackConfig.splineId }
            if (spline != null) {
//                Logger.debug("Segment ${trackConfig.splineId} adding ${spline.nodes.size} nodes")
                segment.add(Vector(), true, *spline.nodes.map { it.toSplineNode() }.toTypedArray())
            } else {
                if (segment !is ForkRouterSegment)
                    Logger.warn("No spline for for segment ${trackConfig.splineId} at ${config.id}", logToCrew = true)
            }
            ride.addTrackSection(segment)
        }

        config.track.forEach {
            val segment = ride.getSegmentById(it.data.id)!!
            try {
                segment.nextTrackSegment = ride.getSegmentById(it.nextSegmentId)!!
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Failed to find next segment ${it.nextSegmentId} for ${it.splineId} for ride ${config.id}/${configFile}",
                    e
                )
            }
            try {
                segment.previousTrackSegment = ride.getSegmentById(it.previousSegmentId)!!
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Failed to find previous segment ${it.previousSegmentId} for ${it.splineId} for ride ${config.id}/${configFile}",
                    e
                )
            }
        }

        config.trains.forEach {
            val train = CoasterRideTrain(ride.getSegmentById(it.spawnSegmentId)!!, it.spawnDistance)
            if (it.sounds.isNotEmpty()) {
                train.setTrainSoundName(config.id, SpatialTrainSounds.Settings(it.sounds))
            }
            it.cars.forEach {
                val carTemplate = config.carTemplate.getValue(it)
                train.addCar(
                    carTemplate.create(ride)/*.also {
                            Logger.debug(
                                "Car with ${it.carFrontBogieDistance.format(2)}-${
                                    it.carRearBogieDistance.format(
                                        2
                                    )
                                } and length ${it.length.format(2)}, template ${carTemplate.carFrontBogieDistance.format(2)}-${
                                    carTemplate.carRearBogieDistance.format(
                                        2
                                    )
                                }"
                            )
                        }*/
                )
            }
            ride.addTrain(train)
        }

        config.overlays?.forEach { overlayId ->
            when (overlayId) {
                "genericsledgeride" -> {
                    ride.getRideTrains().forEach { rideTrain ->
                        if (rideTrain is CoasterRideTrain)
                            rideTrain.setMovementHandler(FloatingMovementHandler(
                                0.0,
                                0.001 + CraftventureCore.getRandom().nextDouble() * 0.001,
                                0.03 + CraftventureCore.getRandom().nextDouble() * 0.005,
                                CraftventureCore.getRandom().nextInt(10000).toLong()
                            ) { rideCar ->
                                true
                            })
                    }
                }
                else -> {
                    Logger.warn("Unknown overlay for ${configFile.path}: $overlayId", logToCrew = true)
                }
            }
        }

        config.preTrainUpdateListeners?.forEach { listenerConfig ->
            ride.addPreTrainUpdateListener(listenerConfig.create())
        }

        val queues = config.queues?.map { it.create(ride) }
        if (queues != null) {
            queues.forEach { queue ->
                queue.start()
                ride.addQueue(queue)
                ride.addDestroyListener {
                    queue.stop()
                    ride.removeQueue(queue)
                }
            }
        }

        config.syncStations?.forEach { (id, segments) ->
            val mainSegment = ride.getSegmentById(id) as StationSegment
            segments.forEach { segmentId ->
                val otherSegment = ride.getSegmentById(segmentId) as StationSegment
                mainSegment.addSyncNode(otherSegment)
            }
        }

        config.addons?.forEach { addon -> addon.installIn(ride) }

        return ride
    }
}