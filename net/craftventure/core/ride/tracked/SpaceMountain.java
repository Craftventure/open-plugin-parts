package net.craftventure.core.ride.tracked;

import net.craftventure.audioserver.api.AudioServerApi;
import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.core.effect.*;
import net.craftventure.core.feature.maxifoto.MaxiFoto;
import net.craftventure.core.ktx.util.DateUtils;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class SpaceMountain {
    private static SpaceMountain spaceMountain;
    private static final boolean DEBUG = false;

    private SpaceMountain(TrackedRide trackedRide) {

    }

    public static SpaceMountain get() {
        if (spaceMountain == null) {
            EffectManager.INSTANCE.add(new SuperNovaEffect());
            EffectManager.INSTANCE.add(new SpaceMountainRingEffect());
            EffectManager.INSTANCE.add(new SpaceMountainCannonEffect());
            EffectManager.INSTANCE.add(new SpaceMountainLaunchEffect());
            EffectManager.INSTANCE.add(new SpaceMountainSolarSystem());

            SimpleArea coasterArea = new SimpleArea("world", );
            final OperableCoasterTrackedRide trackedRide = new OperableCoasterTrackedRide("spacemountain", coasterArea, new Location(Bukkit.getWorld("world"), ),
                    "ride_spacemountain", "spacemountain");
            trackedRide.setOperatorArea(new SimpleArea("world", ));
            initTrack(trackedRide);

            for (int t = 0; t < (DEBUG ? 1 : 3); t++) {
                TrackSegment segment = t == 0 ? trackedRide.getSegmentById("station1") :
                        t == 1 ? trackedRide.getSegmentById("station2") :
                                trackedRide.getSegmentById("block1");
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, 5);

                for (int i = 0; i < 5; i++) {
                    DynamicSeatedRideCar rideCar = DynamicSeatedRideCar.fromLegacyFormat("spacemountain", 2, 2, 0.7, 1.1, 2.7);
                    if (i == 0) {
                        ((ArmorStandSeat) rideCar.getSeat(0)).setModel(MaterialConfig.INSTANCE.getSM_FRONT());
                    } else {
                        ((ArmorStandSeat) rideCar.getSeat(0)).setModel(MaterialConfig.INSTANCE.getSM_NORMAL());
                    }
//                    rideCar.setCarFrontBogieDistance(i == 0 ? 0.6 : 0.4 + 0.75);
//                    rideCar.setCarRearBogieDistance(-2.3 + 0.75);
                    rideCar.carFrontBogieDistance = 1.0;
                    rideCar.carRearBogieDistance = -2.3 + 0.75;
                    rideTrain.addCar(rideCar);
                }
                trackedRide.addTrain(rideTrain);
            }

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            trackedRide.setPukeRate(0.05);
            trackedRide.addOnRideCompletionListener((player, rideCar) -> {
                AchievementProgressRepository database = MainRepositoryProvider.INSTANCE.getAchievementProgressRepository();
//                if (DateUtils.INSTANCE.isAprilFools()) {
//                    database.reward(player.getUniqueId(), "spacerokmountain");
//                }
                if (DateUtils.INSTANCE.isCoasterDay()) {
                    database.reward(player.getUniqueId(), "coaster_day");
                    database.reward(player.getUniqueId(), "coaster_day_" + LocalDateTime.now().getYear());
                }
            });
            spaceMountain = new SpaceMountain(trackedRide);

//            String[] tracks = new String[]{"track3", "mcbr1", "track4", "lift1", "track5"};
//            for (String trackName : tracks) {
//                TrackSegment track = trackedRide.getSegmentById(trackName);
//                if (track != null) {
//                    World world = Bukkit.getWorld("world");
//                    Vector location = new Vector(0, 0, 0);
//                    for (double l = 0; l < track.getLength(); l += 0.5) {
//                        track.getPosition(l, location);
//                        location.setY(location.getY() - 0.7);
//                        Block block = location.toLocation(world).getBlock();
//                        if (block.getType() == Material.AIR) {
//                            block.setType(Material.COAL_BLOCK);
//                        }
//                    }
//                }
//            }
        }
        return spaceMountain;
    }

    private static void initTrack(final TrackedRide trackedRide) {
        final double INTRO_TRANSPORT_SPEED = CoasterMathUtils.kmhToBpt(14);
        final Vector offset = new Vector(0, 0, 0);

        StationSegment station1 = new StationSegment("station1", "Station 1", trackedRide, 14, 14, 2.1);
        StationSegment station2 = new StationSegment("station2", "Station 2", trackedRide, 14, 14, 2.1);
        station1.setSlowBrakingDistance(5.0);
        station2.setSlowBrakingDistance(5.0);

        station1.setHoldDistance(17.1);
        station2.setHoldDistance(17.1);

        station1.setDispatchIntervalTime(34, TimeUnit.SECONDS);
        station1.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station1.setEjectType(TrackSegment.EjectType.EJECT_TO_SEAT);
//        station1.setAutoDispatchTime(120, TimeUnit.SECONDS);
//        station2.setAutoDispatchTime(120, TimeUnit.SECONDS);
//        station1.setStopMeasureDistanceOffset(-0.5);
//        if (DEBUG)
//            station1.setDebugInstantStart(true);
        station1.setOnStationGateListener(open -> {
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
        });
        station1.setOnStationStateChangeListener((newState, oldState) -> {
            if (newState == StationSegment.StationState.DISPATCHING) {
                station2.setLastDepartureTime(System.currentTimeMillis());
                RideTrain rideTrain = station1.getAnyRideTrainOnSegment();
                if (rideTrain != null) {
                    rideTrain.setOnboardSynchronizedAudio("onride_spacemountain", System.currentTimeMillis());
//                    rideTrain.setOnboardSynchronizedAudio("onride_spacemountain_p1", System.currentTimeMillis());
                }
            }
        });
        station1.add(offset,);

        station2.setDispatchIntervalTime(34, TimeUnit.SECONDS);
        station2.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station2.setEjectType(TrackSegment.EjectType.EJECT_TO_SEAT);
//        station2.setStopMeasureDistanceOffset(-0.5);
//        if (DEBUG)
//            station2.setDebugInstantStart(true);
        station2.setOnStationGateListener(open -> {
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(), open);
        });
        station2.setOnStationStateChangeListener((newState, oldState) -> {
            if (newState == StationSegment.StationState.DISPATCHING) {
                station1.setLastDepartureTime(System.currentTimeMillis());
                RideTrain rideTrain = station2.getAnyRideTrainOnSegment();
                if (rideTrain != null) {
                    rideTrain.setOnboardSynchronizedAudio("onride_spacemountain_sm2", System.currentTimeMillis());
//                    rideTrain.setOnboardSynchronizedAudio("onride_spacemountain_p1", System.currentTimeMillis());
                }
            }
        });
        station2.add(offset,);

        SplinedTrackSegment merge1Station1 = new TransportSegment("merge1Station1",
                trackedRide,
                INTRO_TRANSPORT_SPEED,
                CoasterMathUtils.kmhToBpt(1.8),
                INTRO_TRANSPORT_SPEED,
                CoasterMathUtils.kmhToBpt(2.2));
        merge1Station1.add(offset,);

        SplinedTrackSegment merge1Station2 = new TransportSegment("merge1Station2",
                trackedRide,
                INTRO_TRANSPORT_SPEED,
                CoasterMathUtils.kmhToBpt(1.8),
                INTRO_TRANSPORT_SPEED,
                CoasterMathUtils.kmhToBpt(2.2));
        merge1Station2.add(offset,);

        SplinedTrackSegment track1 = new TransportSegment("track1",
                trackedRide,
                INTRO_TRANSPORT_SPEED,
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(50),
                CoasterMathUtils.kmhToBpt(2.2));
        track1.setShouldAutomaticallyReroutePreviousSegment(true);
        track1.add(offset,);

