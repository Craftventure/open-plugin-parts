package net.craftventure.core.ride.shooter

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import net.craftventure.bukkit.ktx.entitymeta.EntityEvents.addListener
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.util.DurationUtils
import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.ProjectileEvents.removeUponEnteringBubbleColumn
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.actor.ActorPlayback
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.ride.shooter.config.ShooterSceneConfig
import net.craftventure.core.ride.shooter.config.ShooterSceneEntityConfig
import net.craftventure.core.ride.shooter.config.ShooterSceneIdleEntityConfig
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import net.craftventure.core.script.particle.ParticlePlayback
import net.craftventure.core.serverevent.IdentifiedItemUseEvent
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ShooterScene(
    val id: String,
    val config: ShooterSceneConfig,
    val shooterRideContext: ShooterRideContext,
) {
    private val area = config.area.create()
    val tracker = NpcAreaTracker(area)
    private var started = false
    private var timeInScene: Long = 0
    private var lastSceneStart = 0L
    private var currentCars: Set<TracklessRideCar> = emptySet()
    private val currentPlayers get() = currentCars.flatMap { it.playerPassengers }
    private var scheduledStopAction: ScheduledFuture<*>? = null

    private var projectiles = mutableSetOf<Snowball>()

    private fun carForPlayer(player: Player): TracklessRideCar? {
        return currentCars.firstOrNull { it.playerPassengers.any { it === player } }
    }

    private fun teamForPlayer(player: Player): ShooterRideContext.Team? {
        currentCars.forEach { car ->
            car.playerPassengers.forEach { passenger ->
                if (passenger === player) {
                    return car.team
                }
            }
        }
        return null
    }

    private val entityData = config.entities.mapNotNull { entityConfig ->
        val factory = shooterRideContext.getEntityFactory(entityConfig.npc)
        if (factory == null) {
            Logger.warn("Failed to find entity factory for id ${entityConfig.npc} at scene $id", logToCrew = true)
            return@mapNotNull null
        }
        val entity = factory.createPlayback(area.world, entityConfig)

        val particles = entityConfig.particles?.mapNotNull { particleConfig ->
            val particleFactory = shooterRideContext.getParticleFactory(particleConfig.file)
            if (particleFactory == null) {
                logcat(
                    LogPriority.WARN,
                    logToCrew = true
                ) { "Failed to find particle factory for id ${particleConfig.file} at NPC ${entityConfig.npc} at scene $id" }
                null
            } else
                particleFactory.createPlayback(tracker)
        }?.flatten()

        return@mapNotNull ManagedEntity(this, entityConfig, entity, particles ?: emptyList())
    }

    private val idleEntities = config.idleEntities.orEmpty().mapNotNull { entityConfig ->
        val factory = shooterRideContext.getEntityFactory(entityConfig.npc)
        if (factory == null) {
            Logger.warn("Failed to find entity factory for id ${entityConfig.npc} at scene $id", logToCrew = true)
            return@mapNotNull null
        }
        IdleEntity(factory.createPlaybackOnly(area.world), entityConfig)
    }

    private val particles = config.particles.orEmpty().mapNotNull {
        val factory = shooterRideContext.getParticleFactory(it.file)
        if (factory == null) {
            logcat(
                LogPriority.WARN,
                logToCrew = true
            ) { "Failed to find particle factory for id ${it.file} at scene $id" }
            return@mapNotNull null
        }
        val particlePlayback = factory.createPlayback(tracker)
        return@mapNotNull particlePlayback
    }.flatten()
    private val idleParticles = config.idleParticles.orEmpty().mapNotNull {
        val factory = shooterRideContext.getParticleFactory(it.file)
        if (factory == null) {
            logcat(
                LogPriority.WARN,
                logToCrew = true
            ) { "Failed to find particle factory for id ${it.file} at scene $id" }
            return@mapNotNull null
        }
        val particlePlayback = factory.createPlayback(tracker)
        return@mapNotNull particlePlayback
    }.flatten()

    private val listener = object : Listener {
        @EventHandler
        fun onTick(event: ServerTickStartEvent) {
            val gravity = shooterRideContext.config.gravity ?: return
            projectiles.forEach { projectile ->
                projectile.velocity = projectile.velocity.add(Vector(0.0, gravity, 0.0))
            }
        }

        @EventHandler(ignoreCancelled = true)
        fun onOwnedItemUse(event: IdentifiedItemUseEvent) {
            if (currentCars.isEmpty()) return
            if (!started) return
            val player = event.player
            when (event.id) {
                "shooter_ride_gun" -> {
                    if (player.hasCooldown(event.player.inventory.itemInMainHand.type)) {
                        event.isCancelled = true
                        return
                    }
                    if (player !in currentPlayers) {
//                        Logger.debug("Player not in current team ${currentTeam}")
                        return
                    }
                    player.setCooldown(
                        event.player.inventory.itemInMainHand.type,
                        DurationUtils.ofSecondsToTicks(
                            (config.gunCooldownSeconds ?: shooterRideContext.config.gunCooldownSeconds)
                        )
                    )
//                    Logger.debug("${player.name} shooting", PluginProvider.isNonProductionServer())
                    throwProjectile(player)
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        fun onWornItemsUpdate(event: PlayerEquippedItemsUpdateEvent) {
            if (currentCars.isEmpty()) return
            if (!started) return
            val player = event.player
            if (player !in currentPlayers) return
            event.appliedEquippedItems.weaponItem = shooterRideContext.gunItem
//                ItemStackUtils.fromString(shooterRideContext.config.gunItem)!!.toCachedItem("shooter_ride_gun")
        }
    }
    private val backgroundService = object : BackgroundService.Animatable {
        override fun onAnimationUpdate() {
            updateEntities()
        }
    }
    private val idleBackgroundService = object : BackgroundService.Animatable {
        override fun onAnimationUpdate() {
            idleEntities.forEach { if (it.shouldStop()) it.stop(tracker) }
        }
    }

    private fun updateEntities() {
        entityData.forEach { entityData ->
            if (entityData.shouldDespawn(lastSceneStart)) {
                entityData.despawn(tracker)
            }
            if (entityData.shouldSpawn(lastSceneStart)) {
                entityData.spawn(tracker)
            }
        }
    }

    init {
        tracker.startTracking()
        idleEntities.forEach { it.play(tracker) }
        idleParticles.forEach { it.play() }
        BackgroundService.add(idleBackgroundService)
    }

    fun destroy() {
        stop()
        actualStop()
        idleEntities.forEach { it.stop(tracker) }
        tracker.release()
        idleParticles.forEach { it.stop() }
        BackgroundService.remove(idleBackgroundService)
    }

    private fun throwProjectile(player: Player) {
//        Logger.debug("Team?")
        val team = teamForPlayer(player) ?: return
//        Logger.debug("Member?")
        if (player !in team) return
        team.dataFor(player)?.shot()

//        Logger.debug("Shoot?")

        var collideBlock: Block? = null

        val thrown = player.launchProjectile(Snowball::class.java).apply {
            removeUponEnteringBubbleColumn()
            item = ItemStack(Material.SNOWBALL).apply {
                updateMeta<ItemMeta> {
                    setCustomModelData(shooterRideContext.config.bulletModel)
                }
            }
            isPersistent = false
            setGravity(false)
            velocity = player.eyeLocation.direction.normalize().multiply(shooterRideContext.config.shootSpeed ?: 1.2)

            this.addListener(object : Listener {
                @EventHandler
                fun onHit(event: ProjectileHitEvent) {
                    collideBlock = event.hitBlock
                }

                @EventHandler
                fun onCollide(event: ProjectileCollideEvent) {
                    event.isCancelled = true
                }
            })
        }
        projectiles.add(thrown)

        val debug = false//CraftventureCore.isNonProductionServer()
        object : BukkitRunnable() {
            override fun run() {
                val location = thrown.location
                val vector = thrown.velocity

                val boundBox = thrown.boundingBox
                val boundBoxMoved = BoundingBox(
                    location.x,
                    location.y,
                    location.z,
                    location.x - vector.x,
                    location.y - vector.y,
                    location.z - vector.z
                )

                val npcBounds = entityData.filter { it.isPlaying }.map { managedEntity ->
                    val entity = managedEntity.entity.playback.npcEntity
                    val entityLocation = entity.getLocation()

                    val boundingBox = (shooterRideContext.config.entityHitboxes[entity.entityType]?.boundingBox()
                        ?: managedEntity.config.hitboxConfig?.boundingBox()
                        ?: entity.entityType.let { EntityMetadata.getBoundingBox(it) } ?: BoundingBox(
                            -0.25,
                            0.0,
                            -0.25,
                            0.25,
                            0.4,
                            0.25
                        ))
                    managedEntity to
                            BoundingBox(
                                entityLocation.x + boundingBox.minX,
                                entityLocation.y + boundingBox.minY,
                                entityLocation.z + boundingBox.minZ,
                                entityLocation.x + boundingBox.maxX,
                                entityLocation.y + boundingBox.maxY,
                                entityLocation.z + boundingBox.maxZ
                            )
                }

                val hitTargets = npcBounds.filter { boundBox.overlaps(it.second) || boundBoxMoved.overlaps(it.second) }
                if (hitTargets.isNotEmpty()) {
                    hitTargets.forEach { target ->
                        handleTarget(player, target.first)
//                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "You hit a NPC target ${target.entity.id}/${target.trigger} canHit=${target.trigger.canHit} timeoutLeftMs=${target.trigger.waitMs}")
//                            Logger.debug("Hit NPC")
                    }
                    projectiles.remove(thrown)
                    cancel()
                    playBreakEffect(location)
                    thrown.remove()
                    return
                }

                val block = collideBlock
                if (block != null) {
//                    handleFail(player)
                    projectiles.remove(thrown)
                    cancel()
                    playBreakEffect(location)
                    thrown.remove()
                    return
                }

                if (!thrown.isValid) {
//                    handleFail(player)
                    projectiles.remove(thrown)
                    cancel()
                    playBreakEffect(location)
                    return
                }
            }
        }.runTaskTimer(CraftventureCore.getInstance(), 0, 1)
    }

    private fun playBreakEffect(location: Location) {
        location.world.playSound(location, Sound.BLOCK_GLASS_BREAK, SoundCategory.AMBIENT, 1f, 1f)
        location.spawnParticleX(
            Particle.WATER_WAKE,
            count = 5,
            offsetX = 0.4,
            offsetY = 0.4,
            offsetZ = 0.4
        )
        location.spawnParticleX(
            Particle.ITEM_CRACK,
            count = 3,
            offsetX = 0.4,
            offsetY = 0.4,
            offsetZ = 0.4,
            data = ItemStack(Material.BLUE_SHULKER_BOX)
        )
        location.spawnParticleX(
            Particle.ITEM_CRACK,
            count = 3,
            offsetX = 0.4,
            offsetY = 0.4,
            offsetZ = 0.4,
            data = ItemStack(Material.WHITE_SHULKER_BOX)
        )
    }

    private fun handleTarget(player: Player, target: ManagedEntity) {
        val car = carForPlayer(player) ?: return
        val currentTeam = teamForPlayer(player) ?: return
        target.entity.hitActions.forEach {
            it.execute(car.tracklessRide, shooterRideContext, currentTeam, player, this, target)
        }
    }

    fun startFor(cars: Set<TracklessRideCar>) {
//        if (car.team == null) return
        if (started) return
        started = true

        if (scheduledStopAction != null) {
            scheduledStopAction?.cancel(false)
            actualStop()
        }
        lastSceneStart = System.currentTimeMillis()

        if (currentCars.isNotEmpty()) {
            logcat(
                LogPriority.WARN,
                logToCrew = true
            ) { "Started scene $id for ride ${cars.firstOrNull()?.tracklessRide?.id} while another team was playing it. Did you forget to stop this scene in the config?" }
        }

        idleParticles.forEach { it.stop() }
        entityData.forEach { it.start() }

        currentCars = cars
        timeInScene = 0
        Bukkit.getPluginManager().registerEvents(listener, PluginProvider.plugin)
        BackgroundService.add(backgroundService)

        updateEntities()

        idleEntities.forEach {
            if (it.config.despawnOnSceneStart) {
                it.stop(tracker)
            }
        }

        particles.forEach {
//            logcat { "Starting scene particle" }
            it.reset()
            it.play()
        }
//        Logger.debug(
//            "Starting scene $id for $currentTeam with ${entityData.size} entities",
//            PluginProvider.isNonProductionServer()
//        )

        if (!shooterRideContext.config.gunItemAlwaysInHand)
            currentPlayers.forEach { player ->
                player.inventory.heldItemSlot = EquipmentManager.SLOT_WEAPON
                EquipmentManager.reapply(player)
            }
    }

    fun stop() {
        if (!started) return
        started = false

        particles.forEach {
//            logcat { "Stopping scene particle" }
            it.stop()
        }
        idleParticles.forEach { it.play() }
        projectiles.forEach { it.remove() }

        if (!shooterRideContext.config.gunItemAlwaysInHand)
            currentPlayers.forEach { player ->
                EquipmentManager.reapply(player)
            }

        currentCars = emptySet()
        val seconds = config.keepPlayingSeconds
        if (seconds != null)
            scheduledStopAction = CraftventureCore.getScheduledExecutorService().schedule({
                actualStop()
            }, (seconds * 1000).toLong(), TimeUnit.MILLISECONDS)
        else
            actualStop()
//        Logger.debug("Stopping scene $id")
    }

    private fun actualStop() {
        scheduledStopAction = null
        HandlerList.unregisterAll(listener)
        BackgroundService.remove(backgroundService)

        entityData.forEach { it.despawn(tracker) }
        idleEntities.forEach { it.play(tracker) }
    }

    class IdleEntity(
        val playback: ActorPlayback,
        val config: ShooterSceneIdleEntityConfig,
    ) {
        fun shouldStop(): Boolean = !playback.shouldKeepPlaying() && config.despawnOnAnimationEnd

        fun play(idleTracker: NpcEntityTracker) {
            playback.reset()
            playback.play()
            idleTracker.addEntity(playback.npcEntity)
        }

        fun stop(idleTracker: NpcEntityTracker) {
            playback.stop()
            idleTracker.removeEntity(playback.npcEntity)
        }
    }

    class ManagedEntity(
        val scene: ShooterScene,
        val config: ShooterSceneEntityConfig,
        val entity: ShooterEntity,
        val particlePlaybacks: List<ParticlePlayback>,
    ) {
        var startedAt = 0L
        var isPlaying: Boolean = false
            private set
        var lastSpawn: Long = 0
        var lastDespawn: Long = 0

        fun start() {
            startedAt = System.currentTimeMillis()
            lastSpawn = 0L
            lastDespawn = 0L
        }

        fun stop() {

        }

        fun shouldDespawn(sceneStartTime: Long): Boolean {
            if (!isPlaying) return false

            val now = System.currentTimeMillis()

            val respawn = config.respawn
            if (respawn == ShooterSceneEntityConfig.Companion.RespawnType.INTERVAL_AFTER_SPAWN) {
                val seconds = ((if (lastSpawn == 0L) config.firstSpawnAtSeconds else config.respawnInterval
                    ?: 0.0) * 1000).toInt()
                val lastSpawnTime = lastSpawn
                val currentSpawnDelay = now - lastSpawnTime
                if (currentSpawnDelay >= seconds) {
                    return true
                }
            }

            return !entity.playback.shouldKeepPlaying()
        }

        fun shouldSpawn(sceneStartTime: Long): Boolean {
            val now = System.currentTimeMillis()
            val respawn = config.respawn

            when (respawn) {
                ShooterSceneEntityConfig.Companion.RespawnType.NEVER -> {
                    if (isPlaying) return false

                    if (lastSpawn == 0L) {
                        val seconds = ((if (lastSpawn == 0L) config.firstSpawnAtSeconds else config.respawnInterval
                            ?: 0.0) * 1000).toInt()
                        val currentSpawnDelay = now - sceneStartTime
                        if (currentSpawnDelay >= seconds) {
                            return true
                        }
                    }
                    return false
                }

                ShooterSceneEntityConfig.Companion.RespawnType.INTERVAL_AFTER_HIT -> {
                    if (isPlaying) return false

                    val seconds = ((if (lastSpawn == 0L) config.firstSpawnAtSeconds else config.respawnInterval
                        ?: 0.0) * 1000).toInt()
                    val lastSpawnTime = max(max(lastSpawn, lastDespawn), sceneStartTime)
                    val currentSpawnDelay = now - lastSpawnTime
                    if (currentSpawnDelay >= seconds) {
                        return true
                    }
                    return false
                }

                ShooterSceneEntityConfig.Companion.RespawnType.INTERVAL_AFTER_SPAWN -> {
                    val seconds = ((if (lastSpawn == 0L) config.firstSpawnAtSeconds else config.respawnInterval
                        ?: 0.0) * 1000).toInt()
                    val lastSpawnTime = max(lastSpawn, sceneStartTime)
                    val currentSpawnDelay = now - lastSpawnTime
                    if (currentSpawnDelay >= seconds) {
                        return true
                    }
                    return false
                }
            }
        }

        fun spawn(tracker: NpcAreaTracker) {
            if (isPlaying) return
            lastSpawn = System.currentTimeMillis()
            entity.playback.play()
            tracker.addEntity(entity.playback.npcEntity)
            isPlaying = true

            particlePlaybacks.forEach {
//                logcat { "Starting particle" }
                it.reset()
                it.play()
            }
        }

        fun despawn(tracker: NpcAreaTracker) {
            if (!isPlaying) return
            lastDespawn = System.currentTimeMillis()
            entity.playback.stop()
            entity.playback.reset()
            tracker.removeEntity(entity.playback.npcEntity)
            isPlaying = false

            particlePlaybacks.forEach {
//                logcat { "Stopping particle" }
                it.stop()
            }
        }
    }
}