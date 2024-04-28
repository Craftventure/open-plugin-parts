package net.craftventure.core.ride.tracked;

import net.craftventure.audioserver.api.AudioServerApi;
import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.core.effect.ContinuousGeyser;
import net.craftventure.core.effect.EffectManager;
import net.craftventure.core.effect.RapidSnakeWaterEffect;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.car.RapidCar;
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.ride.trackedride.segment.TransportSegment;
import net.craftventure.core.ride.trackedride.segment.VerticalAutoLift;
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain;
import net.craftventure.core.script.ScriptManager;
import net.craftventure.core.utils.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import penner.easing.Quart;

import java.util.concurrent.TimeUnit;


public class Rapid {
    private static Rapid rapid;
    private static final int CAR_COUNT = 9;

    private Rapid(TrackedRide trackedRide) {

    }

    public static Rapid get() {
        if (rapid == null) {
            // 174.4, 42.7, -685.5
            ContinuousGeyser continuousGeyser = new ContinuousGeyser("rg1", , 3, 20 * 8, 1 * 20);
            continuousGeyser.play();
            ContinuousGeyser continuousGeyser2 = new ContinuousGeyser("rg2", , 6, 20 * 5, 3 * 20);
            continuousGeyser2.play();

            SimpleArea coasterArea = new SimpleArea("world", );
            final OperableCoasterTrackedRide trackedRide = new OperableCoasterTrackedRide("rapid", coasterArea, new Location(Bukkit.getWorld("world"), ),
                    "ride_rapid", "rapid");
            trackedRide.setOperatorArea(new SimpleArea("world", ));
//            addFenrirSections(trackedRide);
            initTrack(trackedRide);
            TrackSegment segment = trackedRide.getTrackSegments().get(0);//trackedRide.getTrackSegments().size() - 1);
            double distance = 0;//segment.getLength() - 70;
            for (int i = 0; i < CAR_COUNT; i++) {
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, distance);
                RapidCar rideCar = new RapidCar("rapid");//2, 2, 0.7, 1.1, 1.3);
//                rideCar.setMultiSeatRideCarOptions(rideCarOptions);
                rideTrain.addCar(rideCar);
                trackedRide.addTrain(rideTrain);

                distance -= 4.8;
                while (distance < 0) {
                    segment = segment.getPreviousTrackSegment();
                    distance += segment.getLength();
                }
            }

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            trackedRide.setPukeRate(0);
            rapid = new Rapid(trackedRide);
        }
        return rapid;
    }

    private static void initTrack(TrackedRide trackedRide) {//127.970009f, -108.724308f, 27.026110f
        final double SPEED = 9;
        final double MAX_SPEED = 25;
        final double TRACK_FRICTION = 0.985;
        final double TRACK_ACCELERATION = 0.06;
        final double ACCELERATE_FORCE = 1.3;
        final double BRAKE_FORCE = 1.3;
        final double SPACING = 1.4;
        Vector node = new Vector(0, 0, 0); // A point in blender
        Vector target = new Vector(-152, 44, -682); // A target of that same point in MC
        final Vector offset = new Vector(target.getX() - node.getX(), target.getY() - node.getY(), target.getZ() - node.getZ());

        StationSegment station = new StationSegment("station", trackedRide, SPEED, SPEED, 2.1);
        station.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station.setEjectType(TrackSegment.EjectType.EJECT_TO_SEAT);
        station.setDispatchIntervalTime(20, TimeUnit.SECONDS);
        station.setKeepRollingTime((int) ((200) / (double) CAR_COUNT), TimeUnit.SECONDS);
        station.setHoldDistance(4.12);
        station.setAutoDispatchTime(60, TimeUnit.SECONDS);
        station.add(offset,);
        station.setOnStationGateListener(open -> {
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(-152, 44, -677), open);
        });
        station.add(new TrackSegment.DistanceListener(4.5) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                if (rideCar == rideCar.attachedTrain.getCars().get(0)) {
                    rideCar.attachedTrain.setOnboardSynchronizedAudio("onride_rapid_part1", System.currentTimeMillis());
                }
            }
        });

        TransportSegment track = new TransportSegment("track",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track.setFriction(TRACK_FRICTION);
        track.setGravitationalInfluence(TRACK_ACCELERATION);
        track.blockSection(true).setOffsetFromNextSection(7);
        track.setBlockType(TrackSegment.BlockType.CONTINUOUS, SPACING, SPACING);
        track.add(new TrackSegment.DistanceListener(241.5) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                ScriptManager.stop("rapid", "watercurtain");
            }
        });
        track.add(new TrackSegment.DistanceListener(252) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                ScriptManager.start("rapid", "watercurtain");
            }
        });
        track.add(offset,);


        final RapidSnakeWaterEffect rapidSnakeWaterEffect = new RapidSnakeWaterEffect("rapid_snake_1");
        EffectManager.INSTANCE.add(rapidSnakeWaterEffect);

        track.add(new TrackSegment.DistanceListener(83) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
//                Logger.consoleAndIngame("Car hit distance " + getTargetDistance());
                rapidSnakeWaterEffect.play();
            }
        });

        VerticalAutoLift lift = new VerticalAutoLift("lift",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE),
                Quart::easeInOut);
        lift.blockSection(true);
        lift.setLiftListener(new RapidLiftController());
        lift.add(offset,);

        TransportSegment track2 = new TransportSegment("track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(ACCELERATE_FORCE),
                CoasterMathUtils.kmhToBpt(MAX_SPEED),
                CoasterMathUtils.kmhToBpt(BRAKE_FORCE));
        track2.setFriction(TRACK_FRICTION);
        track2.setGravitationalInfluence(TRACK_ACCELERATION);
        track2.blockSection(true);
        track2.setOffsetFromNextSection(5);
        track2.setBlockType(TrackSegment.BlockType.CONTINUOUS, SPACING, SPACING);
        track2.add(offset,);

        station.setNextTrackSegmentRetroActive(track);
        trackedRide.addTrackSection(station);

        track.setNextTrackSegmentRetroActive(lift);
        trackedRide.addTrackSection(track);

        lift.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(lift);

        track2.setNextTrackSegmentRetroActive(station);
        trackedRide.addTrackSection(track2);
    }

    private static class RapidLiftController implements VerticalAutoLift.LiftListener {
        private ArmorStand armorStand;
        private final Vector basePosition = new Vector(-241, 33 - 0.7, -631);
        private double offset = -1;

        private ArmorStand spawnArmorStand() {
            World world = Bukkit.getWorld("world");
            SimpleArea area = new SimpleArea(new Location(world, -244, 29, -634), new Location(world, -239, 46, -629));
            for (Entity entity : world.getEntities()) {
                if (area.isInArea(entity.getLocation())) {
                    if ("liftcar".equals(entity.getCustomName())) {
                        entity.remove();
                    }
                }
            }
            ArmorStand armorStand = world.spawn(new Location(world, basePosition.getX(), basePosition.getY(), basePosition.getZ()), ArmorStand.class);
            armorStand.setPersistent(false);
            armorStand.setHelmet(MaterialConfig.INSTANCE.getRAPID_LIFT().clone());
            armorStand.setCustomName("liftcar");
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            return armorStand;
        }

        @Override
        public void onUpdate(@NotNull VerticalAutoLift segment, double offset) {
            if (this.offset != offset) {
                this.offset = offset;
                if (armorStand == null || !armorStand.isValid()) {
                    armorStand = spawnArmorStand();
                }
                EntityUtils.INSTANCE.teleport(armorStand, basePosition.getX(), basePosition.getY() + offset, basePosition.getZ());
            }
        }

        @Override
        public void onGoingUp(@NotNull VerticalAutoLift segment, @NotNull RideTrain rideTrain) {
            long now = System.currentTimeMillis();
            for (Player player : rideTrain.getPassengers()) {
                AudioServerApi.INSTANCE.addAndSync("onride_rapid_part2", player, now);//now - 5000);
                AudioServerApi.INSTANCE.remove("onride_rapid_part1", player);
            }
        }

        @Override
        public void onGoingDown(@NotNull VerticalAutoLift segment) {

        }

        @Override
        public void onStateChanged(@NotNull VerticalAutoLift segment, @NotNull VerticalAutoLift.LiftState newState) {

        }
    }
}

