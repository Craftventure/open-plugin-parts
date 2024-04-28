package net.craftventure.core.listener

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.craftventure.bukkit.ktx.area.AreaTracker
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.CraftventureCore
import net.craftventure.core.api.CvApi
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.dragonclan.MobHomeGoal
import net.craftventure.core.extension.spawn
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.Permissions
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.ConsumptionEffectTracker
import net.craftventure.core.metadata.EquippedItemsMeta
import net.craftventure.core.metadata.OwnedItemCache
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.ManualNpcTracker
import net.craftventure.core.serverevent.OwnedItemConsumeEvent
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.LookAtUtil
import net.craftventure.core.utils.OwnableItemUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.toLocation
import net.craftventure.database.type.EquippedItemSlot
import net.luckperms.api.LuckPermsProvider
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Switch
import org.bukkit.entity.*
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.io.IOException
import java.time.Duration
import java.util.*
import kotlin.math.roundToInt


class DragonClanListener : Listener {
    private val buttonAreaDragonDance = SimpleArea("world", -54.0, 41.0, -445.0, -44.0, 43.0, -435.0)
    private val warpLoading = "dragonclanloading"
    private val warpInit = "dragonclaninit"
    private val requiredAchievements = setOf<String>()
    private val keys = setOf(
        "dragonclankey_discovery_nautilus",
        "dragonclankey_mexico_stairs",
        "dragonclankey_mexico_cave",
        "dragonclankey_ccr_tomb",
        "dragonclankey_ccr_mine",
        "dragonclankey_singapore_maze",
        "dragonclankey_singapore_restaurant",
        "dragonclankey_viking_mountain_cave",
        "dragonclankey_viking_maze_cave",
        "dragonclankey_atlantis_rocktopus",
    )
    private val costumeItemId = "costume_dragonclan"
    private val titleItemId = "title_dragonclan_join"
    private val effectArea = SimpleArea("world", -541.0, 16.0, 1895.0, -538.0, 20.0, 1900.0)
    private val effectCenter = Location(Bukkit.getWorld("world"), -526.50, 15.00, 1897.50)
    private val rewardArea = SimpleArea("world", -526.0, 15.0, 1897.0, -527.0, 17.0, 1898.0)

    private val rewardAllowed = mutableSetOf<UUID>()
    private val jumpLocation = Location(Bukkit.getWorld("world"), -420.50, 87.00, 2002.50, -180f, 0f)

    private val tombBarrierZone = SimpleArea("world", 352.0, 23.0, -494.0, 397.0, 31.99, -437.0)
    private val playersInTomb = mutableSetOf<UUID>()

    private val singaporeMazeZone = SimpleArea("world", -39.0, 25.0, -501.0, 2.0, 32.0, -451.0)
    private val playersInSingaporeMaze = mutableSetOf<UUID>()

    private val vikingMazeZone = SimpleArea("world", -335.0, 22.0, -605.0, -244.0, 40.0, -556.0)
    private val playersInVikingMaze = mutableSetOf<UUID>()
    private val vikingSkeletonSpawns = listOf(
        Location(Bukkit.getWorld("world"), -320.54, 28.00, -597.37),
        Location(Bukkit.getWorld("world"), -315.07, 28.00, -593.91),
        Location(Bukkit.getWorld("world"), -319.24, 28.00, -588.07),
        Location(Bukkit.getWorld("world"), -321.35, 28.00, -593.50),
        Location(Bukkit.getWorld("world"), -321.26, 28.00, -598.61),
        Location(Bukkit.getWorld("world"), -329.36, 28.00, -593.46),
    )

    //    private val vikingNames = listOf(
//        "Ragnar",
//        "Freydis",
//        "Bjorn",
//        "Astrid",
//        "Gunnar",
//        "Ingrid",
//        "Erik",
//        "Sigrid",
//        "Ragnhild",
//        "Leif",
//        "Thyra",
//        "Harald",
//        "Sigrun",
//        "Olaf",
//        "Helga",
//        "Ivar",
//        "Gudrun",
//        "Thorstein",
//        "Eira",
//        "Halfdan",
//        "Solveig",
//        "Hrothgar",
//        "Siv",
//        "Audun",
//        "Brynhild",
//        "Kolbjorn",
//        "Yrsa",
//        "Ingvar",
//        "Eirik",
//        "Freja",
//        "Ragnvald",
//        "Greta",
//        "Gunnlod",
//        "Sven",
//        "Sigyn",
//        "Asmund",
//        "Gerd",
//        "Valdemar",
//        "Asa",
//        "Vebjorn",
//    )
    private val vikingSkeletons = arrayOfNulls<WitherSkeleton>(vikingSkeletonSpawns.size)
    private val vikingSkeletonKillTimes = vikingSkeletonSpawns.map { 0L }.toMutableList()

