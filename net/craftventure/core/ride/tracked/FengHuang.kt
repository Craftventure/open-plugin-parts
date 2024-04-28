package net.craftventure.core.ride.tracked

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.core.feature.maxifoto.MaxiFoto
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.ride.trackedride.segment.TransportSegment
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class FengHuang private constructor(trackedRide: TrackedRide) {
    companion object {
        private var fengHuang: FengHuang? = null

        fun get(): FengHuang {
            if (fengHuang == null) {
                val coasterArea = SimpleArea("world",)
                val trackedRide = OperableCoasterTrackedRide(
                    "fenghuang", coasterArea, Location(Bukkit.getWorld("world"), ),
                    "ride_fenghuang", "fenghuang"
                )
                trackedRide.setOperatorArea(SimpleArea("world", ))
                initTrack(trackedRide)

                for (t in 0..1) {
                    val segment = if (t == 0)
                        trackedRide.getTrackSegments()[0]
                    else
                        trackedRide.getTrackSegments()[trackedRide.getTrackSegments().size - t * 2 - t]
                    // Create a train
                    val rideTrain = CoasterRideTrain(segment, 0.0)//(2.3 * 5) + 1.2);
                    rideTrain.setTrainSoundName(
                        "fenghuang",
                        SpatialTrainSounds.Settings(
                            setOf(
                                TrackSegment.TrackType.FRICTION_BRAKE,
                                TrackSegment.TrackType.CHAIN_LIFT,
                                TrackSegment.TrackType.WHEEL_TRANSPORT
                            )
                        )
                    )

                    val seat1Offset = 1.4
                    val seat2Offset = 2.15
                    val seatYOffset = -0.35
                    for (i in 0..6) {
                        val dynamicSeatedRideCar = DynamicSeatedRideCar("fenghuang", 2.0)
                        if (i == 2)
                            dynamicSeatedRideCar.setHasTrainSound(true)
                        dynamicSeatedRideCar.setFakeSeatProvider { mountedEntityIndex, currentEntityIndex ->
                            if (currentEntityIndex == 0) {
                                if (mountedEntityIndex == 3 || mountedEntityIndex == 4) {
                                    return@setFakeSeatProvider MaterialConfig.FENG_HUANG_CAR_NO_RIGHT_SEAT
                                } else if (mountedEntityIndex == 1 || mountedEntityIndex == 2) {
                                    return@setFakeSeatProvider MaterialConfig.FENG_HUANG_CAR_NO_LEFT_SEAT
                                }
                            }
                            null
                        }

                        val modelSeat = ArmorStandSeat(0.0, 1.0, 0.0, false, "fenghuang")
                        modelSeat.setModel(MaterialConfig.FENG_HUANG_CAR)
                        dynamicSeatedRideCar.addSeat(modelSeat)
                        dynamicSeatedRideCar.addSeat(ArmorStandSeat(-seat2Offset, seatYOffset, 0.0, true, "fenghuang"))
                        dynamicSeatedRideCar.addSeat(ArmorStandSeat(-seat1Offset, seatYOffset, 0.0, true, "fenghuang"))
                        dynamicSeatedRideCar.addSeat(ArmorStandSeat(seat1Offset, seatYOffset, 0.0, true, "fenghuang"))
                        dynamicSeatedRideCar.addSeat(ArmorStandSeat(seat2Offset, seatYOffset, 0.0, true, "fenghuang"))
                        dynamicSeatedRideCar.carRearBogieDistance = -2.0
                        rideTrain.addCar(dynamicSeatedRideCar)
                    }
                    trackedRide.addTrain(rideTrain)
                }

                //      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
                trackedRide.initialize()
                trackedRide.pukeRate = 0.04
                trackedRide.addOnRideCompletionListener { player, rideCar ->
                    if (DateUtils.isCoasterDay) {
                        val database = MainRepositoryProvider.achievementProgressRepository
                        database.reward(player.uniqueId, "coaster_day")
                        database.reward(player.uniqueId, "coaster_day_" + LocalDateTime.now().year)
                    }
                }
                fengHuang = FengHuang(trackedRide)
            }
            return fengHuang!!
        }

        private fun initTrack(trackedRide: TrackedRide) {
            val offset = Vector(0, 0, 0)//-0.6, 0);

            val station1 = StationSegment("station1", trackedRide, 12.0, 14.0, 2.1)
            station1.setDispatchIntervalTime((48.5 * 1000).milliseconds)
            station1.slowBrakingDistance = 5.0
            station1.holdDistance = 15.18
            station1.leaveMode = TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER
            station1.setAutoDispatchTime(90.seconds)
            station1.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            //        station1.setDebugInstantStart(true);
            station1.setOnStationStateChangeListener { newState, oldState ->
                if (newState == StationSegment.StationState.DISPATCHING) {
                    val rideTrain = station1.anyRideTrainOnSegment
                    rideTrain?.setOnboardSynchronizedAudio("fenghuang_onride", System.currentTimeMillis())
                }
            }
            station1.setOnStationGateListener { open ->
            }
            station1.add(
                offset,
            )

            val track1 = SplinedTrackSegment("track1", trackedRide)
            track1.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            track1.add(
                offset,
            )

            val lift1 = TransportSegment(
                "lift1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            lift1.trackType = TrackSegment.TrackType.CHAIN_LIFT
            lift1.blockSection(true)
            lift1.add(
                offset,
            )

            val track2 = TransportSegment(
                "track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(16.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(150.0), //150),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            track2.add(
                offset,
            )

            val block1 = TransportSegment(
                "block1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(2.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(16.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            block1.trackType = TrackSegment.TrackType.FRICTION_BRAKE
            block1.blockSection(true)
            block1.add(
                offset,
            )

            val track3 = SplinedTrackSegment("track3", trackedRide)
            track3.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            track3.add(
                offset,
            )

            val station2 = TransportSegment(
                "station2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            station2.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            station2.blockSection(true)
            station2.ejectType = TrackSegment.EjectType.EJECT_TO_SEAT

            station2.add(object : TrackSegment.DistanceListener(16.0, true) {
                override fun onTargetHit(triggeredCar: RideCar) {
                    val names = arrayOfNulls<Player>(28)
                    for (i in names.indices)
                        names[i] = null

                    var index = 0
                    for (rideCar in triggeredCar.attachedTrain.cars) {
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

                    val renderSettings = MaxiFoto.RenderSettings("fenghuang", names)
                    if (!onlyNulls) {
                        MaxiFoto.render(renderSettings)
                    }

                    triggeredCar.attachedTrain.eject()
                }
            })
            station2.add(
                offset,
            )

            val track4 = SplinedTrackSegment("track4", trackedRide)
            track4.add(
                offset,
            )

            station1.setNextTrackSegmentRetroActive(track1)
            trackedRide.addTrackSection(station1)

            track1.setNextTrackSegmentRetroActive(lift1)
            trackedRide.addTrackSection(track1)

            lift1.setNextTrackSegmentRetroActive(track2)
            trackedRide.addTrackSection(lift1)

            track2.setNextTrackSegmentRetroActive(block1)
            trackedRide.addTrackSection(track2)

            block1.setNextTrackSegmentRetroActive(track3)
            trackedRide.addTrackSection(block1)

            track3.setNextTrackSegmentRetroActive(station2)
            trackedRide.addTrackSection(track3)

            station2.setNextTrackSegmentRetroActive(track4)
            trackedRide.addTrackSection(station2)

            track4.setNextTrackSegmentRetroActive(station1)
            trackedRide.addTrackSection(track4)
        }
    }
}
