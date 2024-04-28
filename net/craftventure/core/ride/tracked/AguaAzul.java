package net.craftventure.core.ride.tracked;

import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.bukkit.ktx.util.SoundUtils;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.feature.maxifoto.MaxiFoto;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar;
import net.craftventure.core.ride.trackedride.car.effect.Effect;
import net.craftventure.core.ride.trackedride.car.effect.FloatingMovementHandler;
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat;
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide;
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.ride.trackedride.segment.TransportSegment;
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain;
import net.craftventure.core.utils.ParticleSpawnerKt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;


public class AguaAzul {
    private static AguaAzul aguaAzul;
    private static final int CAR_COUNT = 7;
    private static final double CAR_LENGTH = 3.2;

    private AguaAzul(TrackedRide trackedRide) {
    }

    public static AguaAzul get() {
        if (aguaAzul == null) {

            SimpleArea coasterArea = new SimpleArea("world", );
            final OperableCoasterTrackedRide trackedRide = new OperableCoasterTrackedRide("aguaazul", coasterArea, new Location(Bukkit.getWorld("world"), ),
                    "ride_agua_azul", "aguaazul");
            trackedRide.setOperatorArea(new SimpleArea("world", ));
            initTrack(trackedRide);

            TrackSegment segment = trackedRide.getTrackSegments().get(0);//trackedRide.getTrackSegments().size() - 1);
            TrackSegment beginSegment = segment;
            double distance = 0;//segment.getLength() - 70;
            final double splashThressHold = CoasterMathUtils.kmhToBpt(17);
            TrackSegment splashSegment = trackedRide.getSegmentById("splash");
            for (int j = 0; j < CAR_COUNT; j++) {
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, distance);
                rideTrain.setMovementHandler(new FloatingMovementHandler(0,
                        0.001 + (CraftventureCore.getRandom().nextDouble() * 0.001),
                        0.03 + (CraftventureCore.getRandom().nextDouble() * 0.005),
                        CraftventureCore.getRandom().nextInt(10000), rideCar -> {
                    TrackSegment trackSegment = rideCar.getTrackSegment();
                    if (trackSegment instanceof StationSegment)
                        return false;
                    if (trackSegment == splashSegment && rideCar.getDistance() > 70.0) return false;
                    return !trackSegment.getId().startsWith("lift");
                }));

                DynamicSeatedRideCar rideCar = DynamicSeatedRideCar.fromLegacyFormat("aguaazul", 1, 3, 0.7, 0.85, CAR_LENGTH);
                ((ArmorStandSeat) rideCar.getSeat(0)).setModel(MaterialConfig.INSTANCE.getLOGFLUME());
                rideCar.carFrontBogieDistance = 0.8;
                rideCar.carRearBogieDistance = 0.8 - CAR_LENGTH;
                rideTrain.addCar(rideCar);

                rideCar.addEffect(new Effect(0, 1.5, 0) {
                    boolean previousTick = false;

                    @Override
                    public void move(double x, double y, double z, double trackYawRadian, double trackPitchRadian, double bankingDegree, RideCar rideCar) {
                        if (rideCar.getVelocity() > splashThressHold) {
                            if (previousTick) {
                                double particleCount = 10 + ((rideCar.getVelocity() - splashThressHold) * 500);
                                double yOffset = 0.2;
                                if (!(rideCar.getTrackSegment() instanceof TransportSegment)) {
                                    particleCount *= 0.03;
                                } else {
                                    yOffset += (rideCar.getVelocity() - splashThressHold) * 0.2;
                                }

                                ParticleSpawnerKt.spawnParticleX(coasterArea.getWorld(), Particle.WATER_DROP,
                                        x, y, z,
                                        (int) particleCount,
                                        (float) (0.4 + ((rideCar.getVelocity() - splashThressHold) * 2)),
                                        yOffset,
                                        (float) (0.4 + ((rideCar.getVelocity() - splashThressHold) * 2)),
                                        0x0000FF);
                            }
                            previousTick = !previousTick;
                        }
                    }
                });

                trackedRide.addTrain(rideTrain);

                distance -= CAR_LENGTH;
                if (segment == beginSegment) {
                    segment = segment.getPreviousTrackSegment();
                    distance = segment.getLength() - (CAR_LENGTH / 2.0);
                }
                while (distance < 0) {
                    segment = segment.getPreviousTrackSegment();
                    distance += segment.getLength();
                }
            }

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            aguaAzul = new AguaAzul(trackedRide);
        }
        return aguaAzul;
    }

    private static void initTrack(TrackedRide trackedRide) {//127.970009f, -108.724308f, 27.026110f
        final double SPEED = 6;
        final double LIFT_SPEED = 16;
        final double MAX_SPEED = 55;
        final double TRACK_FRICTION = 0.972;
        final double TRACK_FRICTION_DROP = 0.9925;
        final double ACCELERATE_FORCE = 1.3;
        final double BRAKE_FORCE = 1.3;
        Vector node = new Vector(0, 0, 0); // A point in blender
        Vector target = new Vector(330, 35 - 0.5, -662); // A target of that same point in MC
        final Vector offset = new Vector(target.getX() - node.getX(), target.getY() - node.getY(), target.getZ() - node.getZ());

        TransportSegment.Acceleration acceleration = new TransportSegment.Acceleration(2, 4, CoasterMathUtils.kmhToBpt(LIFT_SPEED), true);

        TransportSegment splash = new TransportSegment("splash",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        splash.add(new TrackSegment.DistanceListener(1) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                Location soundLocation = rideCar.getLocation().toLocation(trackedRide.getArea().getWorld());
                soundLocation.getWorld().playSound(soundLocation, SoundUtils.INSTANCE.getSPLASH(), 1.5f, 1f);
            }
        });
        splash.setFriction(TRACK_FRICTION);
        splash.blockSection(true);
        splash.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        splash.add(offset,);
        splash.add(new TrackSegment.DistanceListener(85) {
            int car = 0;

            @Override
            public void onTargetHit(@NotNull RideCar triggeringCar) {

                Player[] names = new Player[3];
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

                MaxiFoto.RenderSettings renderSettings = new MaxiFoto.RenderSettings("aguaazul", names);
                if (!onlyNulls) {
                    renderSettings.setOffset(car);
                    MaxiFoto.INSTANCE.render(renderSettings);
                    car++;
                    if (car >= 4) {
                        car = 0;
                    }
                }

                triggeringCar.attachedTrain.eject();
            }
        });


        SplinedTrackSegment drop = new SplinedTrackSegment("drop", trackedRide);
        drop.setFriction(TRACK_FRICTION_DROP);
        drop.blockSection(true);
        drop.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        drop.add(offset,);

        TransportSegment track6 = new TransportSegment("track6",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track6.setFriction(TRACK_FRICTION);
        track6.blockSection(true);
        track6.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, CAR_LENGTH);
        track6.add(offset,);

        TransportSegment lift5 = new TransportSegment("lift5",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        lift5.setAcceleration(acceleration);
        lift5.setFriction(TRACK_FRICTION);
        lift5.blockSection(true);
        lift5.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        lift5.add(offset,);

        TransportSegment track5 = new TransportSegment("track5",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track5.setFriction(TRACK_FRICTION);
        track5.blockSection(true);
        track5.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track5.add(offset,);

        TransportSegment lift4 = new TransportSegment("lift4",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        lift4.setAcceleration(acceleration);
        lift4.setFriction(TRACK_FRICTION);
        lift4.blockSection(true);
        lift4.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        lift4.add(offset,);

        TransportSegment track4 = new TransportSegment("track4",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track4.setFriction(TRACK_FRICTION);
        track4.blockSection(true);
        track4.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track4.add(offset,);

        TransportSegment lift3 = new TransportSegment("lift3",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        lift3.setAcceleration(acceleration);
        lift3.setFriction(TRACK_FRICTION);
        lift3.blockSection(true);
        lift3.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        lift3.add(offset,);

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

        TransportSegment lift2 = new TransportSegment("lift2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        lift2.setAcceleration(acceleration);
        lift2.setFriction(TRACK_FRICTION);
        lift2.blockSection(true);
        lift2.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        lift2.add(offset,);

        TransportSegment track2 = new TransportSegment("track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track2.setFriction(TRACK_FRICTION);
        track2.blockSection(true);
        track2.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track2.add(offset,);

        TransportSegment lift1 = new TransportSegment("lift1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
//        lift1.setSpacingTicks(20 * 2);
        lift1.setAcceleration(acceleration);
        lift1.setFriction(TRACK_FRICTION);
        lift1.blockSection(true);
        lift1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        lift1.add(offset,);

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

        StationSegment station = new StationSegment("station", trackedRide, SPEED, SPEED, 2.1);
        station.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station.setDispatchIntervalTime(29, TimeUnit.SECONDS);
        station.setKeepRollingTime((long) (170 / (double) CAR_COUNT), TimeUnit.SECONDS);
        station.setHoldDistance(2.75);
        station.setAutoDispatchTime(60, TimeUnit.SECONDS);
        station.setOnStationGateListener(open -> {
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(291, 43, -683), open);
        });
//        station.setMinimalWaitingTimeInMillis(29000);
//        station.setDebugInstantStart(true);
//        station.setStationDelayInTicks(20 * 3);//26 * 20);
//        station.setAutomaticLeaveTimeTicks((int) ((20 * 170) / (double) CAR_COUNT));
        station.add(offset,);

        station.setOnStationStateChangeListener((newState, oldState) -> {
            if (newState == StationSegment.StationState.DISPATCHING) {
                station.getAnyRideTrainOnSegment().setOnboardSynchronizedAudio("onride_agua_azul", System.currentTimeMillis());
            }
        });


//        station.add(new TrackSegment.DistanceListener(4.5) {
//            @Override
//            public void onTargetHit(RideCar rideCar) {
//                if (rideCar == rideCar.getAttachedTrain().getCars().get(0)) {
//                    long now = System.currentTimeMillis();
//                    for (Player player : rideCar.getAttachedTrain().getPassengers()) {
//                        AudioServerApi.addAndSync("onride_rapid_part1", player, now);//now - 5000);
//                    }
//                }
//            }
//        });


        station.setNextTrackSegmentRetroActive(track1);
        trackedRide.addTrackSection(station);
//        station.setNextTrackSegment(track6);

        track1.setNextTrackSegmentRetroActive(lift1);
        trackedRide.addTrackSection(track1);

        lift1.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(lift1);

        track2.setNextTrackSegmentRetroActive(lift2);
        trackedRide.addTrackSection(track2);

        lift2.setNextTrackSegmentRetroActive(track3);
        trackedRide.addTrackSection(lift2);

        track3.setNextTrackSegmentRetroActive(lift3);
        trackedRide.addTrackSection(track3);

        lift3.setNextTrackSegmentRetroActive(track4);
        trackedRide.addTrackSection(lift3);

        track4.setNextTrackSegmentRetroActive(lift4);
        trackedRide.addTrackSection(track4);

        lift4.setNextTrackSegmentRetroActive(track5);
        trackedRide.addTrackSection(lift4);

        track5.setNextTrackSegmentRetroActive(lift5);
        trackedRide.addTrackSection(track5);

        lift5.setNextTrackSegmentRetroActive(track6);
        trackedRide.addTrackSection(lift5);

        track6.setNextTrackSegmentRetroActive(drop);
        trackedRide.addTrackSection(track6);

        drop.setNextTrackSegmentRetroActive(splash);
        trackedRide.addTrackSection(drop);

        splash.setNextTrackSegmentRetroActive(station);
        trackedRide.addTrackSection(splash);
    }
}