//        TransportSegment launch1 = new TransportSegment(trackedRide,
//                INTRO_TRANSPORT_SPEED,
//                CoasterMathUtils.kmhToBpt(1.8),
//                CoasterMathUtils.kmhToBpt(50),
//                CoasterMathUtils.kmhToBpt(2.2)/*,
//                //TODO: Remove debug
//                8, DEBUG ? 5 : 20 * 13*/);
//        launch1.setId("launch1");
//        launch1.add(offset,
//                new SplineNode(new SplineHandle(227.010345f, 39.633957f, -762.550659f),
//                        new SplineHandle(229.465851f, 41.196167f, -762.538025f),
//                        new SplineHandle(230.214554f, 41.672497f, -762.534180f), -0.000000),
//                new SplineNode(new SplineHandle(234.733505f, 44.749111f, -762.550659f),
//                        new SplineHandle(237.188995f, 46.311310f, -762.538025f),
//                        new SplineHandle(237.937698f, 46.787640f, -762.534180f), -0.000000));

        LaunchSegment launch1 = new LaunchSegment("launch2", "Cannon", trackedRide,
                INTRO_TRANSPORT_SPEED,
                CoasterMathUtils.kmhToBpt(1.3),
                INTRO_TRANSPORT_SPEED,
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(50.5), //50 for mission2
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(50.5), //50 for mission2
                CoasterMathUtils.kmhToBpt(1.8),
                DEBUG ? 5 : 20 * 5, 0.4, true);
