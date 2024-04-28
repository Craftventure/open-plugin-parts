package net.craftventure.core.feature.kart

import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.event.FeatureToggledEvent
import net.craftventure.bukkit.ktx.extension.getBoundingBoxForBlock
import net.craftventure.bukkit.ktx.extension.setColor
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.animation.armature.Joint
import net.craftventure.core.animation.dae.DaeLoader
import net.craftventure.core.config.AreaConfigManager
import net.craftventure.core.database.ItemStackLoader
import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.feature.kart.addon.KartAddon
import net.craftventure.core.feature.kart.config.KartOptions
import net.craftventure.core.feature.kart.config.KartsConfig
import net.craftventure.core.feature.kart.inputcontroller.DefaultKartController
import net.craftventure.core.feature.kart.physicscontroller.PhysicsController
import net.craftventure.core.ktx.extension.broadcastAsDebugTimings
import net.craftventure.core.ktx.extension.force
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.Allow
import net.craftventure.core.manager.Deny
import net.craftventure.core.manager.PlayerStateManager.isAllowedToManuallySpawnKart
import net.craftventure.core.serverevent.KartStartEvent
import net.craftventure.core.serverevent.ProvideLeaveInfoEvent
import net.craftventure.core.utils.BoundingBox
import net.craftventure.core.utils.ItemStackUtils
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.set


object KartManager {
    private val karts: ConcurrentLinkedQueue<Kart> = ConcurrentLinkedQueue()
    private var task: Int = 0
    private val listener = object : Listener {
        @EventHandler
        fun onProvideLeaveInfo(event: ProvideLeaveInfoEvent) {
            val kart = kartForPlayer(event.player) ?: return
            if (!kart.kartOwner.allowUserDestroying()) return
            if (!kart.isParked() && kart.kartOwner.canExit(kart)) {
                event.data.add(
                    ProvideLeaveInfoEvent.Entry(
                        ProvideLeaveInfoEvent.Category.Kart,
                        "Eject yourself from your kart",
                        representation = dataItem(
                            Material.FIREWORK_STAR,
                            13
                        ).setColor(Color.fromRGB(CVTextColor.serverNotice.value())),
                    ) {
                        ejectDriver(kart)
                    })
            } else {
                event.data.add(
                    ProvideLeaveInfoEvent.Entry(
                        ProvideLeaveInfoEvent.Category.Kart,
                        "Destroy your kart",
                        representation = dataItem(
                            Material.FIREWORK_STAR,
                            13
                        ).setColor(Color.fromRGB(CVTextColor.serverNotice.value())),
                    ) {
                        kart.player.sendMessage(CVTextColor.serverNotice + "Your kart was destroyed as per your request")
                        kart.destroy()
                        true
                    })
            }
        }

        @EventHandler
        fun onFeatureToggled(event: FeatureToggledEvent) {
            if (event.feature == FeatureManager.Feature.KART_SPAWN_AS_USER) {
                karts.forEach {
                    if (it.kartOwner.allowUserDestroying())
                        it.requestDestroy()
                }
            }
        }

//        @EventHandler(ignoreCancelled = true)
//        fun onRequestExit(event: PlayerHotKeyPressedEvent) {
//            if (ejectDriver(event.player)) {
//                event.isCancelled = true
//            }
//        }
    }

    private var config: KartsConfig = KartsConfig()
    private val validKarts: MutableMap<String, KartOptions> = hashMapOf()
    private val armatures: MutableMap<String, Armature> = hashMapOf()

    val kartOptions: Map<String, KartOptions>
        get() = validKarts.toMap()

