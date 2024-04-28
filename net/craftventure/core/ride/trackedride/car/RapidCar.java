package net.craftventure.core.ride.trackedride.car;

import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata;
import net.craftventure.core.ride.trackedride.RideCar;
import net.craftventure.core.ride.trackedride.TrackSegment;
import net.craftventure.core.ride.trackedride.TrackedRide;
import net.craftventure.core.ride.trackedride.TrackedRideHelper;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.ride.trackedride.segment.VerticalAutoLift;
import net.craftventure.core.utils.EntityUtils;
import net.craftventure.core.utils.InterpolationUtils;
import net.craftventure.core.utils.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class RapidCar extends RideCar {
//    private static boolean isLeft = true;

    //    private int update = 0;
    private final ArmorStand[] armorStands = new ArmorStand[8];
    private ArmorStand model;
    private float carYaw = 0;
    private final double deg40 = Math.toRadians(40);
    private final double deg50 = Math.toRadians(50);

    private Vector forwardVector = new Vector(0, 0, 0);
    private final Vector rightVector = new Vector(0, -1, 0);

    private final Random random = new Random();
    private long offsetTick = 0;
    private long offsetDuration = random.nextInt(20 * 3) + (20 * 5);
    private double startOffset = (random.nextDouble() * 2) - 1;
    private double endOffset = (random.nextDouble() * 2) - 1;
    private double rotationVector = 0;

    public RapidCar(String name) {
        super(name);
        this.length = 3;
        this.carFrontBogieDistance = length / 2;
        this.carRearBogieDistance = -carFrontBogieDistance;
//        startOffset = isLeft ? 1 : -1;
//        endOffset = isLeft ? 1 : -1;
//        isLeft = !isLeft;
    }

    @Override
    public boolean putPassenger(Player player) {
        for (int i = 0; i < armorStands.length; i++) {
            if (armorStands[i].getPassengers().isEmpty())
                player.teleport(armorStands[i]);
            if (armorStands[i].addPassenger(player))
                return true;
        }
        return false;
    }

    @Override
    public List<Entity> getMaxifotoPassengerList() {
        List<Entity> passengers = new ArrayList<>();
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            passengers.add(EntityUtils.INSTANCE.getFirstPassenger(armorStand));
        }
        return passengers;
    }

    @Override
    public List<Player> getPassengers() {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand != null && armorStand.getPassenger() instanceof Player) {
                players.add((Player) armorStand.getPassenger());
            }
        }
        return players;
    }

    @Override
    public int getPassengerCount() {
        int count = 0;
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand != null && armorStand.getPassenger() instanceof Player)
                count++;
        }
        return count;
    }

    @Override
    public boolean isEntitySeatOfCar(int entityId) {
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand != null && armorStand.getEntityId() == entityId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mount(int entityId, Player player) {
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand != null && armorStand.getEntityId() == entityId && armorStand.getPassenger() == null) {
                if (attachedTrain.canEnter()) {
                    armorStand.setPassenger(player);
                }
//                player.setSpectatorTarget(armorStand);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsPlayer(Player player) {
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand.getPassenger() == player) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUnmount(Player player) {
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand.getPassengers().contains(player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean unmount(Player player) {
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand.getPassengers().contains(player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void eject() {
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand != null) {
                List<Entity> passengers = armorStand.getPassengers();
                for (int i1 = 0; i1 < passengers.size(); i1++) {
                    Entity entity = passengers.get(i1);
                    if (entity != null) {
                        entity.leaveVehicle();
                        if (entity instanceof Player) {
                            getTrackSegment().getTrackedRide().onRideCompleted(((Player) entity), this);
                        }
                    }
                }
            }
        }
    }

    public ArmorStand[] getArmorStands() {
        return armorStands;
    }

    @Override
    public void spawn(Location spawnLocation) {
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand != null) {
                armorStand.remove();
            }
        }
        if (model != null)
            model.remove();

        setLocation(spawnLocation.toVector());
    }

    @Override
    public void move(Vector location, double trackYawRadian, double trackPitchRadian, double banking) {
        super.move(location, trackYawRadian, trackPitchRadian, banking);

        double clampedTrackPitchRadian = MathUtil.clampRadian(trackPitchRadian + (Math.PI * 0.5));
        if (clampedTrackPitchRadian > 0.12 && clampedTrackPitchRadian < 1.57079633 ||
                getTrackSegment().getClass() == StationSegment.class ||
                getTrackSegment().getClass() == VerticalAutoLift.class || (getVelocity() < 0.01 && getVelocity() > -0.01)) {
            rotationVector *= 0.965;
            if (rotationVector > 1.1) {
                rotationVector -= 1;
            } else if (rotationVector < -1.1) {
                rotationVector += 1;
            } /*else if (trackSegment.getClass() == StationSegment.class) {
                double targetAngle = RAD_45_DEG;
                double currentAngle = MathUtil.clampRadian(this.carYaw * MathUtil.DEGTORAD);
                if (currentAngle > RAD_45_DEG)
                    targetAngle = RAD_135_DEG;
                if (currentAngle > RAD_135_DEG)
                    targetAngle = RAD_225_DEG;
                if (currentAngle > RAD_225_DEG)
                    targetAngle = RAD_315_DEG;
                rotationVector = 1;
                if (currentAngle + (rotationVector * MathUtil.DEGTORAD) > targetAngle) {
                    rotationVector = 0;
                    this.carYaw = (float) Math.toDegrees(targetAngle) - 0.001f;
                }
//                Logger.console("rotationVector = " + rotationVector + ", targetAngle = " + targetAngle + ", currentAngle = " + currentAngle);
            }*/
        } else {
            rotationVector -= MathUtil.deltaRadian(trackYawRadian - (Math.PI * 0.5), this.yawRadian) * 7;
            rotationVector *= 0.987;
            if (rotationVector > 0.003) {
                rotationVector -= 0.002;
            } else if (rotationVector < -0.003) {
                rotationVector += 0.002;
            }
        }
        if (offsetTick >= offsetDuration) {
            offsetDuration = random.nextInt(20 * 3) + (20 * 5);
            offsetTick = 0;
            startOffset = endOffset;
            endOffset = (random.nextDouble() * 2) - 1;
        }
        double offsetT = offsetTick / (double) offsetDuration;
        double offset = InterpolationUtils.linearInterpolate(startOffset, endOffset, offsetT);
        offsetTick++;
//        Logger.console("TrackPitch " + trackPitch);
        this.getLocation().setX(location.getX());
        this.getLocation().setY(location.getY());
        this.getLocation().setZ(location.getZ());

        double workYaw = (trackYawRadian) + (Math.PI * 0.5);
        double workPitch = trackPitchRadian + (Math.PI * 0.5);

        forwardVector = MathUtil.setYawPitchRadians(forwardVector, workYaw, workPitch);
        forwardVector.multiply(-1);
        forwardVector.setY(forwardVector.getY() * -1);
        forwardVector.normalize();
//        forwardVector.multiply(rowOffset);
        rightVector.setX(0);
        rightVector.setY(-1);//banking);
        rightVector.setZ(0);
        rightVector.crossProduct(forwardVector);
        rightVector.normalize();
        rightVector.multiply(offset * banking);

//        Logger.consoleAndIngame(forwardVector.getX() + ", " + forwardVector.getY() + ", " + forwardVector.getZ());
//        Logger.consoleAndIngame(rightVector.toString());
//        Logger.consoleAndIngame(" ");

        double xOffset = rightVector.getX();
        double yOffset = rightVector.getY();
        double zOffset = rightVector.getZ();

        trackYawRadian -= Math.PI * 0.5;

//        this.carYaw += 1;
        double correctedTrackPitchRadian = trackPitchRadian - 4.71238898038469;
        boolean add40 = true;
        double oldCarYaw = this.carYaw;
        this.carYaw += rotationVector;
        double curAngle = Math.toRadians(this.carYaw) + Math.toRadians(-20);
        int seatAngle = 90;
        for (int i = 0; i < armorStands.length; i++) {
            if (armorStands[i] == null || !armorStands[i].isValid()) {
                Location spawnLocation = new Location(
                        Bukkit.getWorld("world"),
                        location.getX() + (Math.cos(curAngle) * 1.3 + xOffset),
                        location.getY() + (Math.sin(trackYawRadian - curAngle) * correctedTrackPitchRadian) - 1.1 + yOffset,
                        location.getZ() + (Math.sin(curAngle) * 1.3 + zOffset),
                        this.carYaw + seatAngle,//(float) Math.toDegrees(workYaw),
                        0);
                armorStands[i] = Bukkit.getWorld("world").spawn(spawnLocation, ArmorStand.class);
                TrackedRideHelper.setCarEntity(armorStands[i], this);
                TypedInstanceOwnerMetadata.Companion.setOwner(armorStands[i], getTrackedRide());
                armorStands[i].setPersistent(false);
                armorStands[i].setCustomName(getName());
                armorStands[i].setGravity(false);
                armorStands[i].setVisible(false);
                armorStands[i].addDisabledSlots(EquipmentSlot.values());
            } else {
                EntityUtils.INSTANCE.teleport(
                        armorStands[i],
                        location.getX() + (Math.cos(curAngle) * 1.3 + xOffset),
                        location.getY() + (Math.sin(trackYawRadian - curAngle) * correctedTrackPitchRadian) - 1.1 + yOffset,
                        location.getZ() + (Math.sin(curAngle) * 1.3 + zOffset),
                        this.carYaw + seatAngle,
                        0);
            }
            curAngle += add40 ? deg40 : deg50;
            add40 = !add40;
            if (add40)
                seatAngle += 90;
        }

        if (model == null || !model.isValid()) {
            Location spawnLocation = new Location(
                    Bukkit.getWorld("world"),
                    location.getX() + xOffset,
                    location.getY() - 1 + yOffset,
                    location.getZ() + zOffset,
                    (float) (Math.toDegrees(trackYawRadian) + 90),//this.carYaw,//(float) Math.toDegrees(workYaw),
                    0);
            model = Bukkit.getWorld("world").spawn(spawnLocation, ArmorStand.class);
            TrackedRideHelper.setCarModelEntity(model, this);
            model.setPersistent(false);
            TypedInstanceOwnerMetadata.Companion.setOwner(model, getTrackedRide());
            model.setCustomName(getName());
            model.setGravity(false);
            model.setHelmet(MaterialConfig.INSTANCE.getRAPID().clone());
            model.setVisible(false);
            model.addDisabledSlots(EquipmentSlot.values());
        } else {
            EntityUtils.INSTANCE.teleport(
                    model,
                    location.getX() + xOffset,
                    location.getY() - 1 + yOffset,
                    location.getZ() + zOffset,
                    (float) (Math.toDegrees(trackYawRadian) + 90),//(float) Math.toDegrees(trackYawRadian),//this.carYaw,//this.carYaw,
                    0f);
            model.setHeadPose(new EulerAngle(0, Math.toRadians(oldCarYaw) - trackYawRadian, correctedTrackPitchRadian));
//            modelPoseUpdater.setHeadPoseWithPacket(model, 0, Math.toRadians(oldCarYaw) - trackYawRadian, correctedTrackPitchRadian);
        }

        this.yawRadian = trackYawRadian;
        this.pitchRadian = trackPitchRadian;
    }

    @Override
    public void onTrackSegmentChanged(TrackSegment oldSegment, TrackSegment newSegment) {
        super.onTrackSegmentChanged(oldSegment, newSegment);
//        Logger.console("TrackSegment changed");
    }

    @Override
    public void despawn() {
        for (int i = 0; i < armorStands.length; i++) {
            ArmorStand armorStand = armorStands[i];
            if (armorStand != null) {
                armorStand.remove();
            }
        }
        model.remove();
    }

    @NotNull
    @Override
    public Json toJson() {
        return null;
    }

    public static class Json extends RideCar.Json {
        @NotNull
        @Override
        public RideCar create(@NotNull TrackedRide ride) {
            return new RapidCar(ride.getName());
        }
    }
}
