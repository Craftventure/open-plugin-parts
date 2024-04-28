package net.craftventure.core.ride.tracked;

import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar;
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat;
import net.craftventure.core.ride.trackedride.ride.CoasterTrackedRide;
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.ride.trackedride.segment.TransportSegment;
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain;
import net.craftventure.core.utils.ParticleSpawnerKt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.concurrent.TimeUnit;


public class SeaSouls {
    private static SeaSouls seasouls;
    private static final double CAR_LENGTH = 5.9;
    private static final double Y_OFFSET = -1.63;
    private static final double FORWARD_OFFSET = -0.5;
    private static final double SIDE_OFFSET = 0.7;
    private static final int CAR_COUNT = 5;

    private SeaSouls(TrackedRide trackedRide) {

    }

    public static SeaSouls get() {
        if (seasouls == null) {
            SimpleArea coasterArea = new SimpleArea("world", );
            final CoasterTrackedRide trackedRide = new CoasterTrackedRide("seasouls", coasterArea, new Location(Bukkit.getWorld("world"), ),
                    "ride_seasouls", "seasouls");
            initTrack(trackedRide);

            TrackSegment segment = trackedRide.getTrackSegments().get(0);//trackedRide.getTrackSegments().size() - 1);
            TrackSegment beginSegment = segment;
            double distance = 0;//segment.getLength() - 70;

            final double splashThressHold = CoasterMathUtils.kmhToBpt(11);
            for (int t = 0; t < CAR_COUNT; t++) {
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, distance);//(2.3 * 5) + 1.2);

                DynamicSeatedRideCar dynamicSeatedRideCar = new DynamicSeatedRideCar("seasouls", CAR_LENGTH);
                dynamicSeatedRideCar.carFrontBogieDistance = 0;
                dynamicSeatedRideCar.carRearBogieDistance = -CAR_LENGTH;

//                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(0, Y_OFFSET + 2, 0, true, "seasouls")
//                        .setModel(new ItemStack(Material.COBBLE_WALL)));
//                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(0, Y_OFFSET + 2, -CAR_LENGTH, true, "seasouls")
//                        .setModel(new ItemStack(Material.COAL_BLOCK)));

                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(0, Y_OFFSET + 0.7 + 1.5, FORWARD_OFFSET + -1.5, false, "seasouls")
                        .setModel(MaterialConfig.INSTANCE.getSEASOULS_BOAT()));

                // row 1
                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(SIDE_OFFSET, Y_OFFSET - 0.1 + 1.6, FORWARD_OFFSET + -1.5, true, "seasouls"));
//                        .setModel(new ItemStack(Material.STONE)));
                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(-SIDE_OFFSET, Y_OFFSET - 0.1 + 1.6, FORWARD_OFFSET + -1.5, true, "seasouls"));
//                        .setModel(new ItemStack(Material.STONE)));

                // row 2
                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(SIDE_OFFSET, Y_OFFSET + 0 + 1.6, FORWARD_OFFSET + -2.9, true, "seasouls"));
//                        .setModel(new ItemStack(Material.STONE)));
                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(-SIDE_OFFSET, Y_OFFSET + 0 + 1.6, FORWARD_OFFSET + -2.9, true, "seasouls"));
//                        .setModel(new ItemStack(Material.STONE)));

                // row 3
                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(SIDE_OFFSET, Y_OFFSET + 0.1 + 1.6, FORWARD_OFFSET + -4.3, true, "seasouls"));
//                        .setModel(new ItemStack(Material.STONE)));
                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(-SIDE_OFFSET, Y_OFFSET + 0.1 + 1.6, FORWARD_OFFSET + -4.3, true, "seasouls"));
