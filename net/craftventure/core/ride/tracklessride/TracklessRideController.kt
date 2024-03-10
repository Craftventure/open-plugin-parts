package net.craftventure.core.ride.tracklessride

import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ride.tracklessride.navigation.GraphRouter
import net.craftventure.core.ride.tracklessride.navigation.PathPosition
import net.craftventure.core.ride.tracklessride.programpart.ProgramPart
import net.craftventure.core.ride.tracklessride.programpart.data.TagContext
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.scene.trigger.SceneTrigger
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import kotlin.math.max

class TracklessRideController {
    lateinit var ride: TracklessRide
    private var state = hashMapOf<String, Any>()
    private var actionIndex = hashMapOf<String, Int>()
    private var route = hashMapOf<String, List<ProgressedRoute>>()
    private val groupFinishedSince = hashMapOf<Int, Long>()

    fun getProgramPartForCar(car: TracklessRideCar): ProgramPart<Any>? {
        val index = actionIndex.getOrPut(car.key) { 0 }
        val actions = car.group.currentScene.actions[car.idInGroup]
        return actions?.getOrNull(index)
    }

    fun routeEndPoint(car: TracklessRideCar) = route[car.key]?.lastOrNull()?.route?.routeParts?.lastOrNull()?.to

    private fun popCurrentRoute(car: TracklessRideCar) {
        val routes = route[car.key]
        if (routes != null) {
//            if (routes.isNotEmpty())
            val currentRoute = routes.take(1).firstOrNull()
//            Logger.debug("  - Clear current route for ${car.idInGroup} empty=${routes.isEmpty()} current=${currentRoute?.distance?.format(2)} ${currentRoute?.progress?.format(2)}")
            if (currentRoute != null && (currentRoute.progress != 1.0 || currentRoute.distance <= 0.0)) {
//                Logger.debug("  - Current route handling")
                currentRoute.triggers.forEach { trigger ->
                    if (trigger.id !in currentRoute.finishedTriggers) {
//                        Logger.debug("    - Unfinished trigger")
                        if (trigger.forceRunOnCompletion) {
//                            Logger.debug("  - Force completion")
                            trigger.trigger(ride, car.group, car)
                        }
                    }
                }
            }
            route[car.key] = routes.drop(1)
            route[car.key]?.firstOrNull()?.apply {
                applyInitialSettings(car, this)
            }
        }
    }

    private fun applyInitialSettings(car: TracklessRideCar, route: ProgressedRoute) {
//        Logger.debug(
//            "Applying settings to car ${car.idInGroup}g${car.group.groupId}: ${
//                route.initialTargetSpeed?.format(
//                    2
//                )
//            }"
//        )
        if (route.initialTargetSpeed != null)
            car.targetSpeed = route.initialTargetSpeed
        if (route.initialTargetAcceleration != null)
            car.targetAcceleration = route.initialTargetAcceleration
        if (route.initialTargetDeceleration != null)
            car.targetDeceleration = route.initialTargetDeceleration
    }

    private fun handleForceRun(
        tracklessRideCarGroup: TracklessRideCarGroup,
        car: TracklessRideCar,
        scene: TracklessRideScene,
    ) {
        var actionIndex = actionIndex[car.key]
        if (actionIndex != null) {
            val actions = car.group.currentScene.actions[car.idInGroup]
            if (actions != null) {
                var action = actions.getOrNull(actionIndex)
                while (action != null) {
                    val state = getState(car, action)
                    action.handleForceRun(ride, tracklessRideCarGroup, car, state)
                    actionIndex++
                    action = actions.getOrNull(actionIndex)
                }
            }
        }
    }

