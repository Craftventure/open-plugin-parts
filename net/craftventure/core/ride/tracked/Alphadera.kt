package net.craftventure.core.ride.tracked

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.effect.EffectManager
import net.craftventure.core.effect.ItemEmitter
import net.craftventure.core.feature.maxifoto.MaxiFoto
import net.craftventure.core.ktx.extension.equalsWithPrecision
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.ride.operator.controls.ControlColors
import net.craftventure.core.ride.operator.controls.OperatorSwitch
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.TrackSegment.DistanceListener
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.ExtensibleSegment
import net.craftventure.core.ride.trackedride.segment.SidewaysTransferSegment
import net.craftventure.core.ride.trackedride.segment.SidewaysTransferSegment.TransferSegment
import net.craftventure.core.ride.trackedride.segment.SidewaysTransferSegment.TransferTarget
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.ride.trackedride.segment.TransportSegment
import net.craftventure.core.ride.trackedride.segment.extensions.EStopBrakeExtension
import net.craftventure.core.ride.trackedride.segment.extensions.TransportExtension
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemRepresentation
import net.craftventure.database.type.BankAccountType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class Alphadera private constructor(trackedRide: TrackedRide) {
    companion object {
        private var instance: Alphadera? = null

        private const val TRACK_FRICTION = 0.999
        private const val GRAVITATIONAL_INFLUENCE = 0.05

        private val MIN_SPEED = CoasterMathUtils.kmhToBpt(10.0)
        private val TARGET_SPEED = CoasterMathUtils.kmhToBpt(14.0)
        private val MAX_SPEED = CoasterMathUtils.kmhToBpt(18.0)
        private val ACCELERATION_FORCE = CoasterMathUtils.kmhToBpt(0.2)
        private const val CAR_LENGTH = 4.8
        private val BRAKE_FORCE = 1.5
        private val RIDE_DURATION_SECONDS = 60 + 60 + 30 //24 is more correct though
        private val CAR_COUNT = 6

        private var queue: RideQueue? = null

        fun get(): Alphadera {
            if (instance == null) {
                val area = SimpleArea("world", )

                EffectManager.add(
                    ItemEmitter(
                        "alphadera_goldscene_vc_emitter_0",
                        Location(area.world, -165.5, 23.5, -732.5),
                        BankAccountType.VC.itemRepresentation,
                        randomOffset = Vector(2.0, 0.0, 2.0),
                        startVelocity = Vector(0.0, 0.4, 0.0),
                        randomVelocity = Vector(0.4, 0.07, 0.4),
                        spawnRate = -3f,
                        lifeTimeTicksMin = 20 * 6,
                        lifeTimeTicksMax = 20 * 6
                    )
                )

                val trackedRide = OperableCoasterTrackedRide(
                    "alphadera", area,
                    Location(Bukkit.getWorld("world"), ),
                    "ride_alphadera", "alphadera"
                )

                trackedRide.setOperatorArea(SimpleArea("world",))
//                trackedRide.setOperatorArea(SimpleArea("world", -95.0, 36.0, -722.0, -78.0, 44.0, -710.0))
                initTrack(trackedRide)
                addTrain(trackedRide, trackedRide.getSegmentById("station")!!, 0.0)
                trackedRide.getSegmentById("track5")!!.let { section ->
                    for (i in 0 until CAR_COUNT - 1)
                        addTrain(trackedRide, section, section.length - (CAR_LENGTH * i * 1.05) - 2.0)
                }

                trackedRide.initialize()
                trackedRide.pukeRate = 0.0
                instance = Alphadera(trackedRide)

                trackedRide.addOnRideCompletionListener { player, rideCar ->
                    if (DateUtils.isCoasterDay) {
                        val database = MainRepositoryProvider.achievementProgressRepository
                        database.reward(player.uniqueId, "coaster_day")
                        database.reward(player.uniqueId, "coaster_day_" + LocalDateTime.now().year)
                    }
                }

                val joinArea = SimpleArea("world",)

                queue = RideQueue(
                    ride = trackedRide,
                    joinArea = joinArea,
                    passengerCountPerTrain = 6,
                    averageSecondsBetweenDepartures = 30.0,
                    boardingDelegate = RideQueue.RideStationBoardingDelegate(trackedRide.getSegmentById("station") as StationSegment)
                )
                queue!!.start()
                trackedRide.addQueue(queue)
            }
            return instance!!
        }

        private const val SEAT_SIDE_OFFSET = 0.5
        private const val SEAT_HEIGHT_OFFSET = 0.5
        private fun addTrain(trackedRide: TrackedRide, segment: TrackSegment, distance: Double): RideTrain {
            val rideTrain = CoasterRideTrain(segment, distance)
            rideTrain.setTrainSoundName(
                "alphadera",
                SpatialTrainSounds.Settings(
                    setOf(
                        TrackSegment.TrackType.LSM_LAUNCH,
                        TrackSegment.TrackType.MAG_BRAKE,
                        TrackSegment.TrackType.WHEEL_TRANSPORT
                    )
                )
            )

            val dynamicSeatedRideCar = DynamicSeatedRideCar("alphadera", CAR_LENGTH)
            dynamicSeatedRideCar.carFrontBogieDistance = -1.3// -1.4
            dynamicSeatedRideCar.carRearBogieDistance = -CAR_LENGTH + 1.0 // 1.3

            dynamicSeatedRideCar.setFakeSeatProvider { mountedEntityIndex, currentEntityIndex ->
//                Logger.debug("Seat $mountedEntityIndex $currentEntityIndex")
                if (currentEntityIndex != 0) return@setFakeSeatProvider null
                return@setFakeSeatProvider when (mountedEntityIndex) {
                    1 -> dataItem(Material.DIAMOND_SWORD, 38)
                    2 -> dataItem(Material.DIAMOND_SWORD, 39)
                    3 -> dataItem(Material.DIAMOND_SWORD, 40)
                    4 -> dataItem(Material.DIAMOND_SWORD, 41)
                    5 -> dataItem(Material.DIAMOND_SWORD, 42)
                    6 -> dataItem(Material.DIAMOND_SWORD, 43)
                    else -> null
                }
            }

            dynamicSeatedRideCar.addSeat(
                ArmorStandSeat(
                    0.0,
                    0.27,
                    -2.3,
                    false,
                    "alphadera"
                ).apply {
                    setModel(MaterialConfig.ALPHADERA_CAR)
                })

            val seatStart = -1.95
            val rowOffset = 0.95

            dynamicSeatedRideCar.addSeat(
                ArmorStandSeat(
                    SEAT_SIDE_OFFSET,
                    SEAT_HEIGHT_OFFSET,
                    seatStart - (rowOffset * 0),
                    true,
                    "alphadera"
                )
            )
            dynamicSeatedRideCar.addSeat(
                ArmorStandSeat(
                    -SEAT_SIDE_OFFSET,
                    SEAT_HEIGHT_OFFSET,
                    seatStart - (rowOffset * 0),
                    true,
                    "alphadera"
                )
            )

            dynamicSeatedRideCar.addSeat(
                ArmorStandSeat(
                    SEAT_SIDE_OFFSET,
                    SEAT_HEIGHT_OFFSET,
                    seatStart - (rowOffset * 1),
                    true,
                    "alphadera"
                )
            )
            dynamicSeatedRideCar.addSeat(
                ArmorStandSeat(
                    -SEAT_SIDE_OFFSET,
                    SEAT_HEIGHT_OFFSET,
                    seatStart - (rowOffset * 1),
                    true,
                    "alphadera"
                )
            )

            dynamicSeatedRideCar.addSeat(
                ArmorStandSeat(
                    SEAT_SIDE_OFFSET,
                    SEAT_HEIGHT_OFFSET,
                    seatStart - (rowOffset * 2),
                    true,
                    "alphadera"
                )
            )
            dynamicSeatedRideCar.addSeat(
                ArmorStandSeat(
                    -SEAT_SIDE_OFFSET,
                    SEAT_HEIGHT_OFFSET,
                    seatStart - (rowOffset * 2),
                    true,
                    "alphadera"
                )
            )

            rideTrain.addCar(dynamicSeatedRideCar)

            trackedRide.addTrain(rideTrain)
            return rideTrain
        }

        private fun initTrack(trackedRide: OperableCoasterTrackedRide) {
            val station = StationSegment(
                id = "station",
                displayName = "station",
                trackedRide = trackedRide,
                transportSpeed = CoasterMathUtils.bptToKmh(TARGET_SPEED),
                maxSpeed = CoasterMathUtils.bptToKmh(MAX_SPEED),
                brakeForce = 2.1
            ).apply {
                nameOnMap = "Station"
                holdDistance = -0.2
                slowBrakingDistance = 3.0
                ejectType = TrackSegment.EjectType.EJECT_TO_EXIT
                leaveMode = TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER
                accelerateForce = ACCELERATION_FORCE

//                Logger.debug("Alphadera ${ceil(RIDE_DURATION_SECONDS / CAR_COUNT.toDouble()).toLong()}")
                setDispatchIntervalTime(30.seconds)
                setKeepRollingTime(30.seconds)
                setAutoDispatchTime(60.seconds)
            }
            station.setOnStationGateListener { open ->
                Location(Bukkit.getWorld("world"), -84.50, 37.00, -717.50).block.open(open)
                Location(Bukkit.getWorld("world"), -86.50, 37.00, -717.50).block.open(open)
            }
            station.setOnStationStateChangeListener { newState, oldState ->
                if (newState == StationSegment.StationState.DISPATCHING) {
                    val rideTrain = station.anyRideTrainOnSegment
                    rideTrain?.setOnboardSynchronizedAudio("alphadera_onride", System.currentTimeMillis())
                }
            }
//            if (CraftventureCore.isNonProductionServer())
//                station.addUpdateListener {
//                    val train = station.targetTrain
//                    val all = "dispatch=${station.estimatedMillisecondsUntil?.let { it / 1000.0 }
//                        ?.format(2)} next=${station.nextTrackSegment!!.isSectionUnreserved(station)} unreservedOrFully=${station.isSectionUnreservedOrTrainFullyOnSegment()}"
//                    if (train != null)
//                        it.setDebugTag(
//                            "canLeave=${station.canLeaveSection(train)} canAdvance=${station.canAdvanceToNextBlock(
//                                train,
//                                false
//                            )} $all"
//                        )
//                    else
//                        it.setDebugTag(all)
//
//                    fun TrackSegment.debug() {
//                        setDebugTag("block=${isBlockSection} type=${blockType} train=${blockReservedTrain != null}")
//                    }
//
//                    it.nextTrackSegment!!.debug()
//                    it.nextTrackSegment!!.nextTrackSegment!!.debug()
//                    it.nextTrackSegment!!.nextTrackSegment!!.nextTrackSegment!!.debug()
//                    it.nextTrackSegment!!.nextTrackSegment!!.nextTrackSegment!!.nextTrackSegment!!.debug()
//                }

            val track1 = TransportSegment(
                id = "track1",
                trackedRide = trackedRide,
                transportSpeed = TARGET_SPEED,
                accelerateForce = ACCELERATION_FORCE,
                maxSpeed = MAX_SPEED,
                brakeForce = BRAKE_FORCE
            ).apply {
                blockSection(true)
                blockType = TrackSegment.BlockType.CONTINUOUS
                trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }

            val launch1 = TransportSegment(
                id = "launch1",
                trackedRide = trackedRide,
                transportSpeed = CoasterMathUtils.kmhToBpt(90.0),
                accelerateForce = CoasterMathUtils.kmhToBpt(1.5),
                maxSpeed = CoasterMathUtils.kmhToBpt(100.0),
                brakeForce = CoasterMathUtils.kmhToBpt(1.8)
            ).apply {
                blockSection(true)
                blockType = TrackSegment.BlockType.CONTINUOUS
                trackType = TrackSegment.TrackType.LSM_LAUNCH
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }

            val track2 = ExtensibleSegment(
                id = "track2",
                trackedRide = trackedRide
            ).apply {
                blockSection(true)
                blockType = TrackSegment.BlockType.CONTINUOUS
                trackType = TrackSegment.TrackType.DEFAULT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)

                assert(
                    addExtension(
                        TransportExtension(
                            transportSpeed = CoasterMathUtils.kmhToBpt(25.0),
                            accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                            maxSpeed = CoasterMathUtils.kmhToBpt(120.0),
                            brakeForce = CoasterMathUtils.kmhToBpt(1.8)
                        )
                    )
                )
                assert(
                    addExtension(
                        TransportExtension(
                            transportSpeed = CoasterMathUtils.kmhToBpt(0.0),
                            accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                            maxSpeed = CoasterMathUtils.kmhToBpt(0.0),
                            brakeForce = CoasterMathUtils.kmhToBpt(0.22)
                        ).apply {
                            appendAcceleration = true
                            interceptor = object : TransportExtension.Interceptor {
                                override fun shouldApply(car: RideCar, distanceSinceLastUpdate: Double): Boolean =
                                    car.attachedTrain.frontCarDistance > 150.0
                            }
                        })
                )
            }

            val remmen1 = TransportSegment(
                id = "remmen1",
                trackedRide = trackedRide,
                transportSpeed = CoasterMathUtils.kmhToBpt(8.0),
                accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                maxSpeed = CoasterMathUtils.kmhToBpt(8.0),
                brakeForce = CoasterMathUtils.kmhToBpt(0.8)
            ).apply {
                blockSection(true)
                blockType = TrackSegment.BlockType.CONTINUOUS
                trackType = TrackSegment.TrackType.MAG_BRAKE
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }

            remmen1.add(object : DistanceListener(175.0) {
                var car = 0

                override fun onTargetHit(triggeringCar: RideCar) {
                    val names = arrayOfNulls<Player>(6)
                    for (i in names.indices)
                        names[i] = null

                    var index = 0
                    for (rideCar in triggeringCar.attachedTrain.cars) {
                        for (entity in rideCar.getMaxifotoPassengerList()) {
                            if (index < names.size) {
                                if (entity is Player) {
                                    names[index] = entity
                                }
                                index++
                            }
                        }
                    }

                    var onlyNulls = true
                    for (name in names) {
                        if (name != null) {
                            onlyNulls = false
                            break
                        }
                    }

                    val renderSettings = MaxiFoto.RenderSettings("alphadera", names)
                    if (!onlyNulls) {
                        renderSettings.offset = car
                        MaxiFoto.render(renderSettings)
                        car++
                        if (car >= 6) {
                            car = 0
                        }
                    }
                }
            })

            val darkride1 = ExtensibleSegment(
                id = "darkride1",
                trackedRide = trackedRide
            ).apply {
                blockType = TrackSegment.BlockType.CONTINUOUS
                isForceContinuousCheck = true
                val firstPart = object : TransportExtension(
                    transportSpeed = CoasterMathUtils.kmhToBpt(4.0),
                    accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                    maxSpeed = CoasterMathUtils.kmhToBpt(4.0),
                    brakeForce = CoasterMathUtils.kmhToBpt(1.8)
                ) {
                    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
                        if (car.distance > 14) return
                        super.applyForces(car, distanceSinceLastUpdate)
                    }
                }
                addExtension(firstPart)

                val secondPart = object : TransportExtension(
                    transportSpeed = CoasterMathUtils.kmhToBpt(8.0),
                    accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                    maxSpeed = CoasterMathUtils.kmhToBpt(8.0),
                    brakeForce = CoasterMathUtils.kmhToBpt(1.8)
                ) {
                    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
                        if (car.distance <= 14) return
                        super.applyForces(car, distanceSinceLastUpdate)
                    }
                }
                addExtension(secondPart)
                addExtension(EStopBrakeExtension(CoasterMathUtils.kmhToBpt(0.5)))

                trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }

            val track3 = ExtensibleSegment(
                id = "track3",
                trackedRide = trackedRide
            ).apply {
                trackType = TrackSegment.TrackType.DEFAULT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                addExtension(
                    TransportExtension(
                        transportSpeed = CoasterMathUtils.kmhToBpt(8.0),
                        accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                        maxSpeed = CoasterMathUtils.kmhToBpt(18.0),
                        brakeForce = CoasterMathUtils.kmhToBpt(1.8)
                    )
                )
                addExtension(EStopBrakeExtension(CoasterMathUtils.kmhToBpt(0.5)))
            }

            val launch2 = TransportSegment(
                id = "launch2",
                trackedRide = trackedRide,
                transportSpeed = CoasterMathUtils.kmhToBpt(40.0),
                accelerateForce = CoasterMathUtils.kmhToBpt(1.0),
                maxSpeed = CoasterMathUtils.kmhToBpt(100.0),
                brakeForce = CoasterMathUtils.kmhToBpt(1.8)
            ).apply {
                trackType = TrackSegment.TrackType.LSM_LAUNCH
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }

            val track4 = TransportSegment(
                id = "track4",
                trackedRide = trackedRide,
                transportSpeed = CoasterMathUtils.kmhToBpt(14.0),
                accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                maxSpeed = CoasterMathUtils.kmhToBpt(120.0),
                brakeForce = CoasterMathUtils.kmhToBpt(1.8)
            ).apply {
                setOffsetFromNextSection(10.0)
                blockSection(true)
                blockType = TrackSegment.BlockType.CONTINUOUS
                isForceContinuousCheck = true
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }

            val launch3 = ExtensibleSegment(
                id = "launch3",
                trackedRide = trackedRide
            ).apply {
                blockSection(true)
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                blockType = TrackSegment.BlockType.BLOCK_SECTION

                val parent = this
                var state = LaunchState.IDLE
                var lastStateChange = System.currentTimeMillis()

                val launch = TransportExtension(
                    transportSpeed = CoasterMathUtils.kmhToBpt(70.0),
                    accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                    maxSpeed = CoasterMathUtils.kmhToBpt(100.0),
                    brakeForce = CoasterMathUtils.kmhToBpt(1.8),
                    enabled = false
                )

                val backwardsTransport = TransportExtension(
                    transportSpeed = CoasterMathUtils.kmhToBpt(-26.0),
                    accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                    maxSpeed = CoasterMathUtils.kmhToBpt(100.0),
                    brakeForce = CoasterMathUtils.kmhToBpt(1.8),
                    enabled = false
                )

                val stopBrakes = TransportExtension(
                    transportSpeed = CoasterMathUtils.kmhToBpt(0.0),
                    accelerateForce = CoasterMathUtils.kmhToBpt(1.3),
                    maxSpeed = CoasterMathUtils.kmhToBpt(0.000),
                    brakeForce = CoasterMathUtils.kmhToBpt(0.8),
                    enabled = false
                )

                val extensions: Array<ExtensibleSegment.Extension> = arrayOf(launch, stopBrakes, backwardsTransport)

                fun setState(newState: LaunchState) {
                    if (state != newState) {
                        state = newState
                        lastStateChange = System.currentTimeMillis()

//                        Logger.debug("Alphadera launch state $newState", logToCrew = true)

                        when (newState) {
                            LaunchState.IDLE -> {
                                extensions.forEach { it.enabled = false }
                            }

                            LaunchState.ENTERING -> {
                                extensions.forEach { it.enabled = false }
                            }

                            LaunchState.SNAKE_1 -> {
                                extensions.forEach { it.enabled = false }
                                stopBrakes.enabled = true
                            }

                            LaunchState.BACKWARDS -> {
                                extensions.forEach { it.enabled = false }
                                backwardsTransport.enabled = true
                            }

                            LaunchState.SNAKE_2 -> {
                                extensions.forEach { it.enabled = false }
                                stopBrakes.enabled = true
                            }

                            LaunchState.LAUNCH -> {
                                extensions.forEach { it.enabled = false }
                            }
                        }
                    }
                }

                val interceptor = object : ExtensibleSegment.Extension {
                    private val position = Vector()
                    private val startPosition = Vector()
                    override var enabled: Boolean = true
                    override var attachedSegment: TrackSegment? = null

                    override fun update() {
                        super.update()
                        when (state) {
                            LaunchState.IDLE -> {
                            }

                            LaunchState.ENTERING -> {
                                if (!stopBrakes.enabled)
                                    anyRideTrainOnSegment?.let {
                                        if (it.frontCarDistance > 32.0 || (it.frontCarDistance > 14.0 && it.frontCarDistance < 20.0)) {
                                            stopBrakes.enabled = true
//                                            Logger.debug("Brakes enabled ${it.frontCarDistance.format(2)}")
                                        }
                                    }

                                anyRideTrainOnSegment?.let {
                                    if (it.velocity.equalsWithPrecision(0.0)) {
                                        setState(LaunchState.SNAKE_1)
                                    }
                                }
                            }

                            LaunchState.SNAKE_1 -> {
                                if (System.currentTimeMillis() > lastStateChange + 2000 && !trackedRide.isEmergencyStopActive) {
                                    setState(LaunchState.BACKWARDS)
                                }
                            }

                            LaunchState.BACKWARDS -> {
                                anyRideTrainOnSegment?.let {
                                    if (it.frontCarDistance - it.length <= 8.0) {
                                        backwardsTransport.enabled = false
                                        stopBrakes.enabled = true
                                    }
                                }

                                anyRideTrainOnSegment?.let {
                                    if (it.velocity.equalsWithPrecision(0.0)) {
                                        setState(LaunchState.SNAKE_2)
                                    }
                                }
                            }

                            LaunchState.SNAKE_2 -> {
                                if (trackedRide.isEmergencyStopActive) {
                                    setState(LaunchState.ENTERING)
                                } else if (System.currentTimeMillis() > lastStateChange + 2000) {
                                    setState(LaunchState.LAUNCH)
                                }
                            }

                            LaunchState.LAUNCH -> {
                                anyRideTrainOnSegment?.let {
                                    if (it.frontCarDistance >= 25.0) {
                                        launch.enabled = true
                                    }
                                }
                            }
                        }
                    }

                    override fun onTrainEnteredSection(previousSegment: TrackSegment?, rideTrain: RideTrain) {
                        super.onTrainEnteredSection(previousSegment, rideTrain)
                        setState(LaunchState.ENTERING)
                        previousSegment!!.getStartPosition(startPosition)

                        val target =
                            parent.previousTrackSegment!!.length + (rideTrain.frontCarDistance - rideTrain.velocity)
//                        Logger.debug(
//                            "front=${rideTrain.frontCarDistance.format(2)} velocity=${rideTrain.velocity.format(
//                                2
//                            )} target=${target.format(2)}"
//                        )

                        parent.previousTrackSegment!!.getPosition(target, position)

                        val distance = parent.getDistanceClosestTo(position)
//                        Logger.debug("distance=${distance.format(2)} position=${position.asString(2)}")

                        rideTrain.move(parent, distance)
                    }

                    override fun onTrainLeftSection(rideTrain: RideTrain) {
                        super.onTrainLeftSection(rideTrain)
                        setState(LaunchState.IDLE)
                    }
                }

                assert(addExtension(interceptor))
                assert(addExtension(launch))
                assert(addExtension(stopBrakes))
                assert(addExtension(backwardsTransport))
            }

            val track5 = TransportSegment(
                id = "track5",
                trackedRide = trackedRide,
                transportSpeed = CoasterMathUtils.kmhToBpt(18.0),
                accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                maxSpeed = CoasterMathUtils.kmhToBpt(120.0),
                brakeForce = CoasterMathUtils.kmhToBpt(1.8)
            ).apply {
                trackType = TrackSegment.TrackType.DEFAULT
                isForceContinuousCheck = true
                setFriction(0.997)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                blockType = TrackSegment.BlockType.CONTINUOUS
            }

            val remmen2 = TransportSegment(
                id = "remmen2",
                trackedRide = trackedRide,
                transportSpeed = CoasterMathUtils.kmhToBpt(8.0),
                accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                maxSpeed = CoasterMathUtils.kmhToBpt(8.0),
                brakeForce = CoasterMathUtils.kmhToBpt(1.8)
            ).apply {
                blockSection(true)
                trackType = TrackSegment.TrackType.MAG_BRAKE
                trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                blockType = TrackSegment.BlockType.CONTINUOUS
            }

            val track6 = ExtensibleSegment(
                id = "track6",
                trackedRide = trackedRide
            ).apply {
                addExtension(
                    TransportExtension(
                        transportSpeed = CoasterMathUtils.kmhToBpt(8.0),
                        accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                        maxSpeed = CoasterMathUtils.kmhToBpt(8.0),
                        brakeForce = CoasterMathUtils.kmhToBpt(1.8)
                    )
                )
                trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }

            track6.add(object : DistanceListener(3.0) {
                var car = 0

                override fun onTargetHit(triggeringCar: RideCar) {
                    val names = arrayOfNulls<Player>(6)
                    for (i in names.indices)
                        names[i] = null

                    var index = 0
                    for (rideCar in triggeringCar.attachedTrain.cars) {
                        for (entity in rideCar.getMaxifotoPassengerList()) {
                            if (index < names.size) {
                                if (entity is Player) {
                                    names[index] = entity
                                }
                                index++
                            }
                        }
                    }

                    var onlyNulls = true
                    for (name in names) {
                        if (name != null) {
                            onlyNulls = false
                            break
                        }
                    }

                    val renderSettings = MaxiFoto.RenderSettings("alphadera", names)
                    if (!onlyNulls) {
                        renderSettings.offset = car
                        MaxiFoto.render(renderSettings)
                        car++
                        if (car >= 6) {
                            car = 0
                        }
                    }
                }
            })

            val prestation = ExtensibleSegment(
                id = "prestation",
                trackedRide = trackedRide
            ).apply {
                blockSection(true)
                trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }
            prestation.add(object : DistanceListener(5.5) {
                override fun onTargetHit(rideCar: RideCar) {
                    rideCar.attachedTrain.eject()
                }
            })

            val switchToRemise = ExtensibleSegment(
                id = "switchToRemise",
                trackedRide = trackedRide
            ).apply {
//                setActive(false, true)
                trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            }

            val switchSegmentsTransportForwards = TransportExtension(
                transportSpeed = CoasterMathUtils.kmhToBpt(8.0),
                accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                maxSpeed = CoasterMathUtils.kmhToBpt(8.0),
                brakeForce = CoasterMathUtils.kmhToBpt(1.8)
            )

            val switchSegmentsTransportBackwards = TransportExtension(
                transportSpeed = CoasterMathUtils.kmhToBpt(-8.0),
                accelerateForce = CoasterMathUtils.kmhToBpt(1.8),
                maxSpeed = CoasterMathUtils.kmhToBpt(8.0),
                brakeForce = CoasterMathUtils.kmhToBpt(1.8)
            )

            switchSegmentsTransportBackwards.enabled = false

            prestation.addExtension(switchSegmentsTransportForwards)
            switchToRemise.addExtension(switchSegmentsTransportForwards)

            prestation.addExtension(switchSegmentsTransportBackwards)
            switchToRemise.addExtension(switchSegmentsTransportBackwards)

            val maintenance1 = TransferSegment(
                id = "maintenance1",
                trackedRide = trackedRide,
                inDirection = SidewaysTransferSegment.Direction.BACKWARDS
            )
            maintenance1.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT

            val maintenance2 = TransferSegment(
                id = "maintenance2",
                trackedRide = trackedRide,
                inDirection = SidewaysTransferSegment.Direction.BACKWARDS
            )
            maintenance2.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT

            val maintenance3 = TransferSegment(
                id = "maintenance3",
                trackedRide = trackedRide,
                inDirection = SidewaysTransferSegment.Direction.BACKWARDS
            )
            maintenance3.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT

            val switchtrackRemise = SidewaysTransferSegment(
                "switchtrackRemise",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(8.0),
                CoasterMathUtils.kmhToBpt(2.2)
            )
            switchtrackRemise.type = SidewaysTransferSegment.SegmentType.PULL_IN
            switchtrackRemise.blockSection(true)
            switchtrackRemise.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            switchtrackRemise.addTransferTarget(
                TransferTarget(
                    Vector(0, 0, 3),
                    maintenance1
                )
            )
            switchtrackRemise.addTransferTarget(
                TransferTarget(
                    Vector(0, 0, 0),
                    maintenance2
                ), true
            )
            switchtrackRemise.addTransferTarget(
                TransferTarget(
                    Vector(0, 0, -3),
                    maintenance3
                )
            )

            switchtrackRemise.addUpdateListener(object : SidewaysTransferSegment.UpdateListener {
                var blockArea: SimpleArea? = null
                var areaTracker: NpcAreaTracker? = null
                var isTracking = false
                var blockLocations = arrayOfNulls<Vector>(6)
                var blockEntities = arrayOfNulls<NpcEntity>(6)
                override fun onUpdate(track: SidewaysTransferSegment) {
                    if (track.isTrackMoving()) {
                        if (!isTracking) {
                            areaTracker!!.startTracking()
                            isTracking = true
                            for (blockLocation in blockLocations) {
                                val block =
                                    blockLocation!!.toLocation(blockArea!!.world).add(track.offset).block
                                block.type = Material.AIR
                            }
                        }
                        //
//                    Logger.info("Updating blocks");
                        for (i in blockEntities.indices) {
                            val npcEntity = blockEntities[i]
                            val target = blockLocations[i]!!.clone().add(track.offset)
                            npcEntity!!.move(target.x, target.y, target.z)
                        }
                    } else {
                        if (isTracking) {
                            areaTracker!!.stopTracking()
                            isTracking = false
                            for (blockLocation in blockLocations) {
                                val block =
                                    blockLocation!!.toLocation(blockArea!!.world).add(track.offset).block
                                block.type = Material.GRAY_CONCRETE
                            }
                        }
                    }
                }

                init {
                    blockArea = SimpleArea("world", )
                    areaTracker = NpcAreaTracker(blockArea!!)
                    blockLocations[0] = Vector().add(Vector(0.5, 0.0, 0.5))
                    blockLocations[1] = Vector().add(Vector(0.5, 0.0, 0.5))
                    blockLocations[2] = Vector().add(Vector(0.5, 0.0, 0.5))
                    blockLocations[3] = Vector().add(Vector(0.5, 0.0, 0.5))
                    blockLocations[4] = Vector().add(Vector(0.5, 0.0, 0.5))
                    blockLocations[5] = Vector().add(Vector(0.5, 0.0, 0.5))
                    for (i in blockLocations.indices) {
                        val npcEntity = NpcEntity(
                            "alphaderaSwitchtrack",
                            EntityType.FALLING_BLOCK,
                            blockLocations[i]!!.toLocation(blockArea!!.world)
                        )
                        npcEntity.setBlockData(Material.GRAY_CONCRETE.createBlockData())
                        npcEntity.noGravity(true)
                        blockEntities[i] = npcEntity
                        areaTracker!!.addEntity(npcEntity)
                    }
                }
            })

            val trackSwitcherControl = OperatorSwitch("switcher", ControlColors.GREEN, ControlColors.RED).apply {
                owner = track6
                name = CVTextColor.MENU_DEFAULT_TITLE + "Switch pre-transfer track"
                description =
                    CVTextColor.MENU_DEFAULT_LORE + "Click to switch transfer mode. Has to be enabled before and while transfering. During transfer mode you can't dispatch trains from the station."
                sort = 0
                group = switchtrackRemise.id
//        permission = Permissions.CREW
                setControlListener { operableRide, player, operatorControl, operatorSlot ->
//                    Logger.debug("Switch track $isEnabled")
                    if (this.isEnabled) {
                        this.toggle()
                        track6.setActive(!this.isOn, true)
//                        switchToRemise.setActive(this.isOn, true)

                        if (this.isOn) {
                            prestation.previousTrackSegment = switchToRemise
                        } else {
                            prestation.previousTrackSegment = track6
                        }
                    }
                }
            }
            val trackSwitcherControlProvided = ExtensibleSegment.ProvidedControl(
                control = trackSwitcherControl,
                active = true
            )
