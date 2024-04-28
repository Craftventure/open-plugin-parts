package net.craftventure.core.ride.flatride

import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.animation.keyframed.DoubleValueKeyFrame
import net.craftventure.core.extension.getFirstPassenger
import net.craftventure.core.extension.hasPassengers
import net.craftventure.core.extension.spawn
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.isOwnedByRide
import net.craftventure.core.script.ScriptManager
import net.craftventure.core.script.action.PlaceSchematicAction
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.InterpolationUtils
import net.craftventure.core.utils.SimpleInterpolator
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import penner.easing.Cubic


class Madhouse private constructor() : Flatride<Madhouse.Bench>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    "madhouse",
    "ride_madhouse"
) {
    protected val rotationAxis = Location(Bukkit.getWorld("world"), )
    private val startTeleport = Location(Bukkit.getWorld("world"), )
    private val teleportArea = SimpleArea("world", )
    private var frame = 0

    private var ticksInState = 0

    private val angleFrames = ArrayList<DoubleValueKeyFrame>()
    private val angleFramesInterpolators = ArrayList<SimpleInterpolator>()
    private var heightFrameIndex = 0
    private var sceneIndex = 0
    private var interpolatedHeight = 0.0
    private var lastProgramTime: Long = 0
    private var isChangingGamemodes = false
    private var showController: ShowController
    private var isCamLocked = false
        set(value) {
            if (field != value) {
                pasteRoom(!value)
            }
            field = value
        }
    private val mainShowDoors = arrayOf(
        Location(Bukkit.getWorld("world"), 1),
        Location(Bukkit.getWorld("world"), 1),
        Location(Bukkit.getWorld("world"), 1),
        Location(Bukkit.getWorld("world"), 1),
        Location(Bukkit.getWorld("world"), 1),
        Location(Bukkit.getWorld("world"), 1)
    )
    private val preshowArea = SimpleArea("world", )
    private val preshowEnterDoors = arrayOf(
        Location(Bukkit.getWorld("world"), ),
        Location(Bukkit.getWorld("world"), )
    )
    private val preshowExitDoors = arrayOf(
        Location(Bukkit.getWorld("world"), ),
        Location(Bukkit.getWorld("world"), ),
        Location(Bukkit.getWorld("world"), ),
        Location(Bukkit.getWorld("world"), )
    )

    private fun pasteRoom(visible: Boolean) {
        if (visible) {
            //174, 40, -309
            PlaceSchematicAction("abbey", "code").apply {
                offsetX = 178
                offsetY = 40
                offsetZ = -290
                offsetIsAbsolute = true
                entities = false
            }.withName("platform_visible").noAir(false).execute(null)
        } else {
            PlaceSchematicAction("abbey", "code").apply {
                offsetX = 178
                offsetY = 40
                offsetZ = -290
                offsetIsAbsolute = true
                entities = false
            }.withName("platform_gone").noAir(false).execute(null)
        }
    }

    private val models = arrayOf(
        // Left side
        Model(Vector(), -90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 86)),
        Model(Vector(), -90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 87)),
        Model(Vector(), -90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 88)),
        // Right side
        Model(Vector(), -90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 91)),
        Model(Vector(), -90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 90)),
        Model(Vector(), -90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 89)),
        // Table
        Model(Vector(), 90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 89)),
        Model(Vector(), 90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 90)),
        Model(Vector(), 90f, MaterialConfig.dataItem(Material.DIAMOND_SWORD, 91))
    )
    private var benchAngle = 0.0

    init {
        for (entity in area.loc1.world!!.entities) {
            if (area.isInArea(entity.location) && entity.customName == rideName) {
                entity.remove()
            }
        }
        showController = ShowController()

        setupFrames()
        interpolatedHeight = angleFrames[0].value

        cars.clear()
        cars.add(Bench(Vector(), BlockFace.WEST))
        cars.add(Bench(Vector(), BlockFace.WEST))
        cars.add(Bench(Vector(), BlockFace.EAST))
        cars.add(Bench(Vector(), BlockFace.EAST))

        updateCarts(true)
        pasteRoom(true)
        for (location in mainShowDoors)
            location.block.open(true)

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            showController.update()
        }, 1, 1)
    }

    private fun setupFrames() {
        addFrame(0,0, Cubic::easeIn)
        addFrame(0,0, Cubic::easeIn)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)

        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)

        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
    }

    private fun addFrame(
        time: Double,
        angle: Double,
        interpolator: (a: Double, b: Double, c: Double, d: Double) -> Double
    ) {
        angleFrames.add(DoubleValueKeyFrame(toMillis(time), angle))
        angleFramesInterpolators.add(SimpleInterpolator { t, b, c, d -> interpolator(t, b, c, d) })
    }

    private fun toMillis(seconds: Double): Double = seconds * 1000

    override fun stop() {
        super.stop()
        heightFrameIndex = 0

        ScriptManager.stop("abbey", "ride")
        AudioServerApi.disable("abbey_onride")
        cars.forEach { it.stopSpectatorMode() }
//        rotationAxis.block.setLightLevel(13, BlockLightType.BLOCK)
        updateCarts(true)
        isCamLocked = false
        models.forEach { it.cleanup() }
        for (location in mainShowDoors)
            location.block.open(true)
    }

    override fun prepareStart() {
        super.prepareStart()
        updateCarts(true)
        lastProgramTime = 0
        sceneIndex = 0
        heightFrameIndex = 0

        ScriptManager.start("abbey", "ride")

        val time = System.currentTimeMillis()
        AudioServerApi.enable("abbey_onride")
        AudioServerApi.sync("abbey_onride", time)

        for (player in Bukkit.getOnlinePlayers()) {
            if (teleportArea.isInArea(player)) {
                val isInMadhouse = player.vehicle?.isOwnedByRide(this) == true
                if (!isInMadhouse) {
                    player.teleport(startTeleport, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    sendMoveAwayMessage(player)
                }
            }
        }
        for (location in mainShowDoors)
            location.block.open(false)
    }

    private fun switchToCamLockMode() {
        isChangingGamemodes = true
        cars.forEach {
            it.enterSpectatorMode()
        }
        isChangingGamemodes = false
        isCamLocked = true
    }

    override fun onLeave(player: Player, vehicle: Entity?) {
        super.onLeave(player, vehicle)
        GameModeManager.setDefaults(player)
        AudioServerApi.remove("madhouse_onride", player)

        if (vehicle is ArmorStand) {
            vehicle.setHelmet(ItemStack(Material.AIR))
        }
    }

    override fun provideRunnable(): FlatrideRunnable {
        frame = 0
        ticksInState = 0
        return object : FlatrideRunnable() {
            var frame = 0
            override fun updateTick() {
                frame++
                val programTime = System.currentTimeMillis() - startTime

                if (programTime > angleFrames[1].time) {
                    if (!isCamLocked) {
                        switchToCamLockMode()
                    }
                }

//                val lightTime = 2000
//                if (programTime < lightTime) {
//                    val light = ((1 - (programTime / lightTime.toDouble())) * 13).toInt()
//                    if (rotationAxis.block.lightLevel != light.toByte()) {
////                        Logger.info("Changing light to $light")
//                        rotationAxis.block.setLightLevel(light, BlockLightType.BLOCK)
//                    }
//                }

                if (update(angleFrames, heightFrameIndex, programTime))
                    heightFrameIndex++

                interpolatedHeight = interpolate(angleFrames, angleFramesInterpolators, heightFrameIndex, programTime)
                update(startTime)

                frame = 0

//                if (angleFrames[angleFrames.size - 1].time - lightTime < programTime) {
//                    val light =
//                        ((1 - ((angleFrames[angleFrames.size - 1].time - lightTime) / lightTime.toDouble())) * 13).toInt()
//                    if (rotationAxis.block.lightLevel != light.toByte()) {
////                        Logger.info("Changing light to $light")
//                        rotationAxis.block.setLightLevel(light, BlockLightType.BLOCK)
//                    }
//                }

                if (programTime > angleFrames[angleFrames.size - 1].time) {
                    updateCarts(true)
                    stop()
                    return
                }

                updateCarts(false)
                lastProgramTime = programTime
            }
        }
    }

    fun update(startTime: Long) {
//        Logger.info("Tick")
        frame++
        ticksInState++

        benchAngle = Math.toRadians(interpolatedHeight)
    }

    override fun eject() {
        super.eject()

        for (car in cars) {
            val entities = car.entities
            for (i in entities.indices) {
                if (entities[i] is ArmorStand) {
                    entities[i].setHelmet(ItemStack(Material.AIR))
                }
                if (car.players[i] != null) {
                    val player = car.players[i] ?: continue
                    onLeave(player, null)

                    car.players[i] = null
                    postLeave(player)
                    playerLeftRideCompleted(player)
                }
            }
        }
    }

    override fun handlesExit(player: Player, dismounted: Entity, car: Bench): Boolean {
        if (isChangingGamemodes) {
            return true
        }
        return super.handlesExit(player, dismounted, car)
    }

    override fun updateCarts(forceTeleport: Boolean) {
        if (isCamLocked)
            for (model in models) {
                model.update()
            }
        for (bench in cars) {
            bench.teleport(0.0, 0.0, 0.0, 0f, benchAngle.toFloat(), forceTeleport)
            if (isCamLocked)
                bench.validateSpectators()
        }
    }

    private fun interpolate(
        frames: List<DoubleValueKeyFrame>,
        interpolators: List<SimpleInterpolator>,
        index: Int,
        time: Long
    ): Double {
        if (frames.size > index + 1) {
            val current = frames[index]
            val next = frames[index + 1]
            val interpolator = interpolators[index]
            val t = interpolator.interpolate(
                (time - current.time).toFloat().toDouble(),
                0.0, 1.0, (next.time - current.time).toFloat().toDouble()
            )
            return InterpolationUtils.linearInterpolate(current.value, next.value, t)
        } else {
            return frames[index].value
        }
    }

    private fun update(frames: List<DoubleValueKeyFrame>, index: Int, time: Long): Boolean =
        frames.size > index + 1 && frames[index + 1].time <= time


    inner class Model(
        offsetIn: Vector,
        val yaw: Float,
        val modelItem: ItemStack
    ) {
        val offset = rotationAxis.toVector().subtract(offsetIn).subtract(Vector(0.0, 1.5, 0.0))
        var modelHolder: ArmorStand? = null
        val calculationVector = Vector()

        fun cleanup() {
            modelHolder?.remove()
            modelHolder = null
        }

        fun update() {
            calculationVector.set(offset)

            val x = rotationAxis.x - calculationVector.x
            val y = rotationAxis.y - calculationVector.y - 1.5
            val z = rotationAxis.z - calculationVector.z

            val pitch = 0f
            if (modelHolder == null || modelHolder?.isValid == false) {
                modelHolder = Location(rotationAxis.world, x, y, z, yaw, pitch).spawn()
                modelHolder!!.getOrCreateMetadata { TypedInstanceOwnerMetadata(ride = this@Madhouse) }
                modelHolder!!.setGravity(false)
                modelHolder!!.isInvulnerable = true
                modelHolder!!.isVisible = false
                modelHolder!!.setBasePlate(false)
                modelHolder!!.setHelmet(modelItem)
                modelHolder!!.customName = this@Madhouse.rideName
                modelHolder!!.addDisabledSlots(*EquipmentSlot.values())
            } else {
                modelHolder?.headPose = EulerAngle(if (yaw == 90f) benchAngle else -benchAngle, 0.0, 0.0)
            }
        }
    }

    inner class Bench(
        base: Vector,
        val facing: BlockFace
    ) : FlatrideCar<ArmorStand>(arrayOfNulls(8)) {
        val offset = rotationAxis.clone().subtract(base)
        val calculationVector = Vector()
        val locationCalculationVector = Vector()
        val players = arrayOfNulls<Player>(entities.size)
        private var spectatorMode = false

        fun stopSpectatorMode() {
            for (i in players.indices)
                players[i] = null
            spectatorMode = false
        }

        fun validateSpectators() {
            for (i in players.indices) {
                val player = players[i] ?: continue
//                Logger.info("Validating player ${players[i]?.name}")
                if (player.gameMode != GameMode.SPECTATOR || player.spectatorTarget !== entities[i] || !player.isConnected()) {
//                    Logger.info("${player.name} left madhouse")
                    onLeave(player, null)
                    players[i] = null
                    postLeave(player)
                }
            }
            spectatorMode = true
        }

        fun enterSpectatorMode() {
            for (i in entities.indices) {
                val armorStand = entities[i]
                val passenger = armorStand.getFirstPassenger() as? Player ?: continue
//                Logger.info("Preparing start for ${armorStand.getFirstPassenger()?.name}")

//                Logger.info("${passenger.name} entered madhouse")
                armorStand.setHelmet(passenger.playerProfile.toSkullItem())
                passenger.gameMode = GameMode.SPECTATOR
                passenger.spectatorTarget = armorStand
                players[i] = passenger
            }
        }

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            calculationVector.set(offset)
            calculationVector.rotateZ(pitch.toDouble() - Math.PI)

            val entityYaw = when (facing) {
                BlockFace.WEST -> 90f
                else -> -90f
            }
            val entityPitch = Math.toDegrees(
                when (facing) {
                    BlockFace.WEST -> pitch
                    else -> -pitch
                }.toDouble()
            ).toFloat()

            for (i in entities.indices) {
                val location = locationCalculationVector
                    .set(rotationAxis)
                    .add(calculationVector)
                    .apply {
                        this.z -= (i.toDouble() + 1)
                        if (i >= 2) this.z -= 1
                        if (i >= 6) this.z -= 1
                    }
                    .toLocation(area.world)
                    .apply {
                        this.yaw = entityYaw
                        this.pitch = entityPitch
//                            spawnParticle<Any>(Particle.END_ROD)

                        val hasSpectatorPlayer = (players.getOrNull(i)
                            ?: entities.getOrNull(i)?.getFirstPassenger() as? Player)?.gameMode == GameMode.SPECTATOR
                        if (!hasSpectatorPlayer) {
                            this.y -= 1.45
                        }
                        this.y -= 1.3
                    }

                if (entities[i] == null || !entities[i].isValid) {
                    entities[i] = location.spawn()
                    entities[i].getOrCreateMetadata { TypedInstanceOwnerMetadata(ride = this@Madhouse) }
                    entities[i].setGravity(false)
                    entities[i].setBasePlate(false)
                    entities[i].isVisible = false
                    entities[i].customName = this@Madhouse.rideName
                    entities[i].addDisabledSlots(*EquipmentSlot.values())
//                    entities[i].helmet = ItemStackUtils.createHead("Joeywp")
                    EntityUtils.teleport(entities[i], location)
                } else if (players[i] != null || entities[i].hasPassengers() || forceTeleport) {
                    EntityUtils.teleport(
                        entities[i],
                        location.x,
                        location.y,
                        location.z,
                        location.yaw,
                        location.pitch
                    )
                    entities[i].headPose = EulerAngle(Math.toRadians(entityPitch.toDouble()), 0.0, 0.0)
                } else if (players[i] == null && !entities[i].hasPassengers() && entities[i].location.y > 5) {
                    EntityUtils.teleport(entities[i], location.x, 5.0, location.z, location.yaw, location.pitch)
                }
            }
        }
    }

    protected inner class ShowController {
        private var state = ShowState.IDLE
        private var inStateSince = System.currentTimeMillis()
        private var gatheringPlayerStartingTime: Long = 0

        init {
            preshowEnterDoors.forEach {
                it.block.open(true)
            }
            preshowExitDoors.forEach {
                it.block.open(false)
            }
        }

        fun setState(newState: ShowState) {
            if (this.state != newState) {
//                Logger.info("Preshow state=$newState (was $state)")
                this.state = newState
                if (this.state == ShowState.IDLE) {
                    AudioServerApi.disable("abbey_preshow")
                }
                if (this.state != ShowState.PLAYING) {
                    stop()
                } else if (this.state == ShowState.PLAYING) {
                    start()
                }
                inStateSince = System.currentTimeMillis()

                preshowEnterDoors.forEach {
                    it.block.open(state == ShowState.IDLE)
                }
                preshowExitDoors.forEach {
                    it.block.open(state == ShowState.WAITING_FOR_EXIT)
                }
            }
        }

        fun update() {
            if (state == ShowState.IDLE) {
                var containsPlayers = false
                for (player in Bukkit.getOnlinePlayers()) {
                    val inArea = preshowArea.isInArea(player)
                    if (inArea) {
                        display(
        player,
        Message(
            id = ChatUtils.ID_RIDE,
            text = Component.text(
                                "Please wait for Van Lamsbergen to prepare his presentation",
                                CVTextColor.serverNotice
                            ),
            type = MessageBarManager.Type.RIDE,
            untilMillis = TimeUtils.secondsFromNow(1.0),
        ),
        replace = true,
    )
                        containsPlayers = true
                    }
                }
                if (containsPlayers) {
                    if (gatheringPlayerStartingTime == -1L) {
                        gatheringPlayerStartingTime = System.currentTimeMillis()
                    }
                } else {
                    gatheringPlayerStartingTime = -1
                }
                if (gatheringPlayerStartingTime > 0) {
                    if (gatheringPlayerStartingTime < System.currentTimeMillis() - 15000) {
                        gatheringPlayerStartingTime = -1
                        //                        Logger.console("Starting gonbao because a player was inside the area for 5 secs");
                        setState(ShowState.PLAYING)
                    }
                }
            } else if (state == ShowState.PLAYING) {
                if (inStateSince < System.currentTimeMillis() - 65000) {
                    setState(ShowState.WAITING_FOR_EXIT)
                }
            } else if (state == ShowState.WAITING_FOR_EXIT) {
                if (inStateSince < System.currentTimeMillis() - 15000) {
                    setState(ShowState.IDLE)
                }
            }
        }

        private fun start() {
            ScriptManager.stop("abbey", "preshow")
            ScriptManager.start("abbey", "preshow")
            AudioServerApi.enable("abbey_preshow")
            AudioServerApi.sync("abbey_preshow", System.currentTimeMillis())
        }

        private fun stop() {
//            ScriptManager.stop("abbey", "preshow")
//            AudioServerApi.disable("abbey_preshow")
        }
    }

    enum class ShowState {
        IDLE, PLAYING, WAITING_FOR_EXIT
    }

    companion object {
        private var instance: Madhouse? = null

        fun getInstance(): Madhouse {
            if (instance == null) {
                instance = Madhouse()
            }
            return instance!!
        }
    }
}