    private val vikingSpiderSpawns = listOf(
        Location(Bukkit.getWorld("world"), -326.48, 31.00, -578.36),
        Location(Bukkit.getWorld("world"), -324.69, 28.04, -562.08),
        Location(Bukkit.getWorld("world"), -314.37, 29.00, -570.19),
        Location(Bukkit.getWorld("world"), -307.52, 29.00, -565.46),
        Location(Bukkit.getWorld("world"), -306.57, 29.00, -575.24),
        Location(Bukkit.getWorld("world"), -313.35, 28.00, -581.15),
        Location(Bukkit.getWorld("world"), -298.40, 28.50, -589.31),
        Location(Bukkit.getWorld("world"), -305.00, 30.00, -596.15),
        Location(Bukkit.getWorld("world"), -284.38, 29.00, -598.32),
        Location(Bukkit.getWorld("world"), -284.36, 29.00, -564.09),
        Location(Bukkit.getWorld("world"), -269.82, 29.00, -565.30),
        Location(Bukkit.getWorld("world"), -265.95, 28.00, -576.15),
        Location(Bukkit.getWorld("world"), -256.92, 28.00, -574.05),
        Location(Bukkit.getWorld("world"), -266.51, 28.00, -584.31),
        Location(Bukkit.getWorld("world"), -267.46, 28.00, -595.80),
        Location(Bukkit.getWorld("world"), -274.62, 28.00, -583.46),
        Location(Bukkit.getWorld("world"), -282.46, 28.00, -577.54),
        Location(Bukkit.getWorld("world"), -295.20, 29.00, -567.45),
        Location(Bukkit.getWorld("world"), -302.36, 28.00, -582.29),
        Location(Bukkit.getWorld("world"), -329.78, 30.00, -570.30),
        Location(Bukkit.getWorld("world"), -321.33, 30.00, -574.94),
        Location(Bukkit.getWorld("world"), -318.69, 29.00, -569.19),
    )
    private val vikingSpiders = arrayOfNulls<CaveSpider>(vikingSpiderSpawns.size)
    private val vikingSpiderKillTimes = vikingSpiderSpawns.map { 0L }.toMutableList()

    private fun Block.isValidTombBarrier(): Boolean {
        val blocks = listOf(
            this.getRelative(BlockFace.NORTH),
            this.getRelative(BlockFace.EAST),
            this.getRelative(BlockFace.SOUTH),
            this.getRelative(BlockFace.WEST),
        )
        return blocks.all { !it.type.isSolid || it.type == Material.BARRIER }
    }

    private val tombBlocks = tombBarrierZone.let {
        (it.min.x.roundToInt()..it.max.x.roundToInt()).flatMap { x ->
            (it.min.y.roundToInt()..it.max.y.roundToInt()).flatMap { y ->
                (it.min.z.roundToInt()..it.max.z.roundToInt()).map { z ->
                    val block = tombBarrierZone.world.getBlockAt(x, y, z)
                    if (block.type == Material.BARRIER && block.isValidTombBarrier()) {
                        block
                    } else null
                }.filterNotNull()
            }
        }
    }

