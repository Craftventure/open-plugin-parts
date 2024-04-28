package net.craftventure.core.ride.tracked;

import com.google.common.collect.Sets;
import net.craftventure.audioserver.api.AudioServerApi;
import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.animation.SquareJurassicDoor;
import net.craftventure.core.animation.VerticalDoor;
import net.craftventure.core.feature.maxifoto.MaxiFoto;
import net.craftventure.core.ktx.util.DateUtils;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcAreaTracker;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar;
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat;
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide;
import net.craftventure.core.ride.trackedride.segment.*;
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain;
import net.craftventure.core.script.ScriptManager;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.repository.AchievementProgressRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import penner.easing.Bounce;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class Fenrir {
    private static Fenrir fenrir;

    private Fenrir(TrackedRide trackedRide) {

    }

    public static Fenrir get() {
        if (fenrir == null) {
            SimpleArea coasterArea = new SimpleArea(new Location(Bukkit.getWorld("world"), ), new Location(Bukkit.getWorld("world"), ));
            final OperableCoasterTrackedRide trackedRide = new OperableCoasterTrackedRide("fenrir", coasterArea, new Location(Bukkit.getWorld("world"), ),
                    "ride_fenrir", "fenrir");
            trackedRide.setOperatorArea(new SimpleArea("world", ));
            initTrack(trackedRide);

            DynamicSeatedRideCar.FakeSeatProvider fakeSeatProvider = (mountedEntityIndex, currentEntityIndex) -> {
                if (currentEntityIndex == 0) { // First seat (one with model)
                    if (mountedEntityIndex == 0 || mountedEntityIndex == 1)
                        return MaterialConfig.INSTANCE.getFENRIR_CAR_WITHOUT_FRONT_SEATS().clone();
                    return MaterialConfig.INSTANCE.getFENRIR_CAR_WITHOUT_BACK_SEATS().clone();
                }
                return null;
            };

            for (int t = 0; t < 2; t++) {
                TrackSegment segment = t == 0 ? trackedRide.getSegmentById("station") : trackedRide.getSegmentById("transfer");
                // Create a train
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, 0.0);
                rideTrain.setTrainSoundName("fenrir",
                        new SpatialTrainSounds.Settings(
                                Sets.newHashSet(
                                        TrackSegment.TrackType.FRICTION_BRAKE,
                                        TrackSegment.TrackType.CHAIN_LIFT,
                                        TrackSegment.TrackType.WHEEL_TRANSPORT
                                )
                        ));
                // Add cars to the train
                DynamicSeatedRideCar frontCar = DynamicSeatedRideCar.fromLegacyFormat("fenrir", 2, 1, 0.7, 1.1, 1.2);
                frontCar.getSeat(0).setNoPassengers();
                frontCar.getSeat(1).setNoPassengers();
//                frontCar.setCarFrontBogieDistance(0.5);
                ((ArmorStandSeat) frontCar.getSeat(0)).setModel(MaterialConfig.INSTANCE.getFENRIR_FRONT());
                rideTrain.addCar(frontCar);

                for (int i = 0; i < 4; i++) {
                    DynamicSeatedRideCar rideCar = DynamicSeatedRideCar.fromLegacyFormat("fenrir", 2, 2, 0.7, 1.1, 2.3);
                    if (i == 0) rideCar.setHasTrainSound(true);
                    rideCar.carFrontBogieDistance = 0.8;
                    rideCar.carRearBogieDistance = -1.5;

                    ((ArmorStandSeat) rideCar.getSeat(0)).setModel(MaterialConfig.INSTANCE.getFENRIR_CAR());
                    rideCar.setFakeSeatProvider(fakeSeatProvider);
//                    rideCar.setFakeSeatProvider(fakeSeatProvider);
                    rideTrain.addCar(rideCar);
                }
                trackedRide.addTrain(rideTrain);
            }

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            trackedRide.setPukeRate(0.07);
            trackedRide.addOnRideCompletionListener((player, rideCar) -> {
                if (DateUtils.INSTANCE.isCoasterDay()) {
                    AchievementProgressRepository database = MainRepositoryProvider.INSTANCE.getAchievementProgressRepository();
                    database.reward(player.getUniqueId(), "coaster_day");
                    database.reward(player.getUniqueId(), "coaster_day_" + LocalDateTime.now().getYear());
                }
            });
            fenrir = new Fenrir(trackedRide);
        }
        return fenrir;
    }

    private static void initTrack(final TrackedRide trackedRide) {
        //127.970009f, -108.724308f, 27.026110f
        Vector node = new Vector(-13.513251f, 0.000000f, -0.522309f); // A point in blender
        Vector target = new Vector(-83.50, 39.00, -572.5); // A target of that same point in MC
        final Vector offset = new Vector(target.getX() - node.getX(), target.getY() - node.getY(), target.getZ() - node.getZ());

        TransportSegment track3 = new TransportSegment("track3",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(8),
                CoasterMathUtils.kmhToBpt(2.2));
        track3.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        track3.add(offset,);


        SidewaysTransferSegment.TransferSegment transfer1 = new SidewaysTransferSegment.TransferSegment("transfer1", trackedRide);
        transfer1.setHoldingBias(1.0);
        transfer1.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        transfer1.add(offset,);
        trackedRide.addTrackSection(transfer1);

        SidewaysTransferSegment.TransferSegment transfer2 = new SidewaysTransferSegment.TransferSegment("transfer2", trackedRide);
        transfer2.setHoldingBias(1.0);
        transfer2.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        transfer2.add(offset,);
        trackedRide.addTrackSection(transfer2);

        SidewaysTransferSegment blockbrake = new SidewaysTransferSegment("transfer",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(8),
                CoasterMathUtils.kmhToBpt(2.2));
        blockbrake.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        blockbrake.addTransferTarget(new SidewaysTransferSegment.TransferTarget(
                new Vector(0, 0, 6),
                transfer1
        ));
        blockbrake.addTransferTarget(new SidewaysTransferSegment.TransferTarget(
                new Vector(0, 0, 10),
                transfer2
        ));

        blockbrake.addUpdateListener(new SidewaysTransferSegment.UpdateListener() {
            final SimpleArea blockArea;
            final NpcAreaTracker areaTracker;

            boolean isTracking = false;
            final Vector[] blockLocations = new Vector[11];
            final NpcEntity[] blockEntities = new NpcEntity[11];

            {
                blockArea = new SimpleArea("world", -133, 33, -579, -84, 45, -559);
                areaTracker = new NpcAreaTracker(blockArea);

                blockLocations[0] = new Vector(-102, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[1] = new Vector(-103, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[2] = new Vector(-104, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[3] = new Vector(-105, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[4] = new Vector(-106, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[5] = new Vector(-107, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[6] = new Vector(-108, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[7] = new Vector(-109, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[8] = new Vector(-110, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[9] = new Vector(-111, 38, -573).add(new Vector(0.5, 0, 0.5));
                blockLocations[10] = new Vector(-112, 38, -573).add(new Vector(0.5, 0, 0.5));

                for (int i = 0; i < blockLocations.length; i++) {
                    NpcEntity npcEntity = new NpcEntity("fenrirDoor", EntityType.FALLING_BLOCK, blockLocations[i].toLocation(blockArea.getWorld()));
                    npcEntity.setBlockData(Material.BLUE_TERRACOTTA.createBlockData());
                    npcEntity.noGravity(true);
                    blockEntities[i] = npcEntity;

                    areaTracker.addEntity(npcEntity);
                }
            }

            @Override
            public void onUpdate(@NotNull SidewaysTransferSegment track) {
                if (track.isTrackMoving()) {
                    if (!isTracking) {
                        areaTracker.startTracking();
                        isTracking = true;

                        for (Vector blockLocation : blockLocations) {
                            Block block = blockLocation.toLocation(blockArea.getWorld()).add(track.getOffset()).getBlock();
                            block.setType(Material.AIR);
                        }
                    }
//
//                    Logger.info("Updating blocks");
                    for (int i = 0; i < blockEntities.length; i++) {
                        NpcEntity npcEntity = blockEntities[i];
                        Vector target = blockLocations[i].clone().add(track.getOffset());
                        npcEntity.move(target.getX(), target.getY(), target.getZ());
                    }
                } else {
                    if (isTracking) {
                        areaTracker.stopTracking();
                        isTracking = false;

                        for (Vector blockLocation : blockLocations) {
                            Block block = blockLocation.toLocation(blockArea.getWorld()).add(track.getOffset()).getBlock();
                            block.setType(Material.BLUE_TERRACOTTA);
                        }
                    }
                }
            }
        });

        transfer1.setNextTrackSegment(blockbrake);
        transfer1.setPreviousTrackSegment(blockbrake);
        transfer2.setNextTrackSegment(blockbrake);
        transfer2.setPreviousTrackSegment(blockbrake);


//        SidewaysTransferSegment blockbrake = new SidewaysTransferSegment("blockbrake",
//                trackedRide,
//                CoasterMathUtils.kmhToBpt(4),
//                CoasterMathUtils.kmhToBpt(1.8),
//                CoasterMathUtils.kmhToBpt(8),
//                CoasterMathUtils.kmhToBpt(2.2), new ArrayList<Vector>() {{
//            add(new Vector(0, 0, -2));
//        }});
        blockbrake.blockSection(true);
        blockbrake.add(offset,);

        SplinedTrackSegment curve = new SplinedTrackSegment("curve", trackedRide);
        curve.setTrackType(TrackSegment.TrackType.FRICTION_BRAKE);
        curve.add(offset,);

        TransportSegment brake = new TransportSegment("brake",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(8),
                CoasterMathUtils.kmhToBpt(2.2));
        brake.setTrackType(TrackSegment.TrackType.FRICTION_BRAKE);
        brake.blockSection(false);
        brake.add(offset,);

        SplinedTrackSegment kwartloop = new SplinedTrackSegment("kwartloop", trackedRide);
        kwartloop.add(offset,);

        SplinedTrackSegment track2 = new SplinedTrackSegment("track2", trackedRide);
        track2.add(offset,);

        SplinedTrackSegment loop = new InvertedSegment("loop", trackedRide);
        loop.add(offset,);

        SplinedTrackSegment track1b = new SplinedTrackSegment("track1b", trackedRide);
        track1b.add(offset,);

//        TransportSegment launch2 = new TransportSegment(
//                trackedRide,
//                CoasterMathUtils.kmhToBpt(86),
//                CoasterMathUtils.kmhToBpt(2.3),
//                CoasterMathUtils.kmhToBpt(90),
//                CoasterMathUtils.kmhToBpt(1.8));

        //(TrackedRide trackedRide, double TRANSPORT_SPEED, double ACCELERATE_FORCE, double MAX_SPEED, double BRAKE_FORCE
//        TrackedRide trackedRide, double TRANSPORT_SPEED, double ACCELERATE_FORCE, double MAX_SPEED, double BRAKE_FORCE,
//        double LAUNCH_TRANSPORT_SPEED, double LAUNCH_ACCELERATE_FORCE, double LAUNCH_MAX_SPEED, double LAUNCH_BRAKE_FORCE,
//        int STATIONARY_TICKS, double FRONT_CAR_STATIONARY_PERCENTAGE, boolean isBlockSection
        LaunchSegment launch2 = new LaunchSegment("launch2", trackedRide,
                CoasterMathUtils.kmhToBpt(86),
                CoasterMathUtils.kmhToBpt(2.3),
                CoasterMathUtils.kmhToBpt(90),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(86),
                CoasterMathUtils.kmhToBpt(2.3),
                CoasterMathUtils.kmhToBpt(90),
                CoasterMathUtils.kmhToBpt(1.8),
                0, 0, true);
        launch2.setTrackType(TrackSegment.TrackType.LSM_LAUNCH);
        launch2.blockSection(true);
        launch2.add(offset,);
        launch2.addOnSectionLeaveListener((trackSegment, rideTrain) -> {
            if (rideTrain.getFrontCarTrackSegment() != trackSegment.getPreviousTrackSegment())
                rideTrain.triggerResync(60100, 1000);
        });
        launch2.setOnLaunchStateChangedListener((newState, oldState) -> {
            if (newState == LaunchSegment.LaunchState.FAILED_LAUNCH_RECOVER) {
                RideTrain rideTrain = launch2.getAnyRideTrainOnSegment();
                if (rideTrain != null) {
                    rideTrain.pauzeOnboardSynchronizedAudio();
                }
            }
        });

//        launch2.add(new TrackSegment.DistanceListener(0) {
//            @Override
//            public void onTargetHit(RideCar rideCar) {
//                if (rideCar == rideCar.getAttachedTrain().getCars().get(0)) {
//                    long now = System.currentTimeMillis() - 67000;
//                    for (Player player : rideCar.getAttachedTrain().getPassengers()) {
//                        AudioServerApi.addAndSync("fenrir", player, now - 5000);
//                    }
//                }
//            }
//        });

        SplinedTrackSegment track1 = new SplinedTrackSegment("track1", trackedRide);
        track1.add(offset,);

        track1.add(new TrackSegment.DistanceListener(264, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                AudioServerApi.INSTANCE.enable("viking_fenrir_launch2");
                AudioServerApi.INSTANCE.sync("viking_fenrir_launch2", System.currentTimeMillis());
            }
        });

        Location[] blocks = new Location[3 * 3];
        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                Location location = new Location(Bukkit.getWorld("world"), -66.5 + column, 39 + row, -599.5);
                blocks[index] = location;
                index++;
            }
        }

        World world = Bukkit.getWorld("world");
        NpcAreaTracker areaTracker = new NpcAreaTracker(new SimpleArea(new Location(world, -100, 44, -566), new Location(world, -62, 37, -587)));
        areaTracker.startTracking();

        double startAngleLeft = -45;
        double endAngleLeft = 45;
        double startAngleRight = 135;
        double endAngleRight = 45;

        SquareJurassicDoor.JurassicLocation[] leftDoorLocations = new SquareJurassicDoor.JurassicLocation[]{
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleLeft, endAngleLeft, Material.DARK_OAK_WOOD.createBlockData())
        };
        final SquareJurassicDoor leftDoor = new SquareJurassicDoor(Bukkit.getWorld("world"), leftDoorLocations, Bounce::easeOut, areaTracker);

        SquareJurassicDoor.JurassicLocation[] rightDoorLocations = new SquareJurassicDoor.JurassicLocation[]{
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 1, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData()),
                new SquareJurassicDoor.JurassicLocation(new Vector(), 2, startAngleRight, endAngleRight, Material.DARK_OAK_WOOD.createBlockData())
        };
        final SquareJurassicDoor rightDoor = new SquareJurassicDoor(Bukkit.getWorld("world"), rightDoorLocations, Bounce::easeOut, areaTracker);

//        AreaTracker area
        NpcAreaTracker verticalDoorTracker = new NpcAreaTracker(new SimpleArea(new Location(world, -50, 34, -573), new Location(world, -84, 62, -639)));
        verticalDoorTracker.startTracking();
        final VerticalDoor verticalDoor = new VerticalDoor(Material.DARK_OAK_WOOD.createBlockData(), 3, blocks, Bounce::easeOut, verticalDoorTracker);

        LaunchSegment launch = new LaunchSegment("launch", trackedRide, CoasterMathUtils.kmhToBpt(4), CoasterMathUtils.kmhToBpt(1.3), CoasterMathUtils.kmhToBpt(10), CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(90), CoasterMathUtils.kmhToBpt(1.8), CoasterMathUtils.kmhToBpt(90), CoasterMathUtils.kmhToBpt(1.8),
                20 * 10, 0.25, true);
        launch.setTrackType(TrackSegment.TrackType.LSM_LAUNCH);
        launch.blockSection(true);
        launch.setOnLaunchStateChangedListener((newState, oldState) -> {
//            Logger.console("New launch state %s (was %s)", newState, oldState);
            if (newState == LaunchSegment.LaunchState.STATIONARY) {
                leftDoor.close(20 * 3);
                rightDoor.close(20 * 3);
                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), () -> {
                    AudioServerApi.INSTANCE.enable("viking_fenrir_launch");
                    AudioServerApi.INSTANCE.sync("viking_fenrir_launch", System.currentTimeMillis());
                }, 20 * 5);
                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), () -> verticalDoor.open(35), 20 * 9);
            }
//
//                if (newState == LaunchSegment.LaunchState.ENTERING) {
//                    ScriptManager.start("ride_fenrir_preshow");
//                } else
            if (newState == LaunchSegment.LaunchState.IDLE) {
                ScriptManager.stop("fenrir", "scene2");
            }
//                if (newState == LaunchSegment.LaunchState.IDLE) {
//                    verticalDoor.close(50);
//                }
        });
        launch.addOnSectionLeaveListener((trackSegment, rideTrain) -> {
//            Logger.console("Sync %d", (System.currentTimeMillis() - rideTrain.getSync()));
            verticalDoor.close(35);
            ScriptManager.stop("fenrir", "scene2");
        });
        launch.add(offset,);


        LaunchSegment preShow = new LaunchSegment("preShow", trackedRide, CoasterMathUtils.kmhToBpt(6), CoasterMathUtils.kmhToBpt(1.3), CoasterMathUtils.kmhToBpt(10), CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(6), CoasterMathUtils.kmhToBpt(1.8), CoasterMathUtils.kmhToBpt(6), CoasterMathUtils.kmhToBpt(1.8),
                20 * 5, 0.65, false);
        preShow.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        preShow.add(offset,);
        preShow.setOnLaunchStateChangedListener((newState, oldState) -> {
            if (newState == LaunchSegment.LaunchState.IDLE) {
                ScriptManager.stop("fenrir", "scene1");
            } else if (newState == LaunchSegment.LaunchState.LAUNCHING) {
                ScriptManager.start("fenrir", "scene2");
                leftDoor.open(20 * 7);
                rightDoor.open(20 * 7);
            }
        });

        final StationSegment station = new StationSegment("station", trackedRide, 4, 5, 2.1);
        station.setSlowBrakingDistance(3.0);
        station.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station.setEjectType(TrackSegment.EjectType.EJECT_TO_SEAT);
        station.setHoldDistance(12.7);
        station.setAutoDispatchTime(90, TimeUnit.SECONDS);
//        station.setDebugInstantStart(true);
        station.setDispatchIntervalTime(50, TimeUnit.SECONDS);
//        station.setKeepRollingTime(100, TimeUnit.SECONDS);
        station.setTrackType(TrackSegment.TrackType.WHEEL_TRANSPORT);
        station.setOnStationGateListener(open -> {
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(5), open);
        });
        station.setOnStationStateChangeListener((newState, oldState) -> {
            if (newState == StationSegment.StationState.DISPATCHING) {
                ScriptManager.start("fenrir", "scene1");

                RideTrain rideTrain = station.getAnyRideTrainOnSegment();
                if (rideTrain != null) {
                    rideTrain.setOnboardSynchronizedAudio("onride_fenrir", System.currentTimeMillis());
                }
            }
        });
