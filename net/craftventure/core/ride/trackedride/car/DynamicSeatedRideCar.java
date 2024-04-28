package net.craftventure.core.ride.trackedride.car;

import net.craftventure.bukkit.ktx.entitymeta.Meta;
import net.craftventure.bukkit.ktx.util.PermissionChecker;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.protocol.packet.PacketEquipment;
import net.craftventure.core.ride.trackedride.RideCar;
import net.craftventure.core.ride.trackedride.car.effect.Effect;
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat;
import net.craftventure.core.ride.trackedride.car.seat.Seat;
import net.craftventure.core.utils.EntityUtils;
import net.craftventure.core.utils.MathUtil;
import net.craftventure.core.utils.ParticleSpawnerKt;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class DynamicSeatedRideCar extends RideCar {

    //    private Matrix4x4 matrix4x4 = new Matrix4x4();
//    private Vector3 locationVector = new Vector3();
//    private Quaternion calculationQuaternion = new Quaternion();
    private Vector forwardVector = new Vector(0, 0, 0);
    private final Vector rightVector = new Vector(0, -1, 0);
    private final Vector upVector = new Vector(0, -1, 0);
    private final Vector bankingVector = new Vector(-1, 0, 0);

    private MoveListener moveListener;

    private boolean updateAllCarts = true;
    private double lastVelocity = 0;
    private final List<Seat> seats = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();
    private FakeSeatProvider fakeSeatProvider;

    public DynamicSeatedRideCar(String name, double length) {
        super(name);
        this.length = length;
    }

    @Override
    public boolean putPassenger(Player player) {
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).putPassenger(player)) {
                putInCar(player, i);
                return true;
            }
        }
        return false;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    /**
     * @deprecated It's better to construct the car yourself
     */
    @Deprecated
    public static DynamicSeatedRideCar fromLegacyFormat(String name, int thisCarCount, int thisRowCount, double thisOffset, double thisRowOffset, double length) {
        return fromLegacyFormat(name, thisCarCount, thisRowCount, thisOffset, thisRowOffset, length, 0.5);
    }

    /**
     * @deprecated It's better to construct the car yourself
     */
    @Deprecated
    public static DynamicSeatedRideCar fromLegacyFormat(String name, int thisCarCount, int thisRowCount, double thisOffset, double thisRowOffset, double length, double yOffset) {
//        Logger.debug(String.format("Car: %s", name));
        DynamicSeatedRideCar carBuilder = new DynamicSeatedRideCar(name, length);
        carBuilder.carRearBogieDistance = -length;
        double sideOffset = (thisOffset * ((double) thisCarCount - 1d) * 0.5);

        for (int row = 0; row < thisRowCount; row++) {
            for (int car = 0; car < thisCarCount; car++) {
//                int index = (row * thisCarCount) + car;
                double offset = sideOffset - (thisOffset * (double) car);

                double x = -offset;
                double y = yOffset;
                double z = -(thisRowOffset * row);

//                Logger.info("row=%d car=%d index=%d offset=%2.2f sideoffset=%2.2f %2.2f %2.2f %2.2f", false, row, car, index, offset, sideOffset, x, y, z);

//                Logger.debug(String.format("new ArmorStandSeat(%f, %f, %f, true, %s)", x, y, z, name));
                ArmorStandSeat seat = new ArmorStandSeat(x, y, z, true, name);
                carBuilder.addSeat(seat);
            }
        }
        return carBuilder;
    }

    public void setFakeSeatProvider(FakeSeatProvider fakeSeatProvider) {
        this.fakeSeatProvider = fakeSeatProvider;
    }

    public Seat<?> getSeat(int seat) {
        return seats.get(seat);
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    public void addEffect(Effect effect) {
        effects.add(effect);
    }

    public void setMoveListener(MoveListener moveListener) {
        this.moveListener = moveListener;
    }

    @Override
    public boolean addPassenger(@NotNull Player player, @NotNull Entity entity) {
        try {
            Seat rideCar = Meta.getEntityMeta(entity, Meta.createTempKey(ArmorStandSeat.KEY_SEAT));
            if (rideCar != null && rideCar.getPermission() != null && !player.hasPermission(rideCar.getPermission())) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.addPassenger(player, entity);
    }

    @NotNull
    @Override
    public List<Entity> getMaxifotoPassengerList() {
        List<Entity> passengers = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            if (seat.isPassengerCar()) {
                passengers.add(EntityUtils.INSTANCE.getFirstPassenger(seat.getEntity()));
            }
        }
        return passengers;
    }

    @NotNull
    @Override
    public List<Player> getPassengers() {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            List passengers = seat.getPassengers();
            for (int i1 = 0; i1 < passengers.size(); i1++) {
                Object entity = passengers.get(i1);
                if (entity instanceof Player) {
                    players.add((Player) entity);
                }
            }
        }
        return players;
    }

    @Override
    public int getPassengerCount() {
        int count = 0;
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            List passengers = seat.getPassengers();
            for (int i1 = 0; i1 < passengers.size(); i1++) {
                Object entity = passengers.get(i1);
                if (entity instanceof Player)
                    count++;
            }
        }
        return count;
    }

    @Override
    public boolean isEntitySeatOfCar(int entityId) {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            if (seat.isEntity(entityId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mount(int entityId, Player player) {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            if (seat.isEntity(entityId)) {
                boolean canEnter = attachedTrain.canEnter();
                if (!canEnter && PermissionChecker.INSTANCE.isCrew(player)) {
                    player.sendMessage(Component.text("Crew car enter permission granted (guest would not be able to enter)", CVTextColor.getServerNotice()));
                    canEnter = true;
                }
                if (canEnter) {
                    if (seat.getPermission() != null && !player.hasPermission(seat.getPermission())) {
                        return false;
                    }
                    if (seat.putPassenger(player)) {
                        putInCar(player, i);
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void enteredCar(Player player, Entity entity) {
        super.enteredCar(player, entity);
//        Logger.debug("enteredCar");
        onSendFakeSeat(player, entity);
    }

    @Override
    public void exitedCar(Player player, Entity entity) {
        super.exitedCar(player, entity);
//        Logger.debug("exitedCar");
        onResetFakeSeat(player, entity);
    }

    @Nullable
    private Integer getEntitySeatId(Entity entity) {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            if (seat.isEntity(entity.getEntityId())) {
                return i;
            }
        }
        return null;
    }

    private void onSendFakeSeat(Player player, Entity seat) {
        Integer seatId = getEntitySeatId(seat);
//        Logger.debug("Sending fake seat %s", false, seatId);
        if (seatId == null) return;
        onSendFakeSeat(player, seatId);
    }

    private void onSendFakeSeat(Player player, int seatId) {
        if (fakeSeatProvider != null) {
            for (int s = 0; s < seats.size(); s++) {
                Seat indexSeat = seats.get(s);
                ItemStack itemStack = fakeSeatProvider.dress(seatId, s);
                if (itemStack != null) {
                    setHelmetForSeatForPlayer(itemStack, indexSeat, player);
                }
            }
        }
    }

    private void onResetFakeSeat(Player player, Entity seat) {
        Integer seatId = getEntitySeatId(seat);
//        Logger.debug("Resetting fake seat %s", false, seatId);
        if (seatId == null) return;
        onResetFakeSeat(player, seatId);
    }

    private void onResetFakeSeat(Player player, int seatId) {
        if (fakeSeatProvider != null) {
            for (int s = 0; s < seats.size(); s++) {
                Seat indexSeat = seats.get(s);
                ItemStack itemStack = fakeSeatProvider.dress(seatId, s);
                if (itemStack != null) {
                    setHelmetForSeatForPlayer(null, indexSeat, player);
                }
            }
        }
    }

    private void putInCar(Player player, int seatIndex) {
//        getTrackSegment().onPlayerEnteredCarOnSegment(this, player);
    }

    @Override
    public boolean containsPlayer(Player player) {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            if (seat.getPassengers().contains(player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUnmount(Player player) {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            if (seat.getPassengers().contains(player)) {
//                Logger.console("CANUNMOUNT!!!");
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean unmount(Player player) {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            if (seat.isPassengerCar() && seat.getPassengers().contains(player)) {
                return true;
            }
        }
        return false;
    }

    private void setHelmetForSeatForPlayer(@Nullable ItemStack helmet, Seat seat, Player player) {
        Entity entity = seat.getEntity();
        if (entity != null) {
            if (helmet != null) {
                new PacketEquipment(entity.getEntityId(), PacketEquipment.Slot.HEAD, helmet).sendPlayer(player);
            } else if (entity instanceof ArmorStand) {
                ArmorStand armorStand = (ArmorStand) entity;
                new PacketEquipment(entity.getEntityId(), PacketEquipment.Slot.HEAD, armorStand.getHelmet()).sendPlayer(player);
            } else if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                new PacketEquipment(entity.getEntityId(), PacketEquipment.Slot.HEAD, livingEntity.getEquipment().getHelmet()).sendPlayer(player);
            }
        }
    }

    @Override
    public void eject() {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            List passengers = seat.getPassengers();
            for (int i1 = 0; i1 < passengers.size(); i1++) {
                Object object = passengers.get(i1);
                if (object instanceof Entity) {
                    Entity entity = (Entity) object;
                    entity.leaveVehicle();
                    if (entity instanceof Player) {
                        getTrackSegment().getTrackedRide().onRideCompleted(((Player) entity), this);
                    }
                }
            }
        }
    }

    @Override
    public void spawn(Location spawnLocation) {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            seat.despawn();
        }

        setLocation(spawnLocation.toVector());
    }

    private double calculateX(Vector rightVector, Vector upVector, Vector forwardVector, double right, double up, double forward) {
        return (rightVector.getX() * right) + (upVector.getX() * up) + (forwardVector.getX() * forward);
    }

    private double calculateY(Vector rightVector, Vector upVector, Vector forwardVector, double right, double up, double forward) {
        return (rightVector.getY() * right) + (upVector.getY() * up) + (forwardVector.getY() * forward);
    }

    private double calculateZ(Vector rightVector, Vector upVector, Vector forwardVector, double right, double up, double forward) {
        return (rightVector.getZ() * right) + (upVector.getZ() * up) + (forwardVector.getZ() * forward);
    }

    @Override
    public void move(Vector location, double trackYawRadian, double trackPitchRadian, double banking) {
        double originalTrackYawRadian = trackYawRadian;
        double originalTrackPitchRadian = trackPitchRadian;
        super.move(location, trackYawRadian, trackPitchRadian, banking);

//        Bukkit.getWorlds().get(0).spawnParticle(Particle.END_ROD,
//                location.getX(), location.getY(), location.getZ(),
//                1,
//                0, 0, 0,
//                0);

        if (getVelocity() == 0 && lastVelocity != 0) {
            updateAllCarts = true;
        }
//        calculationQuaternion.setIdentity();
//        calculationQuaternion.rotateYawPitchRoll();
//        matrix4x4.getRotation(calculationQuaternion);
        lastVelocity = getVelocity();
//        update++;
        this.getLocation().setX(location.getX());
        this.getLocation().setY(location.getY());
        this.getLocation().setZ(location.getZ());

        double workYaw = trackYawRadian + (Math.PI * 0.5);
        double workPitch = trackPitchRadian + (Math.PI * 0.5);

        forwardVector = MathUtil.setYawPitchRadians(forwardVector, workYaw, workPitch);
        forwardVector.multiply(-1);
        forwardVector.setY(forwardVector.getY() * -1);
        forwardVector.normalize();

        upVector.setX(0);
        upVector.setY(-1);
        upVector.setZ(0);

        rightVector.setX(0);
        rightVector.setY(-1);
        rightVector.setZ(0);

        bankingVector.setX(-1);
        bankingVector.setY(0);
        bankingVector.setZ(0);
        MathUtil.setYawPitchRadians(bankingVector, workYaw, 0);
        MathUtil.rotate(rightVector, bankingVector, -banking * MathUtil.DEGTORAD);
        rightVector.crossProduct(forwardVector);
        rightVector.normalize();

        MathUtil.rotate(upVector, bankingVector, (-banking * MathUtil.DEGTORAD) - (Math.PI * 0.5));
        upVector.crossProduct(forwardVector);
        upVector.normalize();

        if (CraftventureCore.getSettings().isDebugCoasterOrientation()) {
            World world = Bukkit.getWorlds().get(0);
            double particleOffset = -0.3;
            int particleCount = 3;
            // Red
            for (int upOffset = 0; upOffset < particleCount; upOffset++) {
                ParticleSpawnerKt.spawnParticleX(world, Particle.REDSTONE,
                        location.getX() + calculateX(rightVector, upVector, forwardVector, 0, upOffset * particleOffset, 0),
                        location.getY() + calculateY(rightVector, upVector, forwardVector, 0, upOffset * particleOffset, 0),
                        location.getZ() + calculateZ(rightVector, upVector, forwardVector, 0, upOffset * particleOffset, 0),
                        0,
                        1, 0, 0,
                        1,
                        new Particle.DustOptions(Color.fromRGB(0xFF0000), 1.0f));
            }
            // Yellow
            for (int forwardOffset = 0; forwardOffset < particleCount; forwardOffset++) {
                ParticleSpawnerKt.spawnParticleX(world, Particle.REDSTONE,
                        location.getX() + calculateX(rightVector, upVector, forwardVector, 0, 0, forwardOffset * particleOffset),
                        location.getY() + calculateY(rightVector, upVector, forwardVector, 0, 0, forwardOffset * particleOffset),
                        location.getZ() + calculateZ(rightVector, upVector, forwardVector, 0, 0, forwardOffset * particleOffset),
                        0,
                        0, 1, 0,
                        1,
                        new Particle.DustOptions(Color.fromRGB(0x00FF00), 1.0f));
            }
            // Purple
            for (int rightOffset = 0; rightOffset < particleCount; rightOffset++) {
                ParticleSpawnerKt.spawnParticleX(world, Particle.REDSTONE,
                        location.getX() + calculateX(rightVector, upVector, forwardVector, rightOffset * particleOffset, 0, 0),
                        location.getY() + calculateY(rightVector, upVector, forwardVector, rightOffset * particleOffset, 0, 0),
                        location.getZ() + calculateZ(rightVector, upVector, forwardVector, rightOffset * particleOffset, 0, 0),
                        0,
                        0, 0, 1,
                        1,
                        new Particle.DustOptions(Color.fromRGB(0x0000FF), 1.0f));
            }
        }

        for (int i = 0; i < effects.size(); i++) {
            Effect effect = effects.get(i);
            effect.move(location.getX() - calculateX(rightVector, upVector, forwardVector, effect.getRightOffset(), effect.getUpOffset(), effect.getForwardOffset()),
                    location.getY() - calculateY(rightVector, upVector, forwardVector, effect.getRightOffset(), effect.getUpOffset(), effect.getForwardOffset()),
                    location.getZ() - calculateZ(rightVector, upVector, forwardVector, effect.getRightOffset(), effect.getUpOffset(), effect.getForwardOffset()),
                    trackYawRadian + (Math.PI * 0.5),
                    trackPitchRadian,
                    banking,
                    this);
        }
        World world = Bukkit.getWorlds().get(0);
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            seat.move(location.getX() - calculateX(rightVector, upVector, forwardVector, seat.rightOffset(), seat.upOffset(), seat.forwardOffset()),
                    location.getY() - calculateY(rightVector, upVector, forwardVector, seat.rightOffset(), seat.upOffset(), seat.forwardOffset()),
                    location.getZ() - calculateZ(rightVector, upVector, forwardVector, seat.rightOffset(), seat.upOffset(), seat.forwardOffset()),
                    trackYawRadian + (Math.PI * 0.5),
                    trackPitchRadian,
                    banking,
                    updateAllCarts,
                    this);
//            seat.move(location.getX() + (i * (i % 2 == 0 ? -1 : 1)),
//                    location.getY(),
//                    location.getZ() + (i * (i % 2 == 0 ? -1 : 1)),
//                    trackYawRadian + (Math.PI * 0.5),
//                    trackPitchRadian,
//                    banking,
//                    updateAllCarts || true,
//                    this);

            if (CraftventureCore.getSettings().isDebugCoasterSeatLocations() && seat.isPassengerCar()) {
                ParticleSpawnerKt.spawnParticleX(world, Particle.REDSTONE,
                        location.getX() - calculateX(rightVector, upVector, forwardVector, seat.rightOffset(), seat.upOffset(), seat.forwardOffset()),
                        location.getY() - calculateY(rightVector, upVector, forwardVector, seat.rightOffset(), seat.upOffset(), seat.forwardOffset()),
                        location.getZ() - calculateZ(rightVector, upVector, forwardVector, seat.rightOffset(), seat.upOffset(), seat.forwardOffset()),
                        0,
                        1, 1, 1,
                        1,
                        new Particle.DustOptions(Color.fromRGB(0xedff2a), 1.0f));
            }
        }

        updateAllCarts = false;

        if (this.moveListener != null) {
            this.moveListener.onMove(location, trackYawRadian, trackPitchRadian, banking, this);
        }

        this.yawRadian = originalTrackYawRadian;
        this.pitchRadian = originalTrackPitchRadian;
    }

    @Override
    public void despawn() {
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            seat.despawn();
        }
    }

    public interface MoveListener {
        void onMove(Vector location, double trackYawRadian, double trackPitchRadian, double banking, DynamicSeatedRideCar multiSeatRideCar);
    }

    public interface FakeSeatProvider {
        ItemStack dress(int mountedEntityIndex, int currentEntityIndex);
    }

    @NotNull
    @Override
    public Json toJson() {
        return toJson(new DynamicSeatedRideCarJson());
    }
}
