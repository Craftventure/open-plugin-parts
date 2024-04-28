package net.craftventure.core.ride;

import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.bukkit.ktx.util.PermissionChecker;
import net.craftventure.bukkit.ktx.util.Translation;
import net.craftventure.chat.bungee.util.CVChatColor;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.async.AsyncTask;
import net.craftventure.core.manager.ShutdownManager;
import net.craftventure.core.utils.EntityUtils;
import net.craftventure.core.utils.ParticleSpawnerKt;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.generated.cvdata.tables.pojos.Ride;
import net.craftventure.database.repository.BaseIdRepository;
import org.bukkit.*;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SingaporeBoats implements Listener {
    private static SingaporeBoats singaporeBoats;

    private final List<Station> stations = new ArrayList<>();
    private final List<Boat> boats = new ArrayList<>();
    private Ride ride;

    private SingaporeBoats() {
        SimpleArea area = new SimpleArea("world", );
        area.loadChunks(true);
        for (Entity entity : area.getWorld().getEntities()) {
            if (entity.getCustomName() != null && entity.getCustomName().equals("canoe")) {
                entity.remove();
            }
        }
        ride = MainRepositoryProvider.INSTANCE.getRideRepository().getByName("canoes");
        MainRepositoryProvider.INSTANCE.getRideRepository().addListener(new BaseIdRepository.UpdateListener<>() {
            @Override
            public void onUpdated(Ride item) {
                if (item.getName().equals("canoes")) {
                    ride = item;
                }
            }
        });

        Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance());
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), this::update, 1L, 1L);
        World world = Bukkit.getWorld("world");
        stations.add(new Station(new Location(world, -),
                new Location(world, ),
                4, false));
        stations.add(new Station(new Location(world, ),
                new Location(world, ),
                4, false));
        stations.add(new Station(new Location(world, ),
                new Location(world, ),
                4, false));
        stations.add(new Station(new Location(world, ),
                new Location(world, ),
                4, true));

    }

//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onChunkUnload(ChunkUnloadEvent event) {
//        for (Station station : stations) {
//            if (station.currentBoat != null) {
//                if (event.getChunk() == station.currentBoat.getLocation().getChunk() ||
//                        event.getChunk().equals(station.currentBoat.getLocation().getChunk())) {
//                    event.setCancelled(true);
//                    return;
//                }
//            }
//        }
//        for (int i = 0; i < boats.size(); i++) {
//            Boat boat = boats.get(i);
//            if (event.getChunk() == boat.getLocation().getChunk() ||
//                    event.getChunk().equals(boat.getLocation().getChunk())) {
//                event.setCancelled(true);
//                return;
//            }
//        }
//    }

    private void update() {
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            station.update();
        }
        //TODO: Throttle this
        Iterator<Boat> boatIterator = boats.iterator();
        while (boatIterator.hasNext()) {
            Boat boat = boatIterator.next();
            if (!boat.isValid() || boat.isDead() || !(EntityUtils.INSTANCE.hasPlayerPassengers(boat))) {
                boat.remove();
                boatIterator.remove();
            } else {
                for (int i = 0; i < stations.size(); i++) {
                    Station station = stations.get(i);
                    if (boat != station.currentBoat && boat.getLocation().distanceSquared(station.spawnLocation) < station.stationRadius * station.stationRadius) {
//                        Logger.console("Eject");
                        boat.eject();
                        boat.remove();
                        boatIterator.remove();
                    }
                }
            }
        }
    }

//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
//        Logger.info("onPlayerInteractAtEntity");
//        if (event.getRightClicked() instanceof Player)
//            return;
//        checkForEnter(event, event.getPlayer(), event.getRightClicked());
//    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
//        Logger.info("onPlayerInteractEntity");
        if (event.getRightClicked() instanceof Player)
            return;
