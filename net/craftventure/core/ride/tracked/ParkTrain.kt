package net.craftventure.core.ride.tracked

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.CombinedArea
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.Permissions
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.ride.trackedride.car.effect.Effect
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.car.seat.Seat
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.ride.trackedride.segment.TransportSegment
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import net.craftventure.core.serverevent.PacketPlayerSteerEvent
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.repository.PlayerKeyValueRepository
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.time.Duration.Companion.seconds


class ParkTrain private constructor(trackedRide: TrackedRide) {
    companion object {
        private var parkTrain: ParkTrain? = null

        private const val TRACK_FRICTION = 0.996
        private const val GRAVITATIONAL_INFLUENCE = 0.03

        private val MIN_SPEED = CoasterMathUtils.kmhToBpt(10.0)
        private val TARGET_SPEED = CoasterMathUtils.kmhToBpt(14.0)
        private val MAX_SPEED = CoasterMathUtils.kmhToBpt(18.0)
        private val ACCELERATION_FORCE = CoasterMathUtils.kmhToBpt(0.1)
        private val BRAKE_FORCE = 1.5

        val totalLength: Double
            get() = get().let {
                TrackedRideManager.getTrackedRide("parktrain")!!
                    .trackSegments.map { it.length }.sum()
            }

        fun get(): ParkTrain {
            if (parkTrain == null) {
                val discovery = SimpleArea("world",)
                val mexico = SimpleArea("world", )
                val ccr = SimpleArea("world", )
                val fantasy = SimpleArea("world", )
                val southernSingapore = SimpleArea("world", )
                val northernSingapore = SimpleArea("world", )
                val viking = SimpleArea("world", )
                val mainstreetLeft = SimpleArea("world", )
                val mainstreetRight = SimpleArea("world", -)

                val area = CombinedArea(
                    discovery,
                    mexico,
                    ccr,
                    fantasy,
                    southernSingapore,
                    northernSingapore,
                    viking,
                    mainstreetLeft,
                    mainstreetRight
                )

//                val coasterArea = SimpleArea("world", 33.0, 0.0, -642.0, 141.0, 255.0, -565.0)
                val trackedRide = OperableCoasterTrackedRide(
                    "parktrain", area,
                    Bukkit.getWorld("world")!!.spawnLocation,
                    "ride_parktrain", "parktrain"
                )

                trackedRide.setOperatorArea(SimpleArea("world", ))
                initTrack(trackedRide)
                addTrain(trackedRide, trackedRide.getSegmentById("mainstreet")!!, 0.0)
                addTrain(trackedRide, trackedRide.getSegmentById("fantasy")!!, 0.0)

                trackedRide.initialize()
                trackedRide.pukeRate = 0.0
                parkTrain = ParkTrain(trackedRide)

                trackedRide.addOnRideCompletionListener { player, rideCar ->
                    val startSegment = rideCar.attachedTrain.frontCarTrackSegment
                    var segment = startSegment
                    var distance = 0.0
                    do {
                        distance += segment.length
                        segment = segment.previousTrackSegment
                    } while (segment !is StationSegment)

//                    Logger.info("${player.name} traveled ${distance.format(2)} meters by train")

                    executeAsync {
                        val database = MainRepositoryProvider.playerKeyValueRepository
                        var value = database.getValue(
                            player.uniqueId,
                            PlayerKeyValueRepository.DISTANCE_TRAVELED_BY_TRAIN
                        )?.toDoubleOrNull()
                            ?: 0.0
                        value += distance
                        player.sendMessage(CVTextColor.serverNotice + "You now traveled a total of ${value.format(2)} meters by train")
                        database.createOrUpdate(
                            player.uniqueId,
                            PlayerKeyValueRepository.DISTANCE_TRAVELED_BY_TRAIN,
                            value.toString()
                        )
                        MainRepositoryProvider.achievementProgressRepository
                            .reward(player.uniqueId, "parktrain_${startSegment.id}")
                    }
                }
            }
            return parkTrain!!
        }

        private fun addTrain(trackedRide: TrackedRide, segment: TrackSegment, distance: Double): RideTrain {
            val world = Bukkit.getWorld("world")
            val rideTrain = CoasterRideTrain(segment, distance)
            rideTrain.targetSpeed = TARGET_SPEED
            rideTrain.setTrainSoundName("parktrain", SpatialTrainSounds.Settings(emptySet()))

            val locomotive = DynamicSeatedRideCar("parktrain", 4.0)
            locomotive.addSeat(ArmorStandSeat(0.0, 0.1875, -2.0, false, "parktrain").apply {
                setModel(MaterialConfig.dataItem(Material.DIAMOND_SWORD, 101))
            })
//            locomotive.isHasTrainSound = true
            val trainListener = object : Seat.OnMoveListener<ArmorStand>, Listener {
                private var operator: Player? = null
                    set(value) {
                        if (field !== value) {
                            field = value
                            if (value == null) {
                                rideTrain.targetSpeed = TARGET_SPEED
                            }
                        }
                    }
                private var lastHorn = 0L
                private var forward: Double = 0.0

                override fun onMove(seat: Seat<ArmorStand>) {
                    val operator = seat.passengers.firstOrNull() as? Player

                    if (operator != null) {
                        this.operator = operator
                        val speedDelta = forward * CoasterMathUtils.kmhToBpt(0.1)
                        val targetSpeed = ((rideTrain.targetSpeed ?: 0.0) + speedDelta).clamp(MIN_SPEED, MAX_SPEED)
                        if (targetSpeed != rideTrain.targetSpeed) {
                            rideTrain.targetSpeed = targetSpeed
                        }
                        val currentSpeed = rideTrain.velocity

                        val currentStationSegment = rideTrain.frontCarTrackSegment as? StationSegment
                        val isInStationWaiting = currentStationSegment?.state == StationSegment.StationState.HOLDING
                        if (isInStationWaiting && forward > 0.1) {
                            currentStationSegment?.tryDispatchNow(StationSegment.DispatchRequestType.AUTO)
                        }

                        display(
                            operator,
                            Message(
                                id = ChatUtils.ID_RIDE,
                                text = Component.text(
                                    "Speed %.1f km/h (targeting %.1f km/h, hold W/S to change speed)".format(
                                        CoasterMathUtils.bptToKmh(currentSpeed),
                                        CoasterMathUtils.bptToKmh(targetSpeed)
                                    ),
                                    CVTextColor.serverNotice
                                ),
                                type = MessageBarManager.Type.SPEEDOMETER,
                                untilMillis = TimeUtils.secondsFromNow(2.0),
                            ),
                            replace = true,
                        )
                    } else {
                        this.operator = null
                    }
                    forward = 0.0
                }

                private fun playHorn() {
                    val now = System.currentTimeMillis()
                    if (lastHorn < now - 8000 && rideTrain.velocity > CoasterMathUtils.kmhToBpt(4.0)) {
                        lastHorn = now
                        locomotive.location.toLocation(trackedRide.area.world).let {
                            it.world?.playSound(it, SoundUtils.TRAIN_HORN, SoundCategory.AMBIENT, 1f, 1f)
                        }
                    }
                }

                @EventHandler(ignoreCancelled = true)
                fun onPlayerSteerEvent(event: PacketPlayerSteerEvent) {
                    if (event.player == operator) {
                        this.forward = event.forwards.toDouble().clamp(-1.0, 1.0)
                    }
                }

                @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
                fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
                    if (event.player === operator) {
                        event.isCancelled = true
                        playHorn()
                    }
                }

                @EventHandler(priority = EventPriority.LOWEST)
                fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
                    if (event.damager === operator) {
                        event.isCancelled = true
                        playHorn()
                    }
                }

                @EventHandler(priority = EventPriority.LOWEST)
                fun onPlayerInteractEvent(event: PlayerInteractEvent) {
                    if (event.player === operator) {
                        event.isCancelled = true
//            Logger.info("Interact type ${event.action}")
                        if (event.action === Action.LEFT_CLICK_AIR || event.action === Action.LEFT_CLICK_BLOCK) {
                            playHorn()
                        } else {
                        }
                    }
                }
            }
            Bukkit.getServer().pluginManager.registerEvents(trainListener, CraftventureCore.getInstance())