    init {
        val effectTracker = AreaTracker(effectArea)
        effectTracker.addListener(object : AreaTracker.StateListener {
            override fun onEnter(areaTracker: AreaTracker, player: Player) {
                val tracker = ManualNpcTracker(player)

//                val armor =
//                    NpcEntity(entityType = EntityType.ARMOR_STAND, location = effectCenter.clone().add(0.0, 2.0, 0.0))
//                armor.invisible(true)
//                armor.hideBasePlate(true)
//                armor.helmet(
//                    ItemStack(Material.LEATHER_HELMET).setLeatherArmorColor(Color.BLACK)
//                        .apply { addEnchantment(Enchantment.DURABILITY, 1) }
//                )
//                armor.chestplate(
//                    ItemStack(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.BLACK)
//                        .apply { addEnchantment(Enchantment.DURABILITY, 1) }
//                )
//                armor.leggings(
//                    ItemStack(Material.LEATHER_LEGGINGS).setLeatherArmorColor(Color.BLACK)
//                        .apply { addEnchantment(Enchantment.DURABILITY, 1) }
//                )
//                armor.boots(
//                    ItemStack(Material.LEATHER_BOOTS).setLeatherArmorColor(Color.BLACK)
//                        .apply { addEnchantment(Enchantment.DURABILITY, 1) }
//                )

                val dragon = NpcEntity(entityType = EntityType.ENDER_DRAGON, location = effectCenter)
                tracker.addEntity(dragon)
//                tracker.addEntity(armor)
                tracker.startTracking()
                tracker.addPlayer(player)

                executeSync {
                    dragon.invisible(true)
                    dragon.setMetadata(EntityMetadata.LivingEntity.health, 0f)
                    dragon.setMetadata(EntityMetadata.Entity.silent, true)
                    dragon.setMetadata(EntityMetadata.EnderDragon.dataPhase, EnderDragonPhase.DYING.id)
                }

                executeSync(20 * 10) {
                    tracker.stopTracking()
                }
            }
        })
        effectTracker.start()
        val rewardTracker = AreaTracker(rewardArea)
        rewardTracker.addListener(object : AreaTracker.StateListener {
            override fun onEnter(areaTracker: AreaTracker, player: Player) {
                reward(player)
            }
        })
        rewardTracker.start()

        AreaTracker(tombBarrierZone).apply {
            addListener(object : AreaTracker.StateListener {
                override fun onEnter(areaTracker: AreaTracker, player: Player) {
                    player.getMetadata<ConsumptionEffectTracker>()?.clearAll()
                    if (player.getMetadata<EquippedItemsMeta>()?.equippedItems?.get(EquippedItemSlot.HANDHELD)?.id == "dragonclankey_ccr_mine") {
                        player.sendMessage(CVTextColor.serverNotice + "The strange artifact from the mine is reacting to this environment...")
                    }

                    playersInTomb.add(player.uniqueId)
                    handleTombBlocks(player)

                    if (player.isGliding)
                        player.isGliding = false

                    if (player.isFlying) {
                        blockFlying(player)
                    }
                }

                override fun onStartGliding(areaTracker: AreaTracker, player: Player, cancellable: Cancellable) {
                    cancellable.isCancelled = true
                }

                override fun onFlightToggled(areaTracker: AreaTracker, player: Player, flying: Boolean) {
                    if (flying) blockFlying(player)
                }

                override fun onLeave(areaTracker: AreaTracker, player: Player) {
                    if (playersInTomb.remove(player.uniqueId)) {
                        if (player.getMetadata<EquippedItemsMeta>()?.equippedItems?.get(EquippedItemSlot.HANDHELD)?.id == "dragonclankey_ccr_mine")
                            player.sendMessage(CVTextColor.serverNotice + "The strange artifact is no longer reacting to this environment...")
                    }
                }
            })
            start()
        }

        AreaTracker(singaporeMazeZone).apply {
            addListener(object : AreaTracker.StateListener {
                override fun onEnter(areaTracker: AreaTracker, player: Player) {
                    player.getMetadata<ConsumptionEffectTracker>()?.clearAll()
                    player.clearActivePotionEffects()

                    player.sendMessage(CVTextColor.serverNotice + "This place is protected by ancient magic. Potion effects are of no use here. This maze will harm those who are not worthy.")

                    if (player.getMetadata<EquippedItemsMeta>()?.equippedItems?.get(EquippedItemSlot.HANDHELD)?.id == "dragonclankey_singapore_restaurant") {
                        player.sendMessage(CVTextColor.serverNotice + "The strange artifact from the restaurant is reacting to this environment...")

                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.WATER_BREATHING,
                                20 * 60 * 2,
                                1,
                                false,
                                false,
                                false
                            )
                        )
                    }
                    playersInSingaporeMaze.add(player.uniqueId)
                    EquipmentManager.reapply(player)

                    if (player.isGliding)
                        player.isGliding = false

                    if (player.isFlying) {
                        blockFlying(player)
                    }
                }

                override fun onStartGliding(areaTracker: AreaTracker, player: Player, cancellable: Cancellable) {
                    cancellable.isCancelled = true
                }

                override fun onFlightToggled(areaTracker: AreaTracker, player: Player, flying: Boolean) {
                    if (flying) blockFlying(player)
                }

                override fun onLeave(areaTracker: AreaTracker, player: Player) {
                    player.removePotionEffect(PotionEffectType.WATER_BREATHING)
                    player.removePotionEffect(PotionEffectType.HARM)
                    if (playersInSingaporeMaze.remove(player.uniqueId)) {
                        if (player.getMetadata<EquippedItemsMeta>()?.equippedItems?.get(EquippedItemSlot.HANDHELD)?.id == "dragonclankey_singapore_restaurant")
                            player.sendMessage(CVTextColor.serverNotice + "The strange artifact is no longer reacting to this environment...")
                        EquipmentManager.reapply(player)
                    }
                }
            })
            start()
        }

        AreaTracker(vikingMazeZone).apply {
            addListener(object : AreaTracker.StateListener {
                override fun onEnter(areaTracker: AreaTracker, player: Player) {
                    player.getMetadata<ConsumptionEffectTracker>()?.clearAll()
                    player.clearActivePotionEffects()

                    player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.SLOW,
                            Int.MAX_VALUE,
                            1,
                            false,
                            false,
                            false
                        )
                    )
                    player.sendMessage(CVTextColor.serverNotice + "You've entered the abandoned Viking mine, which is protected by ancient magic. Your armor is of no value here, neither are potion effects. Fight your way through...")

                    playersInVikingMaze.add(player.uniqueId)
                    EquipmentManager.reapply(player)

                    if (player.isGliding)
                        player.isGliding = false

                    if (player.isFlying) {
                        blockFlying(player)
                    }
                }

                override fun onStartGliding(areaTracker: AreaTracker, player: Player, cancellable: Cancellable) {
                    cancellable.isCancelled = true
                }

                override fun onFlightToggled(areaTracker: AreaTracker, player: Player, flying: Boolean) {
                    if (flying) blockFlying(player)
                }

                override fun onLeave(areaTracker: AreaTracker, player: Player) {
                    player.removePotionEffect(PotionEffectType.SLOW)
                    if (playersInVikingMaze.remove(player.uniqueId)) {
                        EquipmentManager.reapply(player)
                    }
                }
            })
            start()
        }

        executeSync(20, 20) {
            updateVikingMazeMobs()
        }

        executeSync(20 * 5, 20 * 5) {
            playersInTomb.forEach {
                val player = Bukkit.getPlayer(it) ?: return@forEach
                handleTombBlocks(player)
            }
        }

        executeSync(40, 40) {
            playersInSingaporeMaze.forEach {
                val player = Bukkit.getPlayer(it) ?: return@forEach
                if (player.getMetadata<EquippedItemsMeta>()?.equippedItems?.get(EquippedItemSlot.HANDHELD)?.id == "dragonclankey_singapore_restaurant") return@forEach

                player.damage(2.0)
            }
            updateVikingMazeMobs()
        }
    }

    private fun handleTombBlocks(player: Player) {
        val isHolding =
            player.getMetadata<EquippedItemsMeta>()?.equippedItems?.get(EquippedItemSlot.HANDHELD)?.id == "dragonclankey_ccr_mine"
        val blockData = if (isHolding) Material.BONE_BLOCK.createBlockData() else Material.BARRIER.createBlockData()
        tombBlocks.forEach {
            player.sendBlockChange(it.location, blockData)
        }
    }

    private fun updateVikingMazeMobs() {
        if (playersInVikingMaze.isNotEmpty()) {
            val now = System.currentTimeMillis()
            vikingSkeletons.forEachIndexed { index, skeleton ->
                if (skeleton != null && !skeleton.isValid && vikingSkeletonKillTimes[index] == 0L) {
                    vikingSkeletonKillTimes[index] = now
                }

                if ((skeleton == null || !skeleton.isValid) && vikingSkeletonKillTimes[index] < now - 10000) {
                    skeleton?.remove()
                    vikingSkeletons[index] = spawnSkeleton(vikingSkeletonSpawns[index])
                    vikingSkeletonKillTimes[index] = 0L
                }
            }

            vikingSpiders.forEachIndexed { index, spider ->
                if (spider != null && !spider.isValid && vikingSpiderKillTimes[index] == 0L) {
                    vikingSpiderKillTimes[index] = now
                }

                if ((spider == null || !spider.isValid) && vikingSpiderKillTimes[index] < now - 20000) {
                    spider?.remove()
                    vikingSpiders[index] = spawnSpider(vikingSpiderSpawns[index])
                    vikingSpiderKillTimes[index] = 0L
                }
            }
        }
    }

    private fun Monster.setupVikingGoals(home: Location, trackingRange: Double) {
        Bukkit.getMobGoals().addGoal(this, 0, MobHomeGoal(this, home, trackingRange))
    }

    private fun spawnSpider(location: Location): CaveSpider {
        val mob = location.spawn<CaveSpider>()
        mob.setupVikingGoals(location, 15.0)
        return mob
    }

    private fun spawnSkeleton(location: Location): WitherSkeleton {
        val mob = location.spawn<WitherSkeleton>()
        mob.equipment.setItemInMainHand(ItemStack(Material.BOW))
        mob.equipment.setItemInOffHand(ItemStack(Material.NETHERITE_SWORD))
        mob.setupVikingGoals(location, 15.0)
        return mob
    }

    private fun blockFlying(player: Player) {
        if (!PermissionChecker.isCrew(player)) {
            val wasFlying = player.isFlying || player.allowFlight
            player.isFlying = false
            player.allowFlight = false
            if (wasFlying) {
                MessageBarManager.display(
                    player,
                    MessageBarManager.Message(
                        id = ChatUtils.ID_GENERAL_NOTICE,
                        text = Translation.NOFLYZONE_BLOCKED.getTranslation(player)!!,
                        type = MessageBarManager.Type.NOTICE,
                        untilMillis = TimeUtils.secondsFromNow(4.0),
                    ),
                    replace = true,
                )
                if (!player.isInsideVehicle)
                    player.teleport(player.location, PlayerTeleportEvent.TeleportCause.PLUGIN)
            }
        }
    }

    @EventHandler
    fun onElytraToggle(event: EntityToggleGlideEvent) {
        if (!event.isGliding) return

        if (event.entity.uniqueId in playersInTomb || event.entity.uniqueId in playersInVikingMaze || event.entity.uniqueId in playersInSingaporeMaze) {
            event.isCancelled = true
            return
        }

        executeAsync {
            MainRepositoryProvider.achievementProgressRepository.reward(event.entity.uniqueId, "dragonclan_elytra_fly")
        }
    }

    @EventHandler
    fun onOwnedItemConsume(event: OwnedItemConsumeEvent) {
        if (event.player.uniqueId in playersInSingaporeMaze) {
            event.isCancelled = true
        } else if (event.player.uniqueId in playersInVikingMaze) {
            event.isCancelled = true
        }

    }

    @EventHandler
    fun onPlayerWornItemsChanged(event: PlayerEquippedItemsUpdateEvent) {
        if (event.player.uniqueId in playersInTomb) {
            handleTombBlocks(event.player)
//            if (event.appliedEquippedItems.weaponItem?.id == "dragonclankey_ccr_mine") {
//                val replacementItem =
//                    MainRepositoryProvider.itemStackDataRepository.findCached("dragonclankey_ccr_mine_barrier")
//                        ?.getItemStack(ItemType.WEAPON)
//                event.appliedEquippedItems.weaponItem =
//                    (replacementItem ?: ItemStack(Material.BARRIER))
//                        .markAsWornItem()
//                        .hidePlacedOn()
//                        .toEquippedItemData("dragonclan_ccr_mine_barrier_viewer")
//                event.player.inventory.heldItemSlot = EquipmentManager.SLOT_WEAPON
//            }
        }

        if (event.player.uniqueId in playersInVikingMaze) {
            event.appliedEquippedItems.apply {
                clearArmor()
                costumeItem = null
            }
        }
    }

    @EventHandler
    fun onProtectedInteraction(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.type.name.endsWith("BUTTON") && block.type.data == Switch::class.java) {
            if (block.location in buttonAreaDragonDance) {
                event.isCancelled = true

                val loadingWarp = MainRepositoryProvider.warpRepository.findCachedByName(warpLoading) ?: return
                event.player.teleport(loadingWarp.toLocation())
                load(event.player)
            }
        }
    }

    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player

        if (player.hasPermission("craftventure.rank.dragonclan")) return
        if (Math.random() > 0.5) return

        executeAsync {
            val owns =
                MainRepositoryProvider.playerOwnedItemRepository.owns(player.uniqueId, "title_dragonclanancestor")
            if (!owns) return@executeAsync

            executeSync(20 * 3) {
                LookAtUtil.makePlayerLookAt(player, Vector(-48.5, 51.0, -439.5))

                player.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 20 * 5, 1, true, false))
                player.addPotionEffect(PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 20 * 5, 1, true, false))

                player.playSound(player.location, Sound.ENTITY_SKELETON_HORSE_DEATH, 1f, 0.1f)