//            track6.controls.add(trackSwitcherControlProvided)
//            if (CraftventureCore.getEnvironment() != CraftventureCore.Environment.PRODUCTION)
//                switchToRemise.addUpdateListener {
//                    it.setDebugTag("track6=${track6.isActive} switchToRemise=${switchToRemise.isActive} forwards=${switchSegmentsTransportForwards.enabled} backwards=${switchSegmentsTransportBackwards.enabled} previous=${prestation.previousTrackSegment!!.id}")
//                }
            switchtrackRemise.pullListeners += {
                switchSegmentsTransportBackwards.enabled = it
                switchSegmentsTransportForwards.enabled = !switchSegmentsTransportBackwards.enabled
            }
            switchtrackRemise.addUpdateListener {
                val active = track6.isActive
                trackSwitcherControl.isEnabled = (if (active) track6.canBeSetInactive() else track6.canBeSetActive()) &&
                        (if (active) switchToRemise.canBeSetActive() else switchToRemise.canBeSetInactive())

                if (!trackedRide.isBeingOperated && trackSwitcherControl.isOn) {
                    trackSwitcherControl.click(trackedRide, null)
                }

//                switchSegmentsTransportBackwards.enabled =
//                    switchtrackRemise.transferEnabled && switchToRemise.isActive && switchtrackRemise.isPullingIn
//                switchSegmentsTransportForwards.enabled = !switchSegmentsTransportBackwards.enabled

//                if (CraftventureCore.getEnvironment() != CraftventureCore.Environment.PRODUCTION)
//                    it.setDebugTag("state=${switchtrackRemise.state} enabled=${switchtrackRemise.transferEnabled} correctTrain=${switchtrackRemise.anyRideTrainOnSegment == switchtrackRemise.blockReservedTrain} reserverFor=${switchtrackRemise.blockReservedTrain?.trainId} pulling=${switchtrackRemise.isPullingIn} halted=${switchtrackRemise.hasHaltedTrain}")
            }

