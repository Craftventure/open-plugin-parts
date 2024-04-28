package net.craftventure.core.event

import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.area.CombinedArea
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.BaseEntityMetadata
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.playSound
import net.craftventure.bukkit.ktx.extension.removeAllEnchantments
import net.craftventure.bukkit.ktx.extension.unbreakable
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.customNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.event.HalloweenMaze2020.EntitySpawner
import net.craftventure.core.extension.clearId
import net.craftventure.core.extension.spawn
import net.craftventure.core.feature.jumppuzzle.JumpPuzzle
import net.craftventure.core.feature.jumppuzzle.JumpPuzzlePlayer
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.type.EquippedItemSlot
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.ceil
import kotlin.math.min


class HalloweenMaze2020 : Listener {
    private val mainArea = SimpleArea("world", 28.0, 41.0, -687.0, 153.0, 35.0, -564.0)
    private val joinLeftArea = SimpleArea("world", 107.0, 34.0, -682.0, 113.0, 38.0, -676.0)
    private val joinRightArea = SimpleArea("world", 63.0, 34.0, -682.0, 71.0, 38.0, -676.0)
    private val towerArea = SimpleArea("world", 72.0, 35.0, -642.0, 106.0, 99.0, -610.0)
    private val location = Location(Bukkit.getWorld("world"), 57.5, 35.0, -601.5)
    private val playArea = CombinedArea(
        mainArea,
        towerArea
    )
    private val entities = arrayOfNulls<Entity>(70)
    private val exitLocation = Location(location.world, 89.5, 42.0, -690.5, 0f, -16f)
    private val soundLocation = Location(location.world, 89.63, 91.44, -625.45)

    private val centerAreaNoBlindness = SimpleArea("world", 84.0, 36.0, -630.0, 95.0, 42.0, -622.0)
    val puzzle = JumpPuzzle(
        gameId = "halloween2020",
        displayName = "Clocktower Maze",
        description = "Finish it or die trying",
        playArea = playArea,
        startingArea = CombinedArea(joinLeftArea, joinRightArea),
        rewardArea = SimpleArea("world", 86.0, 89.0, -625.0, 88.0, 91.0, -623.0),
        listener = object : JumpPuzzle.PuzzleListener {
            override fun onPlayerUpdate(puzzlePlayer: JumpPuzzlePlayer) {
                super.onPlayerUpdate(puzzlePlayer)
                val player = puzzlePlayer.player
                val shouldHaveBlindness = player.location.y >= 39.0 || player in centerAreaNoBlindness
                if (shouldHaveBlindness) {
                    if (player.hasPotionEffect(PotionEffectType.BLINDNESS))
                        player.removePotionEffect(PotionEffectType.BLINDNESS)
                } else {
                    if (!player.hasPotionEffect(PotionEffectType.BLINDNESS))
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.BLINDNESS,
                                Integer.MAX_VALUE,
                                1,
                                true,
                                false
                            )
                        )
                }
            }

            override fun onStart(puzzlePlayer: JumpPuzzlePlayer) {
                super.onStart(puzzlePlayer)
                val player = puzzlePlayer.player
                player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, true, false))
                EquipmentManager.reapply(player)
