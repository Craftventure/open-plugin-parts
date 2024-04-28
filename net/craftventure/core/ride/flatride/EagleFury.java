package net.craftventure.core.ride.flatride;

import net.craftventure.audioserver.api.AudioServerApi;
import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.AreaTracker;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.chat.bungee.util.CVChatColor;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.animation.keyframed.DoubleValueKeyFrame;
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata;
import net.craftventure.core.ride.operator.OperableRide;
import net.craftventure.core.ride.operator.OperatorAreaTracker;
import net.craftventure.core.ride.operator.controls.*;
import net.craftventure.core.utils.EntityUtils;
import net.craftventure.core.utils.InterpolationUtils;
import net.craftventure.core.utils.MathUtil;
import net.craftventure.core.utils.SimpleInterpolator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import penner.easing.Cubic;
import penner.easing.Linear;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class EagleFury extends Flatride<EagleFury.FurySeat> implements OperableRide, OperatorControl.ControlListener {
    private static final int SEAT_COUNT = 20;
    private static final double RADIUS = 3.4;
    private static EagleFury _this;
    private final List<DoubleValueKeyFrame> heightFrames = new ArrayList<>();
    private final List<SimpleInterpolator> heightFramesInterpolations = new ArrayList<>();
    private int heightFrameIndex = 0;
    private double interpolatedHeight = 0;

    private final Location center = new Location(Bukkit.getWorld("world"), );
    private double rideAngle = 0;

    private final Block entranceGate = new Location(Bukkit.getWorld("world"), ).getBlock();
    private final OperatorLed ledRunning;// = new OperatorLed("running_indicator", this, Material.);
    private final OperatorButton buttonDispatch;
    private final OperatorSwitch buttonGates;

    private boolean gatesOpen = false;
    private Player operator;
    private final SimpleArea operatorArea = new SimpleArea("world", );

    private final OperatorAreaTracker operatorAreaTracker = new OperatorAreaTracker(this, operatorArea);

    @NotNull
    @Override
    public AreaTracker getOperatorAreaTracker() {
        return operatorAreaTracker;
    }

    private EagleFury() {
        super(new SimpleArea("world", ),
                new Location(Bukkit.getWorld("world"), ),
                "eaglesfury", "ride_eaglefury");

        for (Entity entity : area.getLoc1().getWorld().getEntities()) {
            if (entity instanceof ArmorStand && area.isInArea(entity.getLocation()) && entity.getCustomName() != null && entity.getCustomName().equals(rideName)) {
                entity.remove();
            }
        }

        setupFrames();
        interpolatedHeight = heightFrames.get(0).getValue();
        heightFrames.sort((o1, o2) -> (int) (o1.getTime() - o2.getTime()));

        cars.clear();
        for (int i = 0; i < SEAT_COUNT; i++) {
            cars.add(new FurySeat(Math.toRadians((360.0 / (float) SEAT_COUNT) * i)));
        }

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

    public static EagleFury getInstance() {
        if (_this == null)
            _this = new EagleFury();
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

    private double toMillis(double seconds) {
        return seconds * 1000;
    }

    private void setupFrames() {
        addFrame(0, 0, Linear::easeInOut);
        addFrame(0, 0, Cubic::easeInOut);
        addFrame(0, 0, Cubic::easeInOut);
        addFrame(0, 0, Linear::easeInOut);
        addFrame(0, 0, Cubic::easeInOut);
        addFrame(0, 0, Cubic::easeInOut);
    }

    private void addFrame(double time, double height, SimpleInterpolator simpleInterpolator) {
        heightFrames.add(new DoubleValueKeyFrame(toMillis(time), height));
        heightFramesInterpolations.add(simpleInterpolator);
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
        heightFrameIndex = 0;
        AudioServerApi.INSTANCE.disable("eaglefury_onride");
        if (!isBeingOperated())
            openGates(true);

        updateRunningIndicator();
        updateDispatchButton();
        updateGatesButton();
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();
        heightFrameIndex = 0;

        AudioServerApi.INSTANCE.enable("eaglefury_onride");
        AudioServerApi.INSTANCE.sync("eaglefury_onride", System.currentTimeMillis());

//        for (Player player : Bukkit.getOnlinePlayers()) {
//            if (player != operator && teleportArea.isInArea(player)) {
//                if (!player.isInsideVehicle()) {
//                    player.teleport(exitLocation);
//                    sendMoveAwayMessage(player);
//                }
//            }
//        }
    }

    @Override
    protected FlatrideRunnable provideRunnable() {
        return new FlatrideRunnable() {
            int frame = 0;

            @Override
            public void updateTick() {
                frame++;
                long programTime = System.currentTimeMillis() - startTime;

                if (update(heightFrames, heightFrameIndex, programTime))
                    heightFrameIndex++;

                interpolatedHeight = interpolate(heightFrames, heightFramesInterpolations, heightFrameIndex, programTime);

                if (programTime < 38000) {
                    rideAngle += 0.02;
                }

                if (frame > 2) {
                    frame = 0;
                    if (programTime > heightFrames.get(heightFrames.size() - 1).getTime()) {
                        updateCarts(true);
                        stop();
                        return;
                    }

                    updateCarts(true);
                }
            }
        };
    }

    @Override
    protected void updateCarts(boolean forceTeleport) {
        for (FurySeat seat : cars) {
            seat.teleport(0,
                    interpolatedHeight,
                    0,
                    0,
                    0,
                    forceTeleport);
        }
    }


    private double interpolate(List<DoubleValueKeyFrame> frames, List<SimpleInterpolator> interpolators, int index, long time) {
        if (frames.size() > index + 1) {
            DoubleValueKeyFrame current = frames.get(index);
            DoubleValueKeyFrame next = frames.get(index + 1);

            SimpleInterpolator interpolator = interpolators.get(index);

            double t = interpolator.interpolate((float) (time - current.getTime()),
                    0, 1, (float) (next.getTime() - current.getTime()));
            return InterpolationUtils.linearInterpolate(current.getValue(), next.getValue(), t);
        } else {
            return frames.get(index).getValue();
        }
    }

    private boolean update(List<DoubleValueKeyFrame> frames, int index, long time) {
        return frames.size() > index + 1 && (frames.get(index + 1).getTime() <= time);
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
    public void onClick(@NotNull OperableRide operableRide, @Nullable Player player, @NotNull OperatorControl operatorControl, Integer operatorSlot) {
        if (operatorControl.isEnabled()) {
            if (operatorControl == buttonDispatch) {
                tryOperatorStart();
            } else if (operatorControl == buttonGates) {
                tryOpenGatesIfPossible(!buttonGates.isOn());
            }
        }
    }

    protected class FurySeat extends FlatrideCar<ArmorStand> {
        private final Location loc;
        private double angle = 0;

        private FurySeat(double angle) {
            super(new ArmorStand[1]);
            this.loc = center.clone();
            this.angle = angle;
        }

        @Override
        public void teleport(double x, double y, double z, float yaw, float pitch, boolean forceTeleport) {
//            forceTeleport = true;// TODO: Remove debug
            for (int i = 0; i < entities.length; i++) {
                if (entities[i] == null || !entities[i].isValid()) {
                    loc.setYaw((float) (Math.toDegrees(angle + rideAngle) - 90));
                    entities[i] = loc.getWorld().spawn(loc.clone().add(Math.cos(angle + rideAngle) * RADIUS, 0, Math.sin(angle + rideAngle) * RADIUS), ArmorStand.class);
                    entities[i].setPersistent(false);
                    TypedInstanceOwnerMetadata.Companion.setOwner(entities[i], EagleFury.this);
                    entities[i].setGravity(false);
                    entities[i].setBasePlate(false);
                    entities[i].setVisible(false);
                    entities[i].setCustomName(rideName);
                    entities[i].setHelmet(MaterialConfig.INSTANCE.getEAGLE_FURY_SEAT());
                    entities[i].addDisabledSlots(EquipmentSlot.values());
                } else if (i == 2 || entities[i].getPassenger() != null || forceTeleport) {
                    EntityUtils.INSTANCE.teleport(entities[i],
                            loc.getX() + (Math.cos(angle + rideAngle) * RADIUS),
                            y,
                            loc.getZ() + (Math.sin(angle + rideAngle) * RADIUS),
                            (float) ((MathUtil.RADTODEG * (angle + rideAngle)) - 90),
                            0);
                } else if (entities[i].getPassenger() == null && entities[i].getLocation().getY() > 5) {
                    Location location = entities[i].getLocation();
                    EntityUtils.INSTANCE.teleport(entities[i], location.getX(), 5, location.getZ(), location.getYaw(), location.getPitch());
                }

                if (forceModelUpdate) {
                    entities[i].setHelmet(MaterialConfig.INSTANCE.getEAGLE_FURY_SEAT());
                }
            }
        }

    }
}
