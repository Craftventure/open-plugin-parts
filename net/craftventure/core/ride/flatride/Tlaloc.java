package net.craftventure.core.ride.flatride;

import net.craftventure.audioserver.api.AudioServerApi;
import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.area.AreaTracker;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.extension.BlockExtensionsKt;
import net.craftventure.bukkit.ktx.extension.GameProfileExtensionsKt;
import net.craftventure.bukkit.ktx.extension.PlayerExtensionsKt;
import net.craftventure.chat.bungee.util.CVChatColor;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.animation.keyframed.DoubleValueKeyFrame;
import net.craftventure.core.manager.GameModeManager;
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata;
import net.craftventure.core.protocol.packet.PacketEquipment;
import net.craftventure.core.ride.RotationFixer;
import net.craftventure.core.ride.operator.OperableRide;
import net.craftventure.core.ride.operator.OperatorAreaTracker;
import net.craftventure.core.ride.operator.controls.*;
import net.craftventure.core.script.ScriptManager;
import net.craftventure.core.script.action.PlaceSchematicAction;
import net.craftventure.core.utils.EntityUtils;
import net.craftventure.core.utils.MathUtil;
import net.craftventure.core.utils.SimpleInterpolator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class Tlaloc extends Flatride<Tlaloc.TlalocBench> implements OperableRide, OperatorControl.ControlListener {
    private static final int BENCH_COUNT = 1;
    private static final double BENCH_OFFSET = 5.5;
    private static final double SEAT_OFFSET = 1.5;
    private static final double ARM_DISTANCE_FROM_CENTER = 1;
    private static Tlaloc _this;
    private final Vector leftArmPosition = new Vector();
    private final Vector carPosition = new Vector();
    private final Vector rightArmPosition = new Vector();
    private double armAngle = Math.PI;
    private double benchAngle = Math.PI;
    private final ArmorStand leftArm;
    private final ArmorStand leftArm2;
    private final ArmorStand leftArm3;
    private final ArmorStand rightArm;
    private final ArmorStand rightArm2;
    private final ArmorStand rightArm3;
    private final RotationFixer leftArmFixer;
    private final RotationFixer leftArmFixer2;
    private final RotationFixer rightArmFixer;
    private final RotationFixer rightArmFixer2;
    private final List<DoubleValueKeyFrame> armFrames = new LinkedList<>();
    private final List<SimpleInterpolator> armInterpolators = new LinkedList<>();
    private final List<DoubleValueKeyFrame> benchFrames = new LinkedList<>();
    private final List<SimpleInterpolator> benchInterpolators = new LinkedList<>();
    private int armIndex = 0, benchIndex = 0;

    private final Block doorEntrance1Block = new Location(Bukkit.getWorld("world"),).getBlock();
    private final Block doorEntrance2Block = new Location(Bukkit.getWorld("world"),).getBlock();
    private final Block doorExit1Block = new Location(Bukkit.getWorld("world"), ).getBlock();
    private final Block doorExit2Block = new Location(Bukkit.getWorld("world"), ).getBlock();

    private final SimpleArea teleportArea = new SimpleArea("world", );
    private boolean isChangingGamemodes = false;

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

    private Tlaloc() {
        super(new SimpleArea(new Location(Bukkit.getWorld("world"),), new Location(Bukkit.getWorld("world"),)),
                new Location(Bukkit.getWorld("world"), ), "tlaloc", "ride_tlaloc");

        for (Entity entity : area.getLoc1().getWorld().getEntities()) {
            if (entity instanceof ArmorStand && area.isInArea(entity.getLocation())) {
                entity.remove();
            }
        }

        cars.clear(); // Just to be sure
        for (int i = 0; i < BENCH_COUNT; i++) {
            cars.add(new TlalocBench(area.getLoc1()));
        }

        leftArmFixer = new RotationFixer();
        leftArmFixer2 = new RotationFixer();
        rightArmFixer = new RotationFixer();
        rightArmFixer2 = new RotationFixer();

        leftArm = spawnArm(leftArmPosition.getX(),
                leftArmPosition.getY() - (Math.cos(armAngle) * ARM_DISTANCE_FROM_CENTER) - 1.5,
                leftArmPosition.getZ() - (Math.sin(armAngle) * ARM_DISTANCE_FROM_CENTER),
                180, 90);
        leftArm2 = spawnArm(leftArmPosition.getX(),
                leftArmPosition.getY() - (Math.cos(armAngle + MathUtil.RAD_180_DEG) * ARM_DISTANCE_FROM_CENTER) - 1.5,
                leftArmPosition.getZ() - (Math.sin(armAngle + MathUtil.RAD_180_DEG) * ARM_DISTANCE_FROM_CENTER),
                180, 90);
        leftArm3 = spawnArm(leftArmPosition.getX() + 0.3,
                leftArmPosition.getY() - 1.5,
                leftArmPosition.getZ(),
                180, 90);
        leftArm3.setHelmet(MaterialConfig.INSTANCE.getTLALOC_ARM_JOINT());

        rightArm = spawnArm(rightArmPosition.getX(),
                rightArmPosition.getY() - (Math.cos(armAngle) * ARM_DISTANCE_FROM_CENTER) - 1.5,
                rightArmPosition.getZ() - (Math.sin(armAngle) * ARM_DISTANCE_FROM_CENTER),
                0, 90);
        rightArm2 = spawnArm(rightArmPosition.getX(),
                rightArmPosition.getY() - (Math.cos(armAngle + MathUtil.RAD_180_DEG) * ARM_DISTANCE_FROM_CENTER) - 1.5,
                rightArmPosition.getZ() - (Math.sin(armAngle + MathUtil.RAD_180_DEG) * ARM_DISTANCE_FROM_CENTER),
                0, 90);
        rightArm3 = spawnArm(rightArmPosition.getX() - 0.3,
                rightArmPosition.getY() - 1.5,
                rightArmPosition.getZ(),
                0, 90);
        rightArm3.setHelmet(MaterialConfig.INSTANCE.getTLALOC_ARM_JOINT());

        setupFrames();
        updateCarts(true);
        new PlaceSchematicAction("tlaloc", "code").withName("tlaloc_platform_top").noAir(false).execute(null);
        ScriptManager.stop("tlaloc", "ride");

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

    public static Tlaloc getInstance() {
        if (_this == null)
            _this = new Tlaloc();
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
        BlockExtensionsKt.open(doorEntrance1Block, open);
        BlockExtensionsKt.open(doorEntrance2Block, open);
        BlockExtensionsKt.open(doorExit1Block, open);
        BlockExtensionsKt.open(doorExit2Block, open);
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
        double offset = 0;
        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(0), Math.toRadians(0)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(0), Math.toRadians(0)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(1.3), Math.toRadians(0)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(1.3), Math.toRadians(0)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(2.4), Math.toRadians(-16)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(2.4), Math.toRadians(10)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(3.76), Math.toRadians(-16)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(3.76), Math.toRadians(-5)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(6.7), Math.toRadians(44)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(6.7), Math.toRadians(-20)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(12), Math.toRadians(-91)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(12), Math.toRadians(24)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(14.67), Math.toRadians(-29)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(18), Math.toRadians(61)));


        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(19.6), Math.toRadians(-360)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(20), Math.toRadians(0)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(21), Math.toRadians(-360)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(21), Math.toRadians(0)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(43), Math.toRadians(220)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(43), Math.toRadians(580)));


        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(45), Math.toRadians(580)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(45.5), Math.toRadians(220)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(46.67), Math.toRadians(675)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(47), Math.toRadians(242)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(47.5), Math.toRadians(242)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(47.5), Math.toRadians(754)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(49.5), Math.toRadians(265)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(50), Math.toRadians(265)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(50.33), Math.toRadians(1131)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(51), Math.toRadians(1202)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(53.33), Math.toRadians(1490)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(55.66), Math.toRadians(1412)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(58.33), Math.toRadians(469)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(58.33), Math.toRadians(1613)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(62.33), Math.toRadians(1905)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(66), Math.toRadians(1760)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(69), Math.toRadians(248)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(69), Math.toRadians(1870)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(72), Math.toRadians(248)));
        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(72), Math.toRadians(1870)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(83.33), Math.toRadians(1582)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(86.67), Math.toRadians(1329)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(91), Math.toRadians(1546)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(92.67), Math.toRadians(-272)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(94), Math.toRadians(1389)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(96), Math.toRadians(1476)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(98), Math.toRadians(1424)));

        armFrames.add(new DoubleValueKeyFrame(offset + toMillis(99.67), Math.toRadians(-360)));

        benchFrames.add(new DoubleValueKeyFrame(offset + toMillis(100), Math.toRadians(1440)));
    }

