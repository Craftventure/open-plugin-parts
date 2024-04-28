package net.craftventure.core.ride.flatride;

import net.craftventure.audioserver.api.AudioServerApi;
import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.AreaTracker;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.chat.bungee.util.CVChatColor;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata;
import net.craftventure.core.ride.RotationFixer;
import net.craftventure.core.ride.operator.OperableRide;
import net.craftventure.core.ride.operator.OperatorAreaTracker;
import net.craftventure.core.ride.operator.controls.*;
import net.craftventure.core.utils.EntityUtils;
import net.craftventure.core.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class VikingShip extends Flatride<VikingShip.Row> implements OperableRide, OperatorControl.ControlListener {
    private static VikingShip _this;

    private final Vector pivotPoint = new Vector(-);
    private double angleRadian = 0;
    private final List<ModelHolder> models = new LinkedList<>();
    private final Vector vector = new Vector(0, 0, 0);
    private final Block entranceGate = new Location(Bukkit.getWorld("world"),).getBlock();

    private final SimpleArea teleportArea = new SimpleArea("world", );

    private final OperatorLed ledRunning;// = new OperatorLed("running_indicator", this, Material.);
    private final OperatorButton buttonDispatch;
    private final OperatorSwitch buttonGates;

    private boolean gatesOpen = false;
    private Player operator;
    private final SimpleArea operatorArea = area;

    private final OperatorAreaTracker operatorAreaTracker = new OperatorAreaTracker(this, operatorArea);

    @NotNull
    @Override
    public AreaTracker getOperatorAreaTracker() {
        return operatorAreaTracker;
    }

    private VikingShip() {
        super(new SimpleArea(new Location(Bukkit.getWorld("world"), ), new Location(Bukkit.getWorld("world"), )),
                new Location(Bukkit.getWorld("world"), ), "swingingship", "ride_swingingship");

        for (Entity entity : area.getLoc1().getWorld().getEntities()) {
            if (entity instanceof ArmorStand && area.isInArea(entity.getLocation()) && rideName.equals(entity.getCustomName())) {
                entity.remove();
            }
        }

        models.clear();
        double radius = -15;
        models.add(new ModelHolder(0, radius - 1, false, false, false));
        models.add(new ModelHolder(-0, radius - 1, true, false, false));
        models.add(new ModelHolder(3.6, radius - 0.3, false, true, false));
        models.add(new ModelHolder(-3.6, radius - 0.3, true, true, false));
        //leashes
        models.add(new ModelHolder(1.7, (radius - 0.3) * 0.35, false, false, true));
        models.add(new ModelHolder(-1.7, (radius - 0.3) * 0.35, true, false, true));
        models.add(new ModelHolder(4.7, (radius - 0.3) * 0.75, false, false, true));
        models.add(new ModelHolder(-4.7, (radius - 0.3) * 0.75, true, false, true));

        cars.clear();
        double seatOffset = -1 + 0.35;
        cars.add(new Row(3, 1.2, -17 - seatOffset));
        cars.add(new Row(3, -1.2, -17 - seatOffset));
        cars.add(new Row(3, 2.9, -16.8 - seatOffset));
        cars.add(new Row(3, -2.9, -16.8 - seatOffset));
        cars.add(new Row(3, 4.5, -16.4 - seatOffset));
        cars.add(new Row(3, -4.5, -16.4 - seatOffset));
        cars.add(new Row(2, 6, -15.8 - seatOffset));
        cars.add(new Row(2, -6, -15.8 - seatOffset));

        updateCarts(true);

        ledRunning = new OperatorLed("running_indicator", ControlColors.getNEUTRAL_DARK());
        ledRunning
                .setName(CVTextColor.getMENU_DEFAULT_TITLE(), "Running")
                .setDescription(CVTextColor.getMENU_DEFAULT_LORE(), "Indicates wether the ride is running or not")
                .setControlListener(this);

        buttonDispatch = new OperatorButton("dispatcher", OperatorButton.Type.DEFAULT);
        buttonDispatch
                .setFlashing(true)
                .setName(CVTextColor.getMENU_DEFAULT_TITLE(), "Start")
                .setDescription(CVTextColor.getMENU_DEFAULT_LORE(), "Starts the ride if it's not started yet. Requires gates to be closed")
                .setControlListener(this);

        buttonGates = new OperatorSwitch("gates");
        buttonGates
                .setName(CVTextColor.getMENU_DEFAULT_TITLE(), "Gates")
                .setDescription(CVTextColor.getMENU_DEFAULT_LORE(), "Open the gates when the ride is not running")
                .setControlListener(this);

        openGates(true);
    }

    public static VikingShip getInstance() {
        if (_this == null)
            _this = new VikingShip();
//        _this.start();
        return _this;
    }

    private void updateRunningIndicator() {
        ledRunning.setColor(isRunning() ? ControlColors.getNEUTRAL() : ControlColors.getNEUTRAL_DARK());
        ledRunning.setFlashing(isRunning());
    }

    private void updateDispatchButton() {
        buttonDispatch.setEnabled(!isRunning() && !gatesOpen);
    }

    private void updateGatesButton() {
        buttonGates.setEnabled(!isRunning());
        buttonGates.setOn(gatesOpen);
    }

    private void openGates(boolean open) {
        this.gatesOpen = open;
        BlockExtensionsKt.open(entranceGate, open);
        updateGatesButton();
        updateDispatchButton();
    }

    private void tryOperatorStart() {
        if (!isRunning() && !gatesOpen) {
            start();
        }
    }

    private void tryOpenGatesIfPossible(boolean open) {
        if (!isRunning()) {
            openGates(open);
        }
    }

    @Override
    public void start() {
        super.start();
        openGates(false);

        updateRunningIndicator();
        updateDispatchButton();
        updateGatesButton();
    }

    @Override
    public void stop() {
        super.stop();
        AudioServerApi.INSTANCE.disable("vikingship");
        if (!isBeingOperated())
            openGates(true);

        updateRunningIndicator();
        updateDispatchButton();
        updateGatesButton();
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();
        area.getWorld().getBlockAt().setType(Material.AIR);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != operator && teleportArea.isInArea(player)) {
                if (player.getVehicle() == null || !TypedInstanceOwnerMetadata.Companion.isOwnedByRide(player.getVehicle(), this)) {
                    player.teleport(exitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    sendMoveAwayMessage(player);
                }
            }
        }
    }

    @Override
    protected FlatrideRunnable provideRunnable() {
        return new FlatrideRunnable() {
            long lastFrame = System.currentTimeMillis();
            boolean hasStartedMusic = false;
            boolean stopping = false;

            double angleAccel = 0;
            double angleVelocity = 0;//0.01;

            /**
             *
             * @return true if the ride should not update carts
             */
            private boolean updatePhysics(long delta) {
                long currentRunTime = System.currentTimeMillis() - startTime;
                if (!hasStartedMusic) {
                    angleRadian = 0.01;
                    hasStartedMusic = true;
                    AudioServerApi.INSTANCE.enable("vikingship");
                    AudioServerApi.INSTANCE.sync("vikingship", System.currentTimeMillis());
                }
                angleAccel = -9.81 / 18 * Math.sin(angleRadian);
//                    Logger.console("Add " + (angleAccel * (delta / 1000f)));
                angleVelocity += angleAccel * (delta / 1000f);

                if (!stopping) {
                    double value = Math.min(0.3, Math.max(0.12, angleVelocity));
                    if (angleRadian < value && angleRadian > -value) {
                        angleVelocity *= 1.05;
                    }
                } else {
                    if (angleRadian < Math.max(0.15, angleVelocity) && angleRadian > -Math.max(0.15, angleVelocity)) {
                        angleVelocity *= 0.95;
                    }
                }
                if (angleVelocity > 0.7)
                    angleVelocity = 0.7;
                if (angleVelocity < -0.7)
                    angleVelocity = -0.7;
                angleRadian += angleVelocity * (delta / 1000f);

                if (currentRunTime > 45000 && !stopping) {
//                        Logger.consoleAndIngame("Stopping Ship");
                    stopping = true;
                }
//                for (Player player : getPassengers())
//                    if (PlayerExtensionKt.isCrew(player))
//                        MessageBarManager.display(
//                                player,
//                                ChatUtils.createComponent(
//                                        String.format("%1$s, %2$.2f, %3$.2f", stopping, angleVelocity, angleRadian),
//                                        CVChatColor.WARNING
//                                ),
//                                MessageBarManager.Type.WARNING,
//                                TimeUtils.secondsFromNow(5),
//                                ChatUtils.ID_RIDE
//                        );

                if (stopping && angleVelocity < 0.01 && angleVelocity > -0.01 && angleRadian < 0.01 && angleRadian > -0.01) {
//                        Logger.consoleAndIngame("Ship Stopped");
                    updateCarts(true);

                    angleAccel = 0;
                    angleVelocity = 0;
                    stop();
                    return true;
                }

                if (currentRunTime > 65000) {
                    updateCarts(true);

                    angleAccel = 0;
                    angleVelocity = 0;
                    stop();
                    Logger.warn("Forcefully stopping %1$s %2$s, %3$s, %4$s", true, getRide() != null ? getRide().getDisplayName() : null, stopping, angleVelocity, angleRadian);
                    return true;
                }
                return false;
            }

            @Override
            public void updateTick() {
                boolean shouldUpdateCarts = true;
                while (System.currentTimeMillis() - lastFrame > 50 && isRunning()) {
                    lastFrame += 50;
                    shouldUpdateCarts = !updatePhysics(50);
                }

                if (shouldUpdateCarts)
                    updateCarts(false);
            }
        };
    }

    @Override
    protected void updateCarts(boolean forceTeleport) {
        for (ModelHolder modelHolder : models) {
            vector.setX(modelHolder.offsetX);
            vector.setY(modelHolder.offsetY);
            vector.setZ(0);

            VectorUtils.rotateAroundAxisZ(vector, angleRadian);

            modelHolder.teleport(pivotPoint.getX() + vector.getX(), pivotPoint.getY() + vector.getY(), pivotPoint.getZ() + vector.getZ(), angleRadian, forceTeleport);
        }
        for (Row row : cars) {
            vector.setX(row.offsetX);
            vector.setY(row.offsetY);
            vector.setZ(0);

            VectorUtils.rotateAroundAxisZ(vector, angleRadian);

            row.teleport(pivotPoint.getX() + vector.getX(), pivotPoint.getY() + vector.getY(), pivotPoint.getZ() + vector.getZ(), (float) angleRadian, 0, forceTeleport);
        }
    }

    @Override
    public boolean isBeingOperated() {
        return operator != null;
    }

    @Nullable
    @Override
    public Player getOperatorForSlot(int slot) {
        if (slot == 0)
            return operator;
        return null;
    }

    @Override
    public int getOperatorSlot(Player player) {
        return player == operator ? 0 : -1;
    }

    @Override
    public boolean setOperator(@Nullable Player player, int slot) {
        if (slot == 0 && operator == null) {
            operator = player;
            if (operator != null)
                operator.sendMessage(CVChatColor.INSTANCE.getServerNotice() + "You are now operating " + getRide().getDisplayName());
            cancelAutoStart();
            return true;
        }
        return false;
    }

    @Override
    public int getTotalOperatorSpots() {
        return 1;
    }

    @Override
    public void cancelOperating(int slot) {
        if (slot == 0) {
            if (operator != null) {
                operator.sendMessage(CVChatColor.INSTANCE.getServerNotice() + "You are no longer operating " + getRide().getDisplayName());
                operator = null;
                scheduleAutoStart();
                tryOpenGatesIfPossible(true);
            }
        }
    }

    @Override
    public List<OperatorControl> provideControls() {
        List<OperatorControl> controls = new ArrayList<>();

        controls.add(ledRunning);
        controls.add(buttonGates);
        controls.add(buttonDispatch);

        return controls;
    }

    @Override
    public boolean isInOperateableArea(@NotNull Location location) {
        return operatorArea.isInArea(location);
    }

    @Override
    public void updateWhileOperated() {

    }

    @Override
    public @NotNull String getId() {
        return rideName;
    }

    @Override
    public void onClick(@NotNull OperableRide operableRide, @NotNull Player player, @NotNull OperatorControl operatorControl, Integer operatorSlot) {
        if (operatorControl.isEnabled()) {
            if (operatorControl == buttonDispatch) {
                tryOperatorStart();
            } else if (operatorControl == buttonGates) {
                tryOpenGatesIfPossible(!buttonGates.isOn());
            }
        }
    }

    protected class ModelHolder {
        protected ArmorStand modelHolder;
        protected double offsetX;
        protected double offsetY;
        protected boolean westSide;
        protected boolean backModel;
        protected boolean isArm;
        private final RotationFixer rotationFixer = new RotationFixer();

        public ModelHolder(double offsetX, double offsetY, boolean westSide, boolean backModel, boolean isArm) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.westSide = westSide;
            this.backModel = backModel;
            this.isArm = isArm;
        }

        public void teleport(double x, double y, double z, double pitch, boolean forceTeleport) {
            if (modelHolder == null || !modelHolder.isValid()) {
                modelHolder = area.getWorld().spawn(new Location(area.getWorld(), x, y, z, westSide ? -90 : 90, 0), ArmorStand.class);
                modelHolder.setPersistent(false);
                TypedInstanceOwnerMetadata.Companion.setOwner(modelHolder, VikingShip.this);
                modelHolder.setGravity(false);
                modelHolder.setCustomName(rideName);
                if (!isArm)
                    modelHolder.setHelmet(backModel ? MaterialConfig.INSTANCE.getVIKING_SHIP_END().clone() : MaterialConfig.INSTANCE.getVIKING_SHIP_MID().clone());
                modelHolder.setVisible(false);
                modelHolder.addDisabledSlots(EquipmentSlot.values());

                if (isArm) {
                    modelHolder.setHelmet(MaterialConfig.INSTANCE.getVIKING_SHIP_FRAME().clone());
                }
            }

            if (forceTeleport) {
                EntityUtils.INSTANCE.teleport(modelHolder, x, y, z, westSide ? -90 : 90, 0);
            } else {
                Location location = modelHolder.getLocation();
                location.setX(x);
                location.setY(y);
                location.setZ(z);
//                EntityUtils.teleport(modelHolder, x, y, z);
                modelHolder.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
            rotationFixer.setNextRotation((westSide ? -pitch : pitch) + (isArm ? Math.toRadians(26) : 0), 0, 0);
            RotationFixer.Rotation rotation = rotationFixer.getCurrentRotation();
            modelHolder.setHeadPose(new EulerAngle(rotation.getX(), rotation.getY(), rotation.getZ()));

            if (forceModelUpdate) {
                if (isArm) {
                    modelHolder.setHelmet(MaterialConfig.INSTANCE.getVIKING_SHIP_FRAME().clone());
                }
            }
        }
    }

    protected class Row extends FlatrideCar<ArmorStand> {
        private double offsetX = 0;
        private double offsetY = 0;

        private Row(int count, double offsetX, double offsetY) {
            super(new ArmorStand[count]);
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public boolean isPartOf(Entity target) {
            for (ArmorStand armorStand : entities) {
                if (armorStand != null && armorStand.getEntityId() == target.getEntityId())
                    return true;
            }
            return super.isPartOf(target);
        }

        @Override
        public void teleport(double x, double y, double z, float yaw, float pitch, boolean forceTeleport) {
            double rowSpace = 1;
            double startOffset = Math.floor(entities.length / 2f) - (entities.length % 2 == 0 ? rowSpace * 0.5 : 0);
            for (int i = 0; i < entities.length; i++) {
                if (entities[i] == null || !entities[i].isValid()) {
                    entities[i] = area.getWorld().spawn(new Location(area.getWorld(),
                            x,
                            y,
                            z + startOffset - (i * rowSpace),
                            offsetX < 0 ? -90 : 90,//yawRadian + (i == models.length - 1 ? 0 : 180),
                            0//(float) Math.toDegrees(pitchRadian + Math.PI)
                    ), ArmorStand.class);
                    entities[i].setPersistent(false);
                    TypedInstanceOwnerMetadata.Companion.setOwner(entities[i], VikingShip.this);
                    entities[i].setGravity(false);
                    entities[i].setCustomName(rideName);
                    entities[i].setBasePlate(false);
                    entities[i].setVisible(false);
                    entities[i].addDisabledSlots(EquipmentSlot.values());
                } else if (entities[i].getPassenger() != null || forceTeleport) {
                    EntityUtils.INSTANCE.teleport(entities[i],
                            x,
                            y,
                            z + startOffset - (i * rowSpace),
                            offsetX < 0 ? -90 : 90,
                            0
                    );
                }
            }
        }

    }
}