//                player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_DEATH, 1f, 0.1f)
                player.playSound(player.location, Sound.ENTITY_WITHER_DEATH, 1f, 0.1f)
                player.playSound(player.location, Sound.ENTITY_ALLAY_DEATH, 1f, 0.1f)
                player.playSound(player.location, Sound.ENTITY_ELDER_GUARDIAN_DEATH_LAND, 1f, 0.1f)
                player.playSound(player.location, Sound.ENTITY_GUARDIAN_DEATH_LAND, 1f, 0.1f)
            }
        }

    }

    private fun load(player: Player) {
        val ownedItems = player.getMetadata<OwnedItemCache>() ?: return

        val start = System.currentTimeMillis()
        executeAsync {
            val achievements = MainRepositoryProvider.achievementRepository.cachedItems.associateBy { it.id }
            val progresses = MainRepositoryProvider.achievementProgressRepository.findByPlayer(player.uniqueId)
            val unlockedIds = progresses.map { it.achievementId }

            val hasAllRequiredAchievements = requiredAchievements.all { it in unlockedIds }
            val count = progresses.sumOf {
                val achievement = achievements[it.achievementId!!] ?: return@sumOf 0L
                if (achievement.enabled == true) {
                    return@sumOf 1
                }

                0
            }
            val hasAllKeys = ownedItems.ownedItemIds.containsAll(keys)

            logcat { "count=$count keys=$hasAllKeys" }

            val timeDiff = System.currentTimeMillis() - start
            executeSync(if (timeDiff > 6000) 0 else ((6000 - timeDiff) / 50)) {
                if (hasAllKeys && count >= 150 && hasAllRequiredAchievements) {
                    continueWith(player, count)
                } else {
                    end(player)
                }
            }
        }
    }

    private fun end(player: Player) {
        player.damage(player.health * 1.5, player)
        player.health = 0.0
        player.sendMessage(CVTextColor.serverError + "You were not deemed worthy, retrieve the strange artifacts and/or required achievements first!")
    }

    private fun continueWith(player: Player, count: Long) {
        val initWarp = MainRepositoryProvider.warpRepository.findCachedByName(warpInit) ?: return
        player.teleport(initWarp.toLocation())

        player.sendMessage(CVTextColor.serverNoticeAccent + "You were granted access to the library, please walk forward and onto the altar of the Six...")

        rewardAllowed.add(player.uniqueId)
    }

    private fun reward(player: Player) {
        if (player.uniqueId !in rewardAllowed) return
        rewardAllowed.remove(player.uniqueId)

        if (!player.hasPermission(Permissions.DRAGONCLAN)) {
            reportAscension(player)
        }

        player.isFlying = false
        player.allowFlight = false
        player.isGliding = false
        player.teleport(jumpLocation)
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 30, 1))
        player.playSound(player.location, SoundUtils.QUEST_COMPLETED, 10f, 1f)
        reportDragonClan(player)

        executeAsync {
            MainRepositoryProvider.achievementProgressRepository.increaseCounter(player.uniqueId, "dragonclan_join")
        }

        val costumeItem = MainRepositoryProvider.ownableItemRepository.findCached(costumeItemId)
        val titleItem = MainRepositoryProvider.ownableItemRepository.findCached(titleItemId)
        executeAsync {
            try {
                if (costumeItem != null)
                    MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(player.uniqueId, costumeItemId, 0)
            } catch (_: Exception) {
            }
            try {
                if (titleItem != null)
                    MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(player.uniqueId, titleItemId, 0)
            } catch (_: Exception) {
            }

            keys.forEach {
                MainRepositoryProvider.playerOwnedItemRepository.delete(player.uniqueId, it, 1)
            }
            player.sendMessage("<server_notice>The mysterious artifacts you found earlier have disappeared from your inventory".parseWithCvMessage())
        }

        executeSync(50) {
            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f)
            player.velocity = Vector(0.0, 1.3, -10.0)
        }
        executeSync(70) {
            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f)
            player.velocity = Vector(0.0, 1.3, -10.0)
        }
        executeSync(90) {
            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f)
            player.velocity = Vector(0.0, 1.3, -10.0)
        }
        executeSync(110) {
            if (costumeItem != null && costumeItem.enabled!!) {
                OwnableItemUtils.equip(player, null, costumeItem, EquippedItemSlot.COSTUME)
            }
            rewardDragonclanRank(player)
        }

        executeSync(150) {
            player.displayTitle(
                TitleManager.TitleData.of(
                    id = "dragonclan",
                    type = TitleManager.Type.Default,
                    title = CVTextColor.serverNotice + "Unfortunately",
                    subtitle = CVTextColor.serverNotice + "This is where DragonClan ends...",
                    fadeIn = Duration.ofMillis(200),
                    stay = Duration.ofMillis(5000),
                    fadeOut = Duration.ofMillis(200),
                ),
                replace = true,
            )
        }

        executeSync(150 + (20 * 6)) {
            player.displayTitle(
                TitleManager.TitleData.of(
                    id = "dragonclan",
                    type = TitleManager.Type.Default,
                    title = CVTextColor.serverNotice + "Thank you ${player.name}",
                    subtitle = CVTextColor.serverNotice + "For having been with us for such long time!",
                    fadeIn = Duration.ofMillis(200),
                    stay = Duration.ofMillis(5000),
                    fadeOut = Duration.ofMillis(200),
                ),
                replace = true,
            )
        }
    }

    private fun reportAscension(player: Player) {
        executeAsync {
            val jsonObject = JsonObject()
            jsonObject.add("embeds", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("title", "${player.name} has joined the clan")
                    addProperty(
                        "description",
                        "If they linked their Discord account they may join this channel in a few minutes"
                    )
                    addProperty("color", 16750080)
                })
            })

            CvApi.okhttpClient.newCall(
                Request.Builder()
                    .url(dragonClanChannelHook)
                    .post(jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
            ).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    logcat { "Responded" }
                    response.close()
                }
            })
        }
    }

    private fun reportDragonClan(player: Player) {
        executeAsync {
            CvApi.okhttpClient.newCall(
                Request.Builder()
                    .url(CraftventureCore.getSettings().errorWebhook)
                    .post(
                        MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                "content",
                                "`${player.name} has finished a DragonClan ascending ceremony`"
                            )
                            .build()
                    )
                    .build()
            ).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    logcat { "Responded" }
                    response.close()
                }
            })
        }
    }

    private fun rewardDragonclanRank(player: Player) {
        val api = LuckPermsProvider.get()
        val user = api.userManager.getUser(player.uniqueId)
            ?: api.userManager.loadUser(player.uniqueId).join() ?: return

        val dragonclanNode = api.nodeBuilderRegistry.forInheritance().group("dragonclan")
            .build()
        user.data().add(dragonclanNode)
        api.userManager.saveUser(user).thenRunAsync {
//            logcat { "Saved user" }
        }
    }

    companion object {
        val rewardUrl =
            "https://discord.com/api/webhooks/"
        val dragonClanChannelHook =
            "https://discord.com/api/webhooks/"
    }
}