    private fun switchScene(tracklessRideCarGroup: TracklessRideCarGroup, scene: TracklessRideScene): Boolean {
//        Logger.debug("Trying to switch scene ${scene.id}")
        val current = tracklessRideCarGroup.currentScene
        val allowEnter = scene.allowEnter(tracklessRideCarGroup)
        if (!allowEnter) return false

        if (scene.enter(tracklessRideCarGroup)) {
            tracklessRideCarGroup.cars.forEach { car ->
                val program = getProgramPartForCar(car)
                if (program != null)
                    state[car.key]?.let {
                        program.clear(ride, car.group, car, program)
                    }
            }

            tracklessRideCarGroup.allowPrematureSceneSwitching = false

            ride.scenes.forEach { (t, u) -> u.dequeue(tracklessRideCarGroup) }
//            Logger.debug("Switched scene ${scene.id}")
            current.exit(tracklessRideCarGroup)
            tracklessRideCarGroup.exitToOverrides = null
            tracklessRideCarGroup.clear(TagContext.SCENE)
            tracklessRideCarGroup.cars.forEach { it.clear(TagContext.SCENE) }
            tracklessRideCarGroup.currentScene = scene

            tracklessRideCarGroup.cars.forEach { car ->
                handleForceRun(tracklessRideCarGroup, car, scene)
                state.remove(car.key)
//                popCurrentRoute(it, false, true)
//                route.remove(it)
                val currentRoute = route[car.key]?.firstOrNull()
                if (currentRoute?.clearOnSceneSwitch == true)
                    popCurrentRoute(car)

                actionIndex.remove(car.key)
            }
            return true
        }
        return false
    }

    private fun advancedAction(car: TracklessRideCar) {
        val currentActionIndex = actionIndex.getOrPut(car.key) { 0 }
        actionIndex[car.key] = currentActionIndex + 1
        val program = getProgramPartForCar(car)
        if (program != null)
            state[car.key]?.let {
                program.clear(ride, car.group, car, program)
            }
//        route.remove(car)
        state.remove(car.key)

        val currentRoute = route[car.key]?.firstOrNull()
        if (currentRoute?.clearOnActionSwitch == true)
            popCurrentRoute(car)

//        Logger.debug("  - Advancing ${car.idInGroup} to next action, next up ${getProgramPartForCar(car)?.type}")
    }

    fun getState(car: TracklessRideCar, programPart: ProgramPart<*>): Any {
        return state.getOrPut(car.key) { programPart.createInitialState(car.tracklessRide, car.group, car)!! }
    }

    fun getRouteProgress(car: TracklessRideCar) = this.route[car.key]?.firstOrNull()?.progress

    fun queueToScene(group: TracklessRideCarGroup, sceneId: String) {
        val scene = ride.scenes.getValue(sceneId)
        scene.queue(group)
    }

    fun canEnterQueuedScene(group: TracklessRideCarGroup) =
        ride.scenes.values.filter { it.isQueued(group) }.any { it.allowEnter(group) }

    fun allActionsFinished(group: TracklessRideCarGroup) = group.cars.all { getProgramPartForCar(it) == null }

    fun canContinueToNextSceneIfActionsFinished(tracklessRideCarGroup: TracklessRideCarGroup): Boolean {
        val queuedScenes = ride.scenes.values.filter { it.isQueued(tracklessRideCarGroup) }
        for (it in queuedScenes) {
            if (it.allowEnter(tracklessRideCarGroup)) {
                return true
            }
        }

        (tracklessRideCarGroup.exitToOverrides
            ?: tracklessRideCarGroup.currentScene.sceneData.exitsTo).forEach {
            val targetScene = ride.scenes.getValue(it)
            if (targetScene.allowEnter(tracklessRideCarGroup))
                return true
        }
        return false
    }

    fun updateAsync() {

    }