            locomotive.addSeat(ArmorStandSeat(0.6, 0.1, -3.42, true, "parktrain", -90f).apply {
                moveListener = trainListener
                permission = Permissions.VIP
            })
            locomotive.carFrontBogieDistance = -0.5
            locomotive.carRearBogieDistance = -3.8
            locomotive.addEffect(object : Effect(0.0, 2.2, -0.3) {
                var frame = 0

                override fun move(
                    x: Double,
                    y: Double,
                    z: Double,
                    trackYawRadian: Double,
                    trackPitchRadian: Double,
                    bankingDegree: Double,
                    rideCar: RideCar
                ) {
                    frame++
                    if (rideCar.attachedTrain.velocity == 0.0) {
                        if (frame > 10) {
                            frame = 0
                            world?.spawnParticleX(
                                Particle.CAMPFIRE_COSY_SMOKE,
                                x, y, z,
                                0,
                                0.0, 0.05, 0.0,
                                0.5
                            )
                        }
                    } else {
                        world?.spawnParticleX(
                            Particle.CAMPFIRE_COSY_SMOKE,
                            x, y, z,
                            0,
                            0.0, 0.1, 0.0,
                            0.5
                        )
                    }
                }
            })
            for (i in 0..3) {
                val forwardOffset = i * 0.3
                locomotive.addEffect(object : Effect(0.8, -0.5, -1.8 + forwardOffset) {
                    var frame = 0

                    override fun move(
                        x: Double,
                        y: Double,
                        z: Double,
                        trackYawRadian: Double,
                        trackPitchRadian: Double,
                        bankingDegree: Double,
                        rideCar: RideCar
                    ) {
                        frame++
                        if (rideCar.attachedTrain.velocity != 0.0) {
                            if (frame > 10) {
                                frame = 0
                                world?.spawnParticleX(Particle.CLOUD, x, y, z)
                            }
                        }
                    }
                })
                locomotive.addEffect(object : Effect(-0.8, -0.5, -1.8 + forwardOffset) {
                    var frame = 0

                    override fun move(
                        x: Double,
                        y: Double,
                        z: Double,
                        trackYawRadian: Double,
                        trackPitchRadian: Double,
                        bankingDegree: Double,
                        rideCar: RideCar
                    ) {
                        frame++
                        if (rideCar.attachedTrain.velocity != 0.0) {
                            if (frame > 10) {
                                frame = 0
                                world?.spawnParticleX(Particle.CLOUD, x, y, z)
                            }
                        }
                    }
                })
            }
            rideTrain.addCar(locomotive)

