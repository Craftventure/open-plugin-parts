package net.craftventure.core.ride.tracked

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.ride.trackedride.segment.TransportSegment
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.repository.PlayerKeyValueRepository
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds


class Vogelrok private constructor(trackedRide: TrackedRide) {
    companion object {
        private var vogelrok: Vogelrok? = null

        private const val TRACK_FRICTION_TRACK1 = 0.9994
        private const val GRAVITATIONAL_INFLUENCE_TRACK1 = 0.035
        private const val TRACK_FRICTION = 0.9994
        private const val GRAVITATIONAL_INFLUENCE = 0.035
        private const val CAR_LENGTH = 2.9
        private const val SEAT_SIDE_OFFSET = 0.35
        private const val SEAT_HEIGHT_OFFSET = -0.95 + 1.5
        private const val RIDETIME = 60 + 40
        private const val CAR_COUNT = 4

        fun get(): Vogelrok {
            if (vogelrok == null) {
                val coasterArea = SimpleArea("world", )
                val trackedRide = OperableCoasterTrackedRide(
                    "vogelrok", coasterArea,
                    Location(Bukkit.getWorld("world"), ),
                    "ride_vogelrok", "vogelrok"
                )

                trackedRide.setOperatorArea(SimpleArea("world",))
                initTrack(trackedRide)

                for (t in 0 until 3) {
                    val segment = if (t == 0)
                        trackedRide.getSegmentById("station")
                    else if (t == 1)
                        trackedRide.getSegmentById("prestation")
                    else
                        trackedRide.getSegmentById("block1")
                    val rideTrain = CoasterRideTrain(segment, 5.0)
                    rideTrain.setTrainSoundName(
                        "vogelrok",
                        SpatialTrainSounds.Settings(
                            setOf(
                                TrackSegment.TrackType.FRICTION_BRAKE,
                                TrackSegment.TrackType.WHEEL_TRANSPORT
                            )
                        )
                    )

                    for (i in 0..5) {
                        val rideCar = DynamicSeatedRideCar.fromLegacyFormat("vogelrok", 2, 2, 0.7, 1.1, 2.5)
                        rideCar.carFrontBogieDistance = 1.0
                        rideCar.carRearBogieDistance = rideCar.carFrontBogieDistance - rideCar.length
                        if (i == 0) {
                            (rideCar.getSeat(0) as ArmorStandSeat).setModel(MaterialConfig.VR_FRONT)
                            if (t == 0)
                                (rideCar.getSeat(1) as ArmorStandSeat).apply {
                                    setModel(MaterialConfig.ROK_PERSON)
                                    isPassengerCar = false
                                }
                        } else if (i == 5) {
                            (rideCar.getSeat(0) as ArmorStandSeat).setModel(MaterialConfig.VR_REAR)
                        } else {
                            (rideCar.getSeat(0) as ArmorStandSeat).setModel(MaterialConfig.VR_NORMAL)
                        }
                        rideTrain.addCar(rideCar)
                    }
                    trackedRide.addTrain(rideTrain)
                }

                trackedRide.initialize()
                trackedRide.pukeRate = 0.0
                trackedRide.addOnRideCompletionListener { player, rideCar ->
                    if (DateUtils.isCoasterDay) {
                        val database = MainRepositoryProvider.achievementProgressRepository
                        database.reward(player.uniqueId, "coaster_day")
                        database.reward(player.uniqueId, "coaster_day_" + LocalDateTime.now().year)
                    }
                }
                vogelrok = Vogelrok(trackedRide)
            }
            return vogelrok!!
        }

        private fun initTrack(trackedRide: TrackedRide) {
            val station = StationSegment("station", "Station", trackedRide, 9.0, 15.0, 2.1)
//            station.setDebugInstantStart(true)
            station.slowBrakingDistance = 5.0
            station.holdDistance = -1.5
            station.setDispatchIntervalTime(37.seconds)
            station.setKeepRollingTime(37.seconds)
            station.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            station.leaveMode = TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER
            station.ejectType = TrackSegment.EjectType.EJECT_TO_EXIT

            station.addPlayerExitListener { rideCar, player ->
//                Logger.debug("Ejecting ${rideCar.attachedTrain.isEjecting}")

                if (rideCar.attachedTrain.isEjecting) {
                    executeAsync {
                        val database = MainRepositoryProvider.playerKeyValueRepository
                        val value = database.getValue(
                            player.uniqueId,
                            PlayerKeyValueRepository.ROK_EGG_UNUSED_COUNT
                        )?.toIntOrNull() ?: return@executeAsync
                        val newValue = value - 1
                        if (newValue <= 0) {
                            database.deleteByKey(player.uniqueId, PlayerKeyValueRepository.ROK_EGG_UNUSED_COUNT)
                        } else {
                            database.createOrUpdate(
                                player.uniqueId,
                                PlayerKeyValueRepository.ROK_EGG_UNUSED_COUNT,
                                newValue.toString()
                            )
                        }
                        player.sendMessage(CVTextColor.serverNotice + "A \"hatchling\" was used")
                    }
                }
            }
            //        station1.setMinimumHoldTime((long) ((28.5 + 15) * 1000), TimeUnit.MILLISECONDS);
            station.setOnStationGateListener { open ->
                Location(Bukkit.getWorld("world"), ).block.open(open)
                Location(Bukkit.getWorld("world"), ).block.open(open)
                Location(Bukkit.getWorld("world"), ).block.open(open)
                Location(Bukkit.getWorld("world"), ).block.open(open)
                Location(Bukkit.getWorld("world"), ).block.open(open)
                Location(Bukkit.getWorld("world"), ).block.open(open)
                Location(Bukkit.getWorld("world"), ).block.open(open)
            }
            station.setOnStationStateChangeListener { newState, oldState ->
                if (newState == StationSegment.StationState.DISPATCHING) {
                    val rideTrain = station.anyRideTrainOnSegment
                    rideTrain?.setOnboardSynchronizedAudio("vogelrok_onride", System.currentTimeMillis())
                }
            }
            station.add(
            )

            val predrop = TransportSegment(
                "predrop",
                trackedRide,
                CoasterMathUtils.kmhToBpt(9.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            predrop.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            predrop.setFriction(TRACK_FRICTION_TRACK1)
            predrop.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE_TRACK1)
            predrop.add(
            )

            val lift = TransportSegment(
                "lift1", "Supernova lift",
                trackedRide,
                CoasterMathUtils.kmhToBpt(9.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(9.0),
                CoasterMathUtils.kmhToBpt(2.2)
            )
            lift.trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
            lift.blockSection(true)
            lift.add(
            )

            val track1 = TransportSegment(
                "track1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(8.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            track1.setFriction(TRACK_FRICTION_TRACK1)
            track1.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE_TRACK1)
            track1.add(
            )

            val mcbr = TransportSegment(
                "mcbr",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            mcbr.trackType = TrackSegment.TrackType.FRICTION_BRAKE
            mcbr.setFriction(TRACK_FRICTION)
            mcbr.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            mcbr.blockSection(true)
            mcbr.add(
            )

            val track2 = TransportSegment(
                "track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(8.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            track2.setFriction(TRACK_FRICTION)
            track2.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            track2.add(
            )

            val block1 = TransportSegment(
                "block1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            block1.trackType = TrackSegment.TrackType.FRICTION_BRAKE
            block1.setFriction(TRACK_FRICTION)
            block1.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            block1.blockSection(true)
            block1.add(
            )

            val switch = TransportSegment(
                "switch",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            switch.trackType = TrackSegment.TrackType.FRICTION_BRAKE
            switch.setFriction(TRACK_FRICTION)
            switch.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            switch.add(
            )

            val prestation = TransportSegment(
                "prestation",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12.0),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            prestation.trackType = TrackSegment.TrackType.FRICTION_BRAKE
            prestation.setBlockType(TrackSegment.BlockType.BLOCK_SECTION, 1.0, 1.0)
            prestation.setFriction(TRACK_FRICTION)
            prestation.setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
            prestation.blockSection(true)
            prestation.add(
            )

            station
                .setNextTrackSegmentRetroActive(predrop)
                .setNextTrackSegmentRetroActive(lift)
                .setNextTrackSegmentRetroActive(track1)
                .setNextTrackSegmentRetroActive(mcbr)
                .setNextTrackSegmentRetroActive(track2)
                .setNextTrackSegmentRetroActive(block1)
                .setNextTrackSegmentRetroActive(switch)
                .setNextTrackSegmentRetroActive(prestation)
                .setNextTrackSegmentRetroActive(station)

            trackedRide.addTrackSections(
                station,
                predrop,
                lift,
                track1,
                mcbr,
                track2,
                block1,
                switch,
                prestation
            )
        }
    }
}