//                        .setModel(new ItemStack(Material.STONE)));

                dynamicSeatedRideCar.setMoveListener(new DynamicSeatedRideCar.MoveListener() {
                    boolean previousTick = false;

                    @Override
                    public void onMove(Vector location, double trackYawRadian, double trackPitchRadian, double banking, DynamicSeatedRideCar multiSeatRideCar1) {
                        if (multiSeatRideCar1.getVelocity() > splashThressHold) {
                            if (previousTick) {
                                double particleCount = 10 + ((multiSeatRideCar1.getVelocity() - splashThressHold) * 500);
                                double yOffset = 0.2;
                                if (!(multiSeatRideCar1.getTrackSegment() instanceof TransportSegment)) {
                                    particleCount *= 0.03;
                                } else {
                                    yOffset += (multiSeatRideCar1.getVelocity() - splashThressHold) * 0.2;
                                }
                                ParticleSpawnerKt.spawnParticleX(coasterArea.getWorld(), Particle.WATER_DROP,
                                        location.getX(), location.getY() + 3, location.getZ(),
                                        (int) particleCount,
                                        0.4 + ((multiSeatRideCar1.getVelocity() - splashThressHold) * 2), yOffset, 0.4 + ((multiSeatRideCar1.getVelocity() - splashThressHold) * 2),
                                        0x0000FF);
//                                armorStand.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
//                                        armorStand.getLocation().getX(), armorStand.getLocation().getY() + 3, armorStand.getLocation().getZ(),
//                                        (int) (particleCount * 0.05),
//                                        0.4 + ((multiSeatRideCar1.getVelocity() - splashThressHold) * 2), yOffset, 0.4 + ((multiSeatRideCar1.getVelocity() - splashThressHold) * 2));
                            }
                            previousTick = !previousTick;
                        }
                    }
                });

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
                }
            }

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            trackedRide.setPukeRate(0);
            seasouls = new SeaSouls(trackedRide);
        }
        return seasouls;
    }

    private static void initTrack(final TrackedRide trackedRide) {
        final double SPEED = 6;
        final double ACCELERATE_FORCE = 1.8;
        final double MAX_SPEED = 55;
        final double BRAKE_FORCE = 1.8;

        final double LIFT_SPEED = 9;
        final double TRACK_FRICTION = 0.972;
        final double TRACK_FRICTION_DROP = 0.9925;

        final Vector offset = new Vector(0, 0, 0);//-0.6, 0);

        StationSegment station = new StationSegment("station", trackedRide, 6, 6, 2.1);
        station.setSlowBrakingDistance(3.0);
        station.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
//        station.setLeaveMode(TrackSegment.LeaveMode.EXIT_AT_SEAT_WHEN_HALTED);
//        station1.setDebugInstantStart(true);
        station.setHoldDistance(5);
        station.setDispatchIntervalTime((long) (((60 * 5) + 35) / (double) CAR_COUNT), TimeUnit.SECONDS);
        station.setKeepRollingTime((long) (((60 * 5) + 35) / (double) CAR_COUNT), TimeUnit.SECONDS);
        station.add(offset,);

        station.setOnStationGateListener(open -> {
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(-211, 38, -461), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(-211, 38, -459), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(-211, 38, -457), open);
        });
        station.setOnStationStateChangeListener((newState, oldState) -> {
            if (newState == StationSegment.StationState.DISPATCHING) {
                RideTrain rideTrain = station.getAnyRideTrainOnSegment();
                if (rideTrain != null) {
                    rideTrain.setOnboardSynchronizedAudio("onride_seasouls", System.currentTimeMillis());
                }
            }
        });

        TransportSegment track1 = new TransportSegment("track1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track1.blockSection(true);
        track1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track1.setFriction(TRACK_FRICTION);
        track1.add(offset,);

        SplinedTrackSegment drop = new SplinedTrackSegment("drop", trackedRide);
        drop.setFriction(TRACK_FRICTION_DROP);
        drop.blockSection(true);
        drop.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        drop.add(offset,);

        TransportSegment track2 = new TransportSegment("track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track2.blockSection(true);
        track2.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track2.setFriction(TRACK_FRICTION);
        track2.add(offset,);

        TransportSegment lift1 = new TransportSegment("lift1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(LIFT_SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        lift1.setFriction(TRACK_FRICTION);
        lift1.blockSection(true);
        lift1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        lift1.add(offset,);

        TransportSegment track3 = new TransportSegment("track3",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track3.blockSection(true);
        track3.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track3.setFriction(TRACK_FRICTION);
        track3.add(offset,);

        station.setNextTrackSegmentRetroActive(track1);
        trackedRide.addTrackSection(station);

        track1.setNextTrackSegmentRetroActive(drop);
        trackedRide.addTrackSection(track1);

        drop.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(drop);

        track2.setNextTrackSegmentRetroActive(lift1);
        trackedRide.addTrackSection(track2);

        lift1.setNextTrackSegmentRetroActive(track3);
        trackedRide.addTrackSection(lift1);

        track3.setNextTrackSegmentRetroActive(station);
        trackedRide.addTrackSection(track3);
    }
}
