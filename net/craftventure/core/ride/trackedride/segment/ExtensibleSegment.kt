package net.craftventure.core.ride.trackedride.segment

import net.craftventure.core.ktx.extension.forEachAllocationless
import net.craftventure.core.ride.operator.controls.OperatorControl
import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.RideTrain
import net.craftventure.core.ride.trackedride.TrackSegment
import net.craftventure.core.ride.trackedride.TrackedRide
import org.bukkit.entity.Player
import org.bukkit.util.Vector

open class ExtensibleSegment(
    id: String,
    displayName: String = id,
    trackedRide: TrackedRide
) : SplinedTrackSegment(id, displayName, trackedRide), OperableDependentSegment, OperableTrackSegment {
    private val extensions = mutableListOf<Extension>()
    val controls = mutableListOf<ProvidedControl>()

    override fun onOperatorsChanged() {
        controls.forEach { it.operatorChangedListener() }
    }

    override fun provideControls(): List<OperatorControl> = controls.filter { it.active }.map { it.control }

    fun addExtension(extension: Extension): Boolean {
        if (extension.attachedSegment != null) return false
        return extensions.add(extension)
            .apply { extension.attachedSegment = this@ExtensibleSegment } || extensions.contains(extension)
    }

    fun removeExtension(extension: Extension): Boolean {
        return extensions.remove(extension).apply {
            if (this) extension.attachedSegment = null
        }
    }

    override fun onEmergencyStopActivated(eStopActivated: Boolean) {
        super.onEmergencyStopActivated(eStopActivated)
        extensions.forEachAllocationless { if (it.enabled) it.onEmergencyStopActivated(eStopActivated) }
    }

    override fun onPlayerEnteredCarOnSegment(rideCar: RideCar, player: Player) {
        super.onPlayerEnteredCarOnSegment(rideCar, player)
        extensions.forEachAllocationless { if (it.enabled) it.onPlayerEnteredCarOnSegment(rideCar, player) }
    }

    override fun onPlayerExitedCarOnSegment(rideCar: RideCar, player: Player) {
        super.onPlayerExitedCarOnSegment(rideCar, player)
        extensions.forEachAllocationless { if (it.enabled) it.onPlayerExitedCarOnSegment(rideCar, player) }
    }

    override fun onTrainLeftSection(rideTrain: RideTrain) {
        super.onTrainLeftSection(rideTrain)
        extensions.forEachAllocationless { if (it.enabled) it.onTrainLeftSection(rideTrain) }
    }

    override fun onTrainEnteredSection(previousSegment: TrackSegment?, rideTrain: RideTrain) {
        super.onTrainEnteredSection(previousSegment, rideTrain)
        extensions.forEachAllocationless { if (it.enabled) it.onTrainEnteredSection(previousSegment, rideTrain) }
    }

    override fun onDistanceUpdated(car: RideCar, currentDistance: Double, previousDistance: Double) {
        super.onDistanceUpdated(car, currentDistance, previousDistance)
        extensions.forEachAllocationless {
            if (it.enabled) it.onDistanceUpdated(
                car,
                currentDistance,
                previousDistance
            )
        }
    }

    override fun applySecondaryForces(car: RideCar, distanceSinceLastUpdate: Double) {
        super.applySecondaryForces(car, distanceSinceLastUpdate)
        extensions.forEachAllocationless { if (it.enabled) it.applySecondaryForces(car, distanceSinceLastUpdate) }
    }

    override fun update() {
        super.update()
        extensions.forEachAllocationless { if (it.enabled) it.update() }
    }

    override fun getPosition(distance: Double, position: Vector, applyInterceptors: Boolean) {
        super.getPosition(distance, position, applyInterceptors)
        extensions.forEachAllocationless { if (it.enabled) it.getPosition(distance, position, applyInterceptors) }
    }

    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
        super.applyForces(car, distanceSinceLastUpdate)
        extensions.forEachAllocationless { if (it.enabled) it.applyForces(car, distanceSinceLastUpdate) }
    }

    override fun applyForceCheck(car: RideCar, currentDistance: Double, previousDistance: Double) {
        super.applyForceCheck(car, currentDistance, previousDistance)
        extensions.forEachAllocationless { if (it.enabled) it.applyForceCheck(car, currentDistance, previousDistance) }
    }

    interface Extension {
        var enabled: Boolean
        var attachedSegment: TrackSegment?

        val trackedRide: TrackedRide?
            get() = attachedSegment?.trackedRide

        fun onEmergencyStopActivated(eStopActivated: Boolean) {}
        fun onPlayerEnteredCarOnSegment(rideCar: RideCar, player: Player) {}
        fun onPlayerExitedCarOnSegment(rideCar: RideCar, player: Player) {}
        fun onTrainLeftSection(rideTrain: RideTrain) {}
        fun onTrainEnteredSection(previousSegment: TrackSegment?, rideTrain: RideTrain) {}
        fun onDistanceUpdated(car: RideCar, currentDistance: Double, previousDistance: Double) {}
        fun applySecondaryForces(car: RideCar, distanceSinceLastUpdate: Double) {}
        fun update() {}
        fun getPosition(distance: Double, position: Vector, applyInterceptors: Boolean) {}
        fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {}
        fun applyForceCheck(car: RideCar, currentDistance: Double, previousDistance: Double) {}
    }

    data class ProvidedControl(
        val control: OperatorControl,
        var active: Boolean = true,
        val operatorChangedListener: () -> Unit = {}
    )
}