    fun update() {
//        Logger.debug("======================")
//        Logger.debug("Controller update")

        ride.activeGroups().forEach { tracklessRideCarGroup ->
            tracklessRideCarGroup.currentScene.update()

            if (allActionsFinished(tracklessRideCarGroup)) {
                groupFinishedSince.putIfAbsent(tracklessRideCarGroup.groupId, System.currentTimeMillis())
            } else {
                groupFinishedSince.remove(tracklessRideCarGroup.groupId)
            }
        }

        ride.activeGroups().sortedBy { groupFinishedSince[it.groupId] }.forEach { tracklessRideCarGroup ->
            if (tracklessRideCarGroup.allowPrematureSceneSwitching || allActionsFinished(tracklessRideCarGroup)) {
                val queuedScenes = ride.scenes.values.filter { it.isQueued(tracklessRideCarGroup) }
                for (it in queuedScenes) {
                    if (switchScene(tracklessRideCarGroup, it)) {
                        break
                    }
                }
            }
        }

        ride.activeGroups().forEach { tracklessRideCarGroup ->
            tracklessRideCarGroup.cars.toList().forEach { car ->
                var doRouteLoop = true
                var routes = route[car.key]
                if (routes == null || routes.isEmpty()) {
                    car.currentSpeed = 0.0
                } else {
                    val totalDistanceLeft = routes.sumOf { it.distanceLeft }
                    val route = routes.first()
                    val decelTicks = (car.currentSpeed / car.targetDeceleration) - 1.0
                    val currentDecelerateDistance = car.currentSpeed * 0.5 * decelTicks
                    val maxSpeed = car.targetSpeed

                    val newSpeed =
                        when {
                            car.currentSpeed > maxSpeed -> max(
                                maxSpeed,
                                car.currentSpeed - car.targetDeceleration
                            )
                            else -> (if (route.disableBraking || totalDistanceLeft > currentDecelerateDistance) {
                                car.currentSpeed + car.targetAcceleration
                            } else {
                                car.currentSpeed - car.targetDeceleration
                            }).clamp(0.01, 1000.0)
                        }
                    val distanceToTravel = (car.currentSpeed + newSpeed) * 0.5
                    car.currentSpeed = newSpeed

                    if (route.progress < 1.0) {
                        route.distance += distanceToTravel
                    }
                }

                var position: PathPosition? = null
                while (routes != null && routes.isNotEmpty() && doRouteLoop) {
                    doRouteLoop = false
                    val route = routes.firstOrNull()
                    if (route != null) {
                        route.updateTimeRequest()

                        if (route.progress <= 1.0) {
                            position = route.pathPosition

                            route.triggers.forEach { trigger ->
                                if (trigger.id !in route.finishedTriggers) {
                                    if (trigger.shouldTrigger(ride, tracklessRideCarGroup, car, route)) {
                                        trigger.trigger(ride, tracklessRideCarGroup, car)
                                        if (trigger.continuity == SceneTrigger.Continuity.ONCE) {
                                            route.finishedTriggers.add(trigger.id)
                                        }
                                    }
                                }
                            }

                            if (route.progress == 1.0 && route.clearOnComplete) {
                                popCurrentRoute(car)
                                routes = this.route[car.key]
                                routes?.firstOrNull()?.distance = route.excessiveDistance
                                routes?.firstOrNull()?.updateStartTime()
                                doRouteLoop = true
                            } else if (route.progress == 1.0) {
                                route.distance = route.route.length
                            }
                        }
                    }
                }
                if (position != null) {
                    car.moveTo(position)
                }

                var success = false
                do {
                    success = false
                    val action = getProgramPartForCar(car)
                    if (action != null) {
                        val state = getState(car, action)
                        val result = action.execute(ride, tracklessRideCarGroup, car, state)
                        if (result == ProgramPart.ExecuteResult.DONE) {
                            advancedAction(car)
                            success = true
                        } else if (result == ProgramPart.ExecuteResult.DONE_AND_HALT) {
                            advancedAction(car)
                        }
                    }
                } while (success)

                car.update()
            }

            tracklessRideCarGroup.executePostUpdateCalls()

            if (allActionsFinished(tracklessRideCarGroup)) {
//                Logger.debug("Should advance scene")
                (tracklessRideCarGroup.exitToOverrides
                    ?: tracklessRideCarGroup.currentScene.sceneData.exitsTo).forEach {
                    val targetScene = ride.scenes.getValue(it)
                    targetScene.queue(tracklessRideCarGroup)
                }
            }
        }
    }