//        station.add(new TrackSegment.DistanceListener(13) {
//            @Override
//            public void onTargetHit(RideCar rideCar) {
//            }
//        });
        station.add(offset,);


        blockbrake.setContainsListener(containsTrain -> {
            if (containsTrain) {
                if (trackedRide.hasPassengersInNonEnterableTrains()) {
                    station.tryDispatchNow(StationSegment.DispatchRequestType.TRAIN_WITH_PLAYERS);
                }
            }
        });

        station.add(new TrackSegment.DistanceListener(5, true) {
            private boolean firstSlot = true;

            @Override
            public void onTargetHit(@NotNull RideCar triggeringCar) {
                Player[] names = new Player[16];
                for (int i = 0; i < names.length; i++)
                    names[i] = null;

                int index = 0;
                List<RideCar> cars = triggeringCar.attachedTrain.getCars();
                for (int i1 = 1; i1 < cars.size(); i1++) {
                    RideCar rideCar = cars.get(i1);
                    List<Entity> passengers = rideCar.getMaxifotoPassengerList();
                    List<Entity> passengersNew = new ArrayList<>();
//                    Logger.info("Passengers %d", false, passengers.size());
                    for (int i = 0; i < passengers.size(); i += 2) {
                        passengersNew.add(passengers.get(i + 1));
                        passengersNew.add(passengers.get(i));
                    }

                    for (Entity entity : passengersNew) {
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

                MaxiFoto.RenderSettings renderSettings = new MaxiFoto.RenderSettings("fenrir", names);
                if (!onlyNulls) {
                    if (!firstSlot)
                        renderSettings.setOffset(4);
                    MaxiFoto.INSTANCE.render(renderSettings);
                    firstSlot = !firstSlot;
                }
            }
        });

        station.setNextTrackSegmentRetroActive(preShow);
        trackedRide.addTrackSection(station);

        preShow.setNextTrackSegmentRetroActive(launch);
        trackedRide.addTrackSection(preShow);

        launch.setNextTrackSegmentRetroActive(kwartloop);
        trackedRide.addTrackSection(launch);

        kwartloop.setNextTrackSegmentRetroActive(track1);
        trackedRide.addTrackSection(kwartloop);

        track1.setNextTrackSegmentRetroActive(launch2);
        trackedRide.addTrackSection(track1);

        launch2.setNextTrackSegmentRetroActive(track1b);
        trackedRide.addTrackSection(launch2);

        track1b.setNextTrackSegmentRetroActive(loop);
        trackedRide.addTrackSection(track1b);

        loop.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(loop);

        track2.setNextTrackSegmentRetroActive(brake);
        trackedRide.addTrackSection(track2);

        brake.setNextTrackSegmentRetroActive(curve);
        trackedRide.addTrackSection(brake);

        curve.setNextTrackSegmentRetroActive(blockbrake);
        trackedRide.addTrackSection(curve);

        blockbrake.setNextTrackSegmentRetroActive(track3);
        trackedRide.addTrackSection(blockbrake);

        track3.setNextTrackSegmentRetroActive(station);
        trackedRide.addTrackSection(track3);
    }
}
