package net.craftventure.core.feature.minigame.gonbao

/*import net.craftventure.core.CraftventureCore
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.core.async.executeAsync
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import net.craftventure.core.extension.*
import net.craftventure.core.feature.minigame.BaseLobby
import net.craftventure.core.feature.minigame.BaseMinigame
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.core.manager.NameTagManager
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.NpcEntityType
import net.craftventure.core.npc.tracker.AreaTracker
import net.craftventure.core.script.action.PlaceSchematicAction
import net.craftventure.core.serverevent.PlayerStuckEvent
import net.craftventure.core.serverevent.PlayerWornItemPreUpdateEvent
import net.craftventure.core.utils.*
import net.craftventure.extension.*
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Skull
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta


class GonBaoEvent : BaseMinigame<GonBaoPlayer, BaseLobby, GonBaoLevel>(
    internalName = "gonbao_winter2017_event",
    name = "Gon Bao's Mistake",
    minigameLevel = GonBaoLevel(SimpleArea("world", 126.0, 35.0, -462.0, 144.0, 48.0, -422.0)),
    minRequiredPlayers = 8,
    lobby = BaseLobby(
        signLocations = arrayOf(Location(Bukkit.getWorld("world"), 115.0, 40.0, -589.0))
    ),
    exitLocation = Location(Bukkit.getWorld("world"), 110.5, 39.0, -592.0, -77f, 0f),
    preparingTicks = 0,
    minKeepPlayingRequiredPlayers = 1,
    subType = SubType.DUNGEON,
    baseStoppingTicks = 0
), BaseLobby.LobbyListener {
    private val DEBUG = false

    private val killArea = SimpleArea("world", 27.0, 0.0, -679.0, 146.0, 255.0, -567.0)
    private val gonBaoNpcLocation = Location(Bukkit.getWorld("world"), 127.5, 26.0, -599.9, 0f, 3f)
    private val gonBaoNpc: NpcEntity
    private val gonBaoArea = SimpleArea("world", 117.0, 24.0, -602.0, 149.0, 29.0, -583.0)
    private val gonBaoTracker = AreaTracker(gonBaoArea)
    private val playerTracker = AreaTracker(SimpleArea("world", 95.0, 23.0, -621.0, 121.0, 30.0, -599.0))
    private val maxNpcLocation = Location(Bukkit.getWorld("world"), 121.5, 29.5, -587.5, 243f, 40f)
    private val maxNpc: NpcEntity

    private val lever1 = Location(Bukkit.getWorld("world"), 126.5, 27.0, -600.5)
    private val lever2 = Location(Bukkit.getWorld("world"), 128.5, 27.0, -600.5)
    private var cinematicCamera: ArmorStand? = null
    private val cinematicLocation = Location(Bukkit.getWorld("world"), 110.0, 24.7, -608.9, -204f, -25f)
    private val cinematicEndLocation = Location(Bukkit.getWorld("world"), 103.92, 25.4, -609.8, -145f, -13f)
    //    private val gonBaoLookAt = Location(Bukkit.getWorld("world"), 106.29, 27.78, -615.44, -22.10f, 25.20f)
    private var gonBaoNpcWalking: NpcEntity? = null
    private val overShoulderOfPlayers = Location(Bukkit.getWorld("world"), 118.12, 27.1, -616.60, 59.95f, 15.75f)
    private val explosionLocation = Location(Bukkit.getWorld("world"), 107.5, 26.0, -615.5)

    override val minigame: BaseMinigame<*, *, *> = this
    private val spawnLocation = Location(Bukkit.getWorld("world"), 119.5, 36.0, -590.5, -90f, 90f)
    private val candyCane by lazy {
        val item = CraftventureCore.getItemStackDataDatabase().findSilent("candycane")?.itemStack
            ?: ItemStack(Material.IRON_SWORD)
        item.unbreakable()
        item.addEnchantment(Enchantment.DAMAGE_UNDEAD, 1)
        item
    }

    private var eventState = EventState.IDLE
        set(value) {
            field = value
//            Logger.info("New event state is $value")
            eventStateEnterTime = System.currentTimeMillis()
        }
    private var eventStateEnterTime = System.currentTimeMillis()
        set(value) {
            field = value
            lastUpdateTime = System.currentTimeMillis()
        }
    private var lastUpdateTime = System.currentTimeMillis()
    private var updateTick = 0

    private var mobs = mutableListOf<Entity>()
    private var elves = mutableListOf<Villager>()

    private val elfHat = CraftventureCore.getItemStackDataDatabase().findSilent("hat_elf")!!.itemStack!!
    private val elfChestplate =
        CraftventureCore.getItemStackDataDatabase().findSilent("costume_elf_chestplate")!!.itemStack!!
    private val elfLeggings =
        CraftventureCore.getItemStackDataDatabase().findSilent("costume_elf_leggings")!!.itemStack!!
    private val elfBoots = CraftventureCore.getItemStackDataDatabase().findSilent("costume_elf_boots")!!.itemStack!!

    private val bossBar = Bukkit.createBossBar(
        "",
        BarColor.YELLOW,
        BarStyle.SOLID
    )

    private val gen1Location = Location(Bukkit.getWorld("world"), 90.50, 25.00, -615.50, 270.35f, 2.40f)
    private val gen2Location = Location(Bukkit.getWorld("world"), 107.50, 25.00, -641.50, 0.06f, 6.00f)
    private val gen3Location = Location(Bukkit.getWorld("world"), 137.50, 28.00, -620.50, 86.31f, 16.20f)

    private val generators = arrayOfNulls<Blaze>(3)
    private var success = false

    private val room1Spawns = listOf(
        Location(Bukkit.getWorld("world"), 140.07, 24.00, -591.12, -13.40f, 0.45f),
        Location(Bukkit.getWorld("world"), 143.98, 26.00, -594.29, 26.35f, 7.80f),
        Location(Bukkit.getWorld("world"), 147.79, 24.00, -590.85, 93.55f, -0.60f),
        Location(Bukkit.getWorld("world"), 148.35, 24.00, -585.55, 93.10f, 0.90f),
        Location(Bukkit.getWorld("world"), 143.55, 24.00, -585.57, 15.85f, 5.85f),
        Location(Bukkit.getWorld("world"), 141.46, 24.00, -585.03, -6.20f, 6.15f),
        Location(Bukkit.getWorld("world"), 140.28, 24.00, -582.82, -14.45f, 3.90f),
        Location(Bukkit.getWorld("world"), 143.54, 24.00, -581.24, -23.90f, 4.20f),
        Location(Bukkit.getWorld("world"), 147.57, 24.00, -578.22, 73.15f, 2.70f),
        Location(Bukkit.getWorld("world"), 144.86, 24.00, -574.41, -88.25f, 3.60f),
        Location(Bukkit.getWorld("world"), 148.30, 24.00, -573.33, 110.65f, 6.60f),
        Location(Bukkit.getWorld("world"), 144.52, 24.00, -570.41, 160.45f, 0.90f),
        Location(Bukkit.getWorld("world"), 140.44, 24.00, -571.64, 179.05f, 3.60f),
        Location(Bukkit.getWorld("world"), 140.30, 24.00, -574.34, 179.95f, 3.60f),
        Location(Bukkit.getWorld("world"), 140.86, 24.00, -578.03, 176.95f, 4.65f),
        Location(Bukkit.getWorld("world"), 139.34, 24.00, -581.08, 116.50f, 13.80f),
        Location(Bukkit.getWorld("world"), 136.92, 24.00, -580.47, 5.20f, 3.60f),
        Location(Bukkit.getWorld("world"), 136.71, 24.00, -577.18, -0.35f, 2.85f),
        Location(Bukkit.getWorld("world"), 134.91, 24.00, -570.29, 129.10f, 0.15f),
        Location(Bukkit.getWorld("world"), 132.21, 24.00, -571.30, 194.20f, -0.60f),
        Location(Bukkit.getWorld("world"), 132.26, 24.00, -574.37, 266.20f, 4.20f),
        Location(Bukkit.getWorld("world"), 132.35, 24.00, -578.48, 271.30f, 3.60f)
    )
    private val room2Spawns = listOf(
        Location(Bukkit.getWorld("world"), 126.01, 24.00, -571.65, 122.65f, -3.45f),
        Location(Bukkit.getWorld("world"), 123.74, 24.06, -576.33, 90.25f, -2.70f),
        Location(Bukkit.getWorld("world"), 119.94, 24.00, -577.59, 45.40f, -0.30f),
        Location(Bukkit.getWorld("world"), 119.02, 24.06, -573.42, 87.40f, 3.00f),
        Location(Bukkit.getWorld("world"), 115.59, 24.00, -572.20, 90.85f, 2.55f),
        Location(Bukkit.getWorld("world"), 110.06, 24.00, -571.77, 120.25f, 4.95f),
        Location(Bukkit.getWorld("world"), 106.49, 24.06, -573.14, 238.00f, 6.15f),
        Location(Bukkit.getWorld("world"), 107.23, 24.06, -575.63, 259.90f, 3.30f),
        Location(Bukkit.getWorld("world"), 111.79, 24.00, -576.77, 259.90f, 4.35f),
        Location(Bukkit.getWorld("world"), 114.59, 24.00, -578.21, 174.70f, 1.80f),
        Location(Bukkit.getWorld("world"), 113.80, 25.06, -582.38, 179.05f, 1.20f),
        Location(Bukkit.getWorld("world"), 114.17, 24.00, -585.83, 95.95f, 2.70f),
        Location(Bukkit.getWorld("world"), 111.16, 24.00, -585.10, 56.65f, 1.65f),
        Location(Bukkit.getWorld("world"), 107.81, 24.00, -584.95, 158.80f, 4.95f),
        Location(Bukkit.getWorld("world"), 105.63, 24.00, -587.31, 200.20f, 4.95f),
        Location(Bukkit.getWorld("world"), 106.89, 24.06, -589.82, 189.70f, 3.45f),
        Location(Bukkit.getWorld("world"), 105.43, 24.00, -592.46, 191.35f, 3.60f),
        Location(Bukkit.getWorld("world"), 106.99, 24.06, -594.87, 192.10f, 4.05f),
        Location(Bukkit.getWorld("world"), 105.48, 24.00, -597.18, 276.40f, 6.90f),
        Location(Bukkit.getWorld("world"), 112.07, 24.00, -596.18, 4.60f, 7.65f),
        Location(Bukkit.getWorld("world"), 111.84, 24.00, -590.50, 1.45f, 3.45f),
        Location(Bukkit.getWorld("world"), 110.81, 24.00, -586.59, 40.00f, 3.00f),
        Location(Bukkit.getWorld("world"), 107.49, 24.06, -597.83, 180.40f, 3.15f)
    )
    private val dispenserRoomSpawns = listOf(
        Location(Bukkit.getWorld("world"), 104.20, 24.00, -607.47, 166.75f, -0.75f),
        Location(Bukkit.getWorld("world"), 104.34, 24.00, -611.16, 137.80f, 4.50f),
        Location(Bukkit.getWorld("world"), 100.82, 24.00, -612.24, 147.85f, 6.75f),
        Location(Bukkit.getWorld("world"), 97.94, 24.00, -617.58, 220.90f, 5.40f),
        Location(Bukkit.getWorld("world"), 100.24, 24.00, -620.98, 234.40f, 4.80f),
        Location(Bukkit.getWorld("world"), 103.82, 24.00, -622.53, 258.40f, 5.70f),
        Location(Bukkit.getWorld("world"), 106.81, 24.00, -620.12, 243.55f, 9.00f),
        Location(Bukkit.getWorld("world"), 111.56, 24.00, -624.69, 323.65f, 5.40f),
        Location(Bukkit.getWorld("world"), 113.73, 24.00, -622.17, 344.05f, 6.75f),
        Location(Bukkit.getWorld("world"), 112.73, 24.00, -618.95, 22.45f, 9.90f),
        Location(Bukkit.getWorld("world"), 113.32, 24.00, -617.86, 310.90f, 9.00f),
        Location(Bukkit.getWorld("world"), 117.36, 24.00, -616.45, 10.30f, 4.65f),
        Location(Bukkit.getWorld("world"), 117.70, 24.00, -612.76, 42.85f, 5.55f),
        Location(Bukkit.getWorld("world"), 114.76, 24.00, -609.54, 70.60f, 8.25f),
        Location(Bukkit.getWorld("world"), 112.01, 24.00, -608.34, 98.05f, 9.30f),
        Location(Bukkit.getWorld("world"), 109.71, 24.00, -610.64, 76.75f, 10.50f),
        Location(Bukkit.getWorld("world"), 109.93, 24.00, -606.35, 67.45f, 5.40f),
        Location(Bukkit.getWorld("world"), 106.09, 24.00, -604.49, 158.05f, 4.50f),
        Location(Bukkit.getWorld("world"), 103.42, 24.00, -606.49, 191.35f, 5.10f),
        Location(Bukkit.getWorld("world"), 105.77, 24.00, -611.46, 141.10f, 9.60f)
    )
    private val gen1Spawns = listOf(
        Location(Bukkit.getWorld("world"), 93.30, 24.00, -617.70, 331.26f, 2.70f),
        Location(Bukkit.getWorld("world"), 93.30, 24.00, -613.30, 209.16f, 3.15f)
    )
    private val gen2Spawns = listOf(
        Location(Bukkit.getWorld("world"), 106.58, 24.00, -631.31, 166.71f, 1.95f),
        Location(Bukkit.getWorld("world"), 104.85, 24.00, -634.59, 225.96f, 11.55f),
        Location(Bukkit.getWorld("world"), 104.75, 24.00, -637.84, 313.41f, 14.40f),
        Location(Bukkit.getWorld("world"), 107.52, 24.00, -639.19, 5.16f, 13.65f),
        Location(Bukkit.getWorld("world"), 110.15, 24.50, -638.02, 58.41f, 14.10f),
        Location(Bukkit.getWorld("world"), 109.14, 24.13, -634.66, 95.31f, 15.75f),
        Location(Bukkit.getWorld("world"), 107.95, 24.00, -631.55, 153.06f, 9.30f)
    )
    private val gen3Spawns = listOf(
        Location(Bukkit.getWorld("world"), 135.28, 27.00, -622.34, 38.46f, 16.05f),
        Location(Bukkit.getWorld("world"), 135.50, 27.00, -618.69, 77.76f, 13.50f)
    )
    private var presents = listOf(
        CraftventureCore.getItemStackDataDatabase().findSilent("gift1")!!.itemStack!!,
        CraftventureCore.getItemStackDataDatabase().findSilent("gift2")!!.itemStack!!,
        CraftventureCore.getItemStackDataDatabase().findSilent("gift3")!!.itemStack!!,
        CraftventureCore.getItemStackDataDatabase().findSilent("gift4")!!.itemStack!!
    )
    private val presentLocations = listOf(
        Location(Bukkit.getWorld("world"), 87.50, 40.00, -627.50),
        Location(Bukkit.getWorld("world"), 86.50, 40.00, -627.50),
        Location(Bukkit.getWorld("world"), 87.50, 40.00, -626.50),
        Location(Bukkit.getWorld("world"), 87.50, 40.00, -625.50),
        Location(Bukkit.getWorld("world"), 86.50, 40.00, -625.50),
        Location(Bukkit.getWorld("world"), 87.50, 40.00, -624.50),
        Location(Bukkit.getWorld("world"), 86.50, 40.00, -624.50),
        Location(Bukkit.getWorld("world"), 86.50, 40.00, -623.50),
        Location(Bukkit.getWorld("world"), 87.50, 40.00, -623.50),
        Location(Bukkit.getWorld("world"), 88.50, 40.00, -623.50),
        Location(Bukkit.getWorld("world"), 88.50, 40.00, -622.50),
        Location(Bukkit.getWorld("world"), 89.50, 40.00, -623.50),
        Location(Bukkit.getWorld("world"), 90.50, 40.00, -623.50),
        Location(Bukkit.getWorld("world"), 90.50, 40.00, -622.50),
        Location(Bukkit.getWorld("world"), 91.50, 40.00, -623.50),
        Location(Bukkit.getWorld("world"), 91.50, 40.00, -624.50),
        Location(Bukkit.getWorld("world"), 92.50, 40.00, -624.50),
        Location(Bukkit.getWorld("world"), 92.50, 40.00, -625.50),
        Location(Bukkit.getWorld("world"), 91.50, 40.00, -626.50),
        Location(Bukkit.getWorld("world"), 92.50, 40.00, -626.50),
        Location(Bukkit.getWorld("world"), 90.50, 40.00, -626.50),
        Location(Bukkit.getWorld("world"), 90.50, 40.00, -627.50),
        Location(Bukkit.getWorld("world"), 91.50, 40.00, -627.50),
        Location(Bukkit.getWorld("world"), 91.50, 40.00, -628.50),
        Location(Bukkit.getWorld("world"), 89.50, 40.00, -628.50),
        Location(Bukkit.getWorld("world"), 89.50, 40.00, -627.50),
        Location(Bukkit.getWorld("world"), 88.50, 40.00, -627.50),
        Location(Bukkit.getWorld("world"), 88.50, 40.00, -628.50)
    )
    //    private val gameProfiles = listOf(
//            CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_up_mouth_open")!!,
//            CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_down_mouth_open")!!,
//            CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_up_mouth_closed")!!,
//            CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_down_mouth_closed")!!
//    )
    private val headItems = listOf(
        CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_up_mouth_open")!!.toGameProfile().toSkullItem(),
        CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_down_mouth_open")!!.toGameProfile().toSkullItem(),
        CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_up_mouth_closed")!!.toGameProfile().toSkullItem(),
        CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_down_mouth_closed")!!.toGameProfile().toSkullItem()
    )

    private val gonBaoPath = listOf(
        Location(Bukkit.getWorld("world"), 107.47, 24.00, -606.43, -175.40f, -3.30f),
        Location(Bukkit.getWorld("world"), 108.35, 24.00, -609.49, -148.25f, -4.50f),
        Location(Bukkit.getWorld("world"), 110.85, 24.00, -612.47, -127.85f, -5.10f)
    )

    private fun randomHeadItem(mouthOpen: Boolean = false) = headItems.random(if (mouthOpen) 0 else 2, 2)
    private val playerNpcLocation = Location(Bukkit.getWorld("world"), 116.2, 24.0, -615.5, 90f, -2f)
    private val playerNpcs = mutableListOf<NpcEntity>()

    private var isTalking = false
    private var wasTalking = false

    init {
        lobby.lobbyListener = this
        gonBaoNpc = NpcEntity(
            NpcEntityType.PLAYER,
            gonBaoNpcLocation.clone(),
            CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_down_mouth_closed")!!
        )
        gonBaoNpc.held(MaterialConfig.CANDY_CANE)
        gonBaoNpc.name?.let {
            NameTagManager.addNpc(it)
        }
        gonBaoNpc.helmet(headItems.last())

        maxNpc = NpcEntity(NpcEntityType.WOLF, maxNpcLocation.clone())
        maxNpc.customName("§4§lMax")
        maxNpc.customNameVisible(true)
        maxNpc.tamedSit(true)

        gonBaoTracker.addEntity(gonBaoNpc)
        gonBaoTracker.addEntity(maxNpc)
        gonBaoTracker.startTracking()
        playerTracker.startTracking()

        killArea.loadChunks(true)
        Bukkit.getWorld("world")?.entities?.forEach {
            if (killArea.isInArea(it.location)) {
                if (it is Zombie || it is CaveSpider || it is Villager || it is Blaze) {
                    it.remove()
                } else if (it.customName == "cinematic") {
                    it.remove()
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerStuck(event: PlayerStuckEvent) {
        if (state == State.RUNNING) {
            for (player in players) {
                if (event.player == player.player) {
                    player.allowNextTeleport()
                    player.player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    event.isCancelled = true
                    return
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        players.firstOrNull { it.player === event.player }?.let {
            //            Logger.info("onPlayerInteractAtEntity")
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        players.firstOrNull { it.player === event.player }?.let {
            //            Logger.info("firstOrNull")
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        players.firstOrNull { it.player === event.damager }?.let {
            if (!mobs.any { it === event.entity } && !generators.any { it === event.entity }) {
//                Logger.info("onEntityDamageByEntityEvent")
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        players.firstOrNull { it.player === event.player }?.let {
            //            Logger.info("onPlayerInteractEvent")
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (players.any { it.player == event.player }) {
            if (isRunning())
                event.respawnLocation = spawnLocation
            else
                event.respawnLocation = exitLocation
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    override fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (players.any { it.player === event.entity }) {
            if (event.cause == EntityDamageEvent.DamageCause.FALL)
                event.isCancelled = true
        }
    }

    @EventHandler
    fun onTarget(event: EntityTargetEvent) {
        if (mobs.any { it === event.entity }) {
            if (elves.any { it === event.target }) {
                event.isCancelled = true
            } else if (!players.any { it.player === event.target }) {
                event.isCancelled = true
            }
        }
    }

    // TODO: Implement some caching to prevent allocation/sorting a list every tick
    override fun sortedPlayers(): List<GonBaoPlayer> = players//.sortedWith(
//            compareBy(
//                    { it.finishTime < 0 },
//                    { it.finishTime }
//            )
//    )

    override fun update(timePassed: Long) {
        super.update(timePassed)
        if (state == State.RUNNING) {
            updateGonBaoTalking()
            for (player in players) {
                player.update(level = minigameLevel)
            }

            if (eventState != EventState.FINALE) {
                val sortedPlayers = sortedPlayers()

                for (player in sortedPlayers) {
                    val timeLeft = maxGameTimeLength() - playTime()
                    MessageBarManager.display(
                        player.player,
                        ChatUtils.createComponent(
                            "Event ending in ${DateUtils.format(timeLeft, "?")}",
                            CVChatColor.WARNING
                        ),
                        MessageBarManager.Type.MINIGAME,
                        TimeUtils.secondsFromNow(1.0),
                        ChatUtils.ID_MINIGAME
                    )
                }
            }

            val previousTime = lastUpdateTime - eventStateEnterTime
            val nowTime = System.currentTimeMillis() - eventStateEnterTime
            lastUpdateTime = System.currentTimeMillis()

            elves.removeAll {
                val remove = it.ticksLived > 20 * 4 || !it.isValid
                if (remove) {
                    it.location.clone().add(0.0, 0.4, 0.0).spawnParticleX(
                        Particle.CRIT_MAGIC,
                        10,
                        0.1, 0.1, 0.1
                    )
                    it.remove()
                }
                remove
            }
            mobs.removeAll {
                if (!it.isValid) {
                    val elf = spawnElf(it.location)
                    elves.add(elf)
                    return@removeAll true
                }
                return@removeAll false
            }
            when (eventState) {
                GonBaoEvent.EventState.IDLE -> doEventIdle(previousTime, nowTime)
                GonBaoEvent.EventState.PROLOGUE -> doEventPrologue(previousTime, nowTime)
                GonBaoEvent.EventState.WAVE1 -> doEventWave1(previousTime, nowTime)
                GonBaoEvent.EventState.WAVE2 -> doEventWave2(previousTime, nowTime)
                GonBaoEvent.EventState.DISPENSERS_INTRO -> doEventDispenserIntro(previousTime, nowTime)
                GonBaoEvent.EventState.DISPENSER_GEN1 -> doEventDispenserGenerator1(previousTime, nowTime)
                GonBaoEvent.EventState.DISPENSER_GEN2 -> doEventDispenserGenerator2(previousTime, nowTime)
                GonBaoEvent.EventState.DISPENSER_GEN3 -> doEventDispenserGenerator3(previousTime, nowTime)
                GonBaoEvent.EventState.FINALE -> doEventFinale(previousTime, nowTime)
            }
        }
        updateTick++
    }

    private fun cleanupNpcs() {
        playerNpcs.forEach {
            playerTracker.removeEntity(it)
            NameTagManager.removeNpc(it.name!!)
        }
        playerNpcs.clear()
    }

    private fun reset() {
        success = false
        cleanupNpcs()
        if (gonBaoNpcWalking != null) {
            playerTracker.removeEntity(gonBaoNpcWalking)
            NameTagManager.removeNpc(gonBaoNpcWalking!!.name!!)
            gonBaoNpcWalking = null
        }

        bossBar.isVisible = true
        updateTick = 0

        lever1.block.powerAsLever(false)
        lever2.block.powerAsLever(false)

        cinematicCamera?.remove()
        mobs.forEach { it.remove() }
        mobs.clear()
        elves.forEach { it.remove() }
        elves.clear()

        generators.forEach { it?.remove() }
        generators.forEachIndexed { index, blaze -> generators[index] = null }

        wave1Spawned = 0
        wave2Spawned = 0
        dispenserIntroSpawned = 0

        executeAsync {
            PlaceSchematicAction("gon", "code").setName(SCHEM_DOOR_1_CLOSED).noAir(false).execute(gonBaoTracker)
            PlaceSchematicAction("gon", "code").setName(SCHEM_DOOR_2_CLOSED).noAir(false).execute(gonBaoTracker)
            PlaceSchematicAction("gon", "code").setName(SCHEM_DOOR_3_CLOSED).noAir(false).execute(gonBaoTracker)

            PlaceSchematicAction("gon", "code").setName(SCHEM_GENERATOR_1_DOOR_CLOSED).noAir(false)
                .execute(gonBaoTracker)
            PlaceSchematicAction("gon", "code").setName(SCHEM_GENERATOR_2_DOOR_CLOSED).noAir(false)
                .execute(gonBaoTracker)
            PlaceSchematicAction("gon", "code").setName(SCHEM_GENERATOR_3_DOOR_CLOSED).noAir(false)
                .execute(gonBaoTracker)

            PlaceSchematicAction("gon", "code").setName(SCHEM_GENERATOR_AREA).noAir(false).execute(gonBaoTracker)
        }

        bossBar.setTitle("§6Listen to what Mr. Gon Bao has to say about the situation")
        bossBar.progress = 0.0

        for (i in 0 until 3) {
            generators.getOrNull(i)?.remove()
            generators[i] = null
        }
        generators[0] = spawnGeneratorBlaze(gen1Location)
        generators[1] = spawnGeneratorBlaze(gen2Location)
        generators[2] = spawnGeneratorBlaze(gen3Location)
    }

    private fun updateGonBaoTalking() {
        if (shouldUpdateHelmet()) {
//            Logger.info("$isTalking $wasTalking")
            if (isTalking) {
                val head = headItems.random()
                gonBaoNpc.helmet(head)
                gonBaoNpcWalking?.helmet(head)
            } else if (isTalking != wasTalking && !isTalking) {
                gonBaoNpc.helmet(headItems.last())
                gonBaoNpcWalking?.helmet(headItems.last())
            }
            wasTalking = isTalking
            isTalking = false
        }
    }

    private fun shouldUpdateHelmet() = updateTick % 5 == 0

    private fun doEventIdle(previousTime: Long, nowTime: Long) {}

    private fun doEventPrologue(previousTime: Long, nowTime: Long) {
        var time = 10
        val range = previousTime until nowTime

        // start delay
        time += 5000
        if (time in range) {
            players.forEach { it.player.playSound(gonBaoNpcLocation, PROLOGUE_1, SoundCategory.AMBIENT, 1000f, 1f) }
        }

        if (nowTime in (time + 400)..(time + 1100) ||
            nowTime in (time + 1400)..(time + 2200) ||
            nowTime in (time + 2600)..(time + 4600)
        ) {
            isTalking = true
        }

        // prologue 1
        time += 6000
        if (time in range) {
            players.forEach { it.player.playSound(gonBaoNpcLocation, PROLOGUE_2, SoundCategory.AMBIENT, 1000f, 1f) }
        }

        if (nowTime in (time + 600)..(time + 2600) ||
            nowTime in (time + 3700)..(time + 7200) ||
            nowTime in (time + 8200)..(time + 10100)
        ) {
            isTalking = true
        }

        // prologue 2
        time += 11000
        if (time in range) {
            players.forEach { it.player.playSound(gonBaoNpcLocation, PROLOGUE_3, SoundCategory.AMBIENT, 1000f, 1f) }
        }

        if (nowTime in (time + 500)..(time + 3100) ||
            nowTime in (time + 3900)..(time + 7600) ||
            nowTime in (time + 8400)..(time + 10100) ||
            nowTime in (time + 11400)..(time + 16700) ||
            nowTime in (time + 17600)..(time + 19700)
        ) {
            isTalking = true
        }

        // prologue 3
        time += 20000
        if (time in range)
            gonBaoNpc.lookAt(lever1)

        time += 800
        if (time in range) {
            gonBaoNpc.swingMainHand()
            lever1.block.powerAsLever(true)
            players.forEach { it.player.playSound(gonBaoNpcLocation, HISS, SoundCategory.AMBIENT, 1000f, 1f) }

            val area = SimpleArea("world", 122.0, 25.0, -597.0, 133.0, 30.0, -585.0)

            area.loc1.world?.spawnParticleX(
                Particle.EXPLOSION_NORMAL,
                area.loc1.x + (area.loc2.x - area.loc1.x) * (0.5),
                area.loc1.y + (area.loc2.y - area.loc1.y) * (0.5),
                area.loc1.z + (area.loc2.z - area.loc1.z) * (0.5),
                300,
                4.0, 2.0, 4.0
            )

            bossBar.setTitle("§6Advance to the next room and convert the elves in room 1")
            bossBar.progress = 0.0
        }

        time += 800
        if (time in range)
            gonBaoNpc.move(gonBaoNpcLocation)

        time += 1000
        if (time in range) {
            players.forEach { it.player.playSound(gonBaoNpcLocation, PROLOGUE_4, SoundCategory.AMBIENT, 1000f, 1f) }
        }

        if (nowTime in (time + 200)..(time + 3100)) {
            isTalking = true
        }

        time += 4000
        if (time in range)
            gonBaoNpc.lookAt(lever2)

        time += 800
        if (time in range) {
            gonBaoNpc.swingMainHand()
            lever2.block.powerAsLever(true)
            PlaceSchematicAction("gblm", "code").setName(SCHEM_DOOR_1_OPEN).noAir(false).execute(gonBaoTracker)
        }

        time += 800
        if (time in range)
            gonBaoNpc.move(gonBaoNpcLocation)

        time += 300
        if (time in range) {
            players.forEach { it.player.playSound(gonBaoNpcLocation, PROLOGUE_5, SoundCategory.AMBIENT, 1000f, 1f) }
        }

        if (nowTime in (time + 400)..(time + 1500)) {
            isTalking = true
        }

        time += 3000
        if (time in range) {
            eventState = EventState.WAVE1
            bossBar.setTitle("§6Convert the elves in room 1")
            bossBar.progress = 0.0
        }
    }

    private var wave1Spawned = 0
    private fun doEventWave1(previousTime: Long, nowTime: Long) {
        var validCount = mobs.count { it.isValid }
        if (wave1Spawned < 20 && validCount < 3) {
            val zombie = spawnElfZombie(room1Spawns.random()!!)
            mobs.add(zombie)
            wave1Spawned++
            validCount++
        }

        val progress = (wave1Spawned - validCount) / 20.0
        if (bossBar.progress != progress)
            bossBar.progress = progress.clamp(0.0, 1.0)

        if (wave1Spawned == 20 && validCount <= 0) {
            PlaceSchematicAction("gblm", "code").setName(SCHEM_DOOR_2_OPEN).noAir(false).execute(gonBaoTracker)
            players.forEach { it.player.playSound(gonBaoNpcLocation, WAVE1_END, SoundCategory.AMBIENT, 1000f, 1f) }
            eventState = EventState.WAVE2
            bossBar.setTitle("§6Convert the elves in room 2")
            bossBar.progress = 0.0
        }
    }

    private var wave2Spawned = 0
    private fun doEventWave2(previousTime: Long, nowTime: Long) {
        var validCount = mobs.count { it.isValid }
        if (wave2Spawned < 50 && validCount < 6) {
            val zombie = spawnElfZombie(room2Spawns.random()!!)
            mobs.add(zombie)
            wave2Spawned++
            validCount++
        }

        val progress = (wave2Spawned - validCount) / 50.0
        if (bossBar.progress != progress)
            bossBar.progress = progress.clamp(0.0, 1.0)

        if (wave2Spawned == 50 && validCount <= 0) {
            players.forEach { it.player.playSound(gonBaoNpcLocation, WAVE2_END, SoundCategory.AMBIENT, 1000f, 1f) }
            eventState = EventState.DISPENSERS_INTRO
            bossBar.setTitle("§6Move to the dispenser room")
            bossBar.progress = 0.0
        }
    }

    private var dispenserIntroSpawned = 0
    private fun doEventDispenserIntro(previousTime: Long, nowTime: Long) {
        var time = 10
        val range = previousTime until nowTime

        if (time in range) {
            PlaceSchematicAction("gblm", "code").setName(SCHEM_DOOR_3_OPEN).noAir(false).execute(gonBaoTracker)
        }

        time += 8000
        if (time in range) {
            players.forEach {
                it.player.playSound(
                    gonBaoNpcLocation,
                    DISPENSER_INTRO,
                    SoundCategory.AMBIENT,
                    1000f,
                    1f
                )
            }
            bossBar.setTitle("§6Await Mr. Gon Bao's instructions")
            bossBar.progress = 0.0
        }

        if (8000 + 16000 in range) {
            bossBar.setTitle("§6Clear the dispenser's main area")
            bossBar.progress = 0.0
        }

        if (nowTime > 8000 + 16000) {
            var validCount = mobs.count { it.isValid }
            if (dispenserIntroSpawned < 40 && validCount < 5) {
                val zombie = spawnElfZombie(dispenserRoomSpawns.random()!!)
                mobs.add(zombie)
                dispenserIntroSpawned++
                validCount++
            }

            val progress = (dispenserIntroSpawned - validCount) / 40.0
            if (bossBar.progress != progress)
                bossBar.progress = progress.clamp(0.0, 1.0)

            if (dispenserIntroSpawned == 40 && validCount <= 0) {
                PlaceSchematicAction("gblm", "code").setName(SCHEM_GENERATOR_1_DOOR_OPEN).noAir(false)
                    .execute(gonBaoTracker)
                eventState = EventState.DISPENSER_GEN1
                bossBar.setTitle("§6Destroy the first generator")
            }
        }
    }

    private fun doEventDispenserGenerator1(previousTime: Long, nowTime: Long) {
        val blaze = generators.getOrNull(0)
        val maxHp = blaze?.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: 1.0
        val hp = blaze?.health ?: 0.0
        bossBar.progress = (hp / maxHp).clamp(0.0, 1.0)

        if (blaze?.isValid == false || hp == 0.0) {
            PlaceSchematicAction("gblm", "code").setName(SCHEM_GENERATOR_2_DOOR_OPEN).noAir(false)
                .execute(gonBaoTracker)
            players.forEach {
                it.player.playSound(
                    gonBaoNpcLocation,
                    DISPENSER_WAVE1_END,
                    SoundCategory.AMBIENT,
                    1000f,
                    1f
                )
            }
            eventState = EventState.DISPENSER_GEN2
            bossBar.setTitle("§6Destroy the second generator")
        } else {
            val validCount = mobs.count { it.isValid }
            if (validCount < 5) {
                val zombie = spawnElfZombie(gen1Spawns.random()!!)
                mobs.add(zombie)
            }
        }
    }

    private fun doEventDispenserGenerator2(previousTime: Long, nowTime: Long) {
        val blaze = generators.getOrNull(1)
        val maxHp = blaze?.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: 1.0
        val hp = blaze?.health ?: 0.0
        bossBar.progress = (hp / maxHp).clamp(0.0, 1.0)

        if (blaze?.isValid == false || hp == 0.0) {
            PlaceSchematicAction("gblm", "code").setName(SCHEM_GENERATOR_3_DOOR_OPEN).noAir(false)
                .execute(gonBaoTracker)
            players.forEach {
                it.player.playSound(
                    gonBaoNpcLocation,
                    DISPENSER_WAVE2_END,
                    SoundCategory.AMBIENT,
                    1000f,
                    1f
                )
            }
            eventState = EventState.DISPENSER_GEN3
            bossBar.setTitle("§6Destroy the third generator")
        } else {
            val validCount = mobs.count { it.isValid }
            if (validCount < 5) {
                val zombie = spawnElfZombie(gen2Spawns.random()!!)
                mobs.add(zombie)
            }
        }
    }

    private fun doEventDispenserGenerator3(previousTime: Long, nowTime: Long) {
        val blaze = generators.getOrNull(2)
        val maxHp = blaze?.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: 1.0
        val hp = blaze?.health ?: 0.0
        bossBar.progress = (hp / maxHp).clamp(0.0, 1.0)

        if (blaze?.isValid == false || hp == 0.0) {
            eventState = EventState.FINALE
            bossBar.setTitle("")
            overTime += 60
        } else {
            val validCount = mobs.count { it.isValid }
            if (validCount < 8) {
                val zombie = spawnElfZombie(gen3Spawns.random()!!)
                mobs.add(zombie)
            }
        }
    }

    private fun doEventFinale(previousTime: Long, nowTime: Long) {
        if (bossBar.isVisible)
            bossBar.isVisible = false
        var time = 10L
        val range = previousTime until nowTime

        if (playerNpcs.isEmpty()) {
            for ((index, gamePlayer) in players.withIndex()) {
                val player = gamePlayer.player
                val location = playerNpcLocation.clone()
                val zAdd = if (index % 2 == 0)
                    1.0 * (index * 0.8)
                else
                    -1.0 * (index * 0.8)
                location.z += zAdd
                val profile = CachedGameProfile(player)
                profile.name = "npc" + index
                val npc = NpcEntity(NpcEntityType.PLAYER, location, profile)

                NameTagManager.addNpc(npc.name!!)
                playerNpcs.add(npc)
                playerTracker.addEntity(npc)
            }
        }

        if (2500 in range) {
            for (playerNpc in playerNpcs) {
//                playerNpc.allYaws(0f)
                playerNpc.lookAt(explosionLocation)
            }
        }

        if (gonBaoNpcWalking == null) {
            gonBaoNpcWalking = NpcEntity(
                NpcEntityType.PLAYER,
                gonBaoPath[0].clone(),
                CraftventureCore.getGameProfileDatabase().findSilent("gonbao_mustache_down_mouth_closed")!!
            )
            gonBaoNpcWalking!!.held(candyCane)
            gonBaoNpcWalking!!.helmet(headItems.last())
            gonBaoNpcWalking!!.name?.let {
                NameTagManager.addNpc(it)
            }
            playerTracker.addEntity(gonBaoNpcWalking)
        }

        if (time in range) {
            mobs.forEach { it.remove() }
            mobs.clear()
        }

        if (cinematicCamera == null || cinematicCamera?.isValid == false) {
            cinematicCamera = cinematicLocation.spawn()
            cinematicCamera?.apply {
                customName = "cinematic"
                isMarker = true
                isVisible = false
                setAI(false)
                setGravity(false)
                isSmall = true
                setBasePlate(false)
            }
        }

        if (nowTime < 4500.0) {
            val progress = (nowTime / 4500.0).clamp(0.0, 1.0)
            val location = Location(
                cinematicEndLocation.world,
                cinematicLocation.x.progressTo(cinematicEndLocation.x, progress),
                cinematicLocation.y.progressTo(cinematicEndLocation.y, progress),
                cinematicLocation.z.progressTo(cinematicEndLocation.z, progress),
                cinematicLocation.yaw.progressTo(cinematicEndLocation.yaw, progress),
                cinematicLocation.pitch.progressTo(cinematicEndLocation.pitch, progress)
            )
            EntityUtils.teleport(cinematicCamera!!, location)
        }

        if (9500 in range) {
            EntityUtils.teleport(cinematicCamera!!, overShoulderOfPlayers)
            for (playerNpc in playerNpcs) {
                playerNpc.lookAt(gonBaoPath.last())
            }
        }

        if (nowTime in 9500..11500) {
            val progress = ((nowTime - 9500) / 2000.0).clamp(0.0, 1.0)
            val start = gonBaoPath[0]
            val end = gonBaoPath[1]
            val location = Location(
                end.world,
                start.x.progressTo(end.x, progress),
                start.y.progressTo(end.y, progress),
                start.z.progressTo(end.z, progress),
                start.yaw.progressTo(end.yaw, progress),
                start.pitch.progressTo(end.pitch, progress)
            )
            gonBaoNpcWalking!!.move(location)
        }

        if (nowTime in 11500..13500) {
            val progress = ((nowTime - 11500) / 2000.0).clamp(0.0, 1.0)
            val start = gonBaoPath[1]
            val end = gonBaoPath[2]
            val location = Location(
                end.world,
                start.x.progressTo(end.x, progress),
                start.y.progressTo(end.y, progress),
                start.z.progressTo(end.z, progress),
                start.yaw.progressTo(end.yaw, progress),
                start.pitch.progressTo(end.pitch, progress)
            )
            gonBaoNpcWalking!!.move(location)
        }

        val playFinale = 14300
        if (playFinale in range) {
            players.forEach { it.player.playSound(gonBaoNpcLocation, FINALE, SoundCategory.AMBIENT, 1000f, 1f) }
        }

        if (nowTime in (playFinale + 400)..(playFinale + 1500) ||
            nowTime in (playFinale + 2000)..(playFinale + 3500) ||
            nowTime in (playFinale + 3900)..(playFinale + 5200) ||
            nowTime in (playFinale + 5800)..(playFinale + 8100)
        ) {
            isTalking = true
        }

        if (24000 in range) {
            stop(StopReason.ALL_PLAYERS_FINISHED)
        }

//        if (9500 in range) {
//            EntityUtils.teleport(cinematicCamera, overShoulderOfPlayers)
//        }

        when {
            nowTime < 2000 -> explosionLocation.spawnParticleX(
                particle = Particle.SMOKE_NORMAL,
                count = 10,
                offsetX = 0.1,
                offsetZ = 0.1
            )
            nowTime < 3000 -> explosionLocation.spawnParticleX(
                particle = Particle.SMOKE_LARGE,
                count = 10,
                offsetX = 0.1,
                offsetZ = 0.1
            )
            nowTime < 4500 -> explosionLocation.spawnParticleX(
                particle = Particle.EXPLOSION_NORMAL,
                count = 10,
                offsetX = 0.1,
                offsetZ = 0.1
            )
        }

        if (nowTime < 6500 && updateTick % 5 == 0) {
            val random = CraftventureCore.getRandom().nextInt(3)
            when (random) {
                0 -> players.forEach {
                    it.player.playSound(
                        explosionLocation,
                        Sound.ENTITY_CREEPER_PRIMED,
                        SoundCategory.AMBIENT,
                        1000f,
                        1f
                    )
                }
                1 -> players.forEach {
                    it.player.playSound(
                        explosionLocation,
                        Sound.ENTITY_PARROT_IMITATE_CREEPER,
                        SoundCategory.AMBIENT,
                        1000f,
                        1f
                    )
                }
                2 -> players.forEach {
                    it.player.playSound(
                        explosionLocation,
                        Sound.ENTITY_GENERIC_BURN,
                        SoundCategory.AMBIENT,
                        1000f,
                        1f
                    )
                }
            }
        }

        time += 6500
        if (time in range) {
            players.forEach {
                it.player.playSound(
                    explosionLocation,
                    Sound.ENTITY_BLAZE_SHOOT,
                    SoundCategory.AMBIENT,
                    1000f,
                    1f
                )
            }
            players.forEach {
                it.player.playSound(
                    explosionLocation,
                    Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,
                    SoundCategory.AMBIENT,
                    1000f,
                    1f
                )
            }
            explosionLocation.spawnParticleX(
                particle = Particle.EXPLOSION_HUGE,
                count = 10,
                offsetX = 0.1,
                offsetZ = 0.1
            )
        }

        time += 500
        if (time in range) {
            PlaceSchematicAction("gblm", "code").setName(SCHEM_GENERATOR_DESTROYED).noAir(false).execute(gonBaoTracker)
        }

        for (gamePlayer in players) {
            val player = gamePlayer.player

            if (player.gameMode != GameMode.SPECTATOR)
                player.gameMode = GameMode.SPECTATOR

            if (player.spectatorTarget != cinematicCamera) {
                gamePlayer.allowNextTeleport()
                player.teleport(cinematicCamera!!.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
                player.equipment?.helmet = ItemStack(Material.PUMPKIN)
                player.spectatorTarget = cinematicCamera
//                player.resetFloatingState()
            }
        }
    }

    private fun spawnGeneratorBlaze(location: Location): Blaze {
        val blaze = location.spawn<Blaze>()
        blaze.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = 800.0
        blaze.health = 800.0
        blaze.removeWhenFarAway = false
        blaze.setAI(false)
        return blaze
    }

    private fun spawnElfZombie(location: Location): Zombie {
        val zombie = location.spawn<Zombie>()
        zombie.isBaby = true
        zombie.equipment?.clear()
        zombie.vehicle?.remove()
        zombie.removeWhenFarAway = false
        zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue = 1.5
        zombie.equipment?.apply {
            helmet = this@GonBaoEvent.elfHat
            chestplate = this@GonBaoEvent.elfChestplate
            leggings = this@GonBaoEvent.elfLeggings
            boots = this@GonBaoEvent.elfBoots
            setItemInMainHand(this@GonBaoEvent.candyCane)
        }
        return zombie
    }

    private fun spawnElf(location: Location): Villager {
        val villager = location.spawn<Villager>()
        villager.profession = Villager.Profession.NITWIT
        villager.setBaby()
        villager.ageLock = true
        villager.ticksLived = 1
        villager.noDamageTicks = 20 * 10
//        villager.ageLock = true
        villager.equipment?.clear()
        villager.vehicle?.remove()
        villager.removeWhenFarAway = false
        villager.equipment?.apply {
            helmet = this@GonBaoEvent.elfHat
            chestplate = this@GonBaoEvent.elfChestplate
            leggings = this@GonBaoEvent.elfLeggings
            boots = this@GonBaoEvent.elfBoots
            setItemInMainHand(this@GonBaoEvent.candyCane)
        }
        return villager
    }

    override fun onStateChanged(oldState: State, newState: State) {
        super.onStateChanged(oldState, newState)

        eventState = if (newState == State.RUNNING) {
            reset()
            if (DEBUG) {
                PlaceSchematicAction("gblm", "code").setName(SCHEM_DOOR_1_OPEN).noAir(false).execute(gonBaoTracker)
                PlaceSchematicAction("gblm", "code").setName(SCHEM_DOOR_2_OPEN).noAir(false).execute(gonBaoTracker)
                PlaceSchematicAction("gblm", "code").setName(SCHEM_DOOR_3_OPEN).noAir(false).execute(gonBaoTracker)
                EventState.DISPENSER_GEN3
            } else
                EventState.PROLOGUE
        } else {
            EventState.IDLE
        }
    }

    override fun onUpdatePlayerWornItems(player: GonBaoPlayer, event: PlayerWornItemPreUpdateEvent) {
        super.onUpdatePlayerWornItems(player, event)
        event.wornData.balloonItem = null
//        event.wornData.miscItem = null
        event.wornData.title = null

        event.wornData.weaponItem = candyCane.toCachedItem()
        event.wornData.helmetItem = null
        event.wornData.chestplateItem = null
        event.wornData.leggingsItem = null
        event.wornData.bootsItem = null
        player.player.inventory.heldItemSlot = 4
    }

    override fun startWith(players: List<Player>): MutableList<GonBaoPlayer> {
        val gamePlayers = mutableListOf<GonBaoPlayer>()

        for (player in players) {
            player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
            player.gameMode = GameMode.ADVENTURE
            bossBar.addPlayer(player)
        }

        players.mapTo(gamePlayers) { player ->
            GonBaoPlayer(player)
        }

        return gamePlayers
    }

    override fun onPlayerLeft(player: GonBaoPlayer, leftReason: LeftReason) {
        super.onPlayerLeft(player, leftReason)
        bossBar.removePlayer(player.player)
    }

    override fun onPreStop(stopReason: StopReason) {
        super.onPreStop(stopReason)
        val stopState = eventState
        success = stopState == EventState.FINALE

        Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {

            if (stopState == EventState.FINALE) {
                Bukkit.getServer()
                    .broadcastMessage(CVChatColor.COMMAND_GENERAL + "Mr. Gon Bao's sauce dispenser has exploded. Presents have spawned near the christmas tree")

                val spawnTask = Runnable {
                    for (presentLocation in presentLocations) {
                        val fallBlock = presentLocation.block
                        if (fallBlock.type != Material.AIR)
                            continue

                        fallBlock.type = Material.PLAYER_HEAD

                        val state = fallBlock.state
                        if (state is Skull) {
                            val baseItem = presents.random()!!.itemMeta as SkullMeta
//                        Logger.console("Owner ${meta.owningPlayer?.uniqueId?.toString()}")
//                        state.owningPlayer = baseItem.owningPlayer ?: Bukkit.getOfflinePlayer("Joeywp")
                            state.setGameProfile(baseItem.getGameProfile())
                            state.update(true, true)
                        }
                    }
                }
                spawnTask.run()

                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), spawnTask, 20 * 10)
            } else {
                for (i in 0 until 10) {
                    spawnElfZombie(exitLocation)
                }
                Bukkit.getServer()
                    .broadcastMessage(CVChatColor.COMMAND_GENERAL + "Mr. Gon Bao's sauce dispenser is overdispensing. Corrupted elves have been spotted roaming around in the winter area")
            }
        }, 20 * 10)
    }

    override fun onStopped(stopReason: StopReason) {
        cleanupNpcs()
        mobs.forEach { it.remove() }
        elves.forEach { it.remove() }
        generators.forEach { it?.remove() }

        bossBar.removeAll()
        val finishedNormally =
            success && (stopReason == StopReason.ALL_PLAYERS_FINISHED || stopReason == StopReason.OUT_OF_TIME)
        when {
            finishedNormally -> {
                val players = sortedPlayers()
                for (minigamePlayer in players) {
                    val player = minigamePlayer.player
                    player.sendMessage(CVChatColor.COMMAND_GENERAL + "You helped to fix Gon Bao's little mistake! Thank you for helping!")

                    executeAsync {
                        (internalName == "winter2017_jump").let {
                            val delta = 20L
                            val accountType = BankAccountType.WINTERCOIN
                            CraftventureCore.getBankAccountDatabase()
                                .delta(player.uniqueId, accountType, delta, TransactionType.MINIGAME)
                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "+$delta ${accountType.pluralName} for playing")
                        }
                        MainRepositoryProvider.playerOwnedItemRepository
                            .createOneLimited(player.uniqueId, "candy_cane", -1)
                        MainRepositoryProvider.playerOwnedItemRepository
                            .createOneLimited(player.uniqueId, "gonbao_lever_hat", -1)
                        MainRepositoryProvider.achievementProgressRepository
                            .reward(player.uniqueId, "minigame_${internalName}_play")
                    }
                }
            }

            stopReason == StopReason.OUT_OF_TIME -> players
                .asSequence()
                .map { it.player }
                .forEach { it.sendMessage(CVChatColor.COMMAND_GENERAL + "You failed to fix Gon Bao's mistake in time, better luck next time!") }

            else -> players
                .asSequence()
                .map { it.player }
                .forEach { Translation.MINIGAME_TOO_FEW_PLAYERS.getTranslation(it)?.sendTo(it) }
        }
    }

    enum class EventState {
        IDLE,
        PROLOGUE,
        WAVE1,
        WAVE2,
        DISPENSERS_INTRO,
        DISPENSER_GEN1,
        DISPENSER_GEN2,
        DISPENSER_GEN3,
        FINALE
    }

    private val PRE_1 = "craftventure.event.gblm.pre_1"

    private val PROLOGUE_1 = "craftventure.event.gblm.prologue_1"
    private val PROLOGUE_2 = "craftventure.event.gblm.prologue_2"
    private val PROLOGUE_3 = "craftventure.event.gblm.prologue_3"
    private val PROLOGUE_4 = "craftventure.event.gblm.prologue_4"
    private val PROLOGUE_5 = "craftventure.event.gblm.prologue_5"
    private val WAVE1_END = "craftventure.event.gblm.wave1_end"

    private val WAVE2_END = "craftventure.event.gblm.wave2_end"
    private val DISPENSER_INTRO = "craftventure.event.gblm.dispenser_intro"

    private val DISPENSER_WAVE1_END = "craftventure.event.gblm.dispenser_wave1_end"
    private val DISPENSER_WAVE2_END = "craftventure.event.gblm.dispenser_wave2_end"

    private val FINALE = "${SoundUtils.SOUND_PREFIX}:event.gblm.finale"
    private val HISS = "${SoundUtils.SOUND_PREFIX}:tech.hiss1"

    private val SCHEM_DOOR_1_CLOSED = "gblm_door1_closed"
    private val SCHEM_DOOR_1_OPEN = "gblm_door1_open"
    private val SCHEM_DOOR_2_CLOSED = "gblm_door2_closed"
    private val SCHEM_DOOR_2_OPEN = "gblm_door2_open"
    private val SCHEM_DOOR_3_CLOSED = "gblm_door3_closed"
    private val SCHEM_DOOR_3_OPEN = "gblm_door3_open"

    private val SCHEM_GENERATOR_1_DOOR_CLOSED = "gblm_generator1_closed"
    private val SCHEM_GENERATOR_2_DOOR_CLOSED = "gblm_generator2_closed"
    private val SCHEM_GENERATOR_3_DOOR_CLOSED = "gblm_generator3_closed"
    private val SCHEM_GENERATOR_1_DOOR_OPEN = "gblm_generator1_open"
    private val SCHEM_GENERATOR_2_DOOR_OPEN = "gblm_generator2_open"
    private val SCHEM_GENERATOR_3_DOOR_OPEN = "gblm_generator3_open"
    private val SCHEM_GENERATOR_AREA = "gblm_dispenser_area"
    private val SCHEM_GENERATOR_DESTROYED = "gblm_dispenser_area_destroyed"
}*/
