package net.craftventure.core.ride.tracked;

import net.craftventure.audioserver.api.AudioServerApi;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar;
import net.craftventure.core.ride.trackedride.car.seat.MinecartSeat;
import net.craftventure.core.ride.trackedride.ride.CoasterTrackedRide;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.ride.trackedride.segment.TransportSegment;
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.concurrent.TimeUnit;


public class DragonDance {
    private static DragonDance dragonDance;

    private DragonDance(TrackedRide trackedRide) {

    }

    public static DragonDance get() {
        if (dragonDance == null) {
            SimpleArea coasterArea = new SimpleArea("world", );
            final CoasterTrackedRide trackedRide = new CoasterTrackedRide("dragondance", coasterArea, new Location(Bukkit.getWorld("world"), ),
                    "ride_dragondance", "dragondance");
//            addFenrirSections(trackedRide);
            initTrack(trackedRide);
            TrackSegment segment = trackedRide.getTrackSegments().get(0);

            CoasterRideTrain rideTrain = new CoasterRideTrain(segment, 0);//(2.3 * 5) + 1.2);
            for (int i = 0; i < 4; i++) {
                DynamicSeatedRideCar dynamicSeatedRideCar = new DynamicSeatedRideCar("dragondance", 1.5);
                dynamicSeatedRideCar.carFrontBogieDistance = -0.745;
                dynamicSeatedRideCar.carRearBogieDistance = -0.75;

                MinecartSeat seat = new MinecartSeat(0, 0.0, 0, true, "dragondance");
//                seat.setModel(MaterialConfig.INSTANCE.getMINECART());
                dynamicSeatedRideCar.addSeat(seat);

                rideTrain.addCar(dynamicSeatedRideCar);
            }
            trackedRide.addTrain(rideTrain);

//      Initialize the tracked. From this point, you can no longer edit the tracked and doing so will throw an IllegalStateException
            trackedRide.initialize();
            trackedRide.setPukeRate(0);
            dragonDance = new DragonDance(trackedRide);
        }
        return dragonDance;
    }

    private static void initTrack(TrackedRide trackedRide) {//127.970009f, -108.724308f, 27.026110f
        final double SPEED = 20;
        final Vector offset = new Vector(0, 0.2, 0);

        StationSegment station = new StationSegment("station", trackedRide, SPEED, SPEED, 2.1);
        station.setSkipCount(2);
        station.setAutoStartDelay(8, TimeUnit.SECONDS);
        station.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
//        station.setOnStationStateChangeListener((newState, oldState) -> {
//            Logger.info("DragonClan state set to %s after %s", false, newState, oldState);
//            if (oldState == StationSegment.StationState.ENTERING && newState == StationSegment.StationState.HOLDING) {
//                AudioServerApi.disable("dragondance");
//            }
////            if (newState == StationSegment.StationState.IDLE) {
////                AudioServerApi.disable("dragondance");
////            }
//        });
        station.setOnStationGateListener(open -> {
            if (open) {
//                Logger.info("Stopping music for gate event with state %s", false, station.getState());
                AudioServerApi.INSTANCE.disable("dragondance");
            }
        });
        station.setOnCountdownListener(new StationSegment.OnCountdownListener() {
            @Override
            public void onCountdownStarted() {
//                Logger.info("Countdown started");
                AudioServerApi.INSTANCE.sync("dragondance", System.currentTimeMillis());
                AudioServerApi.INSTANCE.enable("dragondance");
            }

            @Override
            public void onCountdownStopped() {
//                Logger.info("Countdown stopped %s", false, station.getState());
                if (station.getState() == StationSegment.StationState.IDLE)
                    AudioServerApi.INSTANCE.disable("dragondance");
            }
        });
        station.setHoldDistance(6.5);
        station.add(offset,);

        TransportSegment track1 = new TransportSegment("track1",
                trackedRide,
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(1.3),
                CoasterMathUtils.kmhToBpt(SPEED),
                CoasterMathUtils.kmhToBpt(1.8));
        track1.blockSection(true);
        track1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 3, 3);
        track1.setOffsetFromNextSection(0.7);
        track1.add(offset,);

        station.setNextTrackSegmentRetroActive(track1);
        trackedRide.addTrackSection(station);

        track1.setNextTrackSegmentRetroActive(station);
        trackedRide.addTrackSection(track1);
    }
}