//        if (DEBUG) {
        launch1.setOnLaunchStateChangedListener((newState, oldState) -> {
            if (newState == LaunchSegment.LaunchState.STATIONARY) {
                SimpleEffect simpleEffect = EffectManager.INSTANCE.findByName("smcannon");
                if (simpleEffect != null && !DateUtils.INSTANCE.isAprilFools())
                    simpleEffect.play();
                AudioServerApi.INSTANCE.sync("sm_canon_area", System.currentTimeMillis());
                AudioServerApi.INSTANCE.enable("sm_canon_area");
            }
            if (newState == LaunchSegment.LaunchState.LAUNCHING) {
//                launch1.getAnyRideTrainOnSegment().setOnboardSynchronizedAudio("onride_spacemountain_p2", System.currentTimeMillis());
                SimpleEffect simpleEffect = EffectManager.INSTANCE.findByName("smlaunch");
                if (simpleEffect != null && !DateUtils.INSTANCE.isAprilFools())
                    simpleEffect.play();
                long time = System.currentTimeMillis() - 20500;
//                    AudioServerApi.sync(audioName(), time);
                RideTrain rideTrain = launch1.getAnyRideTrainOnSegment();
                if (rideTrain != null) {
                    for (Player player : launch1.getAnyRideTrainOnSegment().getPassengers()) {
                        AudioServerApi.INSTANCE.syncPlayer(rideTrain.getAudioName(), player, time);
                    }
                }
            }
        });
