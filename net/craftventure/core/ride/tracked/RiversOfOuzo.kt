package net.craftventure.core.ride.tracked

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.EntityEvents.addListener
import net.craftventure.bukkit.ktx.entitymeta.Meta
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.player
import net.craftventure.bukkit.ktx.extension.renewPotionEffect
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.manager.ProjectileEvents.removeUponEnteringBubbleColumn
import net.craftventure.core.metadata.CooldownTrackerMeta
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.ride.RideInstance
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.ride.trackedride.car.effect.FloatingMovementHandler
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.TransportSegment
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import net.craftventure.core.script.Script
import net.craftventure.core.script.ScriptManager
import net.craftventure.core.serverevent.*
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.core.utils.debug
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.type.MinigameScoreType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BoundingBox
import java.io.File
import java.time.LocalDateTime
import java.util.*


class RiversOfOuzo private constructor(trackedRide: TrackedRide) {
    companion object {
        private var config: Config? = null
        const val NAME = "riversofouzo"
        const val KEY_SCORE = "score"
        private var ride: RiversOfOuzo? = null

        private const val TRACK_FRICTION = 0.9983
        private const val GRAVITATIONAL_INFLUENCE = 0.03
        private const val TOOL_ACTIVATE_DISTANCE = 12.0

        private val CAR_COUNT = 20
        private val RIDE_DURATION_SECONDS = 60 + 60 + 60 + 60 + 30

        private var queue: RideQueue? = null
        private val listener = EventListener()

        private var currentTrigger: SceneTrigger? = null

        @JvmStatic
        fun get(): RiversOfOuzo {
            if (ride == null) {
                reloadConfig()
                Bukkit.getPluginManager().registerEvents(listener, CraftventureCore.getInstance())

                val coasterArea = SimpleArea("world",)
                val trackedRide = OperableCoasterTrackedRide(
                    NAME, coasterArea,
                    Location(Bukkit.getWorld("world"), ),
                    "ride_$NAME", NAME
                )

                trackedRide.setOperatorArea(coasterArea)
                initTrack(trackedRide)

                trackedRide.getSegmentById("track")!!
                    .add(object : TrackSegment.DistanceListener(TOOL_ACTIVATE_DISTANCE) {
                        override fun onTargetHit(rideCar: RideCar) {
                            val score = rideCar.meta[KEY_SCORE] as? Score ?: return
                            score.toolActive = true
                        }
                    })

                val station = trackedRide.getSegmentById("station")!!
                station.addOnSectionEnterListener { trackSegment, rideTrain, previousSegment ->
                    rideTrain.cars.forEach { car ->
                        car.saveOuzoScore()
                    }
                    rideTrain.eject()
                    rideTrain.setCanEnter(true)
                    rideTrain?.cancelAudio()
                    rideTrain?.setOnboardSynchronizedAudio("riversofouzo_onride", System.currentTimeMillis())
                }

                station.addOnSectionLeaveListener { trackSegment, rideTrain ->
                    rideTrain.setCanEnter(false)
                }

                val track = trackedRide.getSegmentById("track")!!
                track.addPlayerExitListener { car, player ->
                    car.saveOuzoScore()
                }
                track.add(object : TrackSegment.DistanceListener(10.0) {
                    override fun onTargetHit(rideCar: RideCar) {
                        rideCar.meta.apply {
                            remove(KEY_SCORE)

                            val player = rideCar.getPassengers().firstOrNull { it is Player }
                            if (player != null) {
                                Logger.debug(
                                    "Tracking score for ${player.name} in ${trackedRide.name}",
                                    PluginProvider.isNonProductionServer()
                                )
                                val score = Score(player.uniqueId)
                                score.toolActive = true
                                put(KEY_SCORE, score)
                            }
                        }
                    }
                })

                val totalLength = trackedRide.trackSegments.sumOf { it.length } / CAR_COUNT.toDouble()
                var distance = 0.0
                var segment = trackedRide.trackSegments.first()
                for (i in 0 until CAR_COUNT) {
                    distance -= totalLength
                    while (distance < 0.0) {
                        segment = segment.previousTrackSegment
                        distance += segment.length
                    }
                    addTrain(trackedRide, segment, distance)
                }

                trackedRide.initialize()
                trackedRide.pukeRate = 0.0
                ride = RiversOfOuzo(trackedRide)

                fun getLowerStationQueueTrain(): RideTrain? =
                    trackedRide.rideTrains
                        .filter { it.canEnter() && it.frontCarTrackSegment.id == station.id }
                        .maxByOrNull {
                            it.frontCarDistance
                        }

                val queueArea = SimpleArea("world", )

                queue = RideQueue(
                    ride = trackedRide,
                    joinArea = queueArea,
                    passengerCountPerTrain = 1,
                    activeThresshold = if (PluginProvider.isTestServer()) 1 else 7,
                    averageSecondsBetweenDepartures = RIDE_DURATION_SECONDS.toDouble() / CAR_COUNT.toDouble(),
                    boardingDelegate = object : RideQueue.BoardingDelegate {
                        private val targetTrain: RideTrain?
                            get() = if (PluginProvider.isTestServer()) null else getLowerStationQueueTrain()

                        override fun getRideName(): String = trackedRide.displayName ?: trackedRide.id

                        override fun isBeingOperated(): Boolean = trackedRide.isBeingOperated
                        override fun isQueueTimeAvailable(): Boolean = !trackedRide.isBeingOperated
                        override fun canPut(): Boolean = targetTrain != null
                        override fun put(passenger: Player): Boolean = targetTrain?.putPassenger(passenger) ?: false

                        override fun setQueueActive(rideQueue: RideQueue, active: Boolean) {}
                        override fun canEnterRide(player: Player): Boolean =
                            trackedRide.canEnter(player).isAllowed() || player.isCrew()

                        override fun getLastDispatch(): Long? = null

                        override val ride: RideInstance = trackedRide

                    })
                queue!!.start()
                trackedRide.addQueue(queue)
//                    if (it == getLowerStationQueueTrain()) queue else null
//                }
            }
            return ride!!
        }

        private fun RideCar.saveOuzoScore() {
            meta.apply {
                val score = get(KEY_SCORE) as? Score
                if (score != null) {
                    if (score.score != 0) {
                        score.toolActive = false
                        score.save()
                        Logger.debug(
                            "${trackedRide!!.name} finished with score ${score.score} for ${score.who}",
                            PluginProvider.isNonProductionServer()
                        )
                    } else {
                        score.who.player?.sendMessage(CVTextColor.serverNotice + "With a score of 0 you won't make it to the leaderboards! Try it again!")
                    }
                }
                remove(KEY_SCORE)
            }
        }

        fun doSpray(player: Player) {
//            Logger.debug("Ouzo gun sprayed by ${player.name}", true)
            val playerLocation = player.location
            val npcs = listener.scripts
                .filter { it.isRunning }
                .mapNotNull { it.scriptController?.npcEntityTracker }
                .flatMap { it.npcs }
                .filter { it.getLocation().distanceSquared(playerLocation) < 20 * 20 }
                .mapNotNull { entity ->
                    val trigger = config?.triggers?.filter { it.id == entity.id } ?: return@mapNotNull null
                    if (trigger.isEmpty()) {
//                        Logger.debug("No trigger for ${entity.id}", CraftventureCore.isNonProductionServer())
                        return@mapNotNull null
                    }
//                    Logger.debug("${trigger.size} triggers for ${entity.id}", CraftventureCore.isNonProductionServer())
                    entity to trigger
                }

//            Logger.debug("NPCS ${npcs.size}")
            spawnProjectile(player, npcs)
        }

        private fun spawnProjectile(player: Player, npcs: List<Pair<NpcEntity, List<Trigger>>>) {
            var collideBlock: Block? = null
            var collideEntity: Entity? = null

            val thrown = player.launchProjectile(Snowball::class.java).apply {
                removeUponEnteringBubbleColumn()
                item = ItemStack(Material.SNOWBALL).apply {
                    updateMeta<ItemMeta> {
                        setCustomModelData(3)
                    }
                }
                velocity = player.eyeLocation.direction.normalize().multiply(0.6)

                this.addListener(object : Listener {
                    @EventHandler
                    fun onHit(event: ProjectileHitEvent) {
                        collideBlock = event.hitBlock
                    }

                    @EventHandler
                    fun onCollide(event: ProjectileCollideEvent) {
                        collideEntity = event.collidedWith
                        event.isCancelled = true
//                        event.entity.remove()
//                        playBreakEffect(event.entity.location)
                        val player = event.collidedWith as? Player
                        if (player != null)
                            applyHitEffect(player)
                    }
                })
            }

            val debug = false//CraftventureCore.isNonProductionServer()
            object : BukkitRunnable() {
                override fun run() {
                    val location = thrown.location
                    val vector = thrown.velocity
//                    Logger.debug("Speed=${vector.length()} ${vector.asString()}")

                    val boundBox = thrown.boundingBox
                    val boundBoxMoved = BoundingBox(
                        location.x,
                        location.y,
                        location.z,
                        location.x - vector.x,
                        location.y - vector.y,
                        location.z - vector.z
                    )

//                    boundBox.debug(
//                        world = location.world,
//                        particle = Particle.REDSTONE,
//                        data = Particle.DustOptions(Color.RED, 0.25f)
//                    )

                    val npcBounds = npcs.flatMap {
                        val entity = it.first
                        val entityLocation = it.first.getLocation()
                        it.second.map { trigger ->
                            val width = trigger.width?.let { it * 0.5 } ?: 0.25
                            val height = trigger.height ?: 0.4
                            val yOffset = trigger.yOffset +
                                    (if (entity.entityType == EntityType.ARMOR_STAND) EntityConstants.ArmorStandHeadOffset else 0.0)

                            val boxEntityType =
                                trigger.forcedBoundingBoxEntityType?.takeIf { trigger.forceCustomBoundingBox }
                                    ?: entity.entityType
                            val entityBox = EntityMetadata.getBoundingBox(boxEntityType)?.let {
                                BoundingBox(
                                    entityLocation.x + it.minX,
                                    entityLocation.y + it.minY,
                                    entityLocation.z + it.minZ,
                                    entityLocation.x + it.maxX,
                                    entityLocation.y + it.maxY,
                                    entityLocation.z + it.maxZ
                                )
                            }

                            val box =
                                (if (!trigger.forceCustomBoundingBox || trigger.forceCustomBoundingBox && trigger.forcedBoundingBoxEntityType != null) entityBox else null)
                                    ?: BoundingBox(
                                        -width,
                                        0.0,
                                        -width,
                                        width,
                                        height,
                                        width
                                    ).let {
                                        BoundingBox(
                                            entityLocation.x + it.minX,
                                            entityLocation.y + it.minY,
                                            entityLocation.z + it.minZ,
                                            entityLocation.x + it.maxX,
                                            entityLocation.y + it.maxY,
                                            entityLocation.z + it.maxZ
                                        )
                                    }
//                            if (debug)
//                                entityLocation.spawnParticleX(Particle.BARRIER)
//                            Logger.debug(
//                                "${entity.id} ${entity.entityType} ${entityLocation.y.format(2)} ${yOffset.format(2)} ${box.center.asString()}"
//                            )
                            if (debug)
                                box.debug(world = location.world, particle = Particle.END_ROD)
                            TriggerBox(
                                entity,
                                box,
                                trigger
                            )
                        }
                    }

                    val hitTargets = npcBounds.filter { boundBox.overlaps(it.box) || boundBoxMoved.overlaps(it.box) }
                    if (hitTargets.isNotEmpty()) {
                        hitTargets.forEach { target ->
                            handleTarget(player, target.trigger)
//                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "You hit a NPC target ${target.entity.id}/${target.trigger} canHit=${target.trigger.canHit} timeoutLeftMs=${target.trigger.waitMs}")
//                            Logger.debug("Hit NPC")
                        }
                        cancel()
                        playBreakEffect(location)
                        thrown.remove()
                        return
                    }

                    val block = collideBlock
                    if (block != null) {
                        val target =
                            config?.triggers?.firstOrNull {
                                it.blockInstance != null && it.blockInstance.x == block.x && it.blockInstance.y == block.y && it.blockInstance.z == block.z
                            }
                        if (target != null) {
                            handleTarget(player, target)
//                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "You hit a block target $target canHit=${target.canHit} timeoutLeftMs=${target.waitMs}")
                        } else {
                            handleFail(player)
//                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "Non-setup block ${block.type} ${block.x}/${block.y}/${block.z}")
                        }
                        cancel()
                        playBreakEffect(location)
                        thrown.remove()
                        return
                    }

                    if (!thrown.isValid) {
                        handleFail(player)
                        cancel()
                        playBreakEffect(location)
                        return
                    }
                }
            }.runTaskTimer(CraftventureCore.getInstance(), 0, 1)
        }

        private fun applyHitEffect(player: Player) {
            player.renewPotionEffect(PotionEffectType.BLINDNESS, duration = 20, amplifier = 1)
            player.renewPotionEffect(PotionEffectType.CONFUSION, duration = 20 * 5, amplifier = 1)
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

        private fun handleFail(player: Player) {
            try {
                val score = player.getOuzoScore() ?: return
                score.score -= 1
                player.sendTitleWithTicks(5, 20, 5, NamedTextColor.GOLD, null, NamedTextColor.RED, "-1")
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
        }

        private fun handleTarget(player: Player, target: Trigger) {
            val canHit = target.canHit
            if (!canHit) return

            target.doHit()
            if (target.score != null && target.score != 0)
                try {
                    val score = player.getOuzoScore() ?: return
                    score.score += target.score
                    player.sendTitleWithTicks(
                        5,
                        20,
                        5,
                        NamedTextColor.GOLD,
                        null,
                        if (target.score > 0) NamedTextColor.GREEN else NamedTextColor.RED,
                        "${if (target.score > 0) "+" else ""}${target.score}"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }

            target.scenes?.forEach { scene ->
                this.currentTrigger = scene
                if (scene.trigger_type != null && scene.trigger_scene != null) {
                    when (scene.trigger_type) {
                        TriggerType.START -> ScriptManager.start(NAME, scene.trigger_scene)
                        TriggerType.STOP -> ScriptManager.stop(NAME, scene.trigger_scene)
                        TriggerType.RESTART -> {
                            ScriptManager.stop(NAME, scene.trigger_scene)
                            ScriptManager.start(NAME, scene.trigger_scene)
                        }
                    }
                }
                this.currentTrigger = null
            }
        }

        private fun addTrain(trackedRide: TrackedRide, segment: TrackSegment, distance: Double) {
            val rideTrain = CoasterRideTrain(segment, distance)
//            rideTrain.setTrainSoundName("skilift")
            rideTrain.setMovementHandler(
                FloatingMovementHandler(
                    0.0,
                    0.001 + (CraftventureCore.getRandom().nextDouble() * 0.001),
                    0.03 + (CraftventureCore.getRandom().nextDouble() * 0.005),
                    CraftventureCore.getRandom().nextInt(10000).toLong()
                ) { true }
            )
            rideTrain.setUpdateListener { train ->
                train.cars.forEach { car ->
                    car.getPassengers().forEach car@{ player ->
                        val score = player?.getOuzoScore() ?: return@car
                        display(
                            player,
                            Message(
                                id = ChatUtils.ID_RIDE,
                                text = Component.text(
                                    "Current score ${score.score}",
                                    CVTextColor.serverNotice
                                ),
                                type = MessageBarManager.Type.RIDE,
                                untilMillis = TimeUtils.secondsFromNow(1.0),
                            ),
                            replace = true,
                        )
                    }
                }
            }

            val dynamicSeatedRideCar = DynamicSeatedRideCar(NAME, 4.0)
            dynamicSeatedRideCar.carFrontBogieDistance = -0.5
            dynamicSeatedRideCar.carRearBogieDistance = -3.5

            dynamicSeatedRideCar.addSeat(
                ArmorStandSeat(0.0, 0.3, -1.0, true, NAME)
                    .setModel(MaterialConfig.OUZO_CAR)
            )

            rideTrain.addCar(dynamicSeatedRideCar)

            trackedRide.addTrain(rideTrain)
        }

        private fun initTrack(trackedRide: TrackedRide) {
            val track = TransportSegment(
                "track",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8)
            ).apply {
                trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
                ejectType = TrackSegment.EjectType.EJECT_TO_SEAT
                leaveMode = TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
                blockSection(true)
            }
//            track.add(object : TrackSegment.DistanceListener(15.0) {
//                override fun onTargetHit(rideCar: RideCar) {
////                    Logger.info("Closing bottom")
//                    rideCar.attachedTrain?.setCanEnter(false)
//                }
//            })
//            track.add(object : TrackSegment.DistanceListener(99.0) {
//                override fun onTargetHit(rideCar: RideCar) {
////                    Logger.info("Opening top")
//                    rideCar.attachedTrain?.eject()
//                    rideCar.attachedTrain?.setCanEnter(true)
//                }
//            })

            val station = TransportSegment(
                "station",
                trackedRide,
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(4.0),
                CoasterMathUtils.kmhToBpt(1.8)
            ).apply {
                trackType = TrackSegment.TrackType.WHEEL_TRANSPORT
                ejectType = TrackSegment.EjectType.EJECT_TO_EXIT
                leaveMode = TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER
                setFriction(TRACK_FRICTION)
                setGravitationalInfluence(GRAVITATIONAL_INFLUENCE)
                setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
                blockSection(true)
            }
            station.add(object : TrackSegment.DistanceListener(91.0) {
                override fun onTargetHit(rideCar: RideCar) {
//                    Logger.info("Opening bottom")
                    rideCar.attachedTrain.eject()
                }
            })

            station.add(
            )
            track.add(
            )

            station.setNextTrackSegmentRetroActive(track)
                .setNextTrackSegmentRetroActive(station)

            trackedRide.addTrackSections(
                station,
                track
            )
        }

        fun reloadConfig() {
            try {
                config = loadConfig()
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.severe("Failed to load RiversOfOuzo: ${e.message}", true)
            }
        }

        private fun loadConfig(): Config? {
            val file = File(CraftventureCore.getInstance().dataFolder, "data/ride/$NAME/config.json")
            if (file.exists()) {
                try {
                    return CvMoshi.adapter(Config::class.java).fromJson(file.readText())
                } catch (e: Exception) {
                    e.printStackTrace()
                    Logger.warn("Failed to load Rivers of Ouzo config: ${e.message}", true)
                }
            }
            return null
        }

        fun Player.getOuzoScore(): Score? {
            val vehicle = player?.vehicle ?: return null
            val car = Meta.getEntityMeta<RideCar>(vehicle, Meta.createTempKey(RideCar.KEY_CAR)) ?: return null
            return car.meta[KEY_SCORE] as? Score ?: return null
        }
    }

    class Score(
        val who: UUID,
        var score: Int = 0,
        toolActive: Boolean = false
    ) {
        fun save() {
            if (score == 0) return
            val player = Bukkit.getPlayer(who) ?: return
            executeAsync {
                val created = MainRepositoryProvider.minigameScoreRepository
                    .createSilent(
                        MinigameScore(
                            UUID.randomUUID(),
                            who,
                            "riversofouzo",
                            score,
                            LocalDateTime.now(),
                            MinigameScoreType.TOTAL,
                            null,
                            player.isCrew()
                        )
                    )
                if (created) {
                    player.sendMessage(CVTextColor.serverNotice + "Your score of ${score} at Rivers of Ouzo has been saved")
                }

                val achievementPrefix = "riversofouzo_score_"
                val achievements =
                    MainRepositoryProvider.achievementRepository.cachedItems.filter {
                        it.id!!.startsWith(
                            achievementPrefix
                        )
                    }
                achievements.forEach { achievement ->
                    if (!achievement.enabled!!) return@forEach
                    val achievementScore =
                        achievement.id!!.removePrefix(achievementPrefix).toIntOrNull() ?: return@forEach
                    if (score > 0 && achievementScore in 1..score) {
                        MainRepositoryProvider.achievementProgressRepository.reward(who, achievement.id!!)
                    } else if (score < 0 && achievementScore in score..-1) {
                        MainRepositoryProvider.achievementProgressRepository.reward(who, achievement.id!!)
                    }
                }
            }
        }

        var toolActive = toolActive
            set(value) {
                if (field != value) {
                    field = value
                    val player = Bukkit.getPlayer(who) ?: return
                    player.inventory.heldItemSlot = EquipmentManager.SLOT_WEAPON
                    EquipmentManager.reapply(player)
                    if (value)
                        player.gameMode = GameMode.ADVENTURE
                }
            }
    }

    class EventListener : Listener {
        val scripts = mutableSetOf<Script>()

        init {
            scripts.addAll(ScriptManager.controllers
                .filter { it.groupId == NAME }
                .flatMap { it.scripts })
        }

        @EventHandler
        fun onIdentifiedItemUse(event: IdentifiedItemUseEvent) {
            if (event.id == "rivers_of_ouzo_gun") {
                val player = event.player
                if (!player.isInsideVehicle) return
                val trackerMeta = player.getOrCreateMetadata { CooldownTrackerMeta(player) }
                val canUse = trackerMeta.use("riversofouzo", 1500)
                if (canUse)
                    doSpray(event.player)
            }
        }

        @EventHandler
        fun onScriptLoaded(event: ScriptLoadedEvent) {
            if (event.group != NAME) return
//            Logger.debug(
//                "Adding scene ${event.name} to $NAME lookup",
//                CraftventureCore.isNonProductionServer()
//            )
            scripts.addAll(event.script.scripts)
        }

        @EventHandler
        fun onScriptStart(event: ScriptStartEvent) {
            if (!event.group.equals(NAME, ignoreCase = true)) return
            val eventsToHandle =
                config?.scene_events?.filter {
                    it.scene.equals(
                        event.name,
                        ignoreCase = true
                    ) && it.type == SceneEventType.START
                }
                    ?.takeIf { it.isNotEmpty() } ?: return
//            Logger.debug(
//                "Handling scene start ${event.name} for $NAME",
//                CraftventureCore.isNonProductionServer()
//            )
            eventsToHandle.forEach {
                it.result.forEach { result ->
                    when (result.type) {
                        TriggerType.START -> ScriptManager.start(NAME, result.scene)
                        TriggerType.STOP -> ScriptManager.stop(NAME, result.scene)
                        TriggerType.RESTART -> ScriptManager.restart(NAME, result.scene)
                    }
                }
            }
        }

        @EventHandler
        fun onScriptStop(event: ScriptStopEvent) {
            if (!event.group.equals(NAME, ignoreCase = true)) return
            if (currentTrigger?.dont_handle_event == true && currentTrigger?.trigger_scene == event.name) return
            val eventsToHandle =
                config?.scene_events?.filter {
                    it.scene.equals(
                        event.name,
                        ignoreCase = true
                    ) && it.type == SceneEventType.STOP
                }
                    ?.takeIf { it.isNotEmpty() } ?: return
//            Logger.debug(
//                "Handling scene stop ${event.name} for $NAME",
//                CraftventureCore.isNonProductionServer()
//            )
            eventsToHandle.forEach {
                it.result.forEach { result ->
                    when (result.type) {
                        TriggerType.START -> ScriptManager.start(NAME, result.scene)
                        TriggerType.STOP -> ScriptManager.stop(NAME, result.scene)
                        TriggerType.RESTART -> ScriptManager.restart(NAME, result.scene)
                    }
                }
            }
        }

        @EventHandler
        fun onScriptUnloaded(event: ScriptUnloadedEvent) {
            if (event.group != NAME) return
//            Logger.debug(
//                "Removing scene ${event.name} from $NAME lookup",
//                CraftventureCore.isNonProductionServer()
//            )
            scripts.removeAll(event.script.scripts)
        }

        @EventHandler(ignoreCancelled = true)
        fun onWornItemsUpdate(event: PlayerEquippedItemsUpdateEvent) {
            val player = event.player
            val vehicle = player.vehicle ?: return
            val isOnOuzo = vehicle.name == NAME
            if (!isOnOuzo) return
            val score = player.getOuzoScore() ?: return
            if (!score.toolActive) return
            event.appliedEquippedItems.weaponItem = MaterialConfig.OUZO_BOTTLE_ITEM.clone().apply {
                displayNameWithBuilder {
                    text("Ouzo Spray")
                    text(" (rightclick to spray)", color = CVTextColor.subtle)
                }
            }.toEquippedItemData("rivers_of_ouzo_gun")
//                val ride = player.getRide()
        }
    }

    data class TriggerBox(
        val entity: NpcEntity,
        val box: BoundingBox,
        val trigger: Trigger
    )

    @JsonClass(generateAdapter = true)
    data class Config(
        val triggers: Array<Trigger>,
        val scene_events: Array<SceneEvent>? = null
    )

    @JsonClass(generateAdapter = true)
    data class Trigger(
        val id: String?,
        val scenes: List<SceneTrigger>?,
        val score: Int?,
        val block: Location?,

        val width: Double?,
        val height: Double?,
        val yOffset: Double = 0.0,
        val forcedBoundingBoxEntityType: EntityType? = null,
        val forceCustomBoundingBox: Boolean = false,

        val timeoutSeconds: Double = 0.0
    ) {
        val blockInstance = block?.block
        var lastUse: Long? = null

        val waitMs: Long?
            get() = lastUse?.let { lastUse ->
                val now = System.currentTimeMillis()
                val target = lastUse + (1000 * timeoutSeconds).toLong()
                if (now < target) null else target - now
            }

        val canHit: Boolean
            get() = timeoutSeconds == 0.0 || lastUse == null || lastUse!! < System.currentTimeMillis() - (1000 * timeoutSeconds).toLong()

        fun doHit() {
            lastUse = System.currentTimeMillis()
        }
    }

    @JsonClass(generateAdapter = true)
    data class SceneTrigger(
        val trigger_scene: String?,
        val trigger_type: TriggerType?,
        val dont_handle_event: Boolean = false
    )

    @JsonClass(generateAdapter = true)
    data class SceneEvent(
        val scene: String,
        val type: SceneEventType,
        val result: Array<SceneEventResult>
    )

    @JsonClass(generateAdapter = true)
    data class SceneEventResult(
        val scene: String,
        val type: TriggerType
    )

    enum class TriggerType {
        START, STOP, RESTART
    }

    enum class SceneEventType {
        START, STOP
    }
}

