package net.craftventure.core;

import com.comphenix.protocol.ProtocolLibrary;
import io.sentry.SentryClient;
import kotlin.Unit;
import net.craftventure.BuildConfig;
import net.craftventure.audioserver.AudioServer;
import net.craftventure.audioserver.spatial.SpatialAudioManager;
import net.craftventure.backup.service.BackupService;
import net.craftventure.bukkit.ktx.MainCommandManager;
import net.craftventure.bukkit.ktx.entitymeta.MetaAnnotations;
import net.craftventure.bukkit.ktx.entitymeta.MetaListener;
import net.craftventure.bukkit.ktx.manager.*;
import net.craftventure.bukkit.ktx.plugin.Environment;
import net.craftventure.bukkit.ktx.plugin.PluginProvider;
import net.craftventure.bukkit.ktx.plugin.PluginState;
import net.craftventure.bukkit.ktx.util.TranslationUtil;
import net.craftventure.core.api.CvApi;
import net.craftventure.core.cache.ProtectionCache;
import net.craftventure.core.command.*;
import net.craftventure.core.config.AreaConfigManager;
import net.craftventure.core.config.CraftventureConfigReloadedEvent;
import net.craftventure.core.config.ItemPackageList;
import net.craftventure.core.config.Settings;
import net.craftventure.core.effect.EffectManager;
import net.craftventure.core.effect.RapidStatueEffect;
import net.craftventure.core.feature.balloon.BalloonManager;
import net.craftventure.core.feature.casino.CasinoManager;
import net.craftventure.core.feature.dressingroom.DressingRoomManager;
import net.craftventure.core.feature.finalevent.FinaleCinematic;
import net.craftventure.core.feature.finalevent.FinaleTimer;
import net.craftventure.core.feature.jumppuzzle.JumpPuzzleManager;
import net.craftventure.core.feature.kart.KartManager;
import net.craftventure.core.feature.minigame.MinigameManager;
import net.craftventure.core.feature.nbsplayback.NbsFileManager;
import net.craftventure.core.feature.shop.ShopPresenterManager;
import net.craftventure.core.ktx.SentryUtils;
import net.craftventure.core.ktx.concurrency.CvExecutors;
import net.craftventure.core.ktx.logging.LogPriority;
import net.craftventure.core.ktx.logging.LogcatLogger;
import net.craftventure.core.ktx.util.BackgroundService;
import net.craftventure.core.ktx.util.GlyphSizes;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.ktx.util.ServiceManager;
import net.craftventure.core.listener.*;
import net.craftventure.core.manager.*;
import net.craftventure.core.map.holder.MatScoreImageHolder;
import net.craftventure.core.map.renderer.*;
import net.craftventure.core.metadata.ManagedSideScoreBoard;
import net.craftventure.core.npc.EntityMetadata;
import net.craftventure.core.npc.tracker.EntitySpawnTrackerManager;
import net.craftventure.core.profiler.ServerWatchDog;
import net.craftventure.core.protocol.ProtocolHandler;
import net.craftventure.core.repository.Repository;
import net.craftventure.core.ride.SingaporeBoats;
import net.craftventure.core.ride.flatride.*;
import net.craftventure.core.ride.operator.OperatorManager;
import net.craftventure.core.ride.tracked.*;
import net.craftventure.core.ride.trackedride.TrackedRideManager;
import net.craftventure.core.ride.trackedride.TracklessRideManager;
import net.craftventure.core.script.ScriptManager;
import net.craftventure.core.serverevent.PlayerLocationChangedEventTask;
import net.craftventure.core.task.*;
import net.craftventure.core.utils.*;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.bukkit.listener.ShopCacheListener;
import net.craftventure.database.generated.cvdata.tables.pojos.Shop;
import net.craftventure.database.generated.cvdata.tables.pojos.Warp;
import net.craftventure.database.repository.BaseIdRepository;
import net.craftventure.database.repository.BaseRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.map.MapPalette;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class CraftventureCore extends PluginProvider.CvPlugin {

    private static CraftventureCore _this = null;
    private static final boolean isInitialized = false;
    private static final Executor executorService = CvExecutors.INSTANCE.getExecutor();
    private static final ScheduledExecutorService scheduledExecutorService = CvExecutors.INSTANCE.getScheduledExecutor();
    private static final Random random = new SecureRandom();
    private static Settings settings;
    private static ItemPackageList itemPackages;
    private static boolean isEnabled = false;

    private static WornTitleManager wornTitleManager;

    private static OperatorManager operatorManager;
    private static long enabledTime = 0;
    private static final SentryClient sentry = null;
    private static PluginState state = PluginState.UNKNOWN;

    public static PluginState getState() {
//        Logger.debug("State is %s", false, state);
        return state;
    }

    public static OperatorManager getOperatorManager() {
        if (operatorManager == null)
            operatorManager = new OperatorManager();
        return operatorManager;
    }

    public static SentryClient getSentry() {
        return sentry;
    }

    public boolean isShuttingDown() {
        return ShutdownManager.INSTANCE.getShuttingDown();
    }

    public static WornTitleManager getWornTitleManager() {
        return wornTitleManager;
    }

    public static CraftventureCore getInstance() {
        return _this;
    }

    public static boolean hasInitializedCorrectly() {
        return isInitialized;
    }

    public static Executor getExecutorService() {
        return executorService;
    }

    public static ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public static Random getRandom() {
        return random;
    }

    public static long getEnabledTime() {
        return enabledTime;
    }

    public static void run(Runnable runnable) {
        Bukkit.getScheduler().runTask(CraftventureCore.getInstance(), runnable);
    }

    @Override
    public void onLoad() {
        Logger.INSTANCE.setTree(new CraftventureLogger());
        LogcatLogger.Companion.install(new CraftventureLogcatLogger(LogPriority.VERBOSE));

        super.onLoad();
        state = PluginState.LOADING;
        System.getProperties().setProperty("org.jooq.no-logo", "true");
//        Logger.console("Thread " + mainThread.getName());
        _this = this;

        if (!reloadSettings()) {
            Logger.severe("Failed to load config.json, exiting now!");
            System.exit(0);
        }
        Repository.INSTANCE.init();
        BackupRestorer.restoreWorldIfNeeded();

//        try {
//            getClass().getClassLoader().loadClass(Area.class.getName());
//            getClass().getClassLoader().loadClass(BaseArea.class.getName());
//            getClass().getClassLoader().loadClass(SimpleArea.class.getName());
//            getClass().getClassLoader().loadClass(AreaTracker.class.getName());
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
        GlyphSizes.INSTANCE.init(new File(getDataFolder(), "resources/glyph_sizes.bin"), new File(getDataFolder(), "resources/ascii.png"));

        try {
            EntityUtils.INSTANCE.setupArmorStandUpdates();
        } catch (Exception e) {
            e.printStackTrace();
        }

        state = PluginState.LOADED;
    }

    public static boolean reloadSettings() {
        try {
            File coinBoostersFile = new File(getInstance().getDataFolder(), "data/tebex.json");
            CraftventureCore.itemPackages = CvApi.getGsonExposed().fromJson(String.join("\n", Files.readAllLines(Paths.get(coinBoostersFile.getPath()))), ItemPackageList.class);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.capture(e);
        }

        try {
            PluginProvider.setEnvironment(Environment.PRODUCTION);
            try {
                FileConfiguration data = YamlConfiguration.loadConfiguration(new File(CraftventureCore.getInstance().getDataFolder(), "env.yml"));
                String environmentValue = data.getString("environment", "production");
                for (Environment environment : Environment.values()) {
                    if (environment.name().equalsIgnoreCase(environmentValue)) {
                        PluginProvider.setEnvironment(environment);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.capture(e);
            }

            Logger.info("Loading config for env=%s", false, PluginProvider.getEnvironment());
//            bugsnag.setAppType(CraftventureCore.environment.name());
//            bugsnag.setAppVersion(CraftventureCore.getInstance().getDescription().getVersion());
//            bugsnag.setReleaseStage(CraftventureCore.environment.name());

            File settingsFile = new File(getInstance().getDataFolder(), "config." + PluginProvider.getEnvironment().name().toLowerCase() + ".json");


            LogcatLogger.Companion.uninstall();
            LogcatLogger.Companion.install(new CraftventureLogcatLogger(PluginProvider.isProductionServer() ? LogPriority.WARN : LogPriority.VERBOSE));

            FeatureManager.reload();

            Settings settings = Settings.Companion.fromFile(settingsFile);
            if (settings != null) {
                if (settings.getSentryDsn() != null) {
                    SentryUtils.use(settings.getSentryDsn(),
                            CraftventureCore.getInstance().getDescription().getVersion() + "/" + BuildConfig.getVERSION_GIT_COMMIT_HASH(),
                            PluginProvider.getEnvironment().name(),
                            BackupService.INSTANCE.getMachineName()
                    );
                } else {
                    SentryUtils.clear();
                }

                if (state == PluginState.ENABLED)
                    getInstance().updateBorder();

                AudioServer.Companion.getInstance().setAudioServerWebsite(settings.getAudioServerWebsite());
                AudioServer.Companion.getInstance().setAudioServerSocket(settings.getAudioServerSocket());

                CraftventureCore.settings = settings;
                EmojiLoader.INSTANCE.loadEmojiMappings();
                Bukkit.getPluginManager().callEvent(new CraftventureConfigReloadedEvent());
                return true;
            } else {
                SentryUtils.clear();
            }

        } catch (Exception e) {
            Logger.capture(e);
        }
        return false;
    }

    protected void updateBorder() {
        if (settings.getBorderConfig() != null)
            WorldBorderManager.INSTANCE.setBorderParts(Arrays.stream(settings.getBorderConfig()).map((item) -> new WorldBorderManager.BorderPart(item.getCrewOnly(), item.getRenderTiles(), item.getArea())).collect(Collectors.toList()));
        else
            WorldBorderManager.INSTANCE.setBorderParts(Collections.emptyList());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = PluginState.ENABLING;
        enabledTime = System.currentTimeMillis();
        TranslationUtil.INSTANCE.load(this);
        updateBorder();

        MapPalette.matchColor(Color.BLACK);

        EntityMetadata.prepare();

        ProtectionCache.INSTANCE.updateCaches();

        Logger.info("Loading databases done at %dms", false, System.currentTimeMillis() - enabledTime);

        AreaConfigManager.getInstance().reload();
        Logger.info("Areas loaded at %dms", false, System.currentTimeMillis() - enabledTime);
        ShopPresenterManager.getInstance().reload();
        Logger.info("Shops loaded at %dms", false, System.currentTimeMillis() - enabledTime);
        DressingRoomManager.INSTANCE.reload();
        Logger.info("Dressing rooms loaded at %dms", false, System.currentTimeMillis() - enabledTime);
        NbsFileManager.INSTANCE.reload();
        Logger.info("NBS files loaded at %dms", false, System.currentTimeMillis() - enabledTime);
        DoorManager.INSTANCE.reload();
        Logger.info("Doormanager loaded at %dms", false, System.currentTimeMillis() - enabledTime);
        TrackedRideManager.INSTANCE.reload();
        Logger.info("TrackedRides loaded at %dms", false, System.currentTimeMillis() - enabledTime);
        TracklessRideManager.INSTANCE.reload();
        Logger.info("TracklessRides loaded at %dms", false, System.currentTimeMillis() - enabledTime);

        wornTitleManager = new WornTitleManager();
        getServer().getPluginManager().registerEvents(wornTitleManager, this);
        registerListeners();
        registerCommands();

        Logger.info("Loading listeners and commands done at %dms", false, System.currentTimeMillis() - enabledTime);

        MessageBarManager.init();
        ProtocolHandler.INSTANCE.init();
        TabHeaderFooterManager.INSTANCE.init();
        CasinoManager.INSTANCE.init();

        loadEffects();

        ActiveMoneyRewardTask.INSTANCE.init();
        FoodTask.INSTANCE.init();
        BlackMarketTask.INSTANCE.init();
        VipTrialCleanupTask.INSTANCE.init();
//        getLogger().info("Recreating CvMetadata");

//        getLogger().info("Initialising rides");
//        getLogger().info("Initialising ScriptManager");
        Logger.info("Loading and compiling scripts into the cache at %dms", false, System.currentTimeMillis() - enabledTime);
        ScriptManager.init();
        Logger.info("Loading and compiling done at %dms", false, System.currentTimeMillis() - enabledTime);

        initRides();
        Logger.info("Rides initialised at %dms", false, System.currentTimeMillis() - enabledTime);
        KartManager.INSTANCE.init();
        BalloonManager.Companion.init();

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, TpsListener.INSTANCE::init);

//        if (getSettings().isRcon()) {
//            RconServer.getInstance().start(getSettings().getRconPort());
//        }

        PlayerLocationChangedEventTask.INSTANCE.init();

//        getSettings().getBorder().loadChunks();

        Warp warp = MainRepositoryProvider.INSTANCE.getWarpRepository().findByName("spawn");
        if (warp != null) {
            for (World world : Bukkit.getWorlds()) {
                world.setSpawnLocation(warp.getX().intValue(), warp.getY().intValue(), warp.getZ().intValue());
            }
        }
        BackgroundService.INSTANCE.init();
        ForcedViewManager.INSTANCE.init();
        Logger.info("Warming shop cache at %dms...", true, System.currentTimeMillis() - enabledTime);
        for (Shop shop : MainRepositoryProvider.INSTANCE.getShopRepository().itemsPojo()) {
            ShopCacheListener.INSTANCE.cached(shop.getId());
        }
        EurosatBotTask.INSTANCE.init();
        AudioServerRideMarkerTask.INSTANCE.init();
        MailManager.INSTANCE.cleanup();

        MinigameManager.INSTANCE.init();
        JumpPuzzleManager.INSTANCE.init();

        Logger.info("Setup KartShop, MinigameManager and JumpPuzzleManager done at %dms", false, System.currentTimeMillis() - enabledTime);

        Logger.info("Most managers initialised at %dms", true, System.currentTimeMillis() - enabledTime);
        Logger.info("Map manager initialised at %dms", true, System.currentTimeMillis() - enabledTime);

        Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), ServerWatchDog.Companion::init, 100L);

//        SummerWvW.init();
        AudioServer.Companion.getInstance().onEnable();
        AudioServer.Companion.getInstance().setOperatorDelegate((player, rideId, controlId) -> {
            operatorManager.clicked(player, rideId, controlId);
            return Unit.INSTANCE;
        });
        AudioServerPlayerMapTask.INSTANCE.start();
        BackupTask.INSTANCE.init();
        PasteManager.init();

//        Logger.info(CVChatColor.INSTANCE.getCOMMAND_GENERAL() + "Loading chunks", true, System.currentTimeMillis() - enabledTime);
//        settings.getBorder().loadChunks(false);
        Logger.info("Craftventure core enabled in %dms!", false, System.currentTimeMillis() - enabledTime);

        SpatialAudioManager.Companion.getInstance();
        SmoothCoastersHelper.INSTANCE.init();

        FinaleCinematic.INSTANCE.reload();

        MessageBarManager.setVcGetter((player) -> {
            ManagedSideScoreBoard board = MetaAnnotations.getMetadata(player, ManagedSideScoreBoard.class);
            if (board == null) return 0L;
            return board.getVc();
        });

        TrackerAreaManager.start();

        MapManager.getInstance()
                .putRenderer("simpleloader", SimpleImageRenderer.class)
                .putRenderer("minigamescore", MinigameScoreboardRenderer.class)
                .putRenderer("ridehighscore", RideScoreboardRenderer.class)
                .putRenderer("teamscore", TeamScoreScoreboardRenderer.class)
                .putRenderer("operatorcontrol", OperatorControlRenderer.class)
                .putRenderer("playerkeyvaluescore", PlayerKeyValueRenderer.class)
                .putRenderer("achievementscore", AchievementScoreboardRenderer.class)
                .putRenderer("casinoleaderboard", CasinoLeaderboardRenderer.class)
                .putRenderer("queuetime", QueueRenderer.class)
                .putRenderer("matscore", MatScoreRenderer.class);
        MapManager.getInstance().addImageHolder(new MatScoreImageHolder("matscore0", 0));
        MapManager.getInstance().addImageHolder(new MatScoreImageHolder("matscore1", 1));
        MapManager.getInstance().addImageHolder(new MatScoreImageHolder("matscore2", 2));
        MapManager.getInstance().updateAllMaps(true);

        FinaleTimer.INSTANCE.setup();

        isEnabled = true;
        state = PluginState.ENABLED;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        state = PluginState.DISABLING;
        TrackerAreaManager.start();

        ServerWatchDog.Companion.destroy();

        MinigameManager.INSTANCE.destroy();
        KartManager.INSTANCE.destroy();
        JumpPuzzleManager.INSTANCE.destroy();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.leaveVehicle();
        }
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                MetaAnnotations.cleanup(player);
            }
        } catch (Exception e) {
            Logger.capture(e);
        }
        CasinoManager.INSTANCE.destroy();
        HandlerList.unregisterAll(this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            wornTitleManager.remove(player);
        }
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getOpenInventory() != null)
                player.closeInventory();

        ScriptManager.destroy();
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        EntitySpawnTrackerManager.unload();
        EffectManager.INSTANCE.shutdown();
        BackgroundService.INSTANCE.destroy();
        ForcedViewManager.INSTANCE.destroy();
        TpsListener.INSTANCE.destroy();

        try {
            EquipmentManager.INSTANCE.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ProtocolHandler.INSTANCE.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            BalloonManager.Companion.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Repository.INSTANCE.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        RconServer.getInstance().stop();
        HandlerList.unregisterAll(this);
//        discordClient.logout();

        AudioServer.Companion.getInstance().onDisable();

        // TODO: Fix this?
//        try {
////            Logger.console("Shutting down executor... This will take a max of 10 seconds");
//            executorService.awaitTermination(2, TimeUnit.SECONDS);
////            Logger.console("Executor has shutdown");
//        } catch (InterruptedException e) {
//            Logger.capture(e);
//            executorService.shutdown();
//        }

        state = PluginState.DISABLED;
        Logger.info("Craftventure core disabled!");
    }

    public static ItemPackageList getItemPackages() {
        return itemPackages;
    }

    public static Settings getSettings() {
        return settings;
    }

    private void loadEffects() {
        Location statue1Location = new Location(Bukkit.getWorld("world"), -144.3, 42.5, -611.3);
        RapidStatueEffect rapidStatue1Effect = new RapidStatueEffect("rapid_statue_1", statue1Location, -220, 0);
        EffectManager.INSTANCE.add(rapidStatue1Effect);

        Location statue2Location = new Location(Bukkit.getWorld("world"), -145.73, 42.5, -636.7);
        RapidStatueEffect rapidStatue2Effect = new RapidStatueEffect("rapid_statue_2", statue2Location, -45, 0);
        EffectManager.INSTANCE.add(rapidStatue2Effect);
    }

    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new KeyListener(), this);
        CraftventureGuardListener craftventureGuardListener = new CraftventureGuardListener();
        getServer().getPluginManager().registerEvents(craftventureGuardListener, this);

        getServer().getMessenger().registerIncomingPluginChannel(
                CraftventureCore.getInstance(),
                "wdl:init",
                craftventureGuardListener
        );
        getServer().getMessenger().registerOutgoingPluginChannel(
                CraftventureCore.getInstance(),
                "wdl:control"
        );

        getServer().getMessenger().registerIncomingPluginChannel(
                CraftventureCore.getInstance(),
                "craftventure:bungee",
                new BungeeListener()
        );