//            if (CraftventureCore.getEnvironment() != CraftventureCore.Environment.PRODUCTION)
//                maintenance1.addUpdateListener {
//                    it.setDebugTag("state=${maintenance1.state}")
//                }
//            if (CraftventureCore.getEnvironment() != CraftventureCore.Environment.PRODUCTION)
//                maintenance2.addUpdateListener {
//                    it.setDebugTag("state=${maintenance2.state}")
//                }
//            if (CraftventureCore.getEnvironment() != CraftventureCore.Environment.PRODUCTION)
//                maintenance3.addUpdateListener {
//                    it.setDebugTag("state=${maintenance3.state}")
//                }

            //<editor-fold desc="Nodes">
            station.add(
            )

            track1.add(
            )

            launch1.add(
            )

            track2.add(
            )

            remmen1.add(
            )

            darkride1.add(
            )

            track3.add(
            )

            launch2.add(
            )

            track4.add(
            )

            launch3.add(
            )

            track5.add(
            )

            remmen2.add(
            )

            track6.add(
            )

            prestation.add(
            )

            switchToRemise.add(
            )

            switchtrackRemise.add(
            )

            maintenance1.add(
            )

            maintenance2.add(
            )

            maintenance3.add(
            )

            //</editor-fold>

            maintenance1.nextTrackSegment = switchtrackRemise
            maintenance1.previousTrackSegment = switchtrackRemise

            maintenance2.nextTrackSegment = switchtrackRemise
            maintenance2.previousTrackSegment = switchtrackRemise

            maintenance3.nextTrackSegment = switchtrackRemise
            maintenance3.previousTrackSegment = switchtrackRemise

            switchtrackRemise.nextTrackSegment = switchToRemise
            switchtrackRemise.previousTrackSegment = maintenance2

            switchToRemise.nextTrackSegment = prestation
            switchToRemise.previousTrackSegment = switchtrackRemise

            station
                .setNextTrackSegmentRetroActive(track1)
                .setNextTrackSegmentRetroActive(launch1)
                .setNextTrackSegmentRetroActive(track2)
                .setNextTrackSegmentRetroActive(remmen1)
                .setNextTrackSegmentRetroActive(darkride1)
                .setNextTrackSegmentRetroActive(track3)
                .setNextTrackSegmentRetroActive(launch2)
                .setNextTrackSegmentRetroActive(track4)
                .setNextTrackSegmentRetroActive(launch3)
                .setNextTrackSegmentRetroActive(track5)
                .setNextTrackSegmentRetroActive(remmen2)
                .setNextTrackSegmentRetroActive(track6)
                .setNextTrackSegmentRetroActive(prestation)
                .setNextTrackSegmentRetroActive(station)

//            if (false&&CraftventureCore.getInstance().environment != Environment.PRODUCTION) {
//            trackedRide.addTrackSections(
//                switchToRemise,
//                switchtrackRemise,
//                maintenance1,
//                maintenance2,
//                maintenance3,
//            )
//            }

            trackedRide.addTrackSections(
                station,
                track1,
                launch1,
                track2,
                remmen1,
                darkride1,
                track3,
                launch2,
                track4,
                launch3,
                track5,
                remmen2,
                track6,
                prestation,
//                switchToRemise,
//                switchtrackRemise,
//                maintenance1,
//                maintenance2,
//                maintenance3,
            )
        }
    }

    enum class LaunchState {
        IDLE,
        ENTERING,
        SNAKE_1,
        BACKWARDS,
        SNAKE_2,
        LAUNCH
    }
}

