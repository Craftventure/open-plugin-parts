package net.craftventure.core.ride.trackedride.segment

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.trackedride.*
import org.bukkit.util.Vector


class ForkRouterSegment(
    id: String,
    displayName: String,
    trackedRide: TrackedRide,
    private val forkSegments: MutableList<RoutingEntry>
) : TransportSegment(id, displayName, trackedRide, 0.0, 0.0, 0.0, 0.0) {
    private var currentChoice: RoutingEntry? = null

    constructor(id: String, trackedRide: TrackedRide, forkSegments: MutableList<RoutingEntry>) : this(
        id,
        id,
        trackedRide,
        forkSegments
    )

    init {
        this.currentChoice = forkSegments.getOrNull(0)
    }

    override fun initialize() {
        super.initialize()
        for (i in forkSegments.indices) {
            val routingEntry = forkSegments[i]
            routingEntry.track.length
        }
        currentChoice = forkSegments[0]
    }

    override fun getSubsegments(): List<TrackSegment> {
        val segments = ArrayList(super.getSubsegments())
        for (routingEntry in forkSegments) {
            segments.add(routingEntry.track)
        }
        return segments
    }

    override fun getNextTrackSegment(): TrackSegment {
        return currentChoice!!.exit
    }

    override fun getLength(): Double {
        return currentChoice!!.track.length
    }

    override fun getBanking(distance: Double, applyInterceptors: Boolean): Double {
        return currentChoice!!.track.getBanking(distance, applyInterceptors)
    }

    override fun getPosition(distance: Double, position: Vector, applyInterceptors: Boolean) {
        currentChoice!!.track.getPosition(distance, position, applyInterceptors)
    }

    override fun reserveBlockForTrain(
        sourceSegment: TrackSegment,
        previousSegment: TrackSegment,
        rideTrain: RideTrain
    ): Boolean {
        if (isContainsTrainCached) {
            return false
        }
        for (i in forkSegments.indices) {
            val routingEntry = forkSegments[i]
            if (routingEntry.exit.reserveBlockForTrain(sourceSegment, this, rideTrain)) {
                currentChoice = routingEntry
                //                setPreviousTrackSegment(sourceSegment);
                return true
            }
        }
        return false
    }

    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
        currentChoice!!.track.applyForces(car, distanceSinceLastUpdate)
    }

    override fun applyForceCheck(car: RideCar, currentDistance: Double, previousDistance: Double) {
        currentChoice!!.track.applyForceCheck(car, currentDistance, previousDistance)
    }

    class RoutingEntry(val track: TrackSegment, val exit: TrackSegment) {
        init {
            this.track.setNextTrackSegmentRetroActive(this.exit)
        }
    }

    override fun toJson(): Json {
        val json = Json()
        return toJson(json)
    }

    override fun <T : TrackSegmentJson?> toJson(source: T): T & Any {
        source as Json
        source.forks = forkSegments.map {
            Fork(it.track.id, it.exit.id)
        }
        return super.toJson(source)
    }

    override fun <T : TrackSegmentJson?> restore(source: T) {
        super.restore(source)
        source as Json
        initializers.add(Runnable {
            source.forks.forEach {
                val track = trackedRide.getSegmentById(it.trackSegmentId)!!
                val exit = trackedRide.getSegmentById(it.exitSegmentId)!!
                this.forkSegments += RoutingEntry(track, exit)
            }
            this.currentChoice = forkSegments[0]
        })
    }

    @JsonClass(generateAdapter = true)
    open class Json : TransportSegment.Json() {
        lateinit var forks: List<Fork>

        override fun create(trackedRide: TrackedRide): TrackSegment =
            ForkRouterSegment(id, displayName, trackedRide, mutableListOf()).apply {
                this.restore(this@Json)
            }
    }

    @JsonClass(generateAdapter = true)
    class Fork(
        val trackSegmentId: String,
        val exitSegmentId: String
    )
}