//        getServer().getMessenger().registerOutgoingPluginChannel(
//                CraftventureCore.getInstance(),
//                "craftventure:bungee"
//        );
        getServer().getMessenger().registerOutgoingPluginChannel(
                CraftventureCore.getInstance(),
                "craftventure:command"
        );

//        getServer().getPluginManager().registerEvents(new HalloweenMaze(), this);
        getServer().getPluginManager().registerEvents(new WorldEditListener(), this);
//        getServer().getPluginManager().registerEvents(new Christmas2020(), this);
        getServer().getPluginManager().registerEvents(new MetaListener(), this);
        getServer().getPluginManager().registerEvents(new SelectorHackListener(), this);
        getServer().getPluginManager().registerEvents(new ItemListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new EntityTrackerListener(), this);
        getServer().getPluginManager().registerEvents(new KickBanListener(), this);
        getServer().getPluginManager().registerEvents(new PettingListener(), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new InfiniteDispensers(), this);
        getServer().getPluginManager().registerEvents(new MapListener(), this);
        getServer().getPluginManager().registerEvents(new BungeeProtection(), this);
        getServer().getPluginManager().registerEvents(AreaListener.Companion.getInstance(), this);
        getServer().getPluginManager().registerEvents(new CommandPreProcessListener(), this);
        getServer().getPluginManager().registerEvents(new PaintingListener(), this);
        getServer().getPluginManager().registerEvents(new BalloonListener(), this);
        getServer().getPluginManager().registerEvents(new AfkListener(), this);
        getServer().getPluginManager().registerEvents(new SeatListener(), this);
        getServer().getPluginManager().registerEvents(new AchievementListener(), this);
        getServer().getPluginManager().registerEvents(new DragonClanListener(), this);
        getServer().getPluginManager().registerEvents(new JoinAndLeaveListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(), this);
        getServer().getPluginManager().registerEvents(new BarrierListener(), this);
        getServer().getPluginManager().registerEvents(new FallingBlockLandListener(), this);
        getServer().getPluginManager().registerEvents(new MenuItemListener(), this);
        getServer().getPluginManager().registerEvents(new TileStateListener(), this);
        getServer().getPluginManager().registerEvents(new EntityPersistentDataListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
//        getServer().getPluginManager().registerEvents(new ChristmasListener(), this);
//        getServer().getPluginManager().registerEvents(new ChristmasMinersCave(), this);
        getServer().getPluginManager().registerEvents(new ScavengerListener(), this);
        getServer().getPluginManager().registerEvents(new SecretListener(), this);
        getServer().getPluginManager().registerEvents(new SkateListener(), this);
        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        getServer().getPluginManager().registerEvents(new DebugListener(), this);
        getServer().getPluginManager().registerEvents(new AprilFoolsListener(), this);
        getServer().getPluginManager().registerEvents(new MountListener(), this);
        getServer().getPluginManager().registerEvents(new ShopListener(), this);
        getServer().getPluginManager().registerEvents(new ToolsListener(), this);
        getServer().getPluginManager().registerEvents(new OperatorListener(), this);
        getServer().getPluginManager().registerEvents(new CrewLampManager(), this);
        getServer().getPluginManager().registerEvents(new LocalisationListener(), this);
        getServer().getPluginManager().registerEvents(new GameModeChangeListener(), this);

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
//            getLogger().info("Registering PEX listener");
            LuckPermsListener.INSTANCE.register();
        }
    }

    public void registerCommands() {
        MainCommandManager.INSTANCE.init(this);

        register(new CommandResetMyself(), "resetmyself");
        register(new CommandFixPassengers(), "fixpassengers");
        register(new CommandMyRokEgg(), "myrokeggandi");
        register(new CommandModelData(), "custommodeldata");
        register(new CommandFtp(), "ftp");
        register(new CommandSync(), "sync");
        register(new CommandFeatureToggle(), "featuretoggle");
        register(new CommandDebugSpread(), "debugspread");
        register(new CommandMigrate(), "migrate");
        register(new CommandMiniMessage(), "mmpreview");
        register(new CommandSceneExport(), "sceneexport");
        register(new CommandEmojis(), "emoji");
        register(new CommandEntityNmsDebug(), "entitynmsdebug");
        register(new CommandItemRefunds(), "itemrefunds");
        register(new CommandTotalRideCount(), "totalridecount");
        register(new CommandMeta(), "meta");
        register(new CommandWebEditor(), "webeditor");
        register(new CommandCleanDoubleEntities(), "cleandoubleentities");
//        register(new CommandWishingWell(), "wishingwell");
        register(new CommandShrug(), "shrug");
        register(new CommandWheelOfDoom(), "wheelofdoom");
        register(new CommandRidecount(), "ridecount");
        register(new CommandTogglePlayers(), "toggleplayers");
        register(new CommandClearLogs(), "clearlogs");
        register(new CommandBackup(), "backup");
        register(new CommandVip(), "vip");
        register(new CommandVipTrial(), "viptrial");
        register(new CommandDiscord(), "discord");
        register(new CommandRank(), "rank");
        register(new CommandDev(), "dev");
        register(new CommandProtection(), "protection");
        register(new CommandProfile(), "profile");
        register(new CommandPuke(), "puke");
        register(new CommandStuck(), "stuck");
        register(new CommandItemPackages(), "itempackages");
        register(new CommandMail(), "mail");
        register(new CommandGiveOwnableItem(), "giveownableitem");
        register(new CommandMap(), "map");
        register(new CommandAge(), "age");
        register(new CommandUserReport(), "userreport");
        register(new CommandToggle(), "toggle");
        register(new CommandUptime(), "uptime");
        register(new CommandChat(), "chat");
        register(new CommandAfk(), "afk");
        register(new CommandRideOp(), "rideop");
        register(new CommandOnride(), "onride");
        register(new CommandFixUuid(), "fixuuid");
        register(new CommandNightVision(), "nv");
        register(new CommandDispatch(), "dispatch");
        register(new CommandTools(), "tools");
        register(new CommandLights(), "lights");
        register(new CommandNearestTrackNode(), "nearesttracknode");
        register(new CommandKillNamed(), "killnamed");
        register(new CommandWeArea(), "wearea");
        register(new CommandFixRides(), "fixrides");
        register(new CommandChatEnable(), "chatenable");
        register(new CommandMaxPlayers(), "maxplayers");
        register(new CommandItemstack(), "itemstack");
        register(new CommandReward(), "reward");
        register(new CommandItemColor(), "itemcolor");
        register(new CommandItemDamage(), "itemdamage");
        register(new CommandItemLore(), "itemlore");
        register(new CommandItemName(), "itemname");
        register(new CommandTeams(), "teams");
        register(new CommandRideLink(), "ridelink");
        register(new CommandAchievement(), "achievement");
        register(new CommandValidate(), "validate");
        register(new CommandRideState(), "ridestate");
        register(new CommandShop(), "shop");
        register(new CommandRankCycle(), "cyclerank");
        register(new CommandNearestWarp(), "nearestwarp");
        register(new CommandRespawn(), "respawn");
        register(new CommandArea(), "area");
        register(new CommandBack(), "back");
        register(new CommandSpecialEffect(), "seffect");
        register(new CommandGetPos(), "getpos");
        register(new CommandGodSword(), "godsword");
        register(new CommandRideDebug(), "ridedebug");
        register(new CommandDump(), "dump");
        register(new CommandHead(), "head");
        register(new CommandScript(), "script");
        register(new CommandRewardAchievement(), "rewardachievement");
        register(new CommandGameMode(), "gamemode");
        register(new CommandClearChat(), "clearchat");
        register(new CommandKickall(), "kickall");
        register(new CommandMessage(), "msg");
        register(new CommandKillall(), "killall");
        register(new CommandKarts(), "karts");
        register(new CommandSetSpawn(), "setspawn");
        register(new CommandEquip(), "equip");
        register(new CommandSpawn(), "spawn");
        register(new CommandInvSee(), "invsee");
        register(new CommandGc(), "gc");
        register(new CommandTop(), "top");
        register(new CommandSudo(), "sudo");
//        register(new CommandKill(), "kill");
        register(new CommandVcDebug(), "vcdebug");
        register(new CommandHeal(), "heal");
        register(new CommandTpall(), "tpall");
        register(new CommandItemDb(), "itemdb");
        register(new CommandFly(), "fly");
        register(new CommandVentureCoins(), "venturecoins");
        register(new CommandCraftventure(), "cv");
        register(new CommandRules(), "rules");
        register(new CommandItems(), "items");
        register(new CommandItem(), "item");
        register(new CommandClearInventory(), "ci");
        register(new CommandHat(), "hat");
        register(new CommandWarp(), "warp");
        register(new CommandWarpPermission(), "warppermission");
        register(new CommandWarps(), "warps");
        register(new CommandSetWarp(), "setwarp");
        register(new CommandDelWarp(), "delwarp");
        register(new CommandSeen(), "seen");
        register(new CommandMore(), "more");
        register(new CommandShutdown(), "shutdown");
        register(new CommandJoin(), "join");
        register(new CommandLeave(), "leave");
        register(new CommandUnbreakable(), "unbreakable");
        register(new CommandFixHacker(), "fixhacker");
        register(new CommandFixSchematic(), "fixschematic");

        new CommandHeadTexture().register();
        new CommandCheckState().register();
        new CommandMetaDebug().register();
        new CommandModelMenu().register();
        new CommandDoor().register();
        new CommandCvReload().register();
        new CommandRecord().register();
        new CommandRunFinale().register();
        new CommandInvalidateCaches().register();
        new CommandEmojiTableGenerator().register();

        ServiceManager.INSTANCE.doEnableStage(getPluginClassLoader());
//        register(new CommandRankSync(), "ranksync");
    }

    public void clearCaches() {
        for (BaseRepository<?> repository : MainRepositoryProvider.INSTANCE.getAllRepositories()) {
            if (repository instanceof BaseIdRepository && ((BaseIdRepository<?, ?, ?>) repository).getShouldCache()) {
                ((BaseIdRepository<?, ?, ?>) repository).invalidateCaches();
                ((BaseIdRepository<?, ?, ?>) repository).requireCache();
            }
        }
//        shopDatabase.invalidateCaches();
//        ownableItemDatabase.invalidateCaches();
//        achievementCategoryDatabase.invalidateCaches();
//        warpDatabase.invalidateCaches();
//        itemStackDataDatabase.invalidateCaches();
//        coinBoosterDatabase.invalidateCaches();
    }

    public void register(Object executor, String command) {
        try {
            if (executor instanceof CommandExecutor) {
                getServer().getPluginCommand(command).setExecutor((CommandExecutor) executor);
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to register CommandExecutor for command %s", command), e);
        }
        try {
            if (executor instanceof TabCompleter) {
                getServer().getPluginCommand(command).setTabCompleter((TabCompleter) executor);
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to register TabCompleter for command %s", command), e);
        }
    }

    public void initRides() {
        MainstreetCaroussel.getInstance();
        MexicanTeacups.getInstance();
        Tlaloc.getInstance();
        Soarin.Companion.getInstance();
        SoarinWaffle.Companion.getInstance();
        VikingShip.getInstance();
        GonBaoCastle.Companion.getInstance();
        EagleFury.getInstance();
        SwingRide.Companion.getInstance();
        Madhouse.Companion.getInstance();
        Rocktopus.Companion.getInstance();
        DolphinRide.Companion.getInstance();

        SingaporeBoats.getInstance();

        CookieRide.Companion.get();
        ParkTrain.Companion.get();
        Skyfoogle.Companion.get();
        Vogelrok.Companion.get();
//        Chernolution.Companion.get();
        IndianaJones.get();
        CreeperCanyonRailroad.get();
        FengHuang.Companion.get();
        Fenrir.get();
        PitchiPitchiLoco.get();
        Hyperion.get();
        Rapid.get();
        AguaAzul.get();
        SeaSouls.get();
        DragonDance.get();
        SpaceMountain.get();

        Alphadera.Companion.get();
        RiversOfOuzo.get();

//        SkiLift.get();
//        SnowTube.get();
//        SchlittenFahrt.get();
//        SantaSledge.get();
//        EuroBat.get();
//        SummerCoaster.Companion.get();
//        VladiMir.Companion.get();
    }

}