//        }
        launch1.add(offset,);

        SplinedTrackSegment track2 = new SplinedTrackSegment("track2", trackedRide);
        track2.add(offset,);

        SplinedTrackSegment track3 = new TransportSegment("track3",
                trackedRide,
                CoasterMathUtils.kmhToBpt(10),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(150),
                CoasterMathUtils.kmhToBpt(2.2));
        track3.blockSection(false);
        track3.setFriction(0.9987);
        track3.add(offset,);


        SplinedTrackSegment mcbr1 = new TransportSegment("mcbr1", "Meteor brakerun",
                trackedRide,
                CoasterMathUtils.kmhToBpt(20),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(20),
                CoasterMathUtils.kmhToBpt(2.2));
        mcbr1.blockSection(true);
        mcbr1.add(offset,);

        SplinedTrackSegment track4 = new TransportSegment("track4",
                trackedRide,
                CoasterMathUtils.kmhToBpt(10),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(150),
                CoasterMathUtils.kmhToBpt(2.2));
        track4.blockSection(false);
        track4.setFriction(0.9987);
        track4.add(offset,);

        track4.add(new TrackSegment.DistanceListener(155, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                if (DateUtils.INSTANCE.isAprilFools()) return;

                SimpleEffect simpleEffect = EffectManager.INSTANCE.findByName("smsupernova");
                if (simpleEffect != null)
                    simpleEffect.play();
            }
        });
        track4.add(new TrackSegment.DistanceListener(80, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                if (DateUtils.INSTANCE.isAprilFools()) return;

                SimpleEffect effect = EffectManager.INSTANCE.findByName("smsolarsystem");
                if (effect != null)
                    effect.play();
            }
        });

        SplinedTrackSegment lift1 = new TransportSegment("lift1", "Supernova lift",
                trackedRide,
                CoasterMathUtils.kmhToBpt(11), //13 for mission 2
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(11), //13 for mission 2
                CoasterMathUtils.kmhToBpt(2.2));
        lift1.blockSection(true);
        lift1.setFriction(0.9987);
        lift1.setContainsListener(containsTrain -> {
            if (containsTrain) {
                if (trackedRide.hasPassengersInNonEnterableTrains()) {
                    station1.tryDispatchNow(StationSegment.DispatchRequestType.TRAIN_WITH_PLAYERS);
                    station2.tryDispatchNow(StationSegment.DispatchRequestType.TRAIN_WITH_PLAYERS);
                }
//                if (trackedRide.getPassengerCount() > 0 && station1.isContainsTrain() && station2.isContainsTrain()) {
//                    station1.requestStationClearance();
//                }
            }
        });
        lift1.add(new TrackSegment.DistanceListener(10, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                AudioServerApi.INSTANCE.enable("spacemountain_supernova");
                AudioServerApi.INSTANCE.sync("spacemountain_supernova", System.currentTimeMillis());
            }
        });
        lift1.add(offset,);

        SplinedTrackSegment track5 = new TransportSegment("track5",
                trackedRide,
                CoasterMathUtils.kmhToBpt(25),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(150),
                CoasterMathUtils.kmhToBpt(2.2));
        track5.blockSection(false);
        track5.setFriction(0.9987);
        track5.add(offset,);

        track5.add(new TrackSegment.DistanceListener(269, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                AudioServerApi.INSTANCE.sync("sm_brakes", System.currentTimeMillis());
                AudioServerApi.INSTANCE.enable("sm_brakes");
                ScriptManager.start("spacemountain", "edv");
            }
        });

        track5.add(new TrackSegment.DistanceListener(151, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                SimpleEffect ringEffect = EffectManager.INSTANCE.findByName("smring");
                if (ringEffect != null)
                    ringEffect.play();
            }
        });

        SplinedTrackSegment block1 = new TransportSegment("block1", "Pre-station brakerun",
                trackedRide,
                CoasterMathUtils.kmhToBpt(8),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(10),
                CoasterMathUtils.kmhToBpt(2.2));
        block1.blockSection(true);
        block1.add(offset,);
        block1.add((trackSegment, containsTrain, isStoppingTrainFromLeaving) -> {
//            Logger.console("SM:Block1 > "+trackSegment.getId() + " " + containsTrain + " " + isStoppingTrainFromLeaving);
            if (isStoppingTrainFromLeaving && containsTrain) {
                AudioServerApi.INSTANCE.enable("sm_brakes_wait");
                AudioServerApi.INSTANCE.sync("sm_brakes_wait", System.currentTimeMillis());
                if (trackedRide.hasPassengersInNonEnterableTrains()) {
                    station1.tryDispatchNow(StationSegment.DispatchRequestType.TRAIN_WITH_PLAYERS);
                    station2.tryDispatchNow(StationSegment.DispatchRequestType.TRAIN_WITH_PLAYERS);
                }
            }
        });

        SplinedTrackSegment track6 = new TransportSegment("track6",
                trackedRide,
                CoasterMathUtils.kmhToBpt(8),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(10),
                CoasterMathUtils.kmhToBpt(2.2));
        track6.add(offset,);

        track6.add(new TrackSegment.DistanceListener(1, false, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                AudioServerApi.INSTANCE.disable("sm_brakes");
                ScriptManager.stop("spacemountain", "edv");
            }
        });

        track6.add(new TrackSegment.DistanceListener(11, true) {
            private boolean firstSlot = true;

            @Override
            public void onTargetHit(@NotNull RideCar triggeringCar) {
                Player[] names = new Player[20];
                for (int i = 0; i < names.length; i++)
                    names[i] = null;

                int index = 0;
                for (RideCar rideCar : triggeringCar.attachedTrain.getCars()) {
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

                MaxiFoto.RenderSettings renderSettings = new MaxiFoto.RenderSettings("spacemountain", names);
                if (!onlyNulls) {
                    if (!firstSlot)
                        renderSettings.setOffset(5);
                    MaxiFoto.INSTANCE.render(renderSettings);
                    firstSlot = !firstSlot;
                }
            }
        });

        SplinedTrackSegment fork1Station1 = new TransportSegment("fork1Station1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(8),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(2.2));
        fork1Station1.setNextTrackSegmentRetroActive(station1);
        fork1Station1.add(offset,);
        fork1Station1.setNextTrackSegment(station1);
        fork1Station1.setPreviousTrackSegment(track6);
        trackedRide.addTrackSection(fork1Station1);

        SplinedTrackSegment fork1Station2 = new TransportSegment("fork1Station2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(8),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(12),
                CoasterMathUtils.kmhToBpt(2.2));
        fork1Station2.setNextTrackSegmentRetroActive(station2);
        fork1Station2.add(offset,);
        fork1Station2.setNextTrackSegment(station1);
        fork1Station2.setPreviousTrackSegment(track6);
        trackedRide.addTrackSection(fork1Station2);

        // Routers
        List<ForkRouterSegment.RoutingEntry> forks = new ArrayList<>();
        forks.add(new ForkRouterSegment.RoutingEntry(fork1Station1, station1));
        forks.add(new ForkRouterSegment.RoutingEntry(fork1Station2, station2));
        ForkRouterSegment toStationForkRouterSegment = new ForkRouterSegment("toStationForkRouter", trackedRide, forks);

        toStationForkRouterSegment.setNextTrackSegmentRetroActive(station1);
        toStationForkRouterSegment.setNextTrackSegmentRetroActive(station2);

        // Station 1
        station1.setNextTrackSegmentRetroActive(merge1Station1);
        trackedRide.addTrackSection(station1);

        merge1Station1.setNextTrackSegmentRetroActive(track1);
        trackedRide.addTrackSection(merge1Station1);

        // Station 2
        station2.setNextTrackSegmentRetroActive(merge1Station2);
        trackedRide.addTrackSection(station2);

        merge1Station2.setNextTrackSegmentRetroActive(track1);
        trackedRide.addTrackSection(merge1Station2);

        // Track
        track1.setNextTrackSegmentRetroActive(launch1);
        trackedRide.addTrackSection(track1);

        launch1.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(launch1);

        track2.setNextTrackSegmentRetroActive(track3);
        trackedRide.addTrackSection(track2);

        track3.setNextTrackSegmentRetroActive(mcbr1);
        trackedRide.addTrackSection(track3);

        mcbr1.setNextTrackSegmentRetroActive(track4);
        trackedRide.addTrackSection(mcbr1);

        track4.setNextTrackSegmentRetroActive(lift1);
        trackedRide.addTrackSection(track4);

        lift1.setNextTrackSegmentRetroActive(track5);
        trackedRide.addTrackSection(lift1);

        track5.setNextTrackSegmentRetroActive(block1);
        trackedRide.addTrackSection(track5);
//
        block1.setNextTrackSegmentRetroActive(track6);
        trackedRide.addTrackSection(block1);

        // Fork
        track6.setNextTrackSegmentRetroActive(toStationForkRouterSegment);
        trackedRide.addTrackSection(track6);

        toStationForkRouterSegment.setNextTrackSegmentRetroActive(station1);
        trackedRide.addTrackSection(toStationForkRouterSegment);

        if (DEBUG) {
            merge1Station1.setNextTrackSegmentRetroActive(launch1);
            merge1Station2.setNextTrackSegmentRetroActive(launch1);
        }
//
//        track4.addOnSectionEnterListener((trackSegment, rideTrain, previousSegment) -> {
//            rideTrain.setOnboardSynchronizedAudio("onride_spacemountain_p3", System.currentTimeMillis());
//        });
//        track5.addOnSectionEnterListener((trackSegment, rideTrain, previousSegment) -> {
//            rideTrain.setOnboardSynchronizedAudio("onride_spacemountain_p4", System.currentTimeMillis());
//        });
    }
}