    fun reloadConfig() {
        try {
            validKarts.clear()
            armatures.clear()
            config = loadConfigs()

            val directory = File(CraftventureCore.getInstance().dataFolder, "data/kart/armature")
            directory.walkTopDown().filter { it.isFile && it.extension == "dae" }.forEach { daeFile ->
                val fileName = daeFile.nameWithoutExtension
                val dbFactory = DocumentBuilderFactory.newInstance()
                val dBuilder = dbFactory.newDocumentBuilder()
                val doc = dBuilder.parse(daeFile)
                DaeLoader.load(doc, daeFile.name).forEach { armature ->
//                    Logger.debug("Loaded armature $fileName/${armature.name}")
                    armatures["$fileName/${armature.name}"] = armature
                }
            }
//            Logger.debug(config.toJsonIndented())
//            val jeep = config.kart.first { it.id == "jeep_sandy" }
//            val final = jeep.resolve(config.kart)
//            Logger.debug(final.toJsonIndented())
//            Logger.debug("valid=${final.isValid()}")
//
//            val properties = kartPropertiesFromConfig("jeep_sandy")
//            Logger.debug(properties.toString())
            config.kart.forEach {
                val resolved = it.resolve(config.kart)
                if (resolved.isValid())
                    try {
                        val config = kartPropertiesFromConfig(resolved)
                        validKarts[resolved.id] = resolved
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Logger.warn(e.message ?: "Failed to setup kart ${resolved.id}", true)
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.severe("Failed to load and handle karts config: ${e.message}", true)
        }
    }

    private fun loadConfigs(): KartsConfig {
        val directory = File(CraftventureCore.getInstance().dataFolder, "data/kart/configs")
        val files = directory.walkTopDown().filter { it.isFile && it.extension == "json" }.toList()
        val configsWithFile = files.mapNotNull { file ->
            try {
                file to loadConfigFile(file)
            } catch (e: Exception) {
                Logger.capture(e)
                null
            }
        }
        val config = configsWithFile.fold(KartsConfig()) { initial, it ->
            try {
                initial.mergeWith(it.second!!)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to merge kart config ${it.first.path}: ${e.message}", e)
            }
        }
        return config
    }

    private fun loadConfigFile(file: File): KartsConfig? {
        if (file.exists()) {
            try {
                return CvMoshi.adapter(KartsConfig::class.java).fromJson(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.warn("Failed to load Karts config: ${e.message}", true)
            }
        }
        return null
    }

    fun ejectDriver(player: Player): Boolean {
        val kart = kartForPlayer(player) ?: return false
        return ejectDriver(kart)
    }

    fun ejectDriver(kart: Kart): Boolean {
        if (kart.kartOwner == null || kart.kartOwner.canExit(kart)) {
            if (!kart.isParked()) {
                kart.player.leaveVehicle()
                return true
            }
        }
        return false
    }

    fun init() {
        reloadConfig()
        Bukkit.getServer().pluginManager.registerEvents(listener, CraftventureCore.getInstance())
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            val start = System.currentTimeMillis()
            val kartIterator = karts.iterator()
            while (kartIterator.hasNext()) {
//                Logger.debug("Kart has next")
                val kart = kartIterator.next()
                if (!kart.isValid()) {
//                    Logger.debug("Destroying invalid kart for ${kart.player.name} hash=${kart.hashCode()}")
                    kart.destroy()
                    kartIterator.remove()
//                    "Destroyed kart for %s (%d still exist)".format(kart.player.name, karts.size)
//                        .broadcastAsDebugTimings()
                } else
                    kart.update()
            }
            val end = System.currentTimeMillis() - start
            if (end > 4) {
                "Updating karts took $end ms".broadcastAsDebugTimings()
            }
        }, 1L, 1L)
    }

    fun destroy() {
        Bukkit.getScheduler().cancelTask(task)
        for (kart in karts) {
            kart.destroy()
        }
    }

    fun cleanupParkedKart(player: Player) {
        val currentKart = kartForPlayer(player)
        if (currentKart?.isParked() == true) {
            currentKart.destroy()
        }
    }

    fun isKarting(player: Player, checkIfValid: Boolean = true): Boolean {
        return karts.any { (!checkIfValid || it.isValid()) && it.player == player }
    }

    fun kartForPlayer(player: Player): Kart? = karts.firstOrNull { it.isValid() && it.player == player }
    fun kartForPassenger(player: Player): Kart? =
        karts.firstOrNull { kart -> kart.seats.any { seat -> seat.entity?.passengers?.contains(player) == true } }

    fun addKart(kart: Kart) {
//        Logger.debug("Adding kart ${kart.player.name}")
        karts.add(kart)
        kart.start()
    }

    fun kartPropertiesFromConfig(id: String, meta: OwnableItemMetadata? = null): KartProperties? {
        val item = validKarts[id] ?: return null
        return kartPropertiesFromConfig(item, meta)
    }

    private fun KartOptions.type(): KartProperties.Type {
        return type ?: extends?.let { validKarts[it]?.type() } ?: KartProperties.Type.KART
    }

    fun kartPropertiesFromConfig(option: KartOptions, meta: OwnableItemMetadata? = null): KartProperties {
        option.requireValid()
        val type = option.type()
        val brakes = option.resolveBrakes(config).requireValid()
        val tires = option.resolveTires(config).requireValid()
        val engine = option.resolveEngine(config).requireValid()
        val handling = option.resolveHandling(config).requireValid()
        val steer = option.resolveSteer(config).requireValid()
        val zeppelinLifter = option.resolveZeppelinLifter(config)
        val planeLifter = option.resolvePlaneLifter(config)

        val physicsControllerClass = PhysicsController.REGISTER[handling.physicsControllerId.force]
            ?: throw IllegalStateException("PhysicsController '${handling.physicsControllerId.force}' not found")

        val wheels = option.wheels.orElse()?.map {
            KartProperties.WheelConfig(
                source = it,
                matrix = Matrix4x4().apply {
                    translate(it.position.force)
                    rotateY((if (it.isLeftSide.orElse() == true) 0.0 else 180.0))
                },
                model = it.model.orElse()?.let { ItemStackUtils.fromString(it) },
                isLeftSide = it.isLeftSide.force,
                hasBrakes = it.hasBrakes.force,
                radius = it.radius.orElse(),
                isSteered = it.isSteered.orElse() ?: false,
                steerAngle = it.steerAngle.orElse(),
                forceCustomParticle = it.forceCustomParticle.orElse() ?: false
            )
        } ?: emptyList()
        val seats = option.seats.force.map {
            KartSeat(
                passengerSeat = true,
                enterPermission = it.enterPermission.orElse(),
                matrix = Matrix4x4().apply {
                    translate(it.position.force)
                    rotateYawPitchRoll(it.pitch.orElse() ?: 0f, it.yaw.orElse() ?: 0f, 0f)
                },
                parentBone = it.parentBone.orElse(),
                shouldPlayerBeInvisible = it.shouldPlayerBeInvisible?.orElse() ?: false,
//                compensateForEyeHeight = it.compensateForEyeHeight?.orElse() ?: false,
                allowItems = it.allowItems?.orElse() ?: true,
            )
        }
        val color = option.colors.orElse()?.firstOrNull()?.defaultColor?.orElse()?.let { Integer.decode(it) }
        val models = (option.legacyModelConfig?.orElse()?.let {
            val item = it.model?.orElse()?.let { ItemStackUtils.fromString(it) }
            if (item != null) {
                if (meta != null) {
                    ItemStackLoader.apply(item, meta)
                } else if (color != null) {
                    item.setColor(Color.fromRGB(color))
                }
            }
            listOf(
                KartNpc(
                    id = it.id,
                    model = item,
                    useHeadRotation = it.useHeadRotation?.orElse() ?: false,
                    entityType = it.entityType.force,
                    matrix = Matrix4x4().apply {
                        translate(it.position.force)
                    },
                    parentBone = it.parentBone.orElse(),
                )
            )
        } ?: listOf()) + (option.legacyModelConfig?.orElse()?.sub?.map {
            KartNpc(
                id = it.id,
                model = it.model?.orElse()?.let { ItemStackUtils.fromString(it) },
                useHeadRotation = it.useHeadRotation?.orElse() ?: false,
                entityType = it.entityType.force,
                matrix = Matrix4x4().apply {
                    translate(it.position.force)
                },
                parentBone = it.parentBone.orElse(),
            )
        } ?: listOf())

//        Logger.debug(
//            "exit=${option.exitAction.orElse()}/${option.exitAction.orElse()?.type}} action=${
//            option.exitAction.orElse()?.toAction()
//            }}"
//        )

        return KartProperties(
            boundingBox = option.boundingBox.force,
            type = type,
            brakes = brakes,
            tires = tires,
            engine = engine,
            handling = handling,
            steer = steer,
            zeppelinLifter = zeppelinLifter,
            planeLifter = planeLifter,
            physicsController = physicsControllerClass,
            seats = seats.toTypedArray(),
            kartNpcs = models.toTypedArray(),
            wheels = wheels.toTypedArray(),
            leftClickAction = option.leftClickAction.orElse()?.toAction(),
            rightClickAction = option.rightClickAction.orElse()?.toAction(),
            addons = option.addons.orElse()?.toSet()?.mapNotNull {
                KartAddon.REGISTER[it]?.constructors?.firstOrNull { it.parameterCount == 0 }
                    ?.newInstance() as? KartAddon
            }?.toSet() ?: emptySet(),
            exitHandler = option.exitAction.orElse()?.toAction(),
            armature = option.armatureName.orElse()?.let { armatures[it]!! } ?: Armature(
                "Armature",
                "Armature",
                joints = mutableListOf(
                    Joint(
                        id = "Armature_Bone",
                        name = "Bone",
                        transform = Matrix4x4().apply {
                            set(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
                        },
                    )
                ),
                transform = Matrix4x4(),
            )
        )
    }

    fun isSafeLocation(player: Player): Boolean {
        val boundingBox = BoundingBox(
            -1.0, 0.0, -1.0,
            1.0, 2.0, 1.0
        )

        val calcBoundingBox = BoundingBox()
        val location = player.location
        val world = location.world

        var xMin = Math.floor(location.x + boundingBox.xMin).toInt()
        var yMin = Math.floor(location.y + boundingBox.yMin).toInt()
        var zMin = Math.floor(location.z + boundingBox.zMin).toInt()

        var xMax = Math.floor(location.x + boundingBox.xMax).toInt()
        var yMax = Math.floor(location.y + boundingBox.yMax).toInt()
        var zMax = Math.floor(location.z + boundingBox.zMax).toInt()

        //region Check for free area
        for (x in xMin..xMax) for (y in yMin..yMax) for (z in zMin..zMax) {
            val blockBoundingBox = world!!.getBoundingBoxForBlock(x, y, z)
            if (blockBoundingBox != null) {
                calcBoundingBox.set(blockBoundingBox)

                val intersectsX = boundingBox.xIntersectsWithXOf(location.x, x.toDouble(), calcBoundingBox)
                val intersectsY = boundingBox.yIntersectsWithYOf(location.y, y.toDouble(), calcBoundingBox)
                val intersectsZ = boundingBox.zIntersectsWithZOf(location.z, z.toDouble(), calcBoundingBox)

                if (intersectsX && intersectsY && intersectsZ) {
//                    Logger.console("Failed free check")
                    return false
                }
            }
        }
        //endregion

        val groundHeightCheck = 2.0
        xMin = Math.floor(location.x + boundingBox.xMin).toInt()
        yMin = Math.floor(location.y - groundHeightCheck).toInt()
        zMin = Math.floor(location.z + boundingBox.zMin).toInt()

        xMax = Math.floor(location.x + boundingBox.xMax).toInt()
        yMax = Math.floor(location.y).toInt()
        zMax = Math.floor(location.z + boundingBox.zMax).toInt()

        //region Check for being at most 2 blocks above the ground
        for (x in xMin..xMax) for (y in yMin..yMax) for (z in zMin..zMax) {
            val blockBoundingBox = world!!.getBoundingBoxForBlock(x, y, z)
            if (blockBoundingBox != null) {
                calcBoundingBox.set(blockBoundingBox)

                val intersectsX = boundingBox.xIntersectsWithXOf(location.x, x.toDouble(), calcBoundingBox)
                val intersectsY =
                    boundingBox.yIntersectsWithYOf(location.y - groundHeightCheck, y.toDouble(), calcBoundingBox)
                val intersectsZ = boundingBox.zIntersectsWithZOf(location.z, z.toDouble(), calcBoundingBox)

                if (intersectsX && intersectsY && intersectsZ) {
//                    Logger.console("Failed ground check")
                    return true
                }
            }
        }
        //endregion

//        Logger.console("Failed all")
        return false
    }

    @JvmOverloads
    @Throws(IllegalStateException::class)
    fun startKarting(
        player: Player,
        kartProperties: KartProperties,
        kartOwner: KartOwner = DefaultPlayerKartOwner(player),
        location: Location = player.location,
        spawnType: SpawnType,
        applySafetyCheck: Boolean = false,
        debugBoundingBox: Boolean = false,
        parkable: Boolean = false
    ): Kart {
        if (spawnType == SpawnType.User || spawnType == SpawnType.Command) {
            if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.KART_SPAWN_AS_USER)) {
                throw IllegalStateException("Karts have been temporarily disabled")
            }

            val result = player.isAllowedToManuallySpawnKart()
            when (result) {
                Allow -> {}
                is Deny -> {
                    throw IllegalStateException(result.reason)
                }
            }
        }
        val existingKart = kartForPlayer(player)
        if (existingKart != null) {
            if (!existingKart.requestDestroy())
                throw IllegalStateException("You are already in a kart!")
        } else if (player.isInsideVehicle)
            throw IllegalStateException("You can't start karting while riding something else!")

        if (applySafetyCheck) {
            if (AreaConfigManager.getInstance().isKartingBlocked(location)) {
                throw IllegalStateException("Using karts is not enabled in this area")
            }
            val event = KartStartEvent(player)
            Bukkit.getPluginManager().callEvent(event)
            if (event.isCancelled)
                throw IllegalStateException("Usage of karts is currently not allowed for you or at this location, try it somewhere else or, if possible, leave your current minigame/lobby")
        }

        player.isSneaking = false
        player.fireTicks = 0
        val controller = DefaultKartController()
        val kart = Kart(
            player,
            controller,
            kartProperties,
            kartOwner,
            location,
            debugBoundingBox = debugBoundingBox,
            parkable = parkable
        )
        addKart(kart)
        return kart
    }

    enum class SpawnType {
        User,
        Command,
        Minigame,
    }
}