//                player.isSprinting = false
//                Logger.debug("start")
            }

            override fun onStop(puzzlePlayer: JumpPuzzlePlayer) {
                super.onStop(puzzlePlayer)
                val player = puzzlePlayer.player
                EquipmentManager.reapply(player)
                clean(player)
//                Logger.debug("stop")
            }
        },
        rewardCallback = object : JumpPuzzle.RewardCallback {
            override fun onReward(puzzle: JumpPuzzle, player: JumpPuzzlePlayer, score: MinigameScore?) {
                soundLocation.playSound("minecraft:kart.horn_bone", SoundCategory.AMBIENT, 10f, 0.01f)
                executeAsync {
                    val id = player.player.uniqueId
                    MainRepositoryProvider.achievementProgressRepository.reward(id, "halloween_2020")
                    MainRepositoryProvider.playerOwnedItemRepository.apply {
                        when {
                            owns(id, "halloween2020tophat_gold") ->
                                createOneLimited(id, "halloween2020tophat_dia", -1)

                            owns(id, "halloween2020tophat_red") ->
                                createOneLimited(id, "halloween2020tophat_gold", -1)

                            else ->
                                createOneLimited(id, "halloween2020tophat_red", -1)
                        }
                        createOneLimited(id, "title_halloween2020", -1)
                    }
                }
            }
        }
    )

    private val spawners = listOf<EntitySpawner>(
        EntitySpawner {
            it.spawn<Piglin>().apply spawn@{
                canPickupItems = false
                isImmuneToZombification = true
                if (CraftventureCore.getRandom().nextDouble() > 0.5) setAdult() else setBaby()
                getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.apply { baseValue = 3.5 }
                getAttribute(Attribute.GENERIC_MAX_HEALTH)?.apply { baseValue *= 0.8 }
                isCollidable = false
            }
        },
        EntitySpawner {
            it.spawn<PiglinBrute>().apply spawn@{
                canPickupItems = false
                isImmuneToZombification = true
                if (CraftventureCore.getRandom().nextDouble() > 0.5) setAdult() else setBaby()
                getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.apply { baseValue = 5.5 }
                getAttribute(Attribute.GENERIC_MAX_HEALTH)?.apply { baseValue *= 0.8 }
                isCollidable = false
            }
        },
        EntitySpawner {
            it.spawn<Hoglin>().apply spawn@{
                canPickupItems = false
                isImmuneToZombification = true
                setBaby()
                getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.apply { baseValue = 0.5 }
                getAttribute(Attribute.GENERIC_MAX_HEALTH)?.apply { baseValue = 15.0 }
                getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.apply { baseValue *= 2.5 }
                isCollidable = false
            }
        },
        EntitySpawner {
            it.spawn<Zombie>().apply {
                canPickupItems = false
                equipment.clear()
                equipment.helmet = ItemStack(Material.PUMPKIN)
                setBaby()
                getAttribute(Attribute.GENERIC_MAX_HEALTH)?.apply { baseValue *= 1.5 }
                isCollidable = false
            }
        },
    )

    init {
        puzzle.start()
        playArea.loadChunks(true)
        location.world?.entities?.forEach {
            if ((it is Monster) && playArea.isInArea(it)) {
                it.remove()
            }
        }
        var tick = 0
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {

            if (tick % 5 == 0)
                entities.forEach {
                    if (it != null && it in centerAreaNoBlindness) {
                        it.remove()
                    }
                }
            tick++
            if (tick > 20 * 10)
                tick = 0

            if (tick == 0) {
                val mobAmount = mobAmount()
//            Logger.console("Mobs $mobAmount ${entities.count { it != null }}")
                for (i in entities.indices) {
                    if (entities[i] == null || !entities[i]!!.isValid || !playArea.isInArea(entities[i]!!) || i >= mobAmount * 1.05) {
                        entities[i]?.remove()
                        entities[i] = null

                        if (i >= mobAmount)
                            continue

                        val location = generateEntityLocation() ?: return@scheduleSyncRepeatingTask

                        val entity = spawners.random().spawn(location)
                        (entity as? LivingEntity)?.updateName()
                        entity.getOrCreateMetadata { EntityTag() }
//                        entity.isGlowing = true
                        entity.vehicle?.remove()

                        entity.debugAttributes()

                        entities[i] = entity
                    }
                }
            }
        }, 1L, 1L)

        Bukkit.getServer().pluginManager.registerEvents(this, CraftventureCore.getInstance())
    }

    private fun Entity.debugAttributes() {
        if (!PluginProvider.isTestServer()) return
        if (true) return
        if (this is LivingEntity) {
            this.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.apply {
                Logger.debug(
                    "${this@debugAttributes.type} ${(this@debugAttributes as? Ageable)?.isAdult} Attack knockback " +
                            "${this.value.format(2)} " +
                            "${this.defaultValue.format(2)} " +
                            "${this.baseValue.format(2)} "
                )
            }
//                getAttribute(Attribute.GENERIC_ATTACK_SPEED)?.apply { baseValue = 0.2 }
            this.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.apply {
//                    baseValue = 0.2
                Logger.debug(
                    "${this@debugAttributes.type} ${(this@debugAttributes as? Ageable)?.isAdult} Attack damage " +
                            "${this.value.format(2)} " +
                            "${this.defaultValue.format(2)} " +
                            "${this.baseValue.format(2)} "
                )
            }
        }
    }

    private fun LivingEntity.updateName(health: Double = this.health) {
        val percentage = health / getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
        val parts = 10
        val fifth = ceil(percentage * parts).toInt().clamp(0, 10)
        customNameWithBuilder {
            text("|".repeat(fifth), NamedTextColor.DARK_GREEN)
            text("|".repeat(parts - fifth), NamedTextColor.DARK_RED)
        }

        debugAttributes()
    }

    private fun mobAmount(): Int =
        30 + min(30, 2 * puzzle.players.filter { it.player.hasPotionEffect(PotionEffectType.BLINDNESS) }.size)
