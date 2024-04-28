package net.craftventure.core.ride.tracked;

import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;


public class Hyperion {
    private static Hyperion hyperion;
    private static final double CAR_LENGTH = 6.4;
    private static final int CAR_COUNT = 5;

    private Hyperion(TrackedRide trackedRide) {

    }

    public static Hyperion get() {
        if (hyperion == null) {

            SimpleArea coasterArea = new SimpleArea("world", );
            final OperableCoasterTrackedRide trackedRide = new OperableCoasterTrackedRide("hyperion", coasterArea,
                    new Location(Bukkit.getWorld("world"), ),
                    "ride_hyperion", "hyperion");
            trackedRide.setOperatorArea(new SimpleArea("world", ));
//            addFenrirSections(trackedRide);
            initTrack(trackedRide);

            TrackSegment segment = trackedRide.getSegmentById("station");//trackedRide.getTrackSegments().size() - 1);
            TrackSegment beginSegment = segment;
            double distance = 0;//segment.getLength() - 70;


            for (int t = 0; t < CAR_COUNT; t++) {
                CoasterRideTrain rideTrain = new CoasterRideTrain(segment, distance);//(2.3 * 5) + 1.2);
                rideTrain.setMovementHandler(new CoasterRideTrain.MovementHandler() {
                    @Override
                    public double handleYawRadian(double yaw, @Nonnull RideCar car) {
                        return yaw;
                    }

                    @Override
                    public double handlePitchRadian(double pitch, @Nonnull RideCar car) {
//                        Logger.info("%2.2f", false, pitch);
                        return -(Math.PI * 0.5) + ((pitch - (Math.PI * 1.5)) * 0.2);
                    }
                });
//                rideTrain.setUseBogieNormalization(false);

                DynamicSeatedRideCar dynamicSeatedRideCar = new DynamicSeatedRideCar("hyperion", CAR_LENGTH);
                dynamicSeatedRideCar.carFrontBogieDistance = 0.2;
                dynamicSeatedRideCar.carRearBogieDistance = -0.2;

                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(0, -4.5, -1.5 + 1.7, false, "hyperion")
                        .setModel(MaterialConfig.INSTANCE.getHYPERION_ZEPPELIN()));

                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(0, -5.0, -2.1 + 1.7, true, "hyperion"));
                dynamicSeatedRideCar.addSeat(new ArmorStandSeat(0, -5.0, -0.9 + 1.7, true, "hyperion", 180));

                rideTrain.addCar(dynamicSeatedRideCar);

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
            trackedRide.setPukeRate(0);
            hyperion = new Hyperion(trackedRide);
        }
        return hyperion;
    }

    private static void initTrack(TrackedRide trackedRide) {//127.970009f, -108.724308f, 27.026110f
        final double SPEED = 4.2;
        final Vector offset = new Vector(0, 0, 0);

        StationSegment station = new StationSegment("station", trackedRide, SPEED, SPEED, 2.1);
        station.setLeaveMode(TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER);
        station.setAutoDispatchTime(90, TimeUnit.SECONDS);
        station.setOnStationGateListener(open -> {
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(186, 45, -798), open);
            BlockExtensionsKt.open(Bukkit.getWorld("world").getBlockAt(186, 45, -797), open);
        });
//        station.setStopMeasureDistanceOffset(1.3 + 3.2);
        station.setHoldDistance(2.86 + 1.5);
        station.setKeepRollingTime((int) (((4.0 * 60.0 + 15.0) / (double) CAR_COUNT)), TimeUnit.SECONDS);
        station.add(offset,);

        SplinedTrackSegment track = new TransportSegment("track", trackedRide, CoasterMathUtils.kmhToBpt(SPEED), CoasterMathUtils.kmhToBpt(1.3), CoasterMathUtils.kmhToBpt(SPEED), CoasterMathUtils.kmhToBpt(1.8));
        track.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track.blockSection(true);
        track.add(offset,);

        SplinedTrackSegment track2 = new TransportSegment("track2", trackedRide, CoasterMathUtils.kmhToBpt(SPEED), CoasterMathUtils.kmhToBpt(1.3), CoasterMathUtils.kmhToBpt(SPEED), CoasterMathUtils.kmhToBpt(1.8));
        track2.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0, 0);
        track2.setEjectType(TrackSegment.EjectType.EJECT_TO_SEAT);
        track2.blockSection(true);
        track2.add(new TrackSegment.DistanceListener(5.25, true) {
            @Override
            public void onTargetHit(@NotNull RideCar rideCar) {
                rideCar.eject();
            }
        });
        track2.add(offset,);


        station.setNextTrackSegmentRetroActive(track);
        trackedRide.addTrackSection(station);

        track.setNextTrackSegmentRetroActive(track2);
        trackedRide.addTrackSection(track);

        track2.setNextTrackSegmentRetroActive(station);
        trackedRide.addTrackSection(track2);
    }
}

