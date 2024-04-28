package net.craftventure.core.ride.tracked

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.add
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.extension.rotateY
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.t
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.SidewaysTransferSegment
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.ride.trackedride.segment.TransportSegment
import net.craftventure.core.ride.trackedride.segment.VerticalAutoLift
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import net.craftventure.core.utils.SimpleInterpolator
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import penner.easing.Quad
import penner.easing.Sine
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.annotation.Nonnull
import kotlin.time.Duration.Companion.seconds


class Skyfoogle private constructor(trackedRide: TrackedRide) {
    companion object {
        private var skyfoogle: Skyfoogle? = null

        private const val TRACK_FRICTION = 0.9983
        private const val GRAVITATIONAL_INFLUENCE = 0.03
        private const val CAR_LENGTH = 2.9
        private const val SEAT_SIDE_OFFSET = 0.35
        private const val SEAT_HEIGHT_OFFSET = -0.95 + 1.5
        private const val RIDETIME = 60 + 40
        private const val CAR_COUNT = 4

        fun get(): Skyfoogle {
            if (skyfoogle == null) {
                val coasterArea = SimpleArea("world", )
                val trackedRide = OperableCoasterTrackedRide(
                    "skyfoogle", coasterArea,
                    Location(Bukkit.getWorld("world"), ),
                    "ride_skyfoogle", "skyfoogle"
                )

                trackedRide.setOperatorArea(SimpleArea("world", ))
                initTrack(trackedRide)
                var distance = 3.0

                var segment = trackedRide.getSegmentById("brakerun")

                val carModelOffset = -0.3
                for (i in 0 until CAR_COUNT) {
                    if (i == CAR_COUNT - 1) {
                        segment = trackedRide.getSegmentById("block1")
                        distance = 0.0
                    }
                    val rideTrain = CoasterRideTrain(segment, distance)
                    rideTrain.setTrainSoundName(
                        "skyfoogle",
                        SpatialTrainSounds.Settings(
                            setOf(
                                TrackSegment.TrackType.FRICTION_BRAKE,
                                TrackSegment.TrackType.CHAIN_LIFT,
                                TrackSegment.TrackType.WHEEL_TRANSPORT
                            )
                        )
                    )

                    val dynamicSeatedRideCar = DynamicSeatedRideCar("skyfoogle", CAR_LENGTH)
                    dynamicSeatedRideCar.carFrontBogieDistance = -1.0
                    dynamicSeatedRideCar.carRearBogieDistance = -CAR_LENGTH + 1.0

                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            0.0,
                            0.5,
                            carModelOffset + -1.6 + 1.25,
                            false,
                            "skyfoogle"
                        ).apply {
                            setModel(MaterialConfig.SKYFOOGLE)
                        })

//                    dynamicSeatedRideCar.addSeat(ArmorStandSeat(0.0, 0.0, 0.0, false, "skyfoogle")
//                            .setModel(ItemStack(Material.STONE)))
//
//                    dynamicSeatedRideCar.addSeat(ArmorStandSeat(0.0, 0.0, -CAR_LENGTH, false, "skyfoogle")
//                            .setModel(ItemStack(Material.REDSTONE_BLOCK)))

                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            SEAT_SIDE_OFFSET,
                            SEAT_HEIGHT_OFFSET,
                            carModelOffset + -0.9,
                            true,
                            "skyfoogle"
                        )
                    )
                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            -SEAT_SIDE_OFFSET,
                            SEAT_HEIGHT_OFFSET,
                            carModelOffset + -0.9,
                            true,
                            "skyfoogle"
                        )
                    )

                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            SEAT_SIDE_OFFSET,
                            SEAT_HEIGHT_OFFSET,
                            carModelOffset + -2.25,
                            true,
                            "skyfoogle"
                        )
                    )
                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            -SEAT_SIDE_OFFSET,
                            SEAT_HEIGHT_OFFSET,
                            carModelOffset + -2.25,
                            true,
                            "skyfoogle"
                        )
                    )

                    rideTrain.addCar(dynamicSeatedRideCar)
                    trackedRide.addTrain(rideTrain)

                    distance -= CAR_LENGTH
                    while (distance < 0) {
                        segment = segment!!.previousTrackSegment
                        distance += segment!!.length
                    }
                }

                trackedRide.initialize()
                trackedRide.pukeRate = 0.06
                trackedRide.addOnRideCompletionListener { player, rideCar ->
                    if (DateUtils.isCoasterDay) {
                        val database = MainRepositoryProvider.achievementProgressRepository
                        database.reward(player.uniqueId, "coaster_day")
                        database.reward(player.uniqueId, "coaster_day_" + LocalDateTime.now().year)
                    }
                }
                skyfoogle = Skyfoogle(trackedRide)
            }
            return skyfoogle!!
        }

        private fun initTrack(trackedRide: TrackedRide) {
            val station = StationSegment("station1", "Station", trackedRide, 4.0, 6.0, 2.1)
//            station.setDebugInstantStart(true)
            station.slowBrakingDistance = 3.0
            station.slowBrakingMinSpeed = CoasterMathUtils.kmhToBpt(1.5)
            station.holdDistance = 3.5 + (CAR_LENGTH / 2.0)
            station.setDispatchIntervalTime(35.seconds)
            station.setKeepRollingTime((RIDETIME / CAR_COUNT.toDouble()).toLong().seconds)
            station.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            station.leaveMode = TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER
            station.setAutoDispatchTime(60.seconds)
            //        station1.setMinimumHoldTime((long) ((28.5 + 15) * 1000), TimeUnit.MILLISECONDS);
            station.add(
            )
            station.setOnStationGateListener { open ->
                Location(trackedRide.area.world, 98.0, 42.0, -330.0).block.open(open)
                Location(trackedRide.area.world, 97.0, 42.0, -330.0).block.open(open)
            }
            station.setOnStationStateChangeListener { newState, oldState ->
                if (newState == StationSegment.StationState.DISPATCHING) {
                    val rideTrain = station.anyRideTrainOnSegment
                    rideTrain?.setOnboardSynchronizedAudio("skyfoogle_onride", System.currentTimeMillis())
                }
            }

            val track1 = TransportSegment(
                "track1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(8.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            track1.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            track1.setFriction(TRACK_FRICTION)
            track1.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            track1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
            track1.blockSection(true)
            track1.add(
            )

            val lift = VerticalAutoLift(
                "lift",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(1.8),
                SimpleInterpolator({ t, b, c, d -> Quad.easeInOut(t, b, c, d) })
            )
            lift.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            lift.blockSection(true)
            lift.setLeavingSpeeds(
                CoasterMathUtils.kmhToBpt(15.0),
                CoasterMathUtils.kmhToBpt(15.0),
                CoasterMathUtils.kmhToBpt(1.5),
                CoasterMathUtils.kmhToBpt(0.2)
            )
            lift.setLiftDuration((20 * 8).toLong())
            lift.setWaitBottomExitDuration(2 * 20)
            lift.setWaitTopEnterDuration(2 * 20)
            lift.setTriggerDistanceFromEnd(0.5)
            lift.add(
            )
            val liftListener = LiftController(lift)
            lift.setLiftListener(liftListener)
            lift.positionInterceptor = liftListener


            val track2 = TransportSegment(
                "track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(15.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            track2.setFriction(TRACK_FRICTION)
            track2.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
//            track2.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
//            track2.blockSection(true)
            track2.add(
            )

            val klok = TransportSegment(
                "klok",
                trackedRide,
                CoasterMathUtils.kmhToBpt(15.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(15.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            klok.setFriction(TRACK_FRICTION)
            klok.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            klok.blockSection(true)
            klok.add(
                SplineNode(
                    SplineHandle(119.428589, 54.017105, -361.415466),
                    SplineHandle(120.259979, 54.036251, -362.271973),
                    SplineHandle(121.741562, 54.070370, -363.798309), -0.000000
                ),
                SplineNode(
                    SplineHandle(122.817810, 54.001286, -364.785400),
                    SplineHandle(123.619080, 54.036251, -365.669708),
                    SplineHandle(124.400940, 54.070370, -366.532593), -0.000000
                )
            )
            KlokController(klok)

            val track3 = TransportSegment(
                "track3",
                trackedRide,
                CoasterMathUtils.kmhToBpt(30.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            track3.setFriction(TRACK_FRICTION)
            track3.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
//            track3.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
//            track3.blockSection(true)
            track3.add(
            )

            val block1 = TransportSegment(
                "block1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(15.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(15.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            block1.trackType = TrackSegment.TrackType.FRICTION_BRAKE
            block1.setFriction(TRACK_FRICTION)
            block1.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            block1.blockSection(true)
            block1.add(
                SplineNode(
                    SplineHandle(143.440674, 53.020367, -342.934021),
                    SplineHandle(143.482559, 53.041565, -344.138336),
                    SplineHandle(143.541870, 53.071579, -345.843536), -0.000000
                ),
                SplineNode(
                    SplineHandle(143.458679, 53.023792, -348.716217),
                    SplineHandle(143.493790, 53.041565, -349.920776),
                    SplineHandle(143.523438, 53.056572, -350.937988), -0.000000
                )
            )

            val track4 = TransportSegment(
                "track4",
                trackedRide,
                CoasterMathUtils.kmhToBpt(25.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            track4.setFriction(TRACK_FRICTION)
            track4.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
//            track4.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
//            track4.blockSection(true)
            track4.add(
            )

            val preBrake = TransportSegment(
                "preBrake",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            preBrake.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
//            preBrake.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
//            preBrake.blockSection(true)
            preBrake.add(
            )


            val transfer1 = SidewaysTransferSegment.TransferSegment(
                "transfer1",
                trackedRide,
                SidewaysTransferSegment.Direction.FORWARDS
            )
            transfer1.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            transfer1.add(
            )

            val transfer2 = SidewaysTransferSegment.TransferSegment(
                "transfer2",
                trackedRide,
                SidewaysTransferSegment.Direction.FORWARDS
            )
            transfer2.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            transfer2.add(
            )

            val transfer3 = SidewaysTransferSegment.TransferSegment(
                "transfer3",
                trackedRide,
                SidewaysTransferSegment.Direction.FORWARDS
            )
            transfer3.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            transfer3.add(
            )

            val transfer = SidewaysTransferSegment(
                "transfer",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
//            transfer.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
            transfer.blockSection(true)
            transfer.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            transfer.add(
            )
            transfer.addTransferTarget(
                SidewaysTransferSegment.TransferTarget(
                    Vector(0, 0, 10),
                    transfer1
                )
            )
            transfer.addTransferTarget(
                SidewaysTransferSegment.TransferTarget(
                    Vector(0, 0, 12),
                    transfer2
                )
            )
            transfer.addTransferTarget(
                SidewaysTransferSegment.TransferTarget(
                    Vector(0, 0, 14),
                    transfer3
                )
            )
            transfer.addUpdateListener(object : SidewaysTransferSegment.UpdateListener {
                val blockArea: SimpleArea = SimpleArea("world", 86.0, 33.0, -339.0, 122.0, 50.0, -309.0)
                var areaTracker: NpcAreaTracker

                var isTracking = false
                var blockLocations = arrayOfNulls<Vector>(3)
                var blockEntities = arrayOfNulls<NpcEntity>(3)

                init {
                    areaTracker = NpcAreaTracker(blockArea)

                    blockLocations[0] = Vector().apply { transfer.getPosition(0.5, this) }.add(0.0, -1.0, 0.0)
                    blockLocations[1] = Vector().apply { transfer.getPosition(1.5, this) }.add(0.0, -1.0, 0.0)
                    blockLocations[2] = Vector().apply { transfer.getPosition(2.5, this) }.add(0.0, -1.0, 0.0)

                    for (i in blockLocations.indices) {
                        val npcEntity =
                            NpcEntity(
                                "skyfoogleLift",
                                EntityType.FALLING_BLOCK,
                                blockLocations[i]!!.toLocation(blockArea.world)
                            )
                        npcEntity.setBlockData(Material.ORANGE_TERRACOTTA.createBlockData())
                        npcEntity.noGravity(true)
                        blockEntities[i] = npcEntity

                        areaTracker.addEntity(npcEntity)
                    }
                }

                override fun onUpdate(track: SidewaysTransferSegment) {
                    blockLocations[0] =
                        blockLocations[0]!!.apply { transfer.getPosition(0.5, this) }.add(0.0, -1.0, 0.0)
                    blockLocations[1] =
                        blockLocations[1]!!.apply { transfer.getPosition(1.5, this) }.add(0.0, -1.0, 0.0)
                    blockLocations[2] =
                        blockLocations[2]!!.apply { transfer.getPosition(2.5, this) }.add(0.0, -1.0, 0.0)

                    if (track.isTrackMoving()) {
                        if (!isTracking) {
                            areaTracker.startTracking()
                            isTracking = true

                            for (blockLocation in blockLocations) {
                                val block = blockLocation!!.toLocation(blockArea.world).block
                                block.type = Material.AIR
                            }
                        }
                        //
                        //                    Logger.info("Updating blocks");
                        for (i in blockEntities.indices) {
                            val npcEntity = blockEntities[i]
                            val target = blockLocations[i]!!.clone()
                            npcEntity?.move(target.x, target.y, target.z)
                        }
                    } else {
                        if (isTracking) {
                            areaTracker.stopTracking()
                            isTracking = false

                            for (blockLocation in blockLocations) {
                                val block = blockLocation!!.toLocation(blockArea.world).block
                                block.type = Material.ORANGE_TERRACOTTA
                            }
                        }
                    }
                }
            })

            val brakerun = TransportSegment(
                "brakerun",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            brakerun.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
            brakerun.blockSection(true)
            brakerun.add(
            )
            brakerun.add(object : TrackSegment.DistanceListener(2.5) {
                override fun onTargetHit(rideCar: RideCar) {
                    rideCar.attachedTrain.eject()
                }
            })

            val exit = TransportSegment(
                "exit", "Exit",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            exit.add(object : TrackSegment.DistanceListener(2.5) {
                override fun onTargetHit(rideCar: RideCar) {
                    rideCar.attachedTrain.eject()
                }
            })
            exit.blockSection(true)

            exit.add(
            )

            station
                .setNextTrackSegmentRetroActive(track1)
                .setNextTrackSegmentRetroActive(lift)
                .setNextTrackSegmentRetroActive(track2)
                .setNextTrackSegmentRetroActive(klok)
                .setNextTrackSegmentRetroActive(track3)
                .setNextTrackSegmentRetroActive(block1)
                .setNextTrackSegmentRetroActive(track4)
                .setNextTrackSegmentRetroActive(preBrake)
                .setNextTrackSegmentRetroActive(transfer)
                .setNextTrackSegmentRetroActive(brakerun)
                .setNextTrackSegmentRetroActive(exit)
                .setNextTrackSegmentRetroActive(station)

            trackedRide.addTrackSections(
                station,
                track1,
                lift,
                track2,
                klok,
                track3,
                block1,
                track4,
                preBrake,
                transfer,
                brakerun,
                exit
            )
        }
    }

    private class KlokController(
        val transportSegment: TransportSegment
    ) : TrackedRide.PreTrainUpdateListener, TrackSegment.PositionInterceptor, TrackSegment.OnSectionLeaveListener {
        val trigger = 3.8
        var animationStartTime: Long = 0
        val base = Vector(122.0, 54.0, -364.0)
        var angle = 0.0
        var task = 0
        var state = State.IDLE

        //            set(value) {
//                field = value
//                Logger.info("State changed to $value")
//            }
        val baseOffset = Vector(-0.5, 0.0, -0.5).normalize()
        val offsetVector = Vector(0, 0, 0)
        val forwardDistance = 7.5
        val bounces = 2
        val bounceTime = 2000
        val totalBounceTime = (bounceTime * bounces)

        init {
            transportSegment.trackedRide.addPreTrainUpdateListener(this)
            transportSegment.positionInterceptor = this
            transportSegment.addOnSectionLeaveListener(this)
        }

        private fun requestUpdates() {
            cancelTask()
            animationStartTime = System.currentTimeMillis()
            task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
                //                Logger.info("Updating animation")
                val animationTime = System.currentTimeMillis() - animationStartTime

                if (animationTime in 500..2000) {
                    val t = Sine.easeInOut((500..2000).t(animationTime), 0.0, 1.0, 1.0)
                    angle = Math.toRadians(90.0 * t)
                } else if (animationTime in 2500..5000) {
                    val t = Sine.easeInOut((2500..5000).t(animationTime), 0.0, 1.0, 1.0)
                    offsetVector.set(baseOffset).multiply(t * forwardDistance)
                    angle = Math.toRadians(90.0)
                } else if (animationTime in 5000..5000 + totalBounceTime) {
                    for (i in 0 until bounces) {
                        if (animationTime in 5000 + (bounceTime * i)..5000 + (bounceTime * (i + 1))) {
                            val tBase = (5000 + (bounceTime * i)..5000 + (bounceTime * (i + 1))).t(animationTime)
                            if (tBase < 0.5) {
                                val t = Sine.easeInOut(tBase * 2, 0.0, 1.0, 1.0)
                                offsetVector.set(baseOffset).multiply((forwardDistance - 2.0) + ((1 - t) * 2))
                            } else {
                                val t = Sine.easeInOut((tBase - 0.5f) * 2, 0.0, 1.0, 1.0)
                                offsetVector.set(baseOffset).multiply((forwardDistance - 2.0) + (t * 2))
                            }
                        }
                    }
                } else if (animationTime in 5000 + totalBounceTime..5000 + totalBounceTime + 2500) {
                    val t = Sine.easeInOut(
                        (5000 + totalBounceTime..5000 + totalBounceTime + 2500).t(animationTime),
                        0.0,
                        1.0,
                        1.0
                    )
                    offsetVector.set(baseOffset).multiply((1 - t) * forwardDistance)
                    angle = Math.toRadians(90.0)
                } else if (animationTime in 5000 + totalBounceTime + 2500..5000 + totalBounceTime + 2500 + 1500) {
                    val t = Sine.easeInOut(
                        (5000 + totalBounceTime + 2500..5000 + totalBounceTime + 2500 + 1500).t(animationTime),
                        0.0,
                        1.0,
                        1.0
                    )
                    angle = Math.toRadians(90.0 * (1 - t))
                } else if (animationTime > 5000 + totalBounceTime + 2500 + 1500 + 500) {
                    angle = 0.0
                    state = State.LEAVING
                    transportSegment.isTransportEnabled = true
                }
            }, 1L, 1L)
        }

        private fun cancelTask() {
            Bukkit.getScheduler().cancelTask(task)
            offsetVector.set(0.0, 0.0, 0.0)
        }

        override fun onCarPreUpdate(rideCar: RideCar?) {
            if (rideCar?.trackSegment !== transportSegment) return

            if (state == State.IDLE && rideCar.distance + rideCar.attachedTrain.velocity > trigger) {
                state = State.ANIMATING
                transportSegment.isTransportEnabled = false
                requestUpdates()
            }

            if (state != State.LEAVING && state != State.IDLE) {
                for (trainCar in rideCar.attachedTrain.cars) {
                    trainCar.velocity = 0.0
                    trainCar.acceleration = 0.0
                }
            }
        }

        override fun getPosition(trackSegment: TrackSegment, distance: Double, position: Vector) {
//            Logger.info("Rotating $position with $angle")
            position.rotateY(angle, base)
            position.add(offsetVector)
        }

        override fun onTrainLeftSection(trackSegment: TrackSegment?, rideTrain: RideTrain?) {
            state = State.IDLE
            cancelTask()
        }

        enum class State {
            IDLE,
            ANIMATING,
            LEAVING
        }
    }

    private class LiftBlock(
        val tracker: NpcEntityTracker,
        val distance: Double,
        val blockData: BlockData,
        val segment: VerticalAutoLift
    ) {
        private var npcEntity: NpcEntity? = null
        private val calculationVector = Vector()
        private val world = Bukkit.getWorld("world")

        private fun updateBlockPosition() {
            segment.getPosition(distance, calculationVector, true)
            calculationVector.y -= 1.0
        }

        fun showBlocks(show: Boolean) {
            updateBlockPosition()
            val location = calculationVector.toLocation(world!!)
            val block = location.block
            if (!show) {
                block.type = Material.AIR
            } else {
                block.blockData = blockData
            }
        }

        fun moveBlocks() {
            npcEntity?.let { npcEntity ->
                updateBlockPosition()
                npcEntity.move(
                    calculationVector.x,
                    calculationVector.y,
                    calculationVector.z
                )
            }
        }

        fun spawn() {
            if (npcEntity == null) {
                updateBlockPosition()
                val npcEntity =
                    NpcEntity("skyfoogleLift", EntityType.FALLING_BLOCK, calculationVector.toLocation(world!!))
                npcEntity.noGravity(true)
                npcEntity.setBlockData(blockData)
                this.npcEntity = npcEntity
                tracker.addEntity(npcEntity)
            }
        }
    }

    private class LiftController(
        private val segment: VerticalAutoLift
    ) : VerticalAutoLift.LiftListener, TrackSegment.PositionInterceptor {
        private val areaTracker = NpcAreaTracker(SimpleArea("world", 83.0, 38.0, -352.0, 102.0, 72.0, -328.0))
        private val liftBlocks = mutableListOf<LiftBlock>()
        private var hasSpawned = false

        init {
            val offset = 1.0 / 16.0
            liftBlocks.add(LiftBlock(areaTracker, 0.5 - offset, Material.ORANGE_TERRACOTTA.createBlockData(), segment))
            liftBlocks.add(LiftBlock(areaTracker, 1.5 - offset, Material.ORANGE_TERRACOTTA.createBlockData(), segment))
            liftBlocks.add(LiftBlock(areaTracker, 2.5 - offset, Material.ORANGE_TERRACOTTA.createBlockData(), segment))
            liftBlocks.add(LiftBlock(areaTracker, 3.5 - offset, Material.ORANGE_TERRACOTTA.createBlockData(), segment))
        }

        //        private var lastT = 0.0
        private var midPosition: Vector? = null

        override fun getPosition(trackSegment: TrackSegment, distance: Double, position: Vector) {
            val lift = trackSegment as VerticalAutoLift
            val t = (lift.offset - lift.startYOffset) / lift.endYOffset

            if (t.isInfinite() || t.isNaN()) return

            if (midPosition === null) {
                midPosition = Vector()
                trackSegment.getPosition(trackSegment.length * 0.5, midPosition, false)
            }

            midPosition?.let { midPosition ->
                val useT = ((t * 1.5) - 0.2).clamp(0.0, 1.0)
                val angle = Math.toRadians(useT * 180.0)

                position.x -= midPosition.x
                position.z -= midPosition.z

                val newX = position.x * Math.cos(angle) - position.z * Math.sin(angle)
                val newZ = position.x * Math.sin(angle) + position.z * Math.cos(angle)

                position.x = newX + midPosition.x
                position.z = newZ + midPosition.z
            }
        }

        override fun onUpdate(@Nonnull segment: VerticalAutoLift, offset: Double) {
            if (hasSpawned) {
                for (i in liftBlocks.indices) {
                    val liftBlock = liftBlocks[i]
                    liftBlock.moveBlocks()
                }
            }
        }

        override fun onGoingUp(@Nonnull segment: VerticalAutoLift, @Nonnull rideTrain: RideTrain) {}

        override fun onGoingDown(@Nonnull segment: VerticalAutoLift) {

        }

        override fun onStateChanged(@Nonnull segment: VerticalAutoLift, @Nonnull newState: VerticalAutoLift.LiftState) {
//            Logger.info("New state " + newState.name)
            if (newState == VerticalAutoLift.LiftState.LIFTING || newState == VerticalAutoLift.LiftState.DOWNING) {
                if (!hasSpawned) {
                    //                    Logger.console("Spawning " + newState.name() + " > " + offset);
                    areaTracker.startTracking()
                    for (liftBlock in liftBlocks) {
                        liftBlock.showBlocks(false)
                        liftBlock.showBlocks(false)
                        liftBlock.spawn()
                    }
                    hasSpawned = true
                }
            } else {
                if (hasSpawned) {
                    //                    Logger.console("Despawning " + newState.name() + " > " + offset);
                    for (i in liftBlocks.indices) {
                        val liftBlock = liftBlocks[i]
                        liftBlock.showBlocks(true)
                    }
                    areaTracker.stopTracking()
                    hasSpawned = false
                }
            }
        }
    }
}