            val coalCar = DynamicSeatedRideCar("parktrain", 2.0)
            coalCar.addSeat(ArmorStandSeat(0.0, 0.1875, -1.0, false, "parktrain").apply {
                setModel(MaterialConfig.dataItem(Material.DIAMOND_SWORD, 102))
            })
            coalCar.carFrontBogieDistance = 0.4
            coalCar.carRearBogieDistance = -1.8
            rideTrain.addCar(coalCar)

            for (i in 0 until 5) {
                val car = DynamicSeatedRideCar("parktrain", 5.0)
//                if (i % 2 == 0)
//                    car.isHasTrainSound = true
                car.addSeat(ArmorStandSeat(0.0, 0.1875, -2.5, false, "parktrain").apply {
                    setModel(MaterialConfig.dataItem(Material.DIAMOND_SWORD, 103))
                })
                val sideOffset = 0.45
                val upOffset = 0.1
                car.addSeat(ArmorStandSeat(sideOffset, upOffset, -1.4 + 0.5, true, "parktrain", 180f))
                car.addSeat(ArmorStandSeat(sideOffset, upOffset, -2.6 + 0.5, true, "parktrain"))
                car.addSeat(ArmorStandSeat(sideOffset, upOffset, -3.4 + 0.5, true, "parktrain", 180f))
                car.addSeat(ArmorStandSeat(sideOffset, upOffset, -4.6 + 0.5, true, "parktrain"))
                car.addSeat(ArmorStandSeat(-sideOffset, upOffset, -1.4 + 0.5, true, "parktrain", 180f))
                car.addSeat(ArmorStandSeat(-sideOffset, upOffset, -2.6 + 0.5, true, "parktrain"))
                car.addSeat(ArmorStandSeat(-sideOffset, upOffset, -3.4 + 0.5, true, "parktrain", 180f))
                car.addSeat(ArmorStandSeat(-sideOffset, upOffset, -4.6 + 0.5, true, "parktrain"))
                car.carFrontBogieDistance = -1.0
                car.carRearBogieDistance = -4.0
                rideTrain.addCar(car)
            }

//            rideTrain.setUpdateListener(object : RideTrain.UpdateListener {
//                private var lastUpdate = 0L
//                override fun onUpdate(rideTrain: RideTrain) {
//                    val now = System.currentTimeMillis()
//                    if (now > lastUpdate + 1000) {
//                        lastUpdate = now
//                    } else return
//
//                    if (rideTrain.frontCarTrackSegment !is StationSegment) {
//                        val passengers = rideTrain.passengers
//                        if (passengers?.isEmpty() == true) return
//
//                        var segment = rideTrain.frontCarTrackSegment
//                        var distanceTillNextStation = segment.length - rideTrain.frontCarDistance
//                        do {
//                            segment = segment.nextTrackSegment
//                            distanceTillNextStation += segment.length
//                        } while (segment !is StationSegment)
//
//                        val secondsUntilNextStation = (distanceTillNextStation / (rideTrain.velocity * 20)).toInt()
//
//                        passengers.forEachAllocationless { player ->
//                            MessageBarManager.display(player,
//                                    ChatUtils.createComponent("Arriving at next station in ${DateUtils.format(secondsUntilNextStation * 1000L, "?")} (at current speed)", CVChatColor.COMMAND_GENERAL),
//                                    MessageBarManager.Type.SPEEDOMETER,
//                                    TimeUtils.secondsFromNow(2.0),
//                                    ChatUtils.ID_RIDE)
//                        }
//                    }
//                }
//            })

