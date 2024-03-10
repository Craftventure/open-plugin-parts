package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.navigation.GraphNode
import net.craftventure.core.ride.tracklessride.navigation.GraphRouter
import net.craftventure.core.ride.tracklessride.programpart.data.MoveType
import net.craftventure.core.ride.tracklessride.programpart.data.NavigationType
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.scene.trigger.SceneTriggerData
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class NavigateProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<NavigateProgramPart.State>(scene) {
    override val type: String = NavigateProgramPart.type
    private val triggers = data.triggers?.map { it.toTrigger(scene) } ?: emptyList()

//    private var targetNode: GraphNode? = null
//
//    private fun getTargetNode(tracklessRide: TracklessRide): GraphNode {
//        if (targetNode != null) return targetNode!!
//        targetNode = tracklessRide.graph.findNode(data.to)
//        return targetNode!!
//    }

    override fun createInitialState(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
    ): State {
        val nearestNode = ride.controller.routeEndPoint(car) ?: ride.graph.nearestNode(car.pathPosition)
        try {
            var previousTarget: GraphNode = nearestNode
            var routes: GraphRouter.Route? = null

            val fullPath = (data.via ?: emptyList()) + data.to
            fullPath.forEach { toName ->
                val node = ride.graph.findNode(toName)!!
                val route = ride.router.findRoute(previousTarget, node)!!
//                Logger.debug("Existing: ${routes?.route?.firstOrNull()?.id} > ${routes?.route?.lastOrNull()?.id}")
//                Logger.debug("New: ${route.route.first().id} > ${route.route.last().id}")
                if (routes == null) routes = route
                else routes = routes!!.append(route)
                previousTarget = node
            }

//            val targetNode = getTargetNode(ride)

//            val route = ride.router.findRoute(previousTarget ?: nearestNode, targetNode)
//            val route = ride.router.findRoute(nearestNode, targetNode)
//            Logger.debug("Route > ${routes!!.route.joinToString(", ") { it.id }}")
            return State(
                route = routes!!,
                clearOnComplete = !data.waitForCompletion,
                clearOnActionEnd = data.waitForCompletion,
                clearOnSceneSwitch = data.waitForCompletion,
            )
        } catch (e: Exception) {
            val message: String =
                "Failed to find path for car ${group.groupId}/${car.idInGroup} between ${nearestNode.id}>${data.to} (via ${
                    data.via?.joinToString(
                        ", "
                    )
                })"

            e.printStackTrace()
            throw IllegalStateException(message, e)
        }
    }

    override fun getPreviousProgress(state: State): Double = state.previousProgress
    override fun getCurrentProgress(state: State): Double = state.progress

    override fun execute(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        state: State
    ): ExecuteResult {
        if (!state.hasRouteSet)
            state.hasRouteSet = ride.controller.setRoute(
                car = car,
                route = state.route,
                clearOnSceneSwitch = state.clearOnSceneSwitch,
                clearOnActionSwitch = state.clearOnActionEnd,
                clearOnComplete = state.clearOnComplete,
                initialTargetSpeed = data.targetSpeed,
                initialTargetAcceleration = data.targetSpeedAccelerationTick,
                initialTargetDeceleration = data.targetSpeedDecelerationTick,
                disableBraking = data.disableBraking,
                triggers = triggers
            )
        val progress = ride.controller.getRouteProgress(car) ?: return ExecuteResult.ON_HOLD
        state.progress = progress
        if (progress == 1.0) return ExecuteResult.DONE
        if (!data.waitForCompletion) return ExecuteResult.DONE
        return ExecuteResult.PROGRESS
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val navType: NavigationType = NavigationType.TO_NODE,
        val moveType: MoveType = MoveType.SPEED,
        val targetSpeed: Double = CoasterMathUtils.kmhToBpt(5.0),
        val targetSpeedAccelerationTick: Double = 0.1,
        val targetSpeedDecelerationTick: Double = 0.1,
        val to: String,
        val via: List<String>? = null,
        val triggers: List<SceneTriggerData>?,
        val waitForCompletion: Boolean,
        val disableBraking: Boolean = false
    ) : ProgramPartData<State>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<State> = NavigateProgramPart(this, scene)
    }

    data class State(
        val route: GraphRouter.Route,
        var hasRouteSet: Boolean = false,
        var clearOnComplete: Boolean = false,
        var clearOnActionEnd: Boolean = true,
        var clearOnSceneSwitch: Boolean = true,
    ) {
        var previousProgress: Double = 0.0
        var progress: Double = 0.0
            set(value) {
                previousProgress = field
                field = value
            }
    }

    companion object {
        const val type = "navigate"
    }
}