package net.craftventure.core.ride.tracked;

import com.google.common.collect.Sets;
import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.core.feature.maxifoto.MaxiFoto;
import net.craftventure.core.ktx.util.DateUtils;
import net.craftventure.core.ktx.util.Permissions;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcAreaTracker;
import net.craftventure.core.npc.tracker.NpcEntityTracker;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar;
import net.craftventure.core.ride.trackedride.car.effect.Effect;
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat;
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide;
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.ride.trackedride.segment.TransportSegment;
import net.craftventure.core.ride.trackedride.segment.VerticalAutoLift;
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain;
import net.craftventure.core.utils.ParticleSpawnerKt;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.repository.AchievementProgressRepository;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import penner.easing.Quart;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class CreeperCanyonRailroad {
    private static CreeperCanyonRailroad creeperCanyonRailroad;

    private CreeperCanyonRailroad(TrackedRide trackedRide) {

    }

    public static CreeperCanyonRailroad get() {
        if (creeperCanyonRailroad == null) {
            SimpleArea coasterArea = new SimpleArea("world", );
            final OperableCoasterTrackedRide trackedRide = new OperableCoasterTrackedRide("ccr", coasterArea, new Location(Bukkit.getWorld("world"), ),
                    "ride_ccr", "ccr");
            trackedRide.setOperatorArea(new SimpleArea("world", ));
            trackedRide.setFixExitLocation(true);
            initTrack(trackedRide);

            final TrackSegment splashSegment = trackedRide.getSegmentById("track3");
            for (int t = 0; t < 2; t++) {
                TrackSegment segment = t == 0 ? trackedRide.getTrackSegments().get(0) :
                        trackedRide.getTrackSegments().get(trackedRide.getTrackSegments().size() - 1 - t);
                // Create a train
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, 0);//(2.3 * 5) + 1.2);
                rideTrain.setTrainSoundName("ccr",
                        new SpatialTrainSounds.Settings(
                                Sets.newHashSet(
                                        TrackSegment.TrackType.FRICTION_BRAKE,
                                        TrackSegment.TrackType.CHAIN_LIFT,
                                        TrackSegment.TrackType.WHEEL_TRANSPORT
                                )
                        ));

                DynamicSeatedRideCar frontCar = new DynamicSeatedRideCar("ccr", 3.5);
//                frontCar.setCarFrontBogieDistance(1.0);
                frontCar.carRearBogieDistance = -2.0;
                ArmorStandSeat frontCarModel = new ArmorStandSeat(-0.4, 0.5, 0, false, "ccr");
                frontCarModel.setModel(MaterialConfig.INSTANCE.getCCR_LOCO());
                frontCar.addSeat(frontCarModel);

                ArmorStandSeat frontCarSeat = new ArmorStandSeat(0, 0.25, -2.0, true, "ccr");
                frontCarSeat.setPermission(Permissions.INSTANCE.getVIP());
//                frontCarSeat.setEnableFollowCam(true);
                frontCar.addSeat(frontCarSeat);

                frontCar.addEffect(new Effect(0, 2, 0.6) {
                    int frame = 0;
                    final World world = Bukkit.getWorld("world");

                    @Override
                    public void move(double x, double y, double z, double trackYawRadian, double trackPitchRadian, double bankingDegree, RideCar rideCar) {
                        frame++;
                        if (rideCar.attachedTrain.getVelocity() == 0) {
                            if (frame > 10) {
                                frame = 0;
                                ParticleSpawnerKt.spawnParticleX(world, Particle.SMOKE_LARGE, x, y, z);
                            }
                        } else {
                            ParticleSpawnerKt.spawnParticleX(world, Particle.SMOKE_LARGE, x, y, z);
                        }
                    }
                });
                rideTrain.addCar(frontCar);

                for (int i = 0; i < 4; i++) {
                    DynamicSeatedRideCar rideCar = DynamicSeatedRideCar.fromLegacyFormat("ccr", 2, 3, 0.8, 0.9, 3.5, 0.39);
                    ArmorStandSeat seat = ((ArmorStandSeat) rideCar.getSeat(0));
                    seat.setModel(MaterialConfig.INSTANCE.getCCR_CAR());
                    if (i == 1) rideCar.setHasTrainSound(true);
//                    multiSeatRideCar.setMultiSeatRideCarOptions(rideCarOptions);
                    rideCar.carFrontBogieDistance = 0.5;
                    rideCar.carRearBogieDistance = -2.0;
                    rideCar.addEffect(new Effect(0.0, 1.5, 0.0) {
                        private boolean previousTick = false;

                        @Override
                        public void move(double x, double y, double z, double trackYawRadian, double trackPitchRadian, double bankingDegree, RideCar rideCar) {
                            if (rideCar.getTrackSegment() == splashSegment && rideCar.getDistance() > 212 && rideCar.getDistance() < 223) {
                                if (previousTick) {
                                    ParticleSpawnerKt.spawnParticleX(coasterArea.getWorld(), Particle.WATER_DROP,
                                            x, y, z,
                                            30,
                                            0.7, 0.7, 0.7,
                                            0x0000FF);
                                }
                                previousTick = !previousTick;
                            }
                        }
                    });
                    rideTrain.addCar(rideCar);
                }
                trackedRide.addTrain(rideTrain);
            }

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            trackedRide.setPukeRate(0);
            trackedRide.addOnRideCompletionListener((player, rideCar) -> {
                if (DateUtils.INSTANCE.isCoasterDay()) {
                    AchievementProgressRepository database = MainRepositoryProvider.INSTANCE.getAchievementProgressRepository();
                    database.reward(player.getUniqueId(), "coaster_day");
                    database.reward(player.getUniqueId(), "coaster_day_" + LocalDateTime.now().getYear());
                }
            });
            creeperCanyonRailroad = new CreeperCanyonRailroad(trackedRide);
        }
        return creeperCanyonRailroad;
    }

    private static void initTrack(final TrackedRide trackedRide) {
        final Vector offset = new Vector(0, 0, 0);

        StationSegment station = new StationSegment("station", trackedRide, 12, 14, 2.1);
        station.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station.setEjectType(TrackSegment.EjectType.EJECT_TO_SEAT);
        station.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        station.setAutoDispatchTime(75, TimeUnit.SECONDS);
        station.setHoldDistance(17.66);
        station.setOnStationGateListener(open -> {
        });
        station.setOnStationStateChangeListener((newState, oldState) -> {
            if (newState == StationSegment.StationState.DISPATCHING) {
                RideTrain rideTrain = station.getAnyRideTrainOnSegment();
                if (rideTrain != null) {
                    rideTrain.setOnboardSynchronizedAudio("ccr_onride", System.currentTimeMillis());
                }
            }
        });
        station.add(offset,);
        station.setDispatchIntervalTime(50, TimeUnit.SECONDS);


        SplinedTrackSegment track1 = new SplinedTrackSegment("track1", trackedRide);
        track1.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        track1.add(offset,);


        TransportSegment lift1 = new TransportSegment("lift1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(1.8));
        lift1.setTrackType(TrackSegment.TrackType.CHAIN_LIFT);
        lift1.blockSection(true);
        lift1.add(offset,);


        TransportSegment track2 = new TransportSegment("track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120),
                CoasterMathUtils.kmhToBpt(1.8));
        track2.add(offset,);

        VerticalAutoLift drop1 = new VerticalAutoLift("drop1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(1.8),
                Quart::easeInOut);
        drop1.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        drop1.blockSection(true);
        drop1.setLeavingSpeeds(CoasterMathUtils.kmhToBpt(30),
                CoasterMathUtils.kmhToBpt(30),
                CoasterMathUtils.kmhToBpt(1.5),
                CoasterMathUtils.kmhToBpt(1.5));
        drop1.setLiftListener(new DropController());
        drop1.setLiftDuration(20 * 3);
        drop1.setWaitBottomExitDuration((long) (5.5 * 20));
        drop1.setTriggerDistanceFromEnd(1);
        drop1.blockSection(true);
        drop1.add(offset,);

        TransportSegment track3 = new TransportSegment("track3",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120),
                CoasterMathUtils.kmhToBpt(1.8));
        track3.add(offset,);

        TransportSegment block1 = new TransportSegment("block1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(8),
                CoasterMathUtils.kmhToBpt(2.2));
        block1.setTrackType(TrackSegment.TrackType.FRICTION_BRAKE);
        block1.blockSection(true);
        block1.add(offset,);

        TransportSegment track4 = new TransportSegment("track4",
                trackedRide,
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(120),
                CoasterMathUtils.kmhToBpt(1.8));
        track4.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        track4.add(offset,);

        track3.add(new TrackSegment.DistanceListener(290, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                if (trackedRide.getPassengerCount() > 0) {
                    station.tryDispatchNow(StationSegment.DispatchRequestType.TRAIN_WITH_PLAYERS);
                }
            }
        });


        track4.add(new TrackSegment.DistanceListener(20, true) {
            private boolean firstSlot;

            @Override
            public void onTargetHit(@NotNull RideCar triggeredCar) {
                Player[] names = new Player[24];
                for (int i = 0; i < names.length; i++)
                    names[i] = null;

                int index = 0;
                boolean first = true;
                for (RideCar rideCar : triggeredCar.attachedTrain.getCars()) {
                    if (first) {
                        first = false;
                        continue;
                    }
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

                MaxiFoto.RenderSettings renderSettings = new MaxiFoto.RenderSettings("ccr", names);
                if (!onlyNulls) {
                    if (!firstSlot)
                        renderSettings.setOffset(4);
                    MaxiFoto.INSTANCE.render(renderSettings);
                    firstSlot = !firstSlot;
                }
            }
        });

        station.setNextTrackSegmentRetroActive(track1);
        trackedRide.addTrackSection(station);

        track1.setNextTrackSegmentRetroActive(lift1);
        trackedRide.addTrackSection(track1);

        lift1.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(lift1);

        track2.setNextTrackSegmentRetroActive(drop1);
        trackedRide.addTrackSection(track2);

        drop1.setNextTrackSegmentRetroActive(track3);
        trackedRide.addTrackSection(drop1);

        track3.setNextTrackSegmentRetroActive(block1);
        trackedRide.addTrackSection(track3);

        block1.setNextTrackSegmentRetroActive(track4);
        trackedRide.addTrackSection(block1);

        track4.setNextTrackSegmentRetroActive(station);
        trackedRide.addTrackSection(track4);

//        station.setNextTrackSegment(drop1);
//        station.setDebugInstantStart(true);
    }

    protected static class DropPlatform {
        private final List<Location> locations = new ArrayList<>();
        private final List<NpcEntity> npcEntities = new ArrayList<>();
        private final NpcEntityTracker npcEntityTracker;
        private final BlockData blockData;

        public DropPlatform(NpcEntityTracker npcEntityTracker, SimpleArea area, BlockData blockData) {
            this.blockData = blockData;
            this.npcEntityTracker = npcEntityTracker;

            for (int x = (int) area.getLoc1().getX(); x <= (int) area.getLoc2().getX(); x++) {
                for (int y = (int) area.getLoc1().getY(); y <= (int) area.getLoc2().getY(); y++) {
                    for (int z = (int) area.getLoc1().getZ(); z <= (int) area.getLoc2().getZ(); z++) {
                        Block block = new Location(area.getLoc1().getWorld(), x, y, z).getBlock();
//                        Logger.console(block.getType().name() + " > " + block.getData());
                        if (block.getType() == blockData.getMaterial() || block.getType() == Material.AIR) {
                            locations.add(block.getLocation().add(0.5, 0, 0.5));
                        }
                    }
                }
            }
        }

        public void showBlocks(boolean show, double yOffset) {
            for (Location location : locations) {
                if (!show) {
                    location.clone().add(0, yOffset, 0).getBlock().setType(Material.AIR);
                } else {
                    location.clone().add(0, yOffset, 0).getBlock().setBlockData(blockData);
                }
            }
        }

        public void moveBlocks(double yOffset) {
//            Logger.console("T " + t + " > " + npcEntities.size());
            for (NpcEntity npcEntity : npcEntities) {
                Location location = (Location) npcEntity.getTag();
                npcEntity.move(
                        location.getX(),
                        location.getY() + yOffset,
                        location.getZ());
            }
        }

        public void spawn(double yOffset) {
            if (npcEntities.size() == 0) {
                for (Location location : locations) {
                    NpcEntity npcEntity = new NpcEntity("ccrDropTrack", EntityType.FALLING_BLOCK, location.clone().add(
                            0,
                            yOffset,
                            0
                    ));
                    npcEntity.noGravity(true);
                    npcEntity.setBlockData(blockData);
                    npcEntities.add(npcEntity);
                    npcEntityTracker.addEntity(npcEntity);
                    npcEntity.setTag(location.clone());
                }
            }
        }
    }

    private static class DropController implements VerticalAutoLift.LiftListener {
        private final NpcAreaTracker areaTracker = new NpcAreaTracker(new SimpleArea("world", 238, 42, -422, 300, 63, -414));
        private final List<DropPlatform> platforms = new ArrayList<>();
        private boolean hasSpawned = false;
        private double offset = 0;

        public DropController() {
            platforms.add(new DropPlatform(areaTracker,
                    new SimpleArea("world", 249, 58, -418, 266, 58, -418),
                    Material.OAK_SLAB.createBlockData()));
            platforms.add(new DropPlatform(areaTracker,
                    new SimpleArea("world", 249, 58, -417, 266, 58, -417),
                    Material.BROWN_TERRACOTTA.createBlockData()));
            platforms.add(new DropPlatform(areaTracker,
                    new SimpleArea("world", 249, 58, -416, 266, 58, -416),
                    Material.OAK_SLAB.createBlockData()));

            for (DropPlatform dropPlatform : platforms) {
                dropPlatform.showBlocks(true, 0);
                dropPlatform.showBlocks(false, -7);
            }
        }

        @Override
        public void onUpdate(@NotNull VerticalAutoLift segment, double offset) {
            if (hasSpawned) {
                this.offset = offset;
                for (int i = 0; i < platforms.size(); i++) {
                    DropPlatform dropPlatform = platforms.get(i);
                    dropPlatform.moveBlocks(offset);
                }
            }
        }

        @Override
        public void onGoingUp(@NotNull VerticalAutoLift segment, @NotNull RideTrain rideTrain) {
        }

        @Override
        public void onGoingDown(@NotNull VerticalAutoLift segment) {

        }

        @Override
        public void onStateChanged(@NotNull VerticalAutoLift segment, @NotNull VerticalAutoLift.LiftState newState) {
//            Logger.console("New state " + newState.name());
            if (newState == VerticalAutoLift.LiftState.LIFTING || newState == VerticalAutoLift.LiftState.DOWNING) {
                if (!hasSpawned) {
//                    Logger.console("Spawning " + newState.name() + " > " + offset);
                    areaTracker.startTracking();
                    for (DropPlatform dropPlatform : platforms) {
                        dropPlatform.showBlocks(false, 0);
                        dropPlatform.showBlocks(false, -7);
                        dropPlatform.spawn(offset);
                    }
                    hasSpawned = true;
                }
            } else {
                if (hasSpawned) {
//                    Logger.console("Despawning " + newState.name() + " > " + offset);
                    for (int i = 0; i < platforms.size(); i++) {
                        DropPlatform dropPlatform = platforms.get(i);
                        dropPlatform.showBlocks(true, newState == VerticalAutoLift.LiftState.LEAVING ? -7 : 0);
                    }
                    areaTracker.stopTracking();
                    hasSpawned = false;
                }
            }
        }
    }
}