//        Logger.info("onPlayerInteractEntity");
        checkForEnter(event, event.getPlayer(), event.getRightClicked());
    }

    //
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleEnterEvent(VehicleEnterEvent event) {
        if (isSingaporeBoat(event.getVehicle())) {
//            Logger.info("onVehicleEnterEvent");
            event.setCancelled(false);
        }
//        Logger.info("onVehicleEnterEvent");
//        if (!(event.getEntered() instanceof Player)) {
//            return;
//        }
//        Player entered = (Player) event.getEntered();
//        checkForEnter(event, entered, event.getVehicle());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityMountEvent(EntityMountEvent event) {
        if (isSingaporeBoat(event.getMount())) {
//            Logger.info("onEntityMountEvent");
            event.setCancelled(false);
        }
//        Logger.info("onEntityMountEvent " + event.getEntity() + " " + event.getMount());
//        if (!(event.getEntity() instanceof Player)) {
//            return;
//        }
//        Player entered = (Player) event.getEntity();
//        checkForEnter(event, entered, event.getMount());
    }

    private void checkForEnter(@Nullable Cancellable event, Player player, Entity vehicle) {
        if (isSingaporeBoat(vehicle)) {
//            if (player.isInsideVehicle()) {
//                return;
//            }
            if (onEnter(player, vehicle)) {
                if (event != null)
                    event.setCancelled(false);
            } else {
                if (event != null)
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleExit(VehicleExitEvent event) {
//        Logger.debug("Exit %s %s", false, event.getExited(), event.getVehicle());
        if (event.getExited() instanceof Player && event.getVehicle() instanceof Boat) {
            Player player = (Player) event.getExited();
            for (int i = 0; i < boats.size(); i++) {
                Boat boat = boats.get(i);
                if (event.getVehicle() == boat) {
//                    Logger.console("Singapore boat exit");
                    event.setCancelled(false);
                    for (int i1 = 0; i1 < stations.size(); i1++) {
                        Station station = stations.get(i1);
                        if (event.getVehicle().getLocation().distance(station.spawnLocation) < station.stationRadius) {
//                            Logger.console("Teleport");
                            Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), () -> {
                                player.teleport(station.playerExitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
//                                Logger.console("Teleport2");
                            }, 1L);
                            new AsyncTask() {
                                @Override
                                public void doInBackground() {
                                    if (station.isBlackMarket())
                                        MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(player.getUniqueId(), "blackmarket");
                                    MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(player.getUniqueId(), "ride_canoes");
                                    MainRepositoryProvider.INSTANCE.getRideCounterRepository().increaseCounter(player.getUniqueId(), "canoes");
                                }
                            }.executeNow();
                            return;
                        }
                    }
//                    Logger.console("Teleport");
                    Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), () -> {
                        player.teleport(stations.get(0).playerExitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
//                        Logger.console("Teleport2");
                    }, 1L);
                    new AsyncTask() {
                        @Override
                        public void doInBackground() {
                            MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(player.getUniqueId(), "ride_canoes");
                            MainRepositoryProvider.INSTANCE.getRideCounterRepository().increaseCounter(player.getUniqueId(), "canoes");
                        }
                    }.executeNow();
                    return;
                }
            }

            for (int i = 0; i < stations.size(); i++) {
                Station station = stations.get(i);
                if (station.currentBoat == event.getVehicle()) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), () -> {
                        if (!player.isInsideVehicle())
                            player.teleport(stations.get(0).playerExitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    }, 1L);
                }
            }
        }
    }

    private boolean isSingaporeBoat(Entity entity) {
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            if (station.currentBoat != null && station.currentBoat == entity) {
                return true;
            }
        }
        return false;
    }

    private boolean onEnter(Player player, Entity entity) {
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            if (station.currentBoat != null && station.currentBoat == entity) {
                if (ShutdownManager.INSTANCE.getShuttingDown()) {
                    player.sendMessage(Translation.SHUTDOWN_PREPARING.getTranslation(player));
//                    Logger.info("onEnter: false");
                    return false;
                }

                if (ride.getState().isOpen() && (ride.getState().getPermission() == null || player.hasPermission(ride.getState().getPermission()))) {
//                    Logger.info("onEnter: true");
                    return true;
                } else {
//                    Logger.info("onEnter: false");
                    player.sendMessage(Translation.RIDE_STATE_CLOSED.getTranslation(player));
                    if (PermissionChecker.INSTANCE.isCrew(player)) {
                        player.sendMessage(CVChatColor.INSTANCE.getServerNotice() + "As crew you can obviously enter ;)");
                        return true;
                    }
                    return false;
                }
            }
        }
