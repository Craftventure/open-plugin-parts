package net.craftventure.core.ride.tracked;

import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.core.feature.maxifoto.MaxiFoto;
import net.craftventure.core.ride.queue.RideQueue;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar;
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat;
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide;
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.ride.trackedride.segment.TransportSegment;
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class PitchiPitchiLoco {
    private static PitchiPitchiLoco ppl;

    private PitchiPitchiLoco(TrackedRide trackedRide) {

    }

    public static PitchiPitchiLoco get() {
        if (ppl == null) {

            SimpleArea coasterArea = new SimpleArea(new Location(Bukkit.getWorld("world"), ), new Location(Bukkit.getWorld("world"), ));
            final OperableCoasterTrackedRide trackedRide = new OperableCoasterTrackedRide("ppl", coasterArea, new Location(Bukkit.getWorld("world"), ),
                    "ride_ppl", "ppl");
//            addFenrirSections(trackedRide);
            trackedRide.setOperatorArea(new SimpleArea("world", ));
            initTrack(trackedRide);
            double distance = 0;
            TrackSegment segment = trackedRide.getTrackSegments().get(0);
            for (int i = 0; i < 4; i++) {
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, distance);
                DynamicSeatedRideCar rideCar = DynamicSeatedRideCar.fromLegacyFormat("ppl", 2, 2, 0.7, 1.1, 1.3);
                ((ArmorStandSeat) rideCar.getSeat(0)).setModel(MaterialConfig.INSTANCE.getPPL_CAR());
                rideCar.carRearBogieDistance = -1.0;
                rideTrain.addCar(rideCar);
                trackedRide.addTrain(rideTrain);

                distance -= 3.2;
                while (distance < 0) {
                    segment = segment.getPreviousTrackSegment();
                    distance += segment.getLength();
                }
            }

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            trackedRide.setPukeRate(0);
            ppl = new PitchiPitchiLoco(trackedRide);

            RideQueue queue = new RideQueue(trackedRide, new SimpleArea("world", 298, 46, -586, 305, 50, -581),
                    4,
                    20.0,
                    new RideQueue.RideStationBoardingDelegate((StationSegment) trackedRide.getSegmentById("station")));
            queue.start();
        }
        return ppl;
    }

    private static void initTrack(TrackedRide trackedRide) {//127.970009f, -108.724308f, 27.026110f
        final double SPEED = 4.2;
        Vector node = new Vector(0, 0, 0); // A point in blender
        Vector target = new Vector(280, 47, -587); // A target of that same point in MC
        final Vector offset = new Vector(target.getX() - node.getX(), target.getY() - node.getY(), target.getZ() - node.getZ());

        TransportSegment preStation = new TransportSegment("preStation",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(1.3),
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(1.8));
        preStation.blockSection(true);
        preStation.setBlockType(TrackSegment.BlockType.CONTINUOUS, 2, 2);
        preStation.setOffsetFromNextSection(0.7);
        preStation.add(offset,);

        SplinedTrackSegment track2 = new TransportSegment("track2",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(1.3),
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(1.8));
        track2.blockSection(true);
        preStation.setBlockType(TrackSegment.BlockType.CONTINUOUS, 2, 2);
        track2.add(offset,);

        TransportSegment exitStation = new TransportSegment("exitStation", trackedRide, CoasterMathUtils.kmhToBpt(SPEED), CoasterMathUtils.kmhToBpt(1.3), CoasterMathUtils.kmhToBpt(SPEED), CoasterMathUtils.kmhToBpt(1.8));
        exitStation.setEjectType(TrackSegment.EjectType.EJECT_TO_SEAT);
        exitStation.add(new TrackSegment.DistanceListener(1) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                rideCar.attachedTrain.eject();
            }
        });
        exitStation.blockSection(true);
        exitStation.add(offset,);

        SplinedTrackSegment track = new TransportSegment("track", trackedRide, CoasterMathUtils.kmhToBpt(SPEED), CoasterMathUtils.kmhToBpt(1.3), CoasterMathUtils.kmhToBpt(SPEED), CoasterMathUtils.kmhToBpt(1.8));
        track.setBlockType(TrackSegment.BlockType.CONTINUOUS, 2, 2);
        track.add(offset,);

        StationSegment station = new StationSegment("station", trackedRide, SPEED, SPEED, 2.1);
        station.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station.setEjectType(TrackSegment.EjectType.EJECT_TO_SEAT);
        station.setHoldDistance(2.16);
        station.setDispatchIntervalTime(15, TimeUnit.SECONDS);
        station.setKeepRollingTime(15, TimeUnit.SECONDS);
        station.add(offset,);
        station.setOnStationGateListener(open -> {
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(302, 47, -584), open);
        });

        track.add(new TrackSegment.DistanceListener(70, true) {
            private int car = 0;

            @Override
            public void onTargetHit(@NotNull RideCar triggeringCar) {
                Player[] names = new Player[4];
                for (int i = 0; i < names.length; i++)
                    names[i] = null;

                int index = 0;
                for (RideCar rideCar : triggeringCar.attachedTrain.getCars()) {
                    List<Entity> passengers = rideCar.getMaxifotoPassengerList();
                    List<Entity> passengersNew = new ArrayList<>();
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

                MaxiFoto.RenderSettings renderSettings = new MaxiFoto.RenderSettings("ppl", names);
                if (!onlyNulls) {
                    renderSettings.setOffset(car);
                    MaxiFoto.INSTANCE.render(renderSettings);
                    car++;
                    if (car >= 3) {
                        car = 0;
                    }
                }
            }
        });


        station.setNextTrackSegmentRetroActive(track);
        trackedRide.addTrackSection(station);

        track.setNextTrackSegmentRetroActive(exitStation);
        trackedRide.addTrackSection(track);

        exitStation.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(exitStation);

        track2.setNextTrackSegmentRetroActive(preStation);
        trackedRide.addTrackSection(track2);

        preStation.setNextTrackSegmentRetroActive(station);
        trackedRide.addTrackSection(preStation);
    }
}

