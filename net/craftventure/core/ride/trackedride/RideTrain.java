package net.craftventure.core.ride.trackedride;

import net.craftventure.audioserver.api.AudioServerApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public abstract class RideTrain {
    public static final String KEY_TRAIN = "trackedRideTrain";
    protected List<RideCar> cars = new ArrayList<>();
    protected double frontCarDistance = 0;
    protected boolean canEnter = false;
    //    private int circuitCount = 0;
    @Deprecated
    @Nullable
    private TrackSegment frontCarTrackSegment = null;
    @Deprecated
    @Nullable
    private TrackSegment lastCarTrackSegment = null;
    private TrackedRide trackedRide;

    private final int trainId;
    private static int TRAIN_ID = 0;

    synchronized public static int getNextTrainId() {
        return TRAIN_ID++;
    }

    private String audioName;
    private Long sync;
    private boolean isEjecting = false;
    @Nullable
    private SpatialTrainSounds spatialTrainSounds;

    private final boolean hasScreamed = true;
    private final double lastSpeed = 0.0;
    @Nullable
    private Double targetSpeed = null;
    @Nullable
    private UpdateListener updateListener;

    public RideTrain(TrackSegment frontCarTrackSegment) {
        this.frontCarTrackSegment = frontCarTrackSegment;
        this.trainId = TRAIN_ID++;
    }

    public void setUpdateListener(@Nullable UpdateListener updateListener) {
        this.updateListener = updateListener;
    }

    public boolean putPassenger(Player player) {
        for (int i = 0; i < cars.size(); i++) {
            if (cars.get(i).putPassenger(player)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Double getTargetSpeed() {
        return targetSpeed;
    }

    public void setTargetSpeed(@Nullable Double targetSpeed) {
        this.targetSpeed = targetSpeed;
    }

    public int getTrainId() {
        return trainId;
    }

    public TrackedRide getTrackedRide() {
        return trackedRide;
    }

    public RideTrain setTrainSoundName(String trainSound, SpatialTrainSounds.Settings settings) {
//        if (!trainSound.equals("fenrir")) return this;
        if (spatialTrainSounds != null) {
            spatialTrainSounds.stop();
        }
        spatialTrainSounds = new SpatialTrainSounds(trainSound, this, settings);
        spatialTrainSounds.start();
        return this;
    }

    public boolean hasTrainSound() {
        return spatialTrainSounds != null;
    }

    public void moveTo(@Nullable TrackSegment trackSegment, double distance) {
//        distance = Math.max(Math.min(trackSegment.getLength(), distance), 0);
        if (trackSegment != null && trackSegment != this.frontCarTrackSegment) {
            this.frontCarDistance = distance;
            this.frontCarTrackSegment = trackSegment;
        }
    }

    public void stopOnboardSynchronizedAudio() {
        this.audioName = null;
        this.sync = null;
    }

    public void cancelAudio() {
        this.audioName = null;
        this.sync = 0L;
        pauzeOnboardSynchronizedAudio();
    }

    public void pauzeOnboardSynchronizedAudio() {
        if (audioName == null) return;
        for (Player player : getPassengers()) {
            AudioServerApi.INSTANCE.remove(audioName, player);
        }
    }

    public void setOnboardSynchronizedAudio(String audioName, long sync) {
        this.audioName = audioName;
        this.sync = sync;

        for (Player player : getPassengers()) {
            AudioServerApi.INSTANCE.addAndSync(audioName, player, sync);//now - 5000);
        }
    }

    public void triggerResync(long syncTarget, long margin) {
        long currentSync = System.currentTimeMillis() - sync;
        if (currentSync > 0) {
            if (currentSync > syncTarget + margin || currentSync < syncTarget - margin) {
//                Logger.info("Triggering a resync of onboard audio of %s", false, getTrackedRide().getName());
                for (Player player : getPassengers()) {
                    AudioServerApi.INSTANCE.addAndSync(audioName, player, System.currentTimeMillis() - syncTarget);
                }
            }
        }
    }

    public String getAudioName() {
        return audioName;
    }

    @Nullable
    public Long getSync() {
        return sync;
    }

    public void setTrackedRide(TrackedRide trackedRide) {
        this.trackedRide = trackedRide;
        for (RideCar rideCar : cars) {
            rideCar.setTrackedRide(trackedRide);
        }
    }

    public void onTrackSegmentChangedForRideCar(RideCar rideCar, TrackSegment oldSegment, TrackSegment newSegment) {
        if (rideCar == cars.get(cars.size() - 1)) {
            lastCarTrackSegment = newSegment;
        }
    }

    @Deprecated
    public TrackSegment getLastCarTrackSegment() {
        return lastCarTrackSegment;
    }

    @Deprecated
    public void setLastCarTrackSegment(TrackSegment lastCarTrackSegment) {
        this.lastCarTrackSegment = lastCarTrackSegment;
    }

    public void addCar(RideCar car) {
        car.attachTrain(this);
        cars.add(car);
        car.setTrackedRide(trackedRide);
    }

    public double getLength() {
        double length = 0;
        for (int i = 0; i < cars.size(); i++) {
            RideCar car = cars.get(i);
            length += car.length;
        }
        return length;
    }

    public boolean canEnter() {
        return canEnter;
    }

    public int getPassengerCount() {
        int count = 0;
        for (int i = 0; i < cars.size(); i++) {
            RideCar car = cars.get(i);
            count += car.getPassengerCount();
        }
        return count;
    }

    public void halt() {
        for (int i = 0; i < cars.size(); i++) {
            RideCar car = cars.get(i);
            car.setVelocity(0.0);
            car.setAcceleration(0.0);
        }
    }

    public void eject() {
        isEjecting = true;
        for (RideCar rideCar : cars) {
            rideCar.eject();
        }
        isEjecting = false;
    }

    public boolean isEjecting() {
        return isEjecting;
    }

    public void setCanEnter(boolean canEnter) {
        if (this.canEnter != canEnter) {
//            Logger.console("Can enter %s", canEnter);
            this.canEnter = canEnter;
        }
    }

    public boolean isEntitySeatOfTrain(int entityId) {
        for (int i = 0; i < cars.size(); i++) {
            RideCar car = cars.get(i);
            if (car.isEntitySeatOfCar(entityId)) {
                return true;
            }
        }
        return false;
    }

    public boolean mountToSeat(int entityId, Player player) {
        for (int i = 0; i < cars.size(); i++) {
            RideCar car = cars.get(i);
            if (car.mount(entityId, player)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPassengers() {
        for (int i = 0; i < cars.size(); i++) {
            RideCar rideCar = cars.get(i);
            if (rideCar.hasPassengers())
                return true;
        }
        return false;
    }

    public List<Player> getPassengers() {
        List<Player> players = new ArrayList<>(cars.size());
        for (int i = 0; i < cars.size(); i++) {
            RideCar rideCar = cars.get(i);
            players.addAll(rideCar.getPassengers());
        }
        return players;
    }

    public double getFrontCarDistance() {
        return frontCarDistance;
    }

    @Deprecated
    public TrackSegment getFrontCarTrackSegment() {
        return frontCarTrackSegment;
    }

    @Deprecated
    public void setFrontCarTrackSegment(@Nullable TrackSegment frontCarTrackSegment) {
        this.frontCarTrackSegment = frontCarTrackSegment;
    }

    public void spawn() {
        TrackSegment segment = frontCarTrackSegment;
        Vector location = new Vector();
        double distance = frontCarDistance;

        for (int i = 0; i < cars.size(); i++) {
            RideCar car = cars.get(i);
            while (distance >= segment.getLength()) {
                distance -= segment.getLength();
                segment = segment.getNextTrackSegment();
            }
            while (distance < 0) {
                segment = segment.getPreviousTrackSegment();
                distance += segment.getLength();
            }

            segment.getPosition(distance, location);
            car.setVelocity(car.getVelocity() + car.getAcceleration());
            car.setTrackSegmentAndDistance(segment, distance);
            car.spawn(location.toLocation(Bukkit.getWorld("world")));
            distance -= car.length;
        }
    }

    public List<RideCar> getCars() {
        return cars;
    }

    public int getCarCount() {
        return cars.size();
    }

    public void onUpdated() {
    }

    public void onUpdateAsync() {
        if (spatialTrainSounds != null) {
            spatialTrainSounds.update();
        }
    }

    public void move(TrackSegment newSegment, double frontCarDistance) {
        if (updateListener != null) {
            updateListener.onUpdate(this);
        }
    }

    public void despawn() {
        for (RideCar car : cars) {
            car.despawn();
        }
        if (spatialTrainSounds != null)
            spatialTrainSounds.stop();
        spatialTrainSounds = null;
    }

    public double getVelocity() {
        double velocity = 0;
        for (int i = 0; i < cars.size(); i++) {
            RideCar rideCar = cars.get(i);
            velocity += rideCar.getVelocity();
        }
        return velocity / (double) cars.size();
    }

    public double getAcceleration() {
        double acceleration = 0;
        for (int i = 0; i < cars.size(); i++) {
            RideCar rideCar = cars.get(i);
            acceleration += rideCar.getAcceleration();
        }
        return acceleration / (double) cars.size();
    }

    @NotNull
    public abstract RideTrainJson toJson();

    @NotNull
    public <T extends RideTrainJson> T toJson(T source) {
        return source;
    }

    public interface UpdateListener {
        void onUpdate(@Nonnull RideTrain rideTrain);
    }
}
