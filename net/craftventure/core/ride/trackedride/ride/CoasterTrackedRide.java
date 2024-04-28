package net.craftventure.core.ride.trackedride.ride;

import net.craftventure.bukkit.ktx.area.Area;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.ride.trackedride.RideCar;
import net.craftventure.core.ride.trackedride.RideTrain;
import net.craftventure.core.ride.trackedride.TrackSegment;
import net.craftventure.core.ride.trackedride.TrackedRide;
import net.craftventure.core.ride.trackedride.config.TrackedRideConfig;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class CoasterTrackedRide extends TrackedRide {
    private long lastUpdate = System.currentTimeMillis();

    public CoasterTrackedRide(String name, Area area, Location exitLocation, String achievementName, String rideName) {
        super(name, area, exitLocation, achievementName, rideName);
    }

    @Override
    public void update() {
        int updateCount = 0;
        int tickTime = CraftventureCore.getSettings().getCoasterTickTime();
//        int tickTimeMargin = CraftventureCore.getSettings().getCoasterTickTimeMargin();
        while (updateCount == 0 || lastUpdate + tickTime < System.currentTimeMillis()) {
//            lastUpdate = System.currentTimeMillis();
            lastUpdate += tickTime;
            updateCount++;
            super.update();
            for (int i = 0; i < trackSegments.size(); i++) {
                TrackSegment segment = trackSegments.get(i);
                segment.update();
            }
            for (int i = 0; i < rideTrains.size(); i++) {
                RideTrain train = rideTrains.get(i);
                List<RideCar> cars = train.getCars();

//                if (CraftventureCore.getEnvironment() == CraftventureCore.Environment.DEVELOPMENT) {
//                    for (Player player : train.getPassengers()) {
//                        MessageBarManager.display(player,
//                                ChatUtils.INSTANCE.createComponent(String.format("Speed %.2f km/h @ %s", CoasterMathUtils.bptToKmh(train.getVelocity()), train.getFrontCarTrackSegment().getDisplayName()), ChatColor.YELLOW),
//                                MessageBarManager.Type.RIDE,
//                                TimeUtils.secondsFromNow(1),
//                                ChatUtils.INSTANCE.getID_RIDE());
//                    }
//                }

                double previousDistance = train.getFrontCarDistance();
                double currentDistance = previousDistance;

                for (int i1 = 0; i1 < cars.size(); i1++) {
                    RideCar car = cars.get(i1);
                    car.getTrackSegment().applyForces(car, currentDistance - previousDistance);
                }
                // TODO: Improve this temporary workaround for forces
                for (int i1 = 0; i1 < cars.size(); i1++) {
                    RideCar car = cars.get(i1);
                    car.getTrackSegment().applySecondaryForces(car, currentDistance - previousDistance);
                }
                for (int i1 = 0; i1 < cars.size(); i1++) {
                    RideCar car = cars.get(i1);
                    car.setVelocity(car.getVelocity() + car.getAcceleration());
                }
                for (int i1 = 0; i1 < cars.size(); i1++) {
                    RideCar car = cars.get(i1);
                    car.getTrackSegment().applyForceCheck(car, currentDistance, previousDistance);
                }

                double velocity = train.getVelocity();
//            Logger.console("Velocity " + velocity);
                TrackSegment segment = train.getFrontCarTrackSegment();

                currentDistance += velocity;

                RideCar firstCar = train.getCars().get(0);
                RideCar lastCar = train.getCars().get(train.getCars().size() - 1);

                TrackSegment firstCarSegment = firstCar.getTrackSegment();
                TrackSegment lastCarSegment = lastCar.getTrackSegment();

                while (currentDistance >= segment.getLength()) {
                    if (segment.isBlockSection() && !segment.canAdvanceToNextBlock(train, false) ||
                            !segment.canLeaveSection(train)) {
                        currentDistance = segment.getLength();
                        break;
                    }
                    currentDistance -= segment.getLength();
//                segment.onTrainLeftSection(train);
                    segment = segment.getNextTrackSegment();
//                segment.onTrainEnteredSection(train);
                }
                while (currentDistance < 0) {
//                segment.onTrainLeftSection(train);
                    segment = segment.getPreviousTrackSegment();
//                segment.onTrainEnteredSection(train);
                    currentDistance += segment.getLength();
                }

                List<RideCar> cars3 = train.getCars();
                for (int i1 = 0; i1 < cars3.size(); i1++) {
                    RideCar car = cars3.get(i1);
                    car.getTrackSegment().onDistanceUpdated(car, currentDistance, previousDistance);
                }

                train.move(segment, currentDistance);

                if (train.getVelocity() >= 0) { // Train going forwards
                    if (lastCarSegment != train.getCars().get(train.getCars().size() - 1).getTrackSegment())
                        lastCarSegment.onTrainLeftSection(train);

                    if (firstCarSegment != firstCar.getTrackSegment())
                        firstCar.getTrackSegment().onTrainEnteredSection(firstCarSegment, train);
                } else { // Train going backwards
                    if (firstCarSegment != train.getCars().get(0).getTrackSegment())
                        firstCarSegment.onTrainLeftSection(train);

                    if (lastCarSegment != lastCar.getTrackSegment())
                        lastCar.getTrackSegment().onTrainEnteredSection(lastCarSegment, train);
                }
            }
            if (updateCount > 200) {
                Logger.warn("Skipping coaster frames for " + getName() + " because 200 updates were triggered this round");
                break;
            }
        }
//        if (updateCount != 1) {
//            Logger.console("Updated " + getName() + " " + updateCount + " times");
//        }
    }

    @NotNull
    public TrackedRideConfig toJson() {
        TrackedRideConfig config = new TrackedRideConfig();
        return toJson(config);
    }

    @NotNull
    public <T extends TrackedRideConfig> T toJson(T source) {
        return source;
    }
}