            trackedRide.addTrain(rideTrain)
            return rideTrain
        }

        private fun StationSegment.setupAsStation() {
            slowBrakingDistance = 15.0
            setDispatchIntervalTime(90.seconds)
            setAutoDispatchTime(30.seconds)
            setAutoStartDelay(15.seconds)
            accelerateForce = ACCELERATION_FORCE
            trackType = TrackSegment.TrackType.DEFAULT
            ejectType = TrackSegment.EjectType.EJECT_TO_SEAT
            leaveMode = TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER
            setFriction(TRACK_FRICTION)
            setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            setBlockType(TrackSegment.BlockType.BLOCK_SECTION, 3.0, 3.0)
            blockSection(true)
            holdDistance = -0.2
            isBrakeSounds = false
            isHarnessSounds = false
            setOnStationStateChangeListener { newState, oldState ->
                if (newState == StationSegment.StationState.DISPATCHING) {
                    anyRideTrainOnSegment?.cars?.firstOrNull()?.location?.toLocation(trackedRide.area.world)?.let {
                        it.world?.playSound(it, SoundUtils.TRAIN_START, SoundCategory.AMBIENT, 1f, 1f)
                    }
                }
                if (newState == StationSegment.StationState.DISPATCHING) {
                    anyRideTrainOnSegment?.setOnboardSynchronizedAudio("park_train", System.currentTimeMillis())
                }
            }
            setOnStationGateListener {
                if (it) {
                    anyRideTrainOnSegment?.cancelAudio()
                }
            }
        }

        private fun initTrack(trackedRide: TrackedRide) {
            val mainstreet = StationSegment(
                "mainstreet",
                "Mainstreet",
                trackedRide,
                CoasterMathUtils.bptToKmh(TARGET_SPEED),
                CoasterMathUtils.bptToKmh(MAX_SPEED),
                2.1
            ).apply {
                nameOnMap = "Mainstreet"
                setupAsStation()
//                setHoldDistance(length - 0.2)
            }
            val mainstreetToMexico = TransportSegment(
                "mainstreetToMexico",
                trackedRide,
                TARGET_SPEED,
                ACCELERATION_FORCE,
                MAX_SPEED,
                BRAKE_FORCE
            ).apply {
                trackType = TrackSegment.TrackType.DEFAULT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                setBlockType(TrackSegment.BlockType.CONTINUOUS, 3.0, 3.0)
                blockSection(true)
                isUseTrainTargetSpeed = true
            }
            val mexico = StationSegment(
                "mexico",
                "Mexico",
                trackedRide,
                CoasterMathUtils.bptToKmh(TARGET_SPEED),
                CoasterMathUtils.bptToKmh(MAX_SPEED),
                2.1
            ).apply {
                nameOnMap = "Mexico"
                setupAsStation()
                holdDistance = -2.2
//                setHoldDistance(length - 0.2)
            }
            val mexicoToFantasy = TransportSegment(
                "mexicoToFantasy",
                trackedRide,
                TARGET_SPEED,
                ACCELERATION_FORCE,
                MAX_SPEED,
                BRAKE_FORCE
            ).apply {
                trackType = TrackSegment.TrackType.DEFAULT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                setBlockType(TrackSegment.BlockType.CONTINUOUS, 3.0, 3.0)
                blockSection(true)
                isUseTrainTargetSpeed = true
            }
            val fantasy = StationSegment(
                "fantasy",
                "Fantasy",
                trackedRide,
                CoasterMathUtils.bptToKmh(TARGET_SPEED),
                CoasterMathUtils.bptToKmh(MAX_SPEED),
                2.1
            ).apply {
                nameOnMap = "Fantasy"
                setupAsStation()
//                setHoldDistance(length - 0.2)
            }
            val fantasyToViking = TransportSegment(
                "fantasyToViking",
                trackedRide,
                TARGET_SPEED,
                ACCELERATION_FORCE,
                MAX_SPEED,
                BRAKE_FORCE
            ).apply {
                trackType = TrackSegment.TrackType.DEFAULT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                setBlockType(TrackSegment.BlockType.CONTINUOUS, 3.0, 3.0)
                blockSection(true)
                isUseTrainTargetSpeed = true
            }
            val viking = StationSegment(
                "viking",
                "Viking",
                trackedRide,
                CoasterMathUtils.bptToKmh(TARGET_SPEED),
                CoasterMathUtils.bptToKmh(MAX_SPEED),
                2.1
            ).apply {
                nameOnMap = "Viking"
                setupAsStation()
//                setHoldDistance(length - 0.2)
            }
            val vikingToMainstreet = TransportSegment(
                "vikingToMainstreet",
                trackedRide,
                TARGET_SPEED,
                ACCELERATION_FORCE,
                MAX_SPEED,
                BRAKE_FORCE
            ).apply {
                trackType = TrackSegment.TrackType.DEFAULT
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                setBlockType(TrackSegment.BlockType.CONTINUOUS, 3.0, 3.0)
                blockSection(true)
                isUseTrainTargetSpeed = true
            }

            vikingToMainstreet.add(
            )

            viking.add(
            )

            fantasyToViking.add(
            )

            fantasy.add(
            )

            mexicoToFantasy.add(
            )

            mexico.add(
            )

            mainstreetToMexico.add(
            )

            mainstreet.add(
            )



            mainstreet
                .setNextTrackSegmentRetroActive(mainstreetToMexico)
                .setNextTrackSegmentRetroActive(mexico)
                .setNextTrackSegmentRetroActive(mexicoToFantasy)
                .setNextTrackSegmentRetroActive(fantasy)
                .setNextTrackSegmentRetroActive(fantasyToViking)
                .setNextTrackSegmentRetroActive(viking)
                .setNextTrackSegmentRetroActive(vikingToMainstreet)
                .setNextTrackSegmentRetroActive(mainstreet)

            trackedRide.addTrackSections(
                mainstreet,
                mainstreetToMexico,
                mexico,
                mexicoToFantasy,
                fantasy,
                fantasyToViking,
                viking,
                vikingToMainstreet
            )
        }
    }
}

