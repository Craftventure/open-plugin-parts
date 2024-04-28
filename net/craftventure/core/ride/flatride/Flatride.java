package net.craftventure.core.ride.flatride;

import kotlin.Unit;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.entitymeta.MetaAnnotations;
import net.craftventure.bukkit.ktx.extension.PlayerExtensionKt;
import net.craftventure.bukkit.ktx.manager.MessageBarManager;
import net.craftventure.bukkit.ktx.util.ChatUtils;
import net.craftventure.bukkit.ktx.util.PermissionChecker;
import net.craftventure.bukkit.ktx.util.Translation;
import net.craftventure.chat.bungee.util.CVChatColor;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.async.AsyncTask;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.ktx.util.TimeUtils;
import net.craftventure.core.manager.PlayerStateManager;
import net.craftventure.core.manager.ShutdownManager;
import net.craftventure.core.metadata.GenericPlayerMeta;
import net.craftventure.core.metadata.PlayerLocationTracker;
import net.craftventure.core.ride.RideInstance;
import net.craftventure.core.ride.operator.OperableRide;
import net.craftventure.core.ride.queue.RideQueue;
import net.craftventure.core.ride.trackedride.FlatrideManager;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.generated.cvdata.tables.pojos.Ride;
import net.craftventure.database.generated.cvdata.tables.pojos.RideLog;
import net.craftventure.database.repository.BaseIdRepository;
import net.craftventure.database.type.RideLogState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.entity.EntityDismountEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Flatride<T extends FlatrideCar> implements Listener, RideInstance {
    protected List<T> cars = new LinkedList<>();
    protected final SimpleArea area;
    protected final Location exitLocation;

    protected boolean isRunning = false;
    protected boolean isEjecting = false;
    protected boolean isAutoStarting = false;
    protected long autostartAt = 0;
    protected int autoStartTask = -1;
    private Ride ride;

    protected String rideName;
    protected String achievementName;

    protected FlatrideRunnable flatrideRunnable;
    private long START_DELAY_SECONDS = 15;

    protected boolean forceModelUpdate = false;

//    private double yawFreedom = 20.0;
//    private double pitchFreedom = 90.0;
//
//    private FpsTicker.Animatable fpsTicker = () -> {
//        List<Player> passengers = getPassengers();
//        for (int i = 0; i < passengers.size(); i++) {
//            Player passenger = passengers.get(i);
//
//            Entity vehicle = passenger.getVehicle();
//            if (vehicle == null)
//                return;
//
//            PlayerExtensionKt.followDirection(
//                    passenger,
//                    yawFreedom,
//                    pitchFreedom,
//                    vehicle.getLocation().getYaw(),
//                    vehicle.getLocation().getPitch(),
//                    0.05
//            );
////            passenger.sendMessage(Math.toDegrees(pitchRadian) + " = " + (Math.toDegrees(-(pitchRadian + (Math.PI * 0.5)))));
//        }
//    };

    public Flatride(SimpleArea area, Location exitLocation, String rideName, String achievementName) {
        this.area = area;
        this.exitLocation = exitLocation;
        this.area.loadChunks(true);
        this.rideName = rideName;
        this.achievementName = achievementName;

        Bukkit.getServer().getPluginManager().registerEvents(this, CraftventureCore.getInstance());

        ride = MainRepositoryProvider.INSTANCE.getRideRepository().getByName(rideName);
        if (ride == null) {
            Logger.severe(CVChatColor.INSTANCE.getServerError() + "RideState for " + rideName + " not found");
        }
        MainRepositoryProvider.INSTANCE.getRideRepository().addListener(new BaseIdRepository.UpdateListener<>() {
            @Override
            public void onUpdated(Ride item) {
                if (item.getName().equals(rideName)) {
                    ride = item;
                }
            }
        });
        FlatrideManager.addFlatride(this);
    }

    @NotNull
    @Override
    public String getId() {
        return rideName;
    }

    @NotNull
    @Override
    public Set<RideQueue> getQueues() {
        return Collections.emptySet();
    }

    public String getRideName() {
        return rideName;
    }

    public SimpleArea getArea() {
        return area;
    }

    protected void debug() {
        START_DELAY_SECONDS = 1;
    }

    @Nullable
    @Override
    public Ride getRide() {
        return ride;
    }

    public final void respawnModels() {
        forceModelUpdate = true;
        updateCarts(true);
        forceModelUpdate = false;
    }

    protected abstract void updateCarts(boolean forceTeleport);

    protected abstract FlatrideRunnable provideRunnable();

    public List<Player> getPassengers() {
        List<Player> passengers = new ArrayList<>();
        for (T car : cars) {
            for (Entity entity : car.getEntities()) {
                for (Entity passenger : entity.getPassengers()) {
                    if (passenger instanceof Player) {
                        passengers.add((Player) passenger);
                    }
                }
            }
        }
        return passengers;
    }

    public boolean hasPassengers() {
        for (T car : cars) {
            if (car.passengerCount() > 0)
                return true;
        }
        return false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean canStart() {
        return !isRunning();
    }

    public void start() {
        if (!canStart())
            return;
        isRunning = true;

        for (Player player : getPassengers()) {
            MessageBarManager.remove(player, ChatUtils.INSTANCE.getID_RIDE());
        }

        prepareStart();
        flatrideRunnable = provideRunnable();
        flatrideRunnable.runTaskTimer(CraftventureCore.getInstance(), 1L, 1L);
//        FpsTicker.add(fpsTicker);
    }

    public void sendMoveAwayMessage(Player player) {
        player.sendMessage(Translation.RIDE_TELEPORT_AWAY.getTranslation(player, ride != null ? ride.getDisplayName() : "ride"));
    }

    protected void prepareStart() {

    }

    public final void awardAchievement(Player player) {
        new AsyncTask() {
            @Override
            public void doInBackground() {
                MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(player.getUniqueId(), achievementName);
                MainRepositoryProvider.INSTANCE.getRideCounterRepository().increaseCounter(player.getUniqueId(), rideName);
            }
        }.executeNow();
    }

    public void playerLeftRideCompleted(Player player) {
        awardAchievement(player);
        new AsyncTask() {
            @Override
            public void doInBackground() {
                MainRepositoryProvider.INSTANCE.getRideLogRepository().insertSilent(new RideLog(UUID.randomUUID(), player.getUniqueId(), rideName, RideLogState.COMPLETED, LocalDateTime.now()));
            }
        }.executeNow();
    }

    public void stop() {
        if (isRunning()) {
            try {
                flatrideRunnable.cancel();
            } catch (Exception e) {
                Logger.capture(e);
            }
            for (T car : cars) {
                for (Entity entity : car.getEntities()) {
                    if (entity.getPassenger() instanceof Player) {
                        playerLeftRideCompleted((Player) entity.getPassenger());
                    }
                }
            }
            isEjecting = true;
            eject();
            isEjecting = false;
            isRunning = false;
            isAutoStarting = false;
//            FpsTicker.remove(fpsTicker);
        }
    }

    protected void eject() {
        for (T car : cars) {
            car.eject();
        }
    }

    protected String getRideDisplayName() {
        if (ride != null)
            return ride.getDisplayName();
        if (rideName != null)
            return rideName;
        return "This ride";
    }

    protected void displayWaitingForStartMessage(Player player) {
//        Logger.consoleAndIngame("Updating message for %s", player.getName());
        boolean isBeingOperated = this instanceof OperableRide && ((OperableRide) this).isBeingOperated();
        Player operator = this instanceof OperableRide ? ((OperableRide) this).getOperatorForSlot(0) : null;

        MessageBarManager.display(player,
                Component.text(isBeingOperated ? String.format("This ride will start when it's operator (%1$s) dispatches it", operator != null ? operator.getName() : "unknown") :
                        String.format("%1$s will start in %2$d seconds", getRideDisplayName(), (int) Math.ceil((autostartAt - System.currentTimeMillis()) / 1000.0)), CVTextColor.getServerNotice()),
                MessageBarManager.Type.RIDE,
                TimeUtils.secondsFromNow(2),
                ChatUtils.INSTANCE.getID_RIDE());
    }

    protected void updateWaitingMessageForAll() {
        for (T car : cars) {
            for (Entity entity : car.getEntities()) {
                if (entity.getPassenger() instanceof Player) {
                    displayWaitingForStartMessage((Player) entity.getPassenger());
                }
            }
        }
    }

    protected boolean cancelAutoStart() {
        if (isAutoStarting) {
            autostartAt = 0;
            isAutoStarting = false;
            Bukkit.getScheduler().cancelTask(autoStartTask);
            updateWaitingMessageForAll();
            onAutoScheduleCanceled();
            return true;
        }
        return false;
    }

    protected void onAutoScheduleStarted() {

    }

    protected void onAutoScheduleCanceled() {

    }

    protected boolean scheduleAutoStart() {
        if (!hasPassengers())
            return false;

        boolean isBeingOperated = this instanceof OperableRide && ((OperableRide) this).isBeingOperated();

        if (!isBeingOperated && !isRunning && !isAutoStarting) {
            autostartAt = System.currentTimeMillis() + (START_DELAY_SECONDS * 1000);
            isAutoStarting = true;

            AtomicInteger ticks = new AtomicInteger(0);

//            Logger.info("Schedule autostart");
            Bukkit.getScheduler().cancelTask(autoStartTask);
            autoStartTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), () -> {
                ticks.incrementAndGet();

                boolean hasPassengers = hasPassengers();
                if (!hasPassengers) {
                    cancelAutoStart();
                    return;
                }

                if (System.currentTimeMillis() >= autostartAt) {
                    if (!Flatride.this.isRunning()) {
                        Bukkit.getScheduler().cancelTask(autoStartTask);
                        Flatride.this.start();
                        autoStartTask = -1;
                        isAutoStarting = false;
                    }
                } else if (ticks.get() % 20 == 0) {
                    updateWaitingMessageForAll();
                }
            }, 1L, 1L);
            updateWaitingMessageForAll();
            onAutoScheduleStarted();
            return true;
        }
        return false;
    }

    protected boolean shouldCancel(Entity clicked, Player player) {
        return false;
    }

    protected boolean handleClick(Entity clicked, Player player) {
        if (player.isInsideVehicle())
            return false;
        if (CraftventureCore.getOperatorManager().isOperatingSomewhere(player))
            return false;
        for (T car : cars) {
            if (car.isPartOf(clicked)) {
                if (isRunning()) {
                    MessageBarManager.display(player,
                            Component.text("You can't enter this ride while it's running", CVTextColor.getServerNotice()),
                            MessageBarManager.Type.RIDE,
                            TimeUtils.secondsFromNow(4),
                            ChatUtils.INSTANCE.getID_RIDE());

                    if (!PlayerExtensionKt.isCrew(player)) {
                        return true;
                    }
                }
                GenericPlayerMeta cvMetadata = MetaAnnotations.getMetadata(player, GenericPlayerMeta.class);
                if (cvMetadata != null) {
                    Location lastExitLocation = cvMetadata.getLastExitLocation();
                    if (lastExitLocation != null) {
                        Location playerLocation = player.getLocation();
                        if (lastExitLocation.getYaw() == playerLocation.getYaw() || lastExitLocation.getPitch() == playerLocation.getPitch()) {
                            return true;
                        }
                    }
                }

                if (ShutdownManager.INSTANCE.getShuttingDown()) {
                    player.sendMessage(Translation.SHUTDOWN_PREPARING.getTranslation(player));
                    return true;
                }

                boolean canEnter = ride.getState().isOpen() && (ride.getState().getPermission() == null || player.hasPermission(ride.getState().getPermission()));

                if (!canEnter) {
                    if (CraftventureCore.getInstance().isShuttingDown())
                        player.sendMessage(Translation.SHUTDOWN_PREPARING.getTranslation(player));
                    else
                        player.sendMessage(Translation.RIDE_STATE_CLOSED.getTranslation(player));

                    if (PermissionChecker.INSTANCE.isCrew(player))
                        player.sendMessage(CVChatColor.INSTANCE.getServerNotice() + "As crew you can obviously enter ;)");
                    else
                        return true;
                }

                if (car.setPassenger(clicked, player)) {
                    new AsyncTask() {
                        @Override
                        public void doInBackground() {
                            MainRepositoryProvider.INSTANCE.getRideLogRepository().insertSilent(new RideLog(UUID.randomUUID(), player.getUniqueId(), rideName, RideLogState.ENTER, LocalDateTime.now()));
                        }
                    }.executeNow();
                    Entity entity = car.getEntity(clicked);
                    onEnter(player, entity);
                    scheduleAutoStart();

                    displayWaitingForStartMessage(player);
                }
                return true;
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof Player)
            return;
        if (event.getPlayer().isInsideVehicle())
            return;
//        Logger.console("Flatride interact " + rideDisplayName);
        if (handleClick(event.getRightClicked(), event.getPlayer())) {
            event.setCancelled(true);
//            Logger.console("Handled by " + rideDisplayName);
        } else if (shouldCancel(event.getRightClicked(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player)
            return;
        if (event.getPlayer().isInsideVehicle())
            return;
//        Logger.console("Flatride interact " + rideDisplayName);
        if (handleClick(event.getRightClicked(), event.getPlayer())) {
            event.setCancelled(true);
//            Logger.console("Handled by " + rideDisplayName);
        } else if (shouldCancel(event.getRightClicked(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDismountEvent(EntityDismountEvent event) {
        if (event.getDismounted() instanceof Player)
            return;
        if (event.getEntity() instanceof Player) {
            DismountType dismountType = onDismount((Player) event.getEntity(), event.getDismounted());
            if (dismountType == DismountType.CANCEL) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleEnterEvent(VehicleEnterEvent event) {
        for (T car : cars) {
            if (car.isPartOf(event.getVehicle())) {
                event.setCancelled(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleExit(VehicleExitEvent event) {
        for (T car : cars) {
            if (car.isPartOf(event.getVehicle())) {
                event.setCancelled(false);
            }
        }
    }

    protected boolean handlesExit(Player player, final Entity dismounted, T car) {
        return false;
    }

    protected void removePlayer(@Nonnull Player player, @Nullable Entity dismounted) {
        MessageBarManager.remove(player, ChatUtils.INSTANCE.getID_RIDE());
        new AsyncTask() {
            @Override
            public void doInBackground() {
                MainRepositoryProvider.INSTANCE.getRideLogRepository().insertSilent(new RideLog(UUID.randomUUID(), player.getUniqueId(), rideName,
                        isRunning ? RideLogState.LEFT : RideLogState.LEFT_BEFORE_START, LocalDateTime.now()));
                if (isRunning) {
                    MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(player.getUniqueId(), "early_leaver");
                }
            }
        }.executeNow();

//                        Logger.console("onLeave");
        onLeave(player, player.getVehicle() != null ? player.getVehicle() : dismounted);
        if (player.getVehicle() != null || dismounted != null) {
            player.leaveVehicle();
//                            event.getPlayer().chat("/ride mainstreetcaroussel");

            if (!hasPassengers() && isAutoStarting) {
                cancelAutoStart();
            }
        }
        postLeave(player);
    }

    private DismountType onDismount(final Player player, final Entity dismounted) {
        for (T car : cars) {
            if (car.isPassengerOrSeat(player, dismounted)) {
                if (!car.isExitAllowed(player, dismounted)) {
                    return DismountType.CANCEL;
                }
                if (handlesExit(player, dismounted, car)) {
                    return DismountType.HANDLED;
                }
                removePlayer(player, dismounted);
                return DismountType.HANDLED;
            }
        }
        return DismountType.UNHANDLED;
    }

    enum DismountType {
        HANDLED,
        CANCEL,
        UNHANDLED
    }

//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onChunkUnload(ChunkUnloadEvent event) {
//        if (area.overlaps(event.getChunk())) {
//            event.setCancelled(true);
//        }
//    }

    protected void onEnter(Player player, @Nullable Entity vehicle) {
        PlayerStateManager.INSTANCE.withGameState(player, (playerState) -> {
            playerState.setRide(this);
            return Unit.INSTANCE;
        });
    }

    protected void onLeave(Player player, @Nullable Entity vehicle) {
        GenericPlayerMeta cvMetadata = MetaAnnotations.getMetadata(player, GenericPlayerMeta.class);
        if (cvMetadata != null) {
            Location exitLocation = player.getLocation().clone().add(CraftventureCore.getRandom().nextBoolean() ? 0.5 : -0.5, 0, CraftventureCore.getRandom().nextBoolean() ? 0.5 : -0.5);
            cvMetadata.setLastExitLocation(exitLocation);
        }
        PlayerStateManager.INSTANCE.withGameState(player, (playerState) -> {
            playerState.clearRide(this);
            return Unit.INSTANCE;
        });
    }

    protected void teleportToExit(Player player) {
        if (isRunning()) {
            GenericPlayerMeta cvMetadata = MetaAnnotations.getMetadata(player, GenericPlayerMeta.class);
            Location location = (exitLocation != null ? exitLocation : player.getLocation())
                    .clone().add(CraftventureCore.getRandom().nextBoolean() ? 0.5 : -0.5, 0, CraftventureCore.getRandom().nextBoolean() ? 0.5 : -0.5);
            PlayerLocationTracker.setLeaveLocation(player, location, true);
            if (cvMetadata != null) {
                cvMetadata.setLastExitLocation(exitLocation);
            }
        }
    }

    protected void postLeave(Player player) {
        teleportToExit(player);
    }

    protected long lastStartTime = 0L;

    protected long getLastStartTime() {
        return lastStartTime;
    }

    public abstract class FlatrideRunnable extends BukkitRunnable {
        protected final long startTime;

        public FlatrideRunnable() {
            this.startTime = lastStartTime = System.currentTimeMillis();
        }

        @Override
        public final void run() {
            updateTick();
        }

        public void updateTick() {

        }
    }
}
