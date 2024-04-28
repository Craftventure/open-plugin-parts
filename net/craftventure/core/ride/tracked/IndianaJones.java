package net.craftventure.core.ride.tracked;

import kotlin.time.Duration;
import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.core.feature.maxifoto.MaxiFoto;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar;
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat;
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide;
import net.craftventure.core.ride.trackedride.segment.LaunchSegment;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.ride.trackedride.segment.TransportSegment;
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;


public class IndianaJones {
    private static IndianaJones indianaJones;
    private static final double CAR_LENGTH = 4;
    private static final double Y_OFFSET = -0.6;
    private static final double FORWARD_OFFSET = 0;
    private static final double SIDE_OFFSET = 0.475;

    private IndianaJones(TrackedRide trackedRide) {
    }

    public static IndianaJones get() {
        if (indianaJones == null) {

            SimpleArea coasterArea = new SimpleArea("world", );
            final OperableCoasterTrackedRide trackedRide = new OperableCoasterTrackedRide("indy", coasterArea, new Location(Bukkit.getWorld("world"), ), "ride_indy", "indy");
            trackedRide.setOperatorArea(new SimpleArea("world", ));
            initTrack(trackedRide);

            TrackSegment segment = trackedRide.getSegmentById("station1");
            TrackSegment beginSegment = segment;
            double distance = 0;

            for (int t = 0; t < 6; t++) {
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, distance);//(2.3 * 5) + 1.2);

                DynamicSeatedRideCar dynamicSeatedRideCar = new DynamicSeatedRideCar("indy", CAR_LENGTH);
                dynamicSeatedRideCar.carFrontBogieDistance = -0.5;
                dynamicSeatedRideCar.carRearBogieDistance = -CAR_LENGTH + 0.5;

                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(SIDE_OFFSET, Y_OFFSET + 1.2, FORWARD_OFFSET - 2.2, true, "indy"));
                ArmorStandSeat delegateSeat = new ArmorStandSeat(-SIDE_OFFSET, Y_OFFSET + 1.2, FORWARD_OFFSET - 2.2, true, "indy");
                delegateSeat.setModel(MaterialConfig.INSTANCE.getINDIANA_JONES());
                dynamicSeatedRideCar.addSeat(delegateSeat);

                rideTrain.addCar(dynamicSeatedRideCar);

                trackedRide.addTrain(rideTrain);

                distance -= CAR_LENGTH;
                if (segment == beginSegment) {
                    segment = segment.getPreviousTrackSegment();
                    distance = segment.getLength() - CAR_LENGTH;
                }
                while (distance < 0) {
                    segment = segment.getPreviousTrackSegment();
                    distance += segment.getLength();
                    distance -= 10;
                }
            }

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            indianaJones = new IndianaJones(trackedRide);
        }
        return indianaJones;
    }

    private static void initTrack(TrackedRide trackedRide) {//127.970009f, -108.724308f, 27.026110f
        final double SPEED = 12;
        final double MAX_SPEED = 55;
        final double TRACK_FRICTION = 0.972;
        final double TRACK_FRICTION_DROP = 0.9925;
        final double ACCELERATE_FORCE = 1.3;
        final double BRAKE_FORCE = 1.3;
        final Vector offset = new Vector(0, 0, 0);

        StationSegment station1 = new StationSegment("station1", trackedRide, SPEED, SPEED, 2.1);
        station1.setSlowBrakingDistance(5.0);
        station1.setSlowBrakingMinSpeed(CoasterMathUtils.kmhToBpt(1.5));
        station1.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station1.setDispatchIntervalTime(35, TimeUnit.SECONDS);
        station1.setKeepRollingTime((long) (170 / (double) 5), TimeUnit.SECONDS);
        station1.setHoldDistance(6.0);
        station1.add(offset,);

        station1.setOnStationStateChangeListener((newState, oldState) -> {
            if (newState == StationSegment.StationState.DISPATCHING) {
                station1.setLastDepartureTime(System.currentTimeMillis());
                RideTrain rideTrain = station1.getAnyRideTrainOnSegment();
                if (rideTrain != null) {
                    rideTrain.setOnboardSynchronizedAudio("indy_onride", System.currentTimeMillis());
//                    rideTrain.setOnboardSynchronizedAudio("onride_spacemountain_p1", System.currentTimeMillis());
                }
            }
        });
        station1.setOnStationGateListener((StationSegment.OnStationGateListener) open -> BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(436, 40, -598), open));


        TransportSegment track1 = new TransportSegment("track1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track1.setFriction(TRACK_FRICTION);
        track1.blockSection(true);
        track1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track1.add(offset,);

        TransportSegment drop1 = new TransportSegment("drop1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        drop1.setFriction(TRACK_FRICTION_DROP);
        drop1.blockSection(true);
        drop1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        drop1.add(offset,);

//        String id, TrackedRide trackedRide, double transportSpeed, double accelerateForce, double maxSpeed, double brakeForce,
//        double launchTransportSpeed, double launchAccelerateForce, double launchMaxSpeed, double launchBrakeForce,
//        int stationaryTicks, double frontCarStationaryPercentage, boolean isBlockSection
        LaunchSegment launch1 = new LaunchSegment("launch1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE),
                CoasterMathUtils.kmhToBpt(SPEED * 2.5),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE * 2),
                CoasterMathUtils.kmhToBpt(MAX_SPEED * 1.2),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE),
                (int) (20 * 9.4),
                0.2,
                true);
        launch1.setFriction(TRACK_FRICTION);
        launch1.blockSection(true);
        launch1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        launch1.add(offset,);

        TransportSegment track2 = new TransportSegment("track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED * 1.8),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track2.setFriction(TRACK_FRICTION);
        track2.blockSection(true);
        track2.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track2.add(offset,);

        TransportSegment track3 = new TransportSegment("track3",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track3.setFriction(TRACK_FRICTION);
        track3.blockSection(true);
        track3.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track3.add(offset,);

        TransportSegment track3b = new TransportSegment("track3b",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track3b.setFriction(TRACK_FRICTION);
        track3b.blockSection(true);
        track3b.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track3b.add(offset,);

        TransportSegment track4 = new TransportSegment("track4",
                trackedRide,
                CoasterMathUtils.kmhToBpt(-SPEED * 1.8),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track4.setFriction(TRACK_FRICTION);
        track4.blockSection(true);
        track4.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track4.add(offset,);

        TransportSegment track5 = new TransportSegment("track5",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED * 1.8),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track5.setFriction(TRACK_FRICTION);
        track5.blockSection(true);
        track5.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track5.add(offset,);

        TransportSegment track5b = new TransportSegment("track5b",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track5b.setFriction(TRACK_FRICTION);
        track5b.blockSection(true);
        track5b.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track5b.add(offset,);

        TransportSegment track6 = new TransportSegment("track6",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track6.setOffsetFromNextSection(1.0);
        track6.setFriction(TRACK_FRICTION);
        track6.blockSection(true);
        track6.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track6.add(offset,);
        track6.add(new TrackSegment.DistanceListener(112.5) {
            int car = 0;

            @Override
            public void onTargetHit(@NotNull RideCar triggeringCar) {
                Player[] names = new Player[2];
                for (int i = 0; i < names.length; i++)
                    names[i] = null;

                int index = 0;
                for (RideCar rideCar : triggeringCar.attachedTrain.getCars()) {
                    for (Entity entity : rideCar.getMaxifotoPassengerList()) {
                        if (index < names.length) {
                            if (entity instanceof Player) {
                                names[index] = (Player) entity;
                            }
                            index++;
                        }
                    }
                }

                boolean onlyNulls = true;
                for (Player name : names) {
                    if (name != null) {
                        onlyNulls = false;
                        break;
                    }
                }

                MaxiFoto.RenderSettings renderSettings = new MaxiFoto.RenderSettings("indy", names);
                if (!onlyNulls) {
                    renderSettings.setOffset(car);
                    MaxiFoto.INSTANCE.render(renderSettings);
                    car++;
                    if (car >= 2) {
                        car = 0;
                    }
                }

                triggeringCar.eject();
            }
        });

        track3b.setBlockSegmentAdvancing(true);
        track3b.setOffsetFromNextSection(3);
        trackedRide.addPreTrainUpdateListener(new TrackedRide.PreTrainUpdateListener() {
            int waitingTicks = 0;

            @Override
            public void onCarPreUpdate(RideCar rideCar) {
                if (rideCar.getTrackSegment() == track3b) {
                    if (rideCar.getDistance() >= track3b.getLength() - 3.1) {
                        waitingTicks++;
                        if (waitingTicks > 20 * 3) {
                            track3b.setTransportEnabled(true);
                            rideCar.attachedTrain.move(track4, track4.getLength() - 3.1);
                            for (RideCar trainCar : rideCar.attachedTrain.getCars()) {
                                trainCar.setVelocity(0);
                                trainCar.setAcceleration(0);
                            }
                            waitingTicks = 0;
                        } else {
                            track3b.setTransportEnabled(false);
                        }
                    }
                }
            }
        });
        track4.setBlockSegmentAdvancing(true);
        track4.setOffsetFromNextSection(3);
        trackedRide.addPreTrainUpdateListener(new TrackedRide.PreTrainUpdateListener() {
            int waitingTicks = 0;

            @Override
            public void onCarPreUpdate(RideCar rideCar) {
                if (rideCar.getTrackSegment() == track4) {
                    if (rideCar.getDistance() <= CAR_LENGTH + 1) {
                        waitingTicks++;
                        if (waitingTicks > 20 * 1) {
                            track4.setTransportEnabled(true);
                            rideCar.attachedTrain.move(track5, rideCar.attachedTrain.getFrontCarDistance());
                            for (RideCar trainCar : rideCar.attachedTrain.getCars()) {
                                trainCar.setVelocity(0);
                                trainCar.setAcceleration(0);
                            }
                            waitingTicks = 0;
                        } else {
                            track4.setTransportEnabled(false);
                        }
                    }
                }
            }
        });

        station1.setNextTrackSegmentRetroActive(track1);
        trackedRide.addTrackSection(station1);

        track1.setNextTrackSegmentRetroActive(drop1);
        trackedRide.addTrackSection(track1);

        drop1.setNextTrackSegmentRetroActive(launch1);
        trackedRide.addTrackSection(drop1);

        launch1.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(launch1);

        track2.setNextTrackSegmentRetroActive(track3);
        trackedRide.addTrackSection(track2);

        track3.setNextTrackSegmentRetroActive(track3b);
        trackedRide.addTrackSection(track3);

        track3b.setNextTrackSegmentRetroActive(track4);
        trackedRide.addTrackSection(track3b);

        track4.setNextTrackSegmentRetroActive(track5);
        trackedRide.addTrackSection(track4);

        track5.setNextTrackSegmentRetroActive(track5b);
        trackedRide.addTrackSection(track5);

        track5b.setNextTrackSegmentRetroActive(track6);
        trackedRide.addTrackSection(track5b);

        track6.setNextTrackSegmentRetroActive(station1);
        trackedRide.addTrackSection(track6);
    }
}