    fun setRoute(
        car: TracklessRideCar,
        route: GraphRouter.Route,
        clearOnSceneSwitch: Boolean,
        clearOnActionSwitch: Boolean,
        clearOnComplete: Boolean,
        initialTargetSpeed: Double?,
        initialTargetAcceleration: Double?,
        initialTargetDeceleration: Double?,
        disableBraking: Boolean,
        triggers: List<SceneTrigger>,
    ): Boolean {
        val routes = this.route.getOrPut(car.key) { emptyList() }
        if (routes.none { it.route === route }) {
//            Logger.debug("Set route for car ${car.idInGroup} (existing=${routes.size})")
//            route.routeParts.forEach {
//                it.by.positionAt(it.fromAt)
//                    .spawnParticleX(ride.world, Particle.REDSTONE, 10, data = Particle.DustOptions(Color.YELLOW, 0.25f))
//                it.by.positionAt(it.toAt)
//                    .spawnParticleX(ride.world, Particle.REDSTONE, 10, data = Particle.DustOptions(Color.RED, 0.25f))
//                Logger.debug(
//                    "  - Part ${it.from.id}@${it.fromAt.format(2)} to ${it.to.id}@${it.toAt.format(2)} over ${it.by.id} with length${
//                        it.by.length.format(
//                            2
//                        )
//                    }"
//                )
//
//                Logger.debug("    ${it.by.positionAt(it.fromAt).asString(2)} > ${it.by.positionAt(it.toAt).asString(2)}")
//            }
            val progressedRoute = ProgressedRoute(
                route,
                clearOnSceneSwitch = clearOnSceneSwitch,
                clearOnActionSwitch = clearOnActionSwitch,
                clearOnComplete = clearOnComplete,
                initialTargetSpeed = initialTargetSpeed,
                initialTargetAcceleration = initialTargetAcceleration,
                initialTargetDeceleration = initialTargetDeceleration,
                disableBraking = disableBraking,
                triggers = triggers,
            )
            this.route[car.key] = routes + progressedRoute
            if (routes.isEmpty()) {
                applyInitialSettings(car, progressedRoute)
            }
            return true
        }
        return false
    }

    class ProgressedRoute(
        val route: GraphRouter.Route,
        distance: Double = 0.0,
        val clearOnSceneSwitch: Boolean,
        val clearOnActionSwitch: Boolean,
        val clearOnComplete: Boolean,
        val initialTargetSpeed: Double?,
        val initialTargetAcceleration: Double?,
        val initialTargetDeceleration: Double?,
        val disableBraking: Boolean,
        val triggers: List<SceneTrigger>,
    ) {
        var start = System.currentTimeMillis()
            private set
        var lastTimeRequestAt = 0L
            private set(value) {
                previousTimeRequestAt = field
                field = value
            }
        var previousTimeRequestAt = 0L
            private set

        fun updateTimeRequest() {
            lastTimeRequestAt = System.currentTimeMillis() - start
        }

        fun updateStartTime() {
            start = System.currentTimeMillis()
            lastTimeRequestAt = 0
        }

        var finishedTriggers = mutableSetOf<Int>()
        var distance: Double = distance
            set(value) {
                distanceOld = field
                oldProgress = progress
                field = value
            }
        var distanceOld: Double = 0.0
            private set

        val excessiveDistance get() = max(distance - route.length, 0.0)

        val progress: Double
            get() = if (route.length <= 0.0) 1.0 else (distance / route.length).clamp(0.0, 1.0)
        var oldProgress: Double = 0.0
            private set

        val distanceLeft: Double
            get() = (route.length - distance).clamp(0.0, route.length)

        val pathPosition: PathPosition
            get() = route.pathPositionAtDistance(distance.clamp(0.0, route.length))
    }
}