//    @Override
//    protected boolean handlesExit(Player player, Entity dismounted, TlalocBench car, boolean isShifting) {
//        if (isRunning && player.getGameMode() == GameMode.SPECTATOR) {
//            if (!isShifting)
//                return true;
//        }
//        return super.handlesExit(player, dismounted, car, isShifting);
//    }

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
        armIndex = 0;
        benchIndex = 0;
        AudioServerApi.INSTANCE.disable("tlaloc");
        ScriptManager.stop("tlaloc", "ride");

        new PlaceSchematicAction("tlaloc", "code").withName("tlaloc_platform_top").noAir(false).execute(null);

        if (!isBeingOperated())
            openGates(true);

        updateRunningIndicator();
        updateDispatchButton();
        updateGatesButton();
    }

    private ArmorStand spawnArm(double x, double y, double z, float yaw, float pitch) {
        Location location = new Location(area.getLoc1().getWorld(), x, y, z, yaw, pitch);
        ArmorStand armorStand = area.getLoc1().getWorld().spawn(location, ArmorStand.class);
        armorStand.setPersistent(false);
        TypedInstanceOwnerMetadata.Companion.setOwner(armorStand, Tlaloc.this);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        armorStand.setBasePlate(false);
        armorStand.setVisible(false);
        armorStand.setHelmet(MaterialConfig.INSTANCE.getTLALOC_ARM());
        return armorStand;
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        isChangingGamemodes = true;
        for (TlalocBench tlalocBench : cars) {
            ArmorStand[] entities = tlalocBench.entities;
            for (int i = 0; i < entities.length; i++) {
                ArmorStand armorStand = entities[i];
                if (armorStand.getPassenger() instanceof Player) {
                    final Player player = (Player) armorStand.getPassenger();
                    armorStand.setHelmet(GameProfileExtensionsKt.toSkullItem(player.getPlayerProfile()));
                    tlalocBench.hideModels(player);
                    player.setGameMode(GameMode.SPECTATOR);
                    player.setSpectatorTarget(armorStand);
                    tlalocBench.players[i] = player;
                }
            }
        }
        isChangingGamemodes = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != operator && teleportArea.isInArea(player)) {
                if (player.getGameMode() != GameMode.SPECTATOR) {// && !player.isInsideVehicle()) {
                    player.teleport(exitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    sendMoveAwayMessage(player);
                }
            }
        }
        new PlaceSchematicAction("tlaloc", "code").withName("tlaloc_platform_bottom").noAir(false).execute(null);
        ScriptManager.start("tlaloc", "ride");
    }

    @Override
    protected void eject() {
        super.eject();
        for (TlalocBench tlalocBench : cars) {
            ArmorStand[] entities = tlalocBench.entities;
            for (int i = 0; i < entities.length; i++) {
                if (entities[i] instanceof ArmorStand) {
                    entities[i].setHelmet(new ItemStack(Material.AIR));
                }
                if (tlalocBench.players[i] != null) {
                    final Player player = tlalocBench.players[i];
                    onLeave(player, null);

                    tlalocBench.players[i] = null;
                    postLeave(player);
                    playerLeftRideCompleted(player);
                }
            }
        }
    }

    private void removeOfflinePlayers() {
        for (TlalocBench tlalocBench : cars) {
            tlalocBench.cleanupOfflinePlayers();
        }
    }

    @Override
    protected boolean handlesExit(Player player, Entity dismounted, TlalocBench car) {
        if (isChangingGamemodes) {
            return true;
        }
        return super.handlesExit(player, dismounted, car);
    }

    @Override
    protected void onLeave(Player player, @Nullable Entity vehicle) {
        super.onLeave(player, vehicle);

        if (vehicle instanceof ArmorStand) {
            ((ArmorStand) vehicle).setHelmet(new ItemStack(Material.AIR));
        }

        for (TlalocBench tlalocBench : cars) {
            tlalocBench.showModels(player);
        }
        GameModeManager.setDefaultGamemode(player);
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

    @Override
    protected FlatrideRunnable provideRunnable() {
        return new FlatrideRunnable() {
            int frame = 0;
            boolean hasStartedMusic = false;

            @Override
            public void updateTick() {
                frame++;
                frame = 0;
                long deltaTime = System.currentTimeMillis() - startTime;
                if (!hasStartedMusic) {
                    hasStartedMusic = true;
                    AudioServerApi.INSTANCE.enable("tlaloc");
                    AudioServerApi.INSTANCE.sync("tlaloc", System.currentTimeMillis());
                }

                if (update(armFrames, armIndex, deltaTime))
                    armIndex++;
                if (update(benchFrames, benchIndex, deltaTime))
                    benchIndex++;

                armAngle = interpolate(armFrames, armInterpolators, armIndex, deltaTime) + Math.PI;
                benchAngle = interpolate(benchFrames, benchInterpolators, benchIndex, deltaTime) + Math.PI;

                if (deltaTime > 100000) {
                    updateCarts(true);
                    stop();
                    return;
                }

                updateCarts(false);
            }
        };
    }

    @Override
    protected void updateCarts(boolean forceTeleport) {
        double radius = BENCH_OFFSET;
        for (TlalocBench seat : cars) {
            seat.cleanupOfflinePlayers();
            seat.teleport(carPosition.getX(),
                    carPosition.getY() + (radius * Math.cos(armAngle)) - 1.5,
                    carPosition.getZ() + (radius * Math.sin(armAngle)),
                    0,
                    (float) benchAngle,
                    forceTeleport);
        }
        EntityUtils.INSTANCE.teleport(leftArm,
                leftArmPosition.getX(),
                leftArmPosition.getY() - (Math.cos(armAngle) * ARM_DISTANCE_FROM_CENTER) - 1.5,
                leftArmPosition.getZ() - (Math.sin(armAngle) * ARM_DISTANCE_FROM_CENTER));
        EntityUtils.INSTANCE.teleport(leftArm2,
                leftArmPosition.getX(),
                leftArmPosition.getY() - (Math.cos(armAngle + MathUtil.RAD_180_DEG) * ARM_DISTANCE_FROM_CENTER) - 1.5,
                leftArmPosition.getZ() - (Math.sin(armAngle + MathUtil.RAD_180_DEG) * ARM_DISTANCE_FROM_CENTER));

        EntityUtils.INSTANCE.teleport(rightArm,
                rightArmPosition.getX(),
                rightArmPosition.getY() - (Math.cos(armAngle) * ARM_DISTANCE_FROM_CENTER) - 1.5,
                rightArmPosition.getZ() - (Math.sin(armAngle) * ARM_DISTANCE_FROM_CENTER));
        EntityUtils.INSTANCE.teleport(rightArm2,
                rightArmPosition.getX(),
                rightArmPosition.getY() - (Math.cos(armAngle + MathUtil.RAD_180_DEG) * ARM_DISTANCE_FROM_CENTER) - 1.5,
                rightArmPosition.getZ() - (Math.sin(armAngle + MathUtil.RAD_180_DEG) * ARM_DISTANCE_FROM_CENTER));

        leftArmFixer.setNextRotation(-armAngle - MathUtil.RAD_180_DEG, 0, 0);
        leftArmFixer2.setNextRotation(-armAngle, 0, 0);
        leftArm.setHeadPose(leftArmFixer.getCurrentRotation().toEuler());
        leftArm2.setHeadPose(leftArmFixer2.getCurrentRotation().toEuler());
        leftArm3.setHeadPose(leftArmFixer2.getCurrentRotation().toEuler());

        rightArmFixer.setNextRotation(armAngle - MathUtil.RAD_180_DEG, 0, 0);
        rightArmFixer2.setNextRotation(armAngle, 0, 0);
        rightArm.setHeadPose(rightArmFixer.getCurrentRotation().toEuler());
        rightArm2.setHeadPose(rightArmFixer2.getCurrentRotation().toEuler());
        rightArm3.setHeadPose(rightArmFixer2.getCurrentRotation().toEuler());
    }


    private double interpolate(List<DoubleValueKeyFrame> frames, List<SimpleInterpolator> interpolators, int index, long time) {
        if (frames.size() > index + 1) {
            DoubleValueKeyFrame current = frames.get(index);
            DoubleValueKeyFrame next = frames.get(index + 1);

            double t = (time == current.getTime()) ? 0 : (float) (time - current.getTime()) / (float) (next.getTime() - current.getTime());
            return current.getValue() + (t * (next.getValue() - current.getValue()));
        } else {
            return frames.get(index).getValue();
        }
    }

    private boolean update(List<DoubleValueKeyFrame> frames, int index, long time) {
        return frames.size() > index + 1 && (frames.get(index + 1).getTime() <= time);
    }

    protected class TlalocBench extends FlatrideCar<ArmorStand> {
        private final RotationFixer[] rotationFixers = new RotationFixer[5];
        private final RotationFixer[] entityRotationFixers;
        private final ArmorStand[] models = new ArmorStand[5];
        private final double radianOffset = Math.toRadians(25);
        private final Player[] players;

        private TlalocBench(Location loc) {
            super(new ArmorStand[(5 * 4 * 2) - 4]);//1]);//type == 1 || type == 2 ? 20 : 8]);
            for (int i = 0; i < rotationFixers.length; i++)
                rotationFixers[i] = new RotationFixer();
            entityRotationFixers = new RotationFixer[getEntities().length];
            for (int i = 0; i < entityRotationFixers.length; i++)
                entityRotationFixers[i] = new RotationFixer();
            players = new Player[entities.length];
            loc = loc.clone();
            loc.setX(loc.getX() + 2);
            loc.setY(loc.getY() + 0.2);
            loc.setZ(loc.getZ() + 2);
        }

        protected void showModels(Player player) {
            for (ArmorStand armorStand : models) {
                new PacketEquipment(armorStand.getEntityId(), PacketEquipment.Slot.HEAD, armorStand.getHelmet()).sendPlayer(player);
            }
        }

        protected void hideModels(Player player) {
            for (ArmorStand armorStand : models) {
                new PacketEquipment(armorStand.getEntityId(), PacketEquipment.Slot.HEAD, new ItemStack(Material.AIR)).sendPlayer(player);
            }
        }

        protected void cleanupOfflinePlayers() {
            for (int i = 0; i < players.length; i++) {
                if (players[i] != null) {
                    if (!PlayerExtensionsKt.isConnected(players[i])) {
                        players[i] = null;
                    }
                }
            }
        }

        @Override
        public boolean isPartOf(Entity target) {
            for (ArmorStand armorStand : models) {
                if (armorStand.getEntityId() == target.getEntityId())
                    return true;
            }
            return super.isPartOf(target);
        }

        @Override
        public void teleport(double x, double y, double z, float yaw, float pitch, boolean forceTeleport) {
            for (int i = 0; i < models.length; i++) {
                if (models[i] == null || !models[i].isValid()) {
                    models[i] = area.getLoc1().getWorld().spawn(new Location(area.getLoc1().getWorld(),
                            x + (3 * i),
                            y /*+ (Math.cos(pitch + radianOffset) * 0.8)*/,
                            z/* + (Math.sin(pitch + radianOffset) * 0.8)*/,
                            yaw + (i == models.length - 1 ? 0 : 180),
                            (float) Math.toDegrees(pitch + Math.PI)
                    ), ArmorStand.class);
                    TypedInstanceOwnerMetadata.Companion.setOwner(models[i], Tlaloc.this);
                    models[i].setPersistent(false);
                    models[i].setGravity(false);
                    models[i].setHelmet(i == 0 || i == models.length - 1 ? MaterialConfig.INSTANCE.getTLALOC_CENTER_CAR() : MaterialConfig.INSTANCE.getTLALOC_SIDE_CAR());
                    models[i].setBasePlate(false);
                    models[i].setVisible(false);
                    models[i].setCustomName(rideName);
                    models[i].addDisabledSlots(EquipmentSlot.values());
//                    EntityUtils.noAI(models[i]);
                } else {
                    EntityUtils.INSTANCE.teleport(models[i],
                            x + (3 * i),
                            y /*+ (Math.cos(pitch + radianOffset) * 0.8)*/,
                            z /*+ (Math.sin(pitch + radianOffset) * 0.8)*/,
                            yaw + (i == models.length - 1 ? 0 : 180),
                            (float) Math.toDegrees(pitch + Math.PI));
                    rotationFixers[i].setNextRotation((i == models.length - 1 ? pitch : -pitch) + Math.PI, 0, 0);
                    models[i].setHeadPose(rotationFixers[i].getCurrentRotation().toEuler());
                }

                if (forceModelUpdate) {
                    models[i].setHelmet(i == 0 || i == models.length - 1 ? MaterialConfig.INSTANCE.getTLALOC_CENTER_CAR() : MaterialConfig.INSTANCE.getTLALOC_SIDE_CAR());
                }
            }

            for (int i = 0; i < players.length; i++) {
                if (players[i] != null) {
                    final Player player = players[i];
                    if (player.getGameMode() != GameMode.SPECTATOR || player.getSpectatorTarget() != entities[i]) {
                        onLeave(player, null);
                        players[i] = null;
                        postLeave(player);
                    }
                }
            }

            boolean left = false;
            int index = 0;
            for (int i = 0; i < entities.length; i++) {
                if (entities[i] == null || !entities[i].isValid()) {
                    entities[i] = area.getLoc1().getWorld().spawn(new Location(area.getLoc1().getWorld(), x - 0.375 + (0.75 * index),
                            y + (SEAT_OFFSET * Math.cos(pitch + (left ? radianOffset : -radianOffset))),
                            z + (SEAT_OFFSET * Math.sin(pitch + (left ? radianOffset : -radianOffset))),
                            yaw + (left ? 180 : 0),
                            left ? (float) -Math.toDegrees(pitch + Math.PI) : (float) Math.toDegrees(pitch + Math.PI)
                    ), ArmorStand.class);
                    TypedInstanceOwnerMetadata.Companion.setOwner(entities[i], Tlaloc.this);
                    entities[i].setPersistent(false);
                    entities[i].setGravity(false);
                    entities[i].setBasePlate(false);
                    entities[i].setVisible(false);
                    entities[i].setCustomName(rideName);
                    entities[i].addDisabledSlots(EquipmentSlot.values());
                } else if (entities[i].getPassenger() != null || forceTeleport || (entities[i].getHelmet() == null || entities[i].getHelmet().getType() != Material.AIR)) {
                    EntityUtils.INSTANCE.teleport(entities[i],
                            x - 0.375 + (0.75 * index),
                            y + (SEAT_OFFSET * Math.cos(pitch + (left ? radianOffset : -radianOffset))),
                            z + (SEAT_OFFSET * Math.sin(pitch + (left ? radianOffset : -radianOffset))),
                            yaw + (left ? 180 : 0),
                            left ? (float) -Math.toDegrees(pitch + Math.PI) : (float) Math.toDegrees(pitch + Math.PI));
                    entityRotationFixers[i].setNextRotation((left ? -pitch : pitch) + Math.PI, 0, 0);
                    entities[i].setHeadPose(entityRotationFixers[i].getCurrentRotation().toEuler());
//                    NmsUtils.setHeadPose(entities[i], new EulerAngle((left ? -pitch : pitch) + Math.PI, 0, 0));
                } else if (entities[i].getPassenger() == null && entities[i].getLocation().getY() > 5) {
                    Location location = entities[i].getLocation();
                    EntityUtils.INSTANCE.teleport(entities[i], location.getX(), 5, location.getZ(), location.getYaw(), location.getPitch());
                }
                if (!left) {
                    left = true;
                } else {
                    left = false;
                    index++;
                }
            }
        }

    }
}