//        Logger.info("onEnter: false");
        return false;
    }

    public static SingaporeBoats getInstance() {
        if (singaporeBoats == null)
            singaporeBoats = new SingaporeBoats();
        return singaporeBoats;
    }

    public class Station {
        private final Location spawnLocation;
        private final Location playerExitLocation;
        private final double stationRadius;
        private final boolean blackMarket;

        private Boat currentBoat;
        private long ticksInCurrentState;
        private StationState stationState;

        public Station(Location spawnLocation, Location playerExitLocation, double stationRadius, boolean blackMarket) {
            this.spawnLocation = spawnLocation;
            this.playerExitLocation = playerExitLocation;
            this.stationRadius = stationRadius;
            this.blackMarket = blackMarket;
        }

        public boolean isBlackMarket() {
            return blackMarket;
        }

        private void setStationState(StationState stationState) {
            this.stationState = stationState;
            this.ticksInCurrentState = 0;
        }

        public void update() {
            ticksInCurrentState++;
            if (currentBoat != null && !ride.getState().isOpen()) {
                currentBoat.remove();
                currentBoat = null;
            }
            if ((currentBoat == null || !currentBoat.isValid() || currentBoat.isDead()) && ride.getState().isOpen()) {
                setStationState(StationState.IDLE);
                ParticleSpawnerKt.spawnParticleX(
                        spawnLocation.clone().add(0, 0.7, 0),
                        Particle.REDSTONE,
                        31,
                        0.5, 0.2, 0.5,
                        0.0, new Particle.DustOptions(Color.fromRGB(0x9E5733), 2.5f));
                currentBoat = CraftventureCore.getRandom().nextDouble() > 0.5 ?
                        spawnLocation.getWorld().spawn(spawnLocation, Boat.class) :
                        spawnLocation.getWorld().spawn(spawnLocation, ChestBoat.class);
                currentBoat.setWoodType(CraftventureCore.getRandom().nextDouble() > 0.5 ?
                        TreeSpecies.REDWOOD :
                        TreeSpecies.JUNGLE);
                currentBoat.setPersistent(false);
                currentBoat.setCustomName("canoe");
                EntityUtils.INSTANCE.teleport(currentBoat, spawnLocation);
            }
            if (stationState == StationState.IDLE && EntityUtils.INSTANCE.hasPlayerPassengers(currentBoat)) {
                setStationState(StationState.LEAVING);
            } else if (stationState == StationState.LEAVING && !(EntityUtils.INSTANCE.hasPlayerPassengers(currentBoat))) {
                setStationState(StationState.IDLE);
                EntityUtils.INSTANCE.teleport(currentBoat, spawnLocation);
            }

            if (stationState == StationState.LEAVING && (ticksInCurrentState > 20 * 10 || currentBoat.getLocation().distance(spawnLocation) > stationRadius)) {
//                if (currentBoat.getLocation().distance(spawnLocation) < stationRadius) {
//                    Logger.console("Teleporting boat away");
//                    currentBoat.teleport(boatMoveToLocation);
////                    EntityUtils.teleport(currentBoat, boatMoveToLocation);
//                }
                boats.add(currentBoat);
//                currentBoat.remove();
                currentBoat = null;
            }
            if (currentBoat != null && stationState == StationState.IDLE) {
                if (currentBoat.getLocation().getX() != spawnLocation.getX() || currentBoat.getLocation().getZ() != spawnLocation.getZ()) {
                    currentBoat.setVelocity(new Vector(0, 0, 0));
                    currentBoat.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            }
        }

        public Boat getCurrentBoat() {
            return currentBoat;
        }

        public StationState getStationState() {
            return stationState;
        }
    }

    public enum StationState {
        IDLE, LEAVING
    }
}