//    private fun mobAmountEnd(): Int = 10 + min(20, 2 * puzzle.players.size)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
//        if (event.entity is Player && event.entity in puzzle.players && event.damager is Player) {
//            event.isCancelled = true
//        }
        if (event.entity.getMetadata<EntityTag>() != null) {
            (event.entity as? LivingEntity)?.apply {
                eyeLocation.spawnParticleX(
                    Particle.REDSTONE,
                    5,
                    0.2, 0.3, 0.2,
                    data = Particle.DustOptions(Color.RED, 2.0f),
                )
                eyeLocation.spawnParticleX(
                    Particle.DRIP_LAVA,
                    5,
                    0.2, 0.3, 0.2,
                )
            }
        }
    }

    @EventHandler
    fun onTarget(event: EntityTargetEvent) {
        if (event.entity.getMetadata<EntityTag>() != null) {
            if (((event.reason == EntityTargetEvent.TargetReason.TARGET_ATTACKED_NEARBY_ENTITY || event.reason == EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY)
                        && CraftventureCore.getRandom().nextDouble() < 0.8) ||
                ((event.reason == EntityTargetEvent.TargetReason.CLOSEST_PLAYER || event.reason == EntityTargetEvent.TargetReason.CLOSEST_ENTITY) &&
                        CraftventureCore.getRandom().nextDouble() < 0.5)
            ) {
                event.target = null
//                Logger.console("Cancelled found ${event?.entity?.name} > ${event?.target?.name} for ${event.reason}")
            }// else
//                Logger.console("Target found ${event?.entity?.name} > ${event?.target?.name} for ${event.reason}")
        }
    }

    @EventHandler
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        val player = event.entity as? Player ?: return
        if (player in puzzle) {
//            Logger.console("Faster regen for ${event.entity.name}")
            event.amount = 1.5
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (event.entity in puzzle) {
//            event.entity.location.playSound("minecraft:kart.horn_burger", SoundCategory.AMBIENT, 5f, 0.01f)
            val msg = NamedTextColor.RED + "${event.entity.name} was slain in the maze"
            for (player in puzzle.players) {
                player.player.sendMessage(msg)
            }
        }
    }

