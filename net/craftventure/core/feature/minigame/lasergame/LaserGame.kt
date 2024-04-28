package net.craftventure.core.feature.minigame.lasergame

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import net.craftventure.bukkit.ktx.extension.coloredItem
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.sendTo
import net.craftventure.bukkit.ktx.extension.setLeatherArmorColor
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.database.ItemStackLoader
import net.craftventure.core.extension.markAsWornItem
import net.craftventure.core.feature.minigame.BaseMinigame
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigamePlayer
import net.craftventure.core.feature.minigame.lasergame.gun.LaserGameGunType
import net.craftventure.core.feature.minigame.lasergame.projectile.LaserGameProjectile
import net.craftventure.core.feature.minigame.lasergame.turret.LaserGameTurret
import net.craftventure.core.feature.minigame.lasergame.turret.TurretItem
import net.craftventure.core.ktx.extension.asOrdinalAppended
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.serverevent.PlayerStuckEvent
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.EquippedItemSlot
import net.craftventure.database.type.TransactionType
import net.craftventure.temporary.getOwnableItemMetadata
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.time.Duration
import kotlin.random.asKotlinRandom


class LaserGame(
    id: String,
    minRequiredPlayers: Int = if (PluginProvider.isTestServer()) 1 else 3,
    minKeepPlayingRequiredPlayers: Int = 1,
    name: String,
    exitLocation: Location,
    val teamMode: LaserGameTeamMode,
    val arenaMode: LaserGameArenaMode,
    override val maxPlayers: Int,
    override val levelBaseTimeLimit: Long,
    description: String,
    representationItem: ItemStack,
    warpName: String,
) : BaseMinigame<LaserGamePlayer>(
    internalName = id,
    displayName = name,
    minRequiredPlayers = minRequiredPlayers,
    exitLocation = exitLocation,
    minKeepPlayingRequiredPlayers = minKeepPlayingRequiredPlayers,
//    startCountdownTicks = 5 * 20,
    preparingTicks = 0,
    description = description,
    representationItem = representationItem,
    warpName = warpName,
) {
    private var arena: LaserGameArena = LaserGameArenaPool.claim(arenaMode, teamMode)!!
    private val projectiles: MutableSet<LaserGameProjectile> = hashSetOf()
    private val turrets: MutableSet<LaserGameTurret> = hashSetOf()
    val targets: Collection<LaserGameEntity>
        get() = players.map { it.metadata } + turrets
    private val ffaColors = sequence {
        val colors = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.SILVER,
            Color.BLACK,
            Color.MAROON,
            Color.YELLOW,
            Color.OLIVE,
            Color.LIME,
            Color.AQUA,
            Color.TEAL,
            Color.NAVY,
            Color.FUCHSIA,
            Color.ORANGE
        )
        var index = 0
        while (true) {
            yield(colors[index])
            index++
            if (index >= colors.size)
                index = 0
        }
    }.iterator()

    override fun onStateChanged(oldState: Minigame.State, newState: Minigame.State) {
        super.onStateChanged(oldState, newState)
        if (newState == Minigame.State.IDLE) {
            LaserGameArenaPool.release(arena)
            arena = LaserGameArenaPool.claim(arenaMode, teamMode)!!
        }
    }

    override fun provideDuration(): Minigame.MinigameDuration = Minigame.MinigameDuration(
        Duration.ofSeconds(levelBaseTimeLimit),
        Minigame.DurationType.EXACT
    )

    fun hasTurret(player: LaserGamePlayer) = turrets.any { it.owner == player }

    fun removeTurret(player: LaserGamePlayer?, silent: Boolean = false) {
        val turret = turrets.firstOrNull { it.owner === player }
        if (turret != null) {
            removeTurret(turret, player, silent)
        }
    }

    fun removeTurret(turret: LaserGameTurret, player: LaserGamePlayer?, silent: Boolean = false) {
        if (turret.owner === player) {
            turret.destroy(silent)
        }
    }

    fun spawnTurret(turret: LaserGameTurret): Boolean {
        if (state != Minigame.State.RUNNING) return false
        val player = turret.rootOwner as? LaserGamePlayer
        val valid = player == null || player.player.isOnGround
        if (!valid) return false

        if (arena.spawns.any { it.distance(turret.currentLocation) < 3.0 }) return false
//        val location = turret.currentLocation
//        val turretBox = BoundingBox(
//            xMin = location.x - 0.1,
//            xMax = location.x + 0.1,
//            yMin = location.y - 0.1,
//            yMax = location.y + 0.1,
//            zMin = location.z - 0.1,
//            zMax = location.z + 0.1
//        )
//        val valid = location.clone().add(0.0, -0.1, 0.0).block.getBoundingBoxes()
//            .overlaps(turretBox)
        turret.spawn()
        turrets.add(turret)
        return true
    }

    override fun update(timePassed: Long) {
        super.update(timePassed)
        arena.extensions.forEach { it.update(this) }

        projectiles.removeAll {
            it.update(this)
            !it.isValid
        }

        turrets.removeAll {
            it.update(this)
            if (!it.isValid) {
                it.destroy(false)
                return@removeAll true
            }
            return@removeAll false
        }

        val now = System.currentTimeMillis()
        players.forEach {
            if (it.player !in arena.area) {
                it.metadata.kill()
            }

            if (it.metadata.invincible) {
                it.player.location.spawnParticleX(
                    Particle.HEART,
                    offsetX = 0.5,
                    offsetY = 0.5,
                    offsetZ = 0.5,
                    count = 2,
                    extra = 0.1
                )
            }

            val timeLeft = maxGameTimeLength() - playTime
            val message =
                "Game ending in ${
                    DateUtils.format(
                        timeLeft,
                        "?"
                    )
                } K${it.metadata.kills}/D${it.metadata.deaths}${if (it.metadata.killStreak > 0) " (killstreak of ${it.metadata.killStreak})" else ""}"
            display(
                it.player,
                Message(
                    id = ChatUtils.ID_MINIGAME,
                    text = Component.text(
                        message,
                        CVTextColor.serverNotice
                    ),
                    type = MessageBarManager.Type.MINIGAME,
                    untilMillis = TimeUtils.secondsFromNow(1.0),
                ),
                replace = true,
            )

            it.player.apply {
                if (inventory.heldItemSlot == EquipmentManager.SLOT_WEAPON) {
                    val itemA = it.metadata.itemA
                    val percentage = itemA.item.getCooldownPercentageLeft(now)
                    level = 0
                    exp = percentage.clamp(0.0, 0.99).toFloat()

//                    inventory.getItem(WornItemManager.SLOT_WEAPON)?.let { itemStack ->
//                        if (itemA.item is TurretItem) {
//                            if (percentage != 0.0) {
//                                if (itemStack.amount != 1) {
//                                    itemStack.amount = 1
//                                    inventory.setItem(WornItemManager.SLOT_WEAPON, itemStack)
//                                }
//                            } else {
//                                if (itemStack.amount != 0) {
//                                    itemStack.amount = 0
//                                    inventory.setItem(WornItemManager.SLOT_WEAPON, itemStack)
//                                }
//                            }
//                        }
//                    }

//                    itemStackA?.apply {
//                        Logger.debug("PercentageA ${percentage.format(2)}")
//                        updateMeta<ItemMeta> {
//                            Logger.debug("Damage ${type.maxDurability} ${(type.maxDurability * percentage).toInt()}")
//                            setDamage((type.maxDurability * percentage).toInt())
//                        }
//                        inventory.setItem(WornItemManager.SLOT_WEAPON, this)
//                    }
                } else if (inventory.heldItemSlot == EquipmentManager.SLOT_EVENT) {
                    val itemB = it.metadata.itemB
                    val percentage = itemB.item.getCooldownPercentageLeft(now)
                    level = 0
                    exp = percentage.clamp(0.0, 0.99).toFloat()

//                    inventory.getItem(WornItemManager.SLOT_EVENT)?.let { itemStack ->
//                        if (itemB.item is TurretItem) {
//                            if (percentage != 0.0) {
//                                if (itemStack.amount != 1) {
//                                    itemStack.amount = 1
//                                    inventory.setItem(WornItemManager.SLOT_EVENT, itemStack)
//                                }
//                            } else {
//                                if (itemStack.amount != 0) {
//                                    itemStack.amount = 0
//                                    inventory.setItem(WornItemManager.SLOT_EVENT, itemStack)
//                                }
//                            }
//                        }
//                    }

//                    val itemStackB = inventory.getItem(WornItemManager.SLOT_EVENT)
//                    itemStackB?.apply {
//                        Logger.debug("PercentageB ${percentage.format(2)}")
//                        updateMeta<ItemMeta> {
//                            Logger.debug("Damage ${type.maxDurability} ${(type.maxDurability * percentage).toInt()}")
//                            setDamage((type.maxDurability * percentage).toInt())
//                        }
//                        inventory.setItem(WornItemManager.SLOT_EVENT, this)
//                    }
                } else {
                    level = 0
                    exp = 0f
                }
            }
        }
    }

    fun handleKill(
        target: LaserGameEntity,
        source: LaserGameEntity,
        hitType: LaserGameEntity.HitType,
        damageType: LaserGameEntity.DamageType
    ) {
        (source as? LaserGamePlayer)?.awardKill()
        Logger.debug("${target.displayName} (${target.rootOwner?.displayName}) damaged by ${source.displayName} (${source.rootOwner?.displayName})")
        val realDamageType =
            if (hitType == LaserGameEntity.HitType.HEADSHOT) LaserGameEntity.DamageType.headshot else damageType
        val targetRoot = target.rootOwner?.takeIf { it !== target }
        val sourceRoot = source.rootOwner?.takeIf { it !== source }
        val message = (CVTextColor.serverNoticeAccent +
                "${source.displayName}${if (sourceRoot != null) " (${sourceRoot.displayName})" else ""} ") +
                (CVTextColor.serverNotice +
                        "${realDamageType.icon} ") +
                (CVTextColor.serverNoticeAccent +
                        "${target.displayName}${if (targetRoot != null) " (${targetRoot.displayName})" else ""}")
        ((targetRoot ?: target) as? LaserGamePlayer)?.player?.sendMessage(message)
        ((sourceRoot ?: source) as? LaserGamePlayer)?.player?.sendMessage(message)
//        players.forEach {
//            it.player.sendMessage(message)
//        }
    }

    override fun sortedPlayers() = players.sortedWith(
        compareBy(
            { -it.metadata.kills },
            { it.metadata.deaths }
        )
    )

    override fun onPreStop(stopReason: Minigame.StopReason) {
        super.onPreStop(stopReason)

        projectiles.clear()
        turrets.forEach { it.destroy(true) }
        turrets.clear()

        if (teamMode == LaserGameTeamMode.FFA) {
            val finishedNormally =
                stopReason == Minigame.StopReason.ALL_PLAYERS_FINISHED || stopReason == Minigame.StopReason.OUT_OF_TIME
            if (finishedNormally) {
                val players = sortedPlayers()
                for (minigamePlayer in players) {
                    val player = minigamePlayer.player
                    Translation.MINIGAME_WIN_HEADER.getTranslation(player)?.sendTo(player)
                    for ((index, otherPlayer) in players.withIndex()) {
                        Translation.MINIGAME_ENTRY_TIMED.getTranslation(
                            player,
                            (index + 1).asOrdinalAppended(),
                            otherPlayer.player.name,
                            "K${otherPlayer.metadata.kills} / D${otherPlayer.metadata.deaths}"
                        )?.sendTo(player)
                    }
                    Translation.MINIGAME_WIN_FOOTER.getTranslation(player)?.sendTo(player)

                    executeAsync {
                        val first = player == players.firstOrNull()?.player
                        val delta = if (first) 10L else 5L
                        val accountType = BankAccountType.VC
                        MainRepositoryProvider.bankAccountRepository
                            .delta(player.uniqueId, accountType, delta, TransactionType.MINIGAME)
                        when {
                            first -> player.sendMessage(CVTextColor.serverNotice + "+$delta ${accountType.pluralName} for playing and finishing first")
                            else -> player.sendMessage(CVTextColor.serverNotice + "+$delta ${accountType.pluralName} for playing")
                        }
                        if (first)
                            MainRepositoryProvider.achievementProgressRepository.increaseCounter(
                                player.uniqueId,
                                "minigame_${internalName}_win"
                            )
                        MainRepositoryProvider.achievementProgressRepository
                            .increaseCounter(player.uniqueId, "minigame_${internalName}_play")
                    }
                }
            } else {
                for (minigamePlayer in players) {
                    val player = minigamePlayer.player
//                if (stopReason == StopReason.TOO_FEW_PLAYERS) {
                    Translation.MINIGAME_TOO_FEW_PLAYERS.getTranslation(player, displayName)?.sendTo(player)
//                } else {
//                    Translation.
//                }
                }
            }
        } else {

        }
    }

    override fun canJoin(player: Player): Boolean {
        return super.canJoin(player) || ((state == Minigame.State.PREPARING_GAME || state == Minigame.State.RUNNING) && players.size < maxPlayers - 2)
    }

    fun shootProjectile(projectile: LaserGameProjectile): Boolean {
        if (state != Minigame.State.RUNNING) return false
        projectile.update(this)
        if (projectile.isValid)
            projectiles.add(projectile)
        return true
    }

    override fun onUpdatePlayerWornItems(
        player: MinigamePlayer<LaserGamePlayer>,
        event: PlayerEquippedItemsUpdateEvent
    ) {
        super.onUpdatePlayerWornItems(player, event)

        event.appliedEquippedItems.clearArmor()
        event.appliedEquippedItems.clearSpecials()

        if (state == Minigame.State.PREPARING_GAME || state == Minigame.State.RUNNING) {
            val itemASlot = event.meta.equippedItems[EquippedItemSlot.LASER_GAME_A]
            val itemAChanged = itemASlot != null && itemASlot.id != player.metadata.itemA.id
            val itemBSlot = event.meta.equippedItems[EquippedItemSlot.LASER_GAME_B]
            val itemBChanged = itemBSlot != null && itemBSlot.id != player.metadata.itemB.id
//            Logger.debug("itemAChanged=$itemAChanged (${itemASlot?.item} vs ${player.metadata.itemA.id}) itemBChanged=$itemBChanged (${itemBSlot?.item} vs ${player.metadata.itemB.id})")

            if (itemAChanged) {
                val newItem =
                    getItem(
                        player.metadata.team.color,
                        event.meta.equippedItems[EquippedItemSlot.LASER_GAME_A]
                    )
                newItem.item.cooldownFinish = player.metadata.itemA.item.cooldownFinish
                newItem.item.lastUse = player.metadata.itemA.item.lastUse
                player.metadata.itemA = newItem
//                Logger.debug("ItemA updated")
            }
            if (itemBChanged) {
                val newItem =
                    getItem(
                        player.metadata.team.color,
                        event.meta.equippedItems[EquippedItemSlot.LASER_GAME_B]
                    )
                newItem.item.cooldownFinish = player.metadata.itemB.item.cooldownFinish
                newItem.item.lastUse = player.metadata.itemB.item.lastUse
                player.metadata.itemB = newItem
//                Logger.debug("ItemB updated")
            }

            event.appliedEquippedItems.weaponItem =
                player.metadata.itemA.representation.toEquippedItemData(player.metadata.itemA.id)
            event.appliedEquippedItems.eventItem =
                player.metadata.itemB.representation.toEquippedItemData(player.metadata.itemB.id)

            val color = player.metadata.team.color
            event.appliedEquippedItems.helmetItem =
                ItemStack(Material.LEATHER_HELMET).setLeatherArmorColor(color).toEquippedItemData()
            event.appliedEquippedItems.chestplateItem =
                ItemStack(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(color).toEquippedItemData()
            event.appliedEquippedItems.leggingsItem =
                ItemStack(Material.LEATHER_LEGGINGS).setLeatherArmorColor(color).toEquippedItemData()
            event.appliedEquippedItems.bootsItem =
                ItemStack(Material.LEATHER_BOOTS).setLeatherArmorColor(color).toEquippedItemData()
        }
    }

    override fun onPreJoin(player: Player) {
        super.onPreJoin(player)
        player.gameMode = GameMode.ADVENTURE
        player.inventory.heldItemSlot = EquipmentManager.SLOT_WEAPON
        player.playSound(player.location, "${SoundUtils.SOUND_PREFIX}:minigame.laser.start", 1f, 1f)
        if (teamMode == LaserGameTeamMode.FFA) {
            player.teleport(arena.spawns.random(), PlayerTeleportEvent.TeleportCause.PLUGIN)
        }
    }

    override fun onPlayerLeft(minigamePlayer: MinigamePlayer<LaserGamePlayer>, reason: Minigame.LeaveReason) {
        super.onPlayerLeft(minigamePlayer, reason)
        minigamePlayer.metadata.cleanup()
    }

    override fun provideMeta(player: Player): LaserGamePlayer {
        val team = when (teamMode) {
            LaserGameTeamMode.FFA -> LaserGameTeam("self", ffaColors.next())
            LaserGameTeamMode.TEAMS -> TODO()
        }
        val meta = player.equippedItemsMeta()
        return LaserGamePlayer(
            game = this,
            player = player,
            team = team,
            itemA = getItem(team.color, meta?.equippedItems?.get(EquippedItemSlot.LASER_GAME_A)),
            itemB = getItem(team.color, meta?.equippedItems?.get(EquippedItemSlot.LASER_GAME_B))
        )
    }

    private fun getItem(
        color: Color,
        equipmentItemData: EquipmentManager.EquippedItemData?,
    ): EquippedLaserGameItem {
        return if (equipmentItemData != null)
            EquippedLaserGameItem(
                equipmentItemData.id,
                if (equipmentItemData.ownableItemMeta != null)
                    ItemStackLoader.apply(
                        equipmentItemData.itemStack ?: coloredItem(
                            Material.FIREWORK_STAR,
                            color,
                            2
                        ), equipmentItemData.ownableItemMeta
                    ) else equipmentItemData.itemStack ?: coloredItem(
                    Material.FIREWORK_STAR,
                    color,
                    2
                ).markAsWornItem().displayName(CVTextColor.serverNoticeAccent + "Item"),
                equipmentItemData.ownableItem?.getOwnableItemMetadata()?.laserGun?.orElse()?.type?.orElse()?.factory?.invoke()
                    ?: equipmentItemData.ownableItem?.getOwnableItemMetadata()?.laserTurret?.orElse()?.type?.orElse()
                        ?.let {
                            TurretItem(it)
                        } ?: LaserGameGunType.DEFAULT.factory.invoke()
            )
        else
            EquippedLaserGameItem(
                "default",
                coloredItem(Material.FIREWORK_STAR, Color.RED, 2).markAsWornItem()
                    .displayName(CVTextColor.serverNoticeAccent + "Gun"),
                LaserGameGunType.DEFAULT.factory.invoke()
            )
    }

    protected fun useItemInHand(player: LaserGamePlayer, other: LaserGamePlayer?, leftClick: Boolean) {
//        Logger.debug("leftClick=$leftClick")
        if (state != Minigame.State.RUNNING) return
        val slot = player.player.inventory.heldItemSlot
        if (slot == EquipmentManager.SLOT_WEAPON) {
            player.clearInvincibility()
            player.itemA.item.use(this, player, leftClick)
        } else if (slot == EquipmentManager.SLOT_EVENT) {
            player.clearInvincibility()
            player.itemB.item.use(this, player, leftClick)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        players.firstOrNull { it.player === event.entity }?.let {
            executeSync(20) {
                if (it.player.isDead)
                    it.player.spigot().respawn()
            }
        }
    }

//    fun broadcastKills(who:LaserGameTarget, kills:)

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (!isRunning) return
        players.firstOrNull { it.player == event.player }?.apply {
            event.respawnLocation = arena.spawns.random(CraftventureCore.getRandom().asKotlinRandom())
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onAfterPlayerRespawn(event: PlayerPostRespawnEvent) {
        players.firstOrNull { it.player == event.player }?.apply {
            metadata.resetAfterRespawn()
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerStuck(event: PlayerStuckEvent) {
        if (!isRunning) return
        val clickedGamePlayer = players.firstOrNull { it.player === event.player } ?: return
        event.isCancelled = true
        clickedGamePlayer.metadata.kill()
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        val player = players.firstOrNull { it.player === event.player } ?: return
        targets.forEach {
            if (it.isPartOfTarget(event.rightClicked)) {
                event.isCancelled = true
                useItemInHand(player.metadata, null, false)
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
//        Logger.debug("EntityDamageByEntityEvent")
        if (!isRunning) return
        val player = players.firstOrNull { it.player === event.damager } ?: return
        event.isCancelled = true

        val clickedGamePlayer =
            (event.entity as? Player)?.let { clickedPlayer -> players.firstOrNull { it.player === clickedPlayer } }
        useItemInHand(player.metadata, clickedGamePlayer?.metadata, false)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
//        Logger.debug("PlayerInteractEntityEvent")
        if (!isRunning) return
        val player = players.firstOrNull { it.player === event.player } ?: return
        event.isCancelled = true
        if (event.hand != EquipmentSlot.HAND) return
        val clickedGamePlayer =
            (event.rightClicked as? Player)?.let { clickedPlayer -> players.firstOrNull { it.player === clickedPlayer } }
        useItemInHand(player.metadata, clickedGamePlayer?.metadata, true)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!isRunning) return
        val player = players.firstOrNull { it.player === event.player } ?: return
        event.isCancelled = true
        if (event.hand != EquipmentSlot.HAND) return
        useItemInHand(
            player.metadata,
            null,
            event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK
        )
    }

    @EventHandler
    fun onPlayerItemHeldChanged(event: PlayerItemHeldEvent) {
        if (!isRunning) return
        val player = players.firstOrNull { it.player === event.player } ?: return
        when (event.newSlot) {
            EquipmentManager.SLOT_WEAPON -> {
                player.metadata.itemA.applySwitchCooldown()
            }

            EquipmentManager.SLOT_EVENT -> {
                player.metadata.itemB.applySwitchCooldown()
            }
        }
    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    override fun onEntityDamageEvent(event: EntityDamageEvent) {
//        if (!isRunning) return
//        val player = players.firstOrNull { it.player === event.entity } ?: return
//        event.isCancelled = true
//        useItemInHand(player.metadata, null)//clickedGamePlayer.metadata)
//    }

    override fun toJson(): Json = toJson(Json())

    class Json : BaseMinigame.Json() {
        override fun createGame(): Minigame {
            TODO("Not yet implemented")
        }
    }
}