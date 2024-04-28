package net.craftventure.core.ride.trackedride;

import kotlin.Unit;
import net.craftventure.audioserver.api.AudioServerApi;
import net.craftventure.audioserver.event.AudioServerConnectedEvent;
import net.craftventure.bukkit.ktx.area.Area;
import net.craftventure.bukkit.ktx.entitymeta.MetaAnnotations;
import net.craftventure.bukkit.ktx.extension.LocationExtensionsKt;
import net.craftventure.bukkit.ktx.manager.FeatureManager;
import net.craftventure.bukkit.ktx.plugin.PluginProvider;
import net.craftventure.bukkit.ktx.util.ItemStackUtils2;
import net.craftventure.bukkit.ktx.util.PermissionChecker;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.async.AsyncTask;
import net.craftventure.core.async.AsyncTaskHelperKt;
import net.craftventure.core.ktx.json.MoshiBase;
import net.craftventure.core.ktx.json.MoshiBaseKt;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.manager.Allow;
import net.craftventure.core.manager.Deny;
import net.craftventure.core.manager.GrantResult;
import net.craftventure.core.manager.PlayerStateManager;
import net.craftventure.core.metadata.GenericPlayerMeta;
import net.craftventure.core.metadata.PlayerLocationTracker;
import net.craftventure.core.ride.PukeEffect;
import net.craftventure.core.ride.RideInstance;
import net.craftventure.core.ride.queue.RideQueue;
import net.craftventure.core.ride.trackedride.config.SceneItem;
import net.craftventure.core.ride.trackedride.config.SceneSettings;
import net.craftventure.core.ride.trackedride.config.TrackedRideConfig;
import net.craftventure.core.ride.trackedride.segment.StationSegment;
import net.craftventure.core.script.ScriptController;
import net.craftventure.core.script.ScriptManager;
import net.craftventure.core.utils.OperatorUtils;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.generated.cvdata.tables.pojos.Ride;
import net.craftventure.database.generated.cvdata.tables.pojos.RideLog;
import net.craftventure.database.repository.BaseIdRepository;
import net.craftventure.database.type.RideLogState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


public abstract class TrackedRide implements Listener, RideInstance {
    private final Area area;
    public List<TrackSegment> trackSegments = new ArrayList<>();
    public List<RideTrain> rideTrains = new ArrayList<>();
    public List<ItemStack> rideTrainsItems = new ArrayList<>();
    private List<TrackSegment.DistanceListener> sceneListeners = new LinkedList<>();
    private final String name;
    private boolean hasInitialised = false;
    //    private int updaterTaskId = -1;
    private final DecimalFormat decimalFormat = new DecimalFormat("#0.00");
    //    private Runnable updaterTask = this::update;
    private Inventory debugInventory;
    private final Location exitLocation;
    private final String achievementName;
    private final String rideName;
    private double pukeRate = 0;
    private Ride ride;
    private boolean emergencyStopActive = false;
    private final Set<OnEmergencyStopListener> onEmergencyStopListenerSet = new HashSet<>();
    protected Set<PreTrainUpdateListener> preTrainUpdateListeners = new HashSet<>();
    protected Set<RideCompletionListener> rideCompletionListeners = new HashSet<>();
    private boolean fixExitLocation = false;
    private boolean fromConfig;

    private final Set<DestroyListener> destroyListeners = new HashSet<>();

    private final Set<RideQueue> queues = new HashSet<>();