//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    fun onChunkUnload(event: ChunkUnloadEvent) {
//        if (castleSquareArea.overlaps(event.chunk)) {
//            event.isCancelled = true
//        }
//    }

    @EventHandler
    fun onEntityCombust(event: EntityCombustEvent) {
        for (entity in entities) {
            if (entity?.entityId == event.entity.entityId) {
                event.entity.fireTicks = 0
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamageMonitor(event: EntityDamageEvent) {
        (event.entity as? LivingEntity)?.apply {
            updateName(health - event.finalDamage)
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity.getMetadata<EntityTag>() != null) {
//            Logger.debug("cause=${event.cause}")
            if (event.cause == EntityDamageEvent.DamageCause.FIRE || event.cause == EntityDamageEvent.DamageCause.WITHER) {
                event.entity.fireTicks = 0
                event.isCancelled = true
            }
        }
    }

    private fun generateEntityLocation(): Location? {
        val xRange = 29..149
        val zRange = -677..-569
        for (i in 0..15) {
            val x = xRange.random() + 0.5
            val z = zRange.random() + 0.5
            val location = Location(this.location.world, x, 36.0, z)
            val locationGround = Location(this.location.world, x, 35.0, z)
            if (location.block.type == Material.AIR && locationGround.block.type.isSolid) {
                return location
            }
        }
        return null
    }

//    private fun generateEntityLocationEnd(): Location {
//        for (i in 0..15) {
//            val x =
//                CraftventureCore.getRandom().nextInt((endArea.loc2.x - endArea.loc1.x).toInt()) + endArea.loc1.x + 0.5
//            val z =
//                CraftventureCore.getRandom().nextInt((endArea.loc2.z - endArea.loc1.z).toInt()) + endArea.loc1.z + 0.5
//            val location = Location(this.location.world, x, 35.0, z)
//            val locationGround = Location(this.location.world, x, 34.0, z)
//            if (location.block.type == Material.AIR && locationGround.block.type == Material.GRASS_PATH) {
//                return location
//            }
//        }
//        return location
//    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clean(event.player)
    }

//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//    fun onPlayerTeleport(event: PlayerTeleportEvent) {
//        val mazePlayer = players.firstOrNull { it.player === event.player }
//        if (mazePlayer != null) {
//            if (mazePlayer.qualified)
//                TitleUtil.send(event.player, 10, 20 * 4, 10,
//                        ChatColor.DARK_RED, "Disqualified",
//                        ChatColor.RED, "You (were) teleported while being inside the maze")
//            mazePlayer.qualified = false
//            if (mazeArea.isInArea(event.to)) {
//                Logger.console(CVChatColor.COMMAND_ERROR + "Player ${event.player.name} teleported within the maze")
//            }
//        }
//        check(event.player)
//    }


    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (event.player in puzzle || event.player in playArea) {
            event.respawnLocation = exitLocation
            event.player.sendTitleWithTicks(
                10, 20 * 4, 10,
                NamedTextColor.DARK_RED, "You failed",
                NamedTextColor.RED, "Better luck next time ;)"
            )
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onWornItems(event: PlayerEquippedItemsUpdateEvent) {
//        Logger.debug("Worn item update ${event.player in puzzle.players}")
//        Logger.debug("Updating items")
        if (event.player !in puzzle) return
//        Logger.debug("Updating items as if in maze")

        val item =
            try {
//                Logger.debug("item=${event.wornData.equippedItems.firstOrNull { it.slot == EquippedItemSlot.HANDHELD }}")
//                Logger.debug("itemId=${event.wornData.equippedItems.firstOrNull { it.slot == EquippedItemSlot.HANDHELD }?.item}")
//                Logger.debug(
//                    "actualItem=${
//                        event.wornData.equippedItems.firstOrNull { it.slot == EquippedItemSlot.HANDHELD }?.item?.let { handheldId ->
//                            CraftventureCore.getItemStackDataDatabase().cachedItems.firstOrNull { it.id == handheldId }
//                        }
//                    }"
//                )
//                Logger.debug(
//                    "actualItemType=${
//                        event.wornData.equippedItems.firstOrNull { it.slot == EquippedItemSlot.HANDHELD }?.item?.let { handheldId ->
//                            CraftventureCore.getItemStackDataDatabase().cachedItems.firstOrNull { it.id == handheldId }
//                        }?.itemStack?.type
//                    }"
//                )
                event.meta.equippedItems[EquippedItemSlot.HANDHELD]?.id?.let { handheldId ->
                    MainRepositoryProvider.itemStackDataRepository.cachedItems.firstOrNull { it.id == handheldId }
                }?.itemStack?.takeIf { it.type == Material.IRON_SWORD }?.also {
                    val enchanted = it.enchantments.isNotEmpty()
                    it.removeAllEnchantments()
                    if (enchanted) {
                        it.addUnsafeEnchantment(Enchantment.LUCK, 1)
                    }
                    it.clearId()
                }
            } catch (e: Exception) {
//                e.printStackTrace()
                null
            }

        event.appliedEquippedItems.clearArmor()
        event.appliedEquippedItems.clearItems()

        val sword = (item ?: dataItem(Material.IRON_SWORD, 2)).apply {
            unbreakable()
            displayName(CVTextColor.serverNotice + (if (item != null) "Nightbladeified Sword" else "Nightblade"))
            loreWithBuilder { text("You need this to protect yourself...") }
            addEnchantment(Enchantment.KNOCKBACK, 1)
            addEnchantment(Enchantment.DAMAGE_ARTHROPODS, 1)
            addEnchantment(Enchantment.DAMAGE_ALL, 1)
        }
        event.appliedEquippedItems.weaponItem = sword.toEquippedItemData("halloween2020")
    }

    private fun clean(player: Player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS)
        player.removePotionEffect(PotionEffectType.CONFUSION)
    }

    fun interface EntitySpawner {
        fun spawn(location: Location): Entity
    }

    class EntityTag : BaseEntityMetadata() {
        val spawnedAt: Long = System.currentTimeMillis()
        override fun debugComponent() = Component.text("spawnedAt=$spawnedAt")
    }
}
