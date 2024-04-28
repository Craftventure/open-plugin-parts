package net.craftventure.core.ride.trackedride.train;

import net.craftventure.core.CraftventureCore;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.ride.trackedride.RideCar;
import net.craftventure.core.ride.trackedride.RideTrain;
import net.craftventure.core.ride.trackedride.RideTrainJson;
import net.craftventure.core.ride.trackedride.TrackSegment;
import net.craftventure.core.utils.LookAtUtil;
import net.craftventure.core.utils.ParticleSpawnerKt;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;


public class CoasterRideTrain extends RideTrain {

    private final LookAtUtil.YawPitch yawPitch = new LookAtUtil.YawPitch();
    private final Vector location = new Vector(); // Used for calculations
    private final Vector locationYawPitch = new Vector(); // Used for calculations
    private final Vector normalizedDirection = new Vector(); // Used for calculations
    private MovementHandler movementHandler;

    public CoasterRideTrain(TrackSegment frontCarTrackSegment, double frontCarDistance) {
        super(frontCarTrackSegment);
        this.frontCarDistance = frontCarDistance;
    }

    public void setMovementHandler(MovementHandler movementHandler) {
        this.movementHandler = movementHandler;
    }

    public void getLocation(TrackSegment segment, double currentDistance, double delta, Vector out) {
        if (delta == 0) {
            segment.getPosition(currentDistance, out);
            return;
        }
        double backCarDistance = currentDistance + delta;
        while (backCarDistance < 0) {
            segment = segment.getPreviousTrackSegment();
            backCarDistance += segment.getLength();
        }
        while (backCarDistance >= segment.getLength()) {
            backCarDistance -= segment.getLength();
            segment = segment.getNextTrackSegment();
        }
        segment.getPosition(backCarDistance, out);
    }

    @Override
    public void move(TrackSegment newSegment, double frontCarDistance) {
        super.move(newSegment, frontCarDistance);
        if (frontCarDistance > newSegment.getLength())
            Logger.severe("CoasterRideTrain:move: Distance is higher than the segments length");

        this.frontCarDistance = frontCarDistance;
        setFrontCarTrackSegment(newSegment);

        TrackSegment segment = newSegment;
        double distance = this.frontCarDistance;

        World world = Bukkit.getWorlds().get(0);
        for (int i = 0; i < cars.size(); i++) {
            RideCar car = cars.get(i);
            while (distance < 0) {
                segment = segment.getPreviousTrackSegment();
                distance += segment.getLength();
            }
            double yaw = 0;
            double pitch = 0;

            getLocation(segment, distance, car.carFrontBogieDistance, location);
            if (car.carFrontBogieDistance <= car.carRearBogieDistance) {
                getLocation(segment, distance, car.carFrontBogieDistance - 0.1, locationYawPitch);
//                Logger.info("INNNNNN %s", false, getTrackedRide().getName());
            } else {
                getLocation(segment, distance, car.carRearBogieDistance, locationYawPitch);
//                Logger.info("CORRRRR %s", false, getTrackedRide().getName());
            }

            LookAtUtil.getYawPitchFromRadian(locationYawPitch, this.location, yawPitch);
            yaw = yawPitch.getYaw();
            pitch = yawPitch.getPitch();

            if (movementHandler != null) {
                yaw = movementHandler.handleYawRadian(yaw, car);
                pitch = movementHandler.handlePitchRadian(pitch, car);
            }

            car.setTrackSegmentAndDistance(segment, distance);
            boolean shouldCalculateFrontCarBogie = car.carFrontBogieDistance != 0;
            boolean debugBogies = CraftventureCore.getSettings().isDebugCoasterBogies();
            if (shouldCalculateFrontCarBogie || debugBogies) {
                normalizedDirection.setX(location.getX() - locationYawPitch.getX());
                normalizedDirection.setY(location.getY() - locationYawPitch.getY());
                normalizedDirection.setZ(location.getZ() - locationYawPitch.getZ());
                normalizedDirection.normalize();
//                Logger.debug(VectorExtensionsKt.asString(normalizedDirection));

//                double length = car.getCarFrontBogieDistance() - car.getCarRearBogieDistance();
//                double percentage = car.getCarFrontBogieDistance() / length;
//                normalizedDirection.multiply(-percentage);
                normalizedDirection.multiply(-car.carFrontBogieDistance);

                if (debugBogies) {
                    ParticleSpawnerKt.spawnParticleX(world,
                            Particle.REDSTONE,
                            location.getX() + normalizedDirection.getX(),
                            location.getY() + normalizedDirection.getY(),
                            location.getZ() + normalizedDirection.getZ(),
                            1,
                            0.0, 0.0, 0.0,
                            0.0,
                            new Particle.DustOptions(Color.fromRGB(0x42f4e5), 1.0f));
                }

//                normalizedDirection.multiply(-car.getCarFrontBogieDistance());
                if (shouldCalculateFrontCarBogie)
                    location.add(normalizedDirection);

//                getLocation(segment, distance, 0, location);
            }

            if (debugBogies) {
                getLocation(segment, distance, car.carFrontBogieDistance, normalizedDirection);
                // Front (brown)
                ParticleSpawnerKt.spawnParticleX(world,
                        Particle.REDSTONE,
                        normalizedDirection.getX(),
                        normalizedDirection.getY(),
                        normalizedDirection.getZ(),
                        1,
                        0.0, 0.0, 0.0,
                        0.0,
                        new Particle.DustOptions(Color.fromRGB(0xffb14c), 1.0f));

                getLocation(segment, distance, car.carRearBogieDistance, normalizedDirection);
                // Back (purple)
                ParticleSpawnerKt.spawnParticleX(world,
                        Particle.REDSTONE,
                        normalizedDirection.getX(),
                        normalizedDirection.getY(),
                        normalizedDirection.getZ(),
                        1,
                        0.0, 0.0, 0.0,
                        0.0,
                        new Particle.DustOptions(Color.fromRGB(0xff8ef7), 1.0f));
            }

            double banking = segment.getBanking(distance);
            if (movementHandler != null) {
                banking = movementHandler.handleBanking(banking, car);
            }
            car.move(location, segment.transformYaw(yaw), segment.transformPitch(pitch), banking);

            distance -= car.length;
        }
    }

    @NotNull
    public RideTrainJson toJson() {
        return toJson(new CoasterRideTrainJson());
    }

    @NotNull
    public <T extends RideTrainJson> T toJson(T source) {
        return source;
    }

    public interface MovementHandler {
        double handleYawRadian(double yaw, @Nonnull RideCar car);

        double handlePitchRadian(double pitch, @Nonnull RideCar car);

        default double handleBanking(double bankingDegrees, @Nonnull RideCar car) {
            return bankingDegrees;
        }
    }
}