    public TrackedRide(String name, Area area, Location exitLocation, String achievementName, String rideName) {
        this.name = name;
        this.area = area;
        this.exitLocation = exitLocation;
        this.achievementName = achievementName;
        this.rideName = rideName;
        area.loadChunks(true);

        for (Entity entity : area.getWorld().getEntities()) {
            if (entity.getCustomName() != null && entity.getCustomName().equals(name)) {
                entity.remove();
            }
        }

        ride = MainRepositoryProvider.INSTANCE.getRideRepository().getByName(rideName);
        if (ride == null) {
            Logger.severe("Ride for " + rideName + " not found");
        }
        MainRepositoryProvider.INSTANCE.getRideRepository().addListener(new BaseIdRepository.UpdateListener<>() {
            @Override
            public void onUpdated(Ride item) {
                if (item.getName().equals(rideName)) {
                    ride = item;
                }
            }
        });

        Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance());
    }

    @NotNull
    @Override
    public String getId() {
        return name;
    }

    public void addDestroyListener(DestroyListener destroyListener) {
        destroyListeners.add(destroyListener);
    }

    public void removeDestroyListener(DestroyListener destroyListener) {
        destroyListeners.remove(destroyListener);
    }

    public void applyQueueToAllStations(RideQueue rideQueue) {
        trackSegments.forEach(segment -> {
            if (segment instanceof StationSegment station) {
                ((StationSegment) segment).setQueue(rideQueue);
            }
        });
    }

    public void addQueue(RideQueue rideQueue) {
        queues.add(rideQueue);
    }

    public void removeQueue(RideQueue rideQueue) {
        queues.remove(rideQueue);
    }

    @Override
    public @NotNull Set<RideQueue> getQueues() {
        return queues;
    }

    public void destroy() {
        if (!hasInitialised) {
            return;
        }
        rideTrains.forEach((train) -> {
            train.eject();
            train.despawn();
        });
        trackSegments.forEach(segment -> segment.destroy());
        destroyListeners.forEach(l -> l.onDestroy(this));

        TrackedRideManager.removeTrackedRide(this);

        hasInitialised = false;
    }

    public boolean isFromConfig() {
        return fromConfig;
    }

    public void setFromConfig(boolean fromConfig) {
        this.fromConfig = fromConfig;
    }

    public boolean isFixExitLocation() {
        return fixExitLocation;
    }

    public void setFixExitLocation(boolean fixExitLocation) {
        this.fixExitLocation = fixExitLocation;
    }

    public boolean isBeingOperated() {
        return OperatorUtils.INSTANCE.isBeingOperated(this);
    }

    public void addPreTrainUpdateListener(PreTrainUpdateListener preTrainUpdateListener) {
        if (preTrainUpdateListener != null) {
            preTrainUpdateListeners.add(preTrainUpdateListener);
        }
    }

    public void removePreTrainUpdateListener(PreTrainUpdateListener preTrainUpdateListener) {
        preTrainUpdateListeners.remove(preTrainUpdateListener);
    }

    public void addOnEmergencyStopListener(OnEmergencyStopListener onEmergencyStopListener) {
        if (onEmergencyStopListener != null) {
            onEmergencyStopListenerSet.add(onEmergencyStopListener);
        }
    }

    public void removeOnEmergencyStopListener(OnEmergencyStopListener onEmergencyStopListener) {
        onEmergencyStopListenerSet.remove(onEmergencyStopListener);
    }

    public void addOnRideCompletionListener(RideCompletionListener rideCompletionListener) {
        if (rideCompletionListener != null) {
            rideCompletionListeners.add(rideCompletionListener);
        }
    }

    public void removeOnRideCompletionListener(RideCompletionListener rideCompletionListener) {
        rideCompletionListeners.remove(rideCompletionListener);
    }

    public boolean isEmergencyStopActive() {
        return emergencyStopActive;
    }

    public void activateEmergencyStop(boolean emergencyStopActive) {
        if (this.emergencyStopActive != emergencyStopActive) {
            this.emergencyStopActive = emergencyStopActive;

            for (TrackSegment trackSegment : getTrackSegments()) {
                trackSegment.onEmergencyStopActivated(this.emergencyStopActive);
            }
            onEmergencyStopChanged(emergencyStopActive);
        }
    }

    protected void onEmergencyStopChanged(boolean emergencyStopActive) {

    }

    @Nullable
    public Ride getRide() {
        return ride;
    }

    @Nullable
    public TrackSegment getSegmentById(String id) {
        for (int i = 0; i < trackSegments.size(); i++) {
            TrackSegment trackSegment = trackSegments.get(i);
            if (id.equalsIgnoreCase(trackSegment.getId())) {
                return trackSegment;
            }
        }
        return null;
    }

    public double getPukeRate() {
        return pukeRate;
    }

    public void setPukeRate(double pukeRate) {
        this.pukeRate = pukeRate;
    }

    public void onRideCompleted(Player player, RideCar rideCar) {
//        Logger.console("Ride " + rideName + " completed by " + player.getName());
        if (pukeRate > 0) {
            PukeEffect.Companion.playRandom(player, pukeRate);
        }
        new AsyncTask() {
            @Override
            public void doInBackground() {
                MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(player.getUniqueId(), achievementName);
                MainRepositoryProvider.INSTANCE.getRideCounterRepository().increaseCounter(player.getUniqueId(), rideName);
                MainRepositoryProvider.INSTANCE.getRideLogRepository().insertSilent(new RideLog(UUID.randomUUID(), player.getUniqueId(), rideName, RideLogState.COMPLETED, LocalDateTime.now()));
                onRideCompletedAsync(player);

                for (RideCompletionListener rideCompletionListener : rideCompletionListeners) {
                    rideCompletionListener.onRideCompletedAsync(player, rideCar);
                }
            }
        }.executeNow();
    }

    public void onRideCompletedAsync(Player player) {

    }

    public Location getExitLocation() {
        return exitLocation;
    }

    public boolean hasPassengers() {
        for (int i = 0; i < rideTrains.size(); i++) {
            RideTrain rideTrain = rideTrains.get(i);
            if (rideTrain.getPassengerCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPassengersInNonEnterableTrains() {
        for (int i = 0; i < rideTrains.size(); i++) {
            RideTrain rideTrain = rideTrains.get(i);
            if (!rideTrain.canEnter && rideTrain.getPassengerCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public int getPassengerCount() {
        int count = 0;
        for (int i = 0; i < rideTrains.size(); i++) {
            RideTrain rideTrain = rideTrains.get(i);
            count += rideTrain.getPassengerCount();
        }
        return count;
    }

    public List<Player> getPassengers() {
        List<Player> passengers = new LinkedList<>();
        for (int i = 0; i < rideTrains.size(); i++) {
            RideTrain rideTrain = rideTrains.get(i);
            passengers.addAll(rideTrain.getPassengers());
        }
        return passengers;
    }

    public void openDebugMenu(Player player) {
        if (debugInventory == null) {
            debugInventory = Bukkit.createInventory(null, 9 * 5, "Debug " + getName());

            for (int i = 0; i < rideTrains.size(); i++) {
                RideTrain rideTrain = rideTrains.get(i);
                ItemStack trainStack = new ItemStack(Material.GRAY_TERRACOTTA);
                ItemStackUtils2.setDisplayName(trainStack, rideTrain.getClass().getSimpleName());
                rideTrainsItems.add(trainStack);
            }
        }
        player.openInventory(debugInventory);
    }

    protected void syncAudio(Player player) {
        for (int i = 0; i < rideTrains.size(); i++) {
            RideTrain rideTrain = rideTrains.get(i);
            List<RideCar> cars = rideTrain.getCars();
            for (int i1 = 0; i1 < cars.size(); i1++) {
                RideCar rideCar = cars.get(i1);
                if (rideCar.containsPlayer(player)) {
                    syncAudio(player, rideCar);
                }
            }
        }
    }

    protected void syncAudio(Player player, RideCar rideCar) {
        if (rideCar.getAudioName() != null && rideCar.getSync() != null) {
            AudioServerApi.INSTANCE.addAndSync(rideCar.getAudioName(), player, rideCar.getSync());
        } else if (rideCar.attachedTrain.getAudioName() != null && rideCar.attachedTrain.getSync() != null) {
            AudioServerApi.INSTANCE.addAndSync(rideCar.attachedTrain.getAudioName(), player, rideCar.attachedTrain.getSync());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAudioServerConnected(AudioServerConnectedEvent audioServerConnectedEvent) {
        syncAudio(audioServerConnectedEvent.getPlayer());
    }

    public Area getArea() {
        return area;
    }

//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onChunkUnload(ChunkUnloadEvent event) {
//        if (area.overlaps(event.getChunk())) {
//            event.setCancelled(true);
//        }
//    }

    public GrantResult canEnter(Player player) {
        return canEnter(player, false);
    }

    public GrantResult canEnter(Player player, boolean ignoreVehicle) {
        if (ride == null) {
            return new Deny("This ride is not properly setup");
        }
        if (CraftventureCore.getOperatorManager().isOperatingSomewhere(player))
            return new Deny("You're currently operating a ride");

        GrantResult joinRide = TrackedRideHelper.getAllowedToManuallyJoinRide(player);
        if (joinRide.isNotAllowed()) {
            return joinRide;
        }
//        if (player.isInsideVehicle() && !ignoreVehicle) {
//            return new Deny("Must leave current vehicle first");
//        }
        if (CraftventureCore.getInstance().isShuttingDown()) {
            return new Deny("The server is shutting down");
        }
        if (!(ride.getState().getPermission() == null || player.hasPermission(ride.getState().getPermission()))) {
            return new Deny("You don't meet the required permissions to enter this ride");
        }
        if (!ride.getState().isOpen() && !PermissionChecker.INSTANCE.isCrew(player)) {
            return new Deny("This ride isn't opened");
        }

        GenericPlayerMeta cvMetadata = MetaAnnotations.getMetadata(player, GenericPlayerMeta.class);
        if (cvMetadata != null) {
            Location lastExitLocation = cvMetadata.getLastExitLocation();
            if (lastExitLocation != null) {
                Location playerLocation = player.getLocation();
//                Logger.console("Checking last exit location %s vs %s and %s vs %s",
//                        lastExitLocation.getYaw(), playerLocation.getYaw(), lastExitLocation.getPitch(), playerLocation.getPitch());
                if (lastExitLocation.getYaw() == playerLocation.getYaw() || lastExitLocation.getPitch() == playerLocation.getPitch()) {
//                    Logger.console("Deny");
                    return new Deny("This action is currently unavailable");
                }
            }
        }
        return Allow.INSTANCE;
    }

    public boolean requestEnter(Player player, RideCar rideCar, Entity entity) {
        TrackSegment segment = rideCar.attachedTrain.getFrontCarTrackSegment();
        if (segment instanceof StationSegment) {
            StationSegment station = (StationSegment) segment;
            if (station.getQueue() != null && station.getQueue().isActive()) {
                player.sendMessage(Component.text("At the moment you can only enter this ride by using the queue", CVTextColor.getServerError()));
                return false;
            }
        }
        for (RideQueue rideQueue : queues) {
            if (rideQueue != null && rideQueue.isActive()) {
                player.sendMessage(Component.text("At the moment you can only enter this ride by using the queue", CVTextColor.getServerError()));
                return false;
            }
        }

        if (player.isInsideVehicle() && !PermissionChecker.INSTANCE.isCrew(player)) {
            return false;
        }

        GrantResult enterState = canEnter(player);
        if (enterState instanceof Deny) {
            Deny deny = (Deny) enterState;
            player.sendMessage(Component.text("Can't enter: " + deny.getReason(), CVTextColor.getServerError()));
            return false;
        }

        return rideCar.addPassenger(player, entity);
    }

    public void onMounted(RideCar rideCar, Player player, Entity entity) {
        AsyncTaskHelperKt.executeAsync(() -> {
            MainRepositoryProvider.INSTANCE.getRideLogRepository().insertSilent(new RideLog(UUID.randomUUID(), player.getUniqueId(), rideName, RideLogState.ENTER, LocalDateTime.now()));
            return null;
        });
        syncAudio(player, rideCar);
        rideCar.enteredCar(player, entity);

        PlayerStateManager.INSTANCE.withGameState(player, (playerState) -> {
            playerState.setRide(this);
            return Unit.INSTANCE;
        });
    }

    public void onDismounted(RideCar rideCar, Player player, Entity entity) {
        GenericPlayerMeta cvMetadata = MetaAnnotations.getMetadata(player, GenericPlayerMeta.class);
//            rideCar.exitedCar(player, entity);
//                        player.teleport(rideCar.getTrackSegment().getLeaveLocation(player, rideCar));
        final boolean isEjecting = rideCar.attachedTrain.isEjecting();
        new AsyncTask() {
            @Override
            public void doInBackground() {
                if (isEjecting || rideCar.attachedTrain.canEnter) {
                    MainRepositoryProvider.INSTANCE.getRideLogRepository().insertSilent(new RideLog(UUID.randomUUID(), player.getUniqueId(), rideName, RideLogState.LEFT, LocalDateTime.now()));
                } else {
                    MainRepositoryProvider.INSTANCE.getRideLogRepository().insertSilent(new RideLog(UUID.randomUUID(), player.getUniqueId(), rideName, RideLogState.LEFT_BEFORE_START, LocalDateTime.now()));
                    MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(player.getUniqueId(), "early_leaver");
                }
            }
        }.executeNow();

        Location leaveLocation = rideCar.getTrackSegment().getLeaveLocation(player, rideCar, isEjecting ? TrackSegment.LeaveType.EJECT : TrackSegment.LeaveType.LEFT);
        if (fixExitLocation) leaveLocation = LocationExtensionsKt.safeHigherLocation(leaveLocation);
        final Location leaveLocationFinal = leaveLocation;

        PlayerLocationTracker.setLeaveLocation(player, leaveLocationFinal);
//        player.teleport(leaveLocationFinal, PlayerTeleportEvent.TeleportCause.PLUGIN);
//                                EntityUtils.teleport(player, leaveLocation);
//                                Logger.info("Teleporting exiting player %s to %.2f, %.2f, %.2f = %s", false, player.getName(), location.getX(), location.getY(), location.getZ(), result);
        if (cvMetadata != null)
            cvMetadata.setLastExitLocation(leaveLocationFinal);

        PlayerStateManager.INSTANCE.withGameState(player, (playerState) -> {
            playerState.clearRide(this);
            return Unit.INSTANCE;
        });
        rideCar.exitedCar(player, entity);
//                    }, 0L);

    }

    public void addTrackSection(TrackSegment trackSegment) {
        if (hasInitialised)
            return;
        trackSegments.add(trackSegment);
    }

    public void addTrackSections(TrackSegment... trackSegments) {
        if (hasInitialised)
            return;
        this.trackSegments.addAll(Arrays.asList(trackSegments));
    }

    public void addTrain(RideTrain rideTrain) {
        if (hasInitialised)
            return;
        if (rideTrain.getFrontCarTrackSegment().isBlockSection()) {
            rideTrain.getFrontCarTrackSegment().setBlockReservedTrain(rideTrain);
        }
        rideTrain.setTrackedRide(this);
        rideTrains.add(rideTrain);
        rideTrain.getFrontCarTrackSegment().onTrainEnteredSection(null, rideTrain);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        if (ride != null && ride.getDisplayName() != null) {
            return ride.getDisplayName();
        }
        return name;
    }

    public void initialize() {
        if (!hasInitialised) {
            TrackedRideManager.addTrackedRide(this);
            for (int i = 0; i < trackSegments.size(); i++) {
                TrackSegment trackSegment = trackSegments.get(i);
                trackSegment.initialize();
            }
            for (int i = 0; i < rideTrains.size(); i++) {
                RideTrain rideTrain = rideTrains.get(i);
                rideTrain.spawn();
            }
//            updaterTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), updaterTask, 1l, 1l);
            hasInitialised = true;
            reloadSceneLinks();
        }
    }

    public void reloadSceneLinks() {
        File file = new File(CraftventureCore.getInstance().getDataFolder(), "data/ride/" + name + "/scenes.json");
        sceneListeners.forEach((listener) -> {
            trackSegments.forEach((segment) -> {
                segment.remove(listener);
            });
        });
        List<TrackSegment.DistanceListener> listeners = new LinkedList<>();
        if (file.exists()) {
            try {
                SceneSettings sceneSettings = MoshiBase.INSTANCE.parseFile(MoshiBaseKt.getCvMoshi().adapter(SceneSettings.class), file);
                List<SceneItem> items = sceneSettings.getItems();
                TrackedRideHelper.INSTANCE.checkSceneItemDuplicates(items);
                for (int i = 0; i < items.size(); i++) {
                    SceneItem sceneItem = items.get(i);
                    boolean found = false;
                    List<TrackSegment> trackSegments1 = getTrackSegments();
                    for (int i1 = 0; i1 < trackSegments1.size(); i1++) {
                        TrackSegment trackSegment = trackSegments1.get(i1);
                        if (trackSegment.getId().equals(sceneItem.getSegmentId())) {
                            if (trackSegment.getLength() >= sceneItem.getAt()) {
                                TrackSegment.DistanceListener listener = new TrackSegment.DistanceListener(sceneItem.getAt(), true) {
                                    @Override
                                    public void onTargetHit(@NotNull RideCar rideCar) {
                                        if (sceneItem.getPlayWithoutPlayers() || !sceneItem.getPlayWithoutPlayers() && rideCar.attachedTrain.getPassengerCount() > 0) {
                                            if (sceneItem.getDebug() && PluginProvider.isNonProductionServer()) {
                                                Logger.debug("Triggered: " + sceneItem.getName() + " at " + sceneItem.getAt() + " segment=" + sceneItem.getSegmentId() + " playWithoutPlayers=" +
                                                        sceneItem.getPlayWithoutPlayers() + " passengerCount=" + rideCar.getAttachedTrain().getPassengerCount() +
                                                        " type=" + sceneItem.getType(), true);
                                            }
                                            ScriptController scriptController = ScriptManager.getScriptController(sceneItem.getGroupId(), sceneItem.getName());
                                            if (scriptController != null) {
                                                if (sceneItem.getType() == SceneItem.Type.START) {
                                                    scriptController.start();
                                                } else if (sceneItem.getType() == SceneItem.Type.RESTART) {
                                                    scriptController.restart();
                                                } else {
                                                    scriptController.stop();
                                                }
                                            } else {
                                                Logger.severe("Script " + sceneItem.getGroupId() + ":" + sceneItem.getName() + " not found for " + file.getPath(), true);
                                            }
                                        } else {
                                            if (sceneItem.getDebug() && PluginProvider.isNonProductionServer()) {
                                                Logger.debug("No players for: " + sceneItem.getName() + " at " + sceneItem.getAt() + " playWithoutPlayers=" +
                                                        sceneItem.getPlayWithoutPlayers() + " passengerCount=" + rideCar.getAttachedTrain().getPassengerCount() +
                                                        " type=" + sceneItem.getType(), true);
                                            }
                                        }
                                    }
                                };
                                listeners.add(listener);
                                trackSegment.add(listener);
//                                Logger.console("Bound " + sceneItem.getSegmentId() + " > " + sceneItem.getAt() + " > " + sceneItem.getType() + " > " + sceneItem.getGroupId() + " > " + sceneItem.getName());
                                found = true;
                                break;
                            } else {
                                Logger.severe("Tried to bind a scene to " + sceneItem.getAt() + " for segment " + sceneItem.getSegmentId() + " while that segment has a length of only " + trackSegment.getLength());
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found)
                        Logger.severe("Failed to bind SceneItem to " + sceneItem.getSegmentId() + ", that segment doesn't seem to exists");
                }

                items.forEach(item -> {
                    List<SceneItem> others = items.stream()
                            .filter(other -> other.getGroupId().equals(item.getGroupId()) && other.getName().equals(item.getName()))
                            .collect(Collectors.toList());

                    boolean starts = others.stream().anyMatch(other -> other.getType().getStarts());
                    boolean stops = others.stream().anyMatch(other -> other.getType().getStops());

                    if (!item.getDontWarnNotStopping()) {
                        if (starts && !stops) {
                            Logger.warn("%s script %s/%s starts but doesn't stop (infinite). Did you forget to add a stoplink?", true, name, item.getGroupId(), item.getName());
                        } else if (stops && !starts) {
                            Logger.warn("%s script %s/%s stops but doesn't start (nostart). Did you forget to add a startlink?", true, name, item.getGroupId(), item.getName());
                        }
                    }
                });
            } catch (Exception e) {
                Logger.capture(e);
            }
        } else {
        }
        this.sceneListeners = listeners;
    }

    public List<TrackSegment> getTrackSegments() {
        return trackSegments;
    }

    public List<RideTrain> getRideTrains() {
        return rideTrains;
    }

    public void update() {
        if (!preTrainUpdateListeners.isEmpty()) {
            for (PreTrainUpdateListener preTrainUpdateListener : preTrainUpdateListeners) {
                for (int i = 0; i < rideTrains.size(); i++) {
                    RideTrain rideTrain = rideTrains.get(i);
                    List<RideCar> cars = rideTrain.getCars();
                    for (int c = 0; c < cars.size(); c++) {
                        RideCar car = cars.get(c);
                        preTrainUpdateListener.onCarPreUpdate(car);
                    }
                }
            }
        }
        if (debugInventory != null && debugInventory.getViewers().size() > 0) {
            int index = 0;
            for (int i = 0; i < rideTrainsItems.size(); i++) {
                ItemStack itemStack = rideTrainsItems.get(i);
                RideTrain rideTrain = rideTrains.get(index);
//                itemStack.setDurability((short) (rideTrain.getVelocity() > 0.001 && rideTrain.getVelocity() < -0.001 ? 13 : 14));
                Vector position = new Vector();
                rideTrain.getFrontCarTrackSegment().getPosition(rideTrain.frontCarDistance, position);
                PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
                itemStack.lore(
                        List.of(
                                serializer.deserialize("§eSpeed §7" + decimalFormat.format(CoasterMathUtils.bptToKmh(rideTrain.getVelocity())) + "km/h"),
                                serializer.deserialize("§eSegment span§7 " + rideTrain.getCars().get(0).getTrackSegment().getId() + " - " + rideTrain.getCars().get(rideTrain.getCars().size() - 1).getTrackSegment().getId()),
                                serializer.deserialize("§eDistance §7" + decimalFormat.format(rideTrain.getFrontCarDistance()) + "m"),
                                serializer.deserialize("§eLength §7" + rideTrain.getLength() + "m"),
                                serializer.deserialize("§eCar count §7" + rideTrain.getCars().size()),
                                serializer.deserialize("§eTrack class \n§7" + rideTrain.getCars().get(0).getTrackSegment().getClass().getSimpleName()),
                                serializer.deserialize(String.format("§ePosition %.2f %.2f %.2f", position.getX(), position.getY(), position.getZ()))
                        )

                );
                debugInventory.setItem(index++, itemStack);
                if (index >= debugInventory.getSize())
                    break;
            }
            List<HumanEntity> viewers = debugInventory.getViewers();
            for (int i = 0; i < viewers.size(); i++) {
                HumanEntity humanEntity = viewers.get(i);
                ((Player) humanEntity).updateInventory();
            }
        }
        for (int i = 0; i < rideTrains.size(); i++) {
            RideTrain rideTrain = rideTrains.get(i);
            rideTrain.onUpdated();
        }
    }

    public void updateAsync() {
        if (FeatureManager.INSTANCE.isFeatureEnabled(FeatureManager.Feature.SPATIAL_SOUNDS))
            for (int i = 0; i < rideTrains.size(); i++) {
                RideTrain rideTrain = rideTrains.get(i);
                rideTrain.onUpdateAsync();
            }
    }

    @NotNull
    public abstract TrackedRideConfig toJson();

    @NotNull
    public <T extends TrackedRideConfig> T toJson(T source) {
        return source;
    }

    public interface OnEmergencyStopListener {
        void onEmergencyStopChanged(boolean active);
    }

    public interface DestroyListener {
        void onDestroy(TrackedRide ride);
    }

    public interface PreTrainUpdateListener {
        void onCarPreUpdate(RideCar rideCar);
    }

    public interface RideCompletionListener {
        void onRideCompletedAsync(Player player, RideCar rideCar);
    }
}
