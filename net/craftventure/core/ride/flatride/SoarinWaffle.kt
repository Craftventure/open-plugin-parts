package net.craftventure.core.ride.flatride

import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.ride.trackedride.SplineHandle
import net.craftventure.core.ride.trackedride.SplineNode
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment
import net.craftventure.core.utils.EntityUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import java.lang.Math.cos
import java.lang.Math.sin
import java.util.concurrent.TimeUnit


class SoarinWaffle private constructor() : Flatride<SoarinWaffle.SoarinBench>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    "soarinwaffle",
    "ride_soarinwaffle"
) {
    private var railsOffset = 0.01
    private val position = Vector(0, 0, 0)
    private val stationSegment: SplinedTrackSegment

    private val rideDoor = arrayOf(
    )

    private val enterDoors = arrayOf(
    )
    private val exitDoors = arrayOf(
    )
    private val trapDoors = arrayOf(
    )

    private var lastStateChange: Long = 0L
    private var state = State.STARTING
        set(value) {
            field = value
            lastStateChange = System.currentTimeMillis()
        }
    private var frame = 0
    private var ticksInState = 0

    private var lastUpdateTime = 0L
//    private var fireworkSpawns = arrayOf<FireworkTrigger>()

    init {
        for (entity in area.loc1.world!!.entities) {
            if (area.isInArea(entity.location) && entity.customName == rideName) {
                entity.remove()
            }
        }

//        fireworkSpawns = loadFireworksConfig()

        cars.add(SoarinBench(57.0))

        stationSegment = SplinedTrackSegment("station", null)
        stationSegment.add(
        )
        stationSegment.length
        //        Logger.console("length " + stationSegment.getLength());

        updateCarts(true)
        rideDoor.forEach {
            it.block.open(true)
        }
    }

    override fun stop() {
        super.stop()

        railsOffset = 0.01

        trapDoors.forEach { it.block.open(true) }
        enterDoors.forEach { it.block.open(true) }
        exitDoors.forEach { it.block.open(true) }

        AudioServerApi.disable("soarin_waffle_onride")
        rideDoor.forEach {
            it.block.open(true)
        }
//        ScriptManager.stop("soarin", "ride")
    }

    override fun prepareStart() {
        super.prepareStart()
        lastUpdateTime = 0

        val time = System.currentTimeMillis()
        AudioServerApi.enable("soarin_waffle_onride")
        AudioServerApi.sync("soarin_waffle_onride", time)

        trapDoors.forEach { it.block.open(false) }
        enterDoors.forEach { it.block.open(false) }
        exitDoors.forEach { it.block.open(false) }

        for (soarinBench in cars) {
            soarinBench.syncMusic(time)
        }
        rideDoor.forEach {
            it.block.open(false)
        }
//        ScriptManager.start("soarin", "ride")
    }

    override fun onLeave(player: Player, vehicle: Entity?) {
        super.onLeave(player, vehicle)
        GameModeManager.setDefaults(player)
        AudioServerApi.remove("soarin_waffle_onride", player)
    }

    override fun provideRunnable(): FlatrideRunnable {
        state = State.STARTING
        frame = 0
        ticksInState = 0
        return object : FlatrideRunnable() {
            override fun updateTick() {
                update(startTime)
            }
        }
    }

    override fun removePlayer(player: Player, dismounted: Entity?) {
        super.removePlayer(player, dismounted)
        player.teleport(exitLocation)
    }

    fun update(startTime: Long) {
//        Logger.info("Tick")
        frame++
        ticksInState++

        if (frame > 2 || state == State.RIDING) {
            frame = 0
            val deltaTime = System.currentTimeMillis() - startTime

            val last = (lastUpdateTime / 1000.0)
            val now = (deltaTime / 1000.0)

//            Logger.info("Updating soarin with $last > $now")
//            Logger.debug(now.format(2), true)
//            fireworkSpawns.filter { it.spawnTime >= last && it.spawnTime < now }.map {
//                //                Logger.debug("Spawning fireworks for ${it.spawnTime.format(2)} ${it.atSeconds.format(2)} at $last > $now")
//                it.execute()
//            }

            lastUpdateTime = deltaTime

            if (state == State.STARTING) {
                if (now > BENCH_ANIMATION_TIME_SECONDS) {
                    ticksInState = 0
                    state = State.RIDING
                } else {
                    val t = (now / BENCH_ANIMATION_TIME_SECONDS).clamp(0.0, 1.0)
                    railsOffset = t * stationSegment.length
                }
            } else if (state == State.RIDING) {
                if (now >= TOTAL_RIDE_DURATION_SECONDS - BENCH_ANIMATION_TIME_SECONDS) {
                    ticksInState = 0
                    state = State.STOPPING
                }
            } else if (state == State.STOPPING) {
                if (now >= TOTAL_RIDE_DURATION_SECONDS) {
                    railsOffset = 0.01
                    updateCarts(true)
                    stop()
                    return
                } else {
                    val t =
                        (1 - ((now - (TOTAL_RIDE_DURATION_SECONDS - BENCH_ANIMATION_TIME_SECONDS)) / BENCH_ANIMATION_TIME_SECONDS)).clamp(
                            0.0,
                            1.0
                        )
                    railsOffset = t * stationSegment.length
                }
            }

            if (deltaTime > TimeUnit.MINUTES.toMillis(3) + TimeUnit.SECONDS.toMillis(35)) {
                railsOffset = 0.01
                updateCarts(true)
                stop()
                return
            }

            updateCarts(false)
        }
    }

    enum class State {
        STARTING, RIDING, STOPPING
    }

    override fun updateCarts(forceTeleport: Boolean) {
        for (soarinBench in cars) {
            stationSegment.getPosition(railsOffset, position)
            if (state == State.RIDING) {

                val nowSeconds = (System.currentTimeMillis() - flatrideRunnable.startTime) / 1000.0
                val t: Double = if (ticksInState < 20 * 3) {
                    ticksInState / (20 * 3.0)
                } else if (flatrideRunnable.startTime < TOTAL_RIDE_DURATION_SECONDS - BENCH_ANIMATION_TIME_SECONDS - 3.0) {
                    0.0
                } else {
                    (1 - ((nowSeconds - (TOTAL_RIDE_DURATION_SECONDS - BENCH_ANIMATION_TIME_SECONDS - 3.0)) / (BENCH_ANIMATION_TIME_SECONDS - 3.0))).coerceIn(
                        0.0,
                        1.0
                    )
                }

//                val deltaTime = System.currentTimeMillis() - startTime
//                val t =
//                    TOTAL_RIDE_DURATION_SECONDS - BENCH_ANIMATION_TIME_SECONDS
                soarinBench.teleport(
                    x = position.x,
                    y = position.y - 4.2,
                    z = position.z,
                    yaw = 0f,
                    pitch = (Math.toDegrees(cos(ticksInState * 0.01500) * 0.2).toFloat()) * t.toFloat(),
                    roll = (Math.toDegrees(sin(ticksInState * 0.0348) * 0.2).toFloat()) * t.toFloat(),
                    forceTeleport = forceTeleport
                )
            } else {
                soarinBench.teleport(position.x, position.y - 4.2, position.z, 0f, 0f, 0f, forceTeleport)
            }
        }
    }

//    override fun postLeave(player: Player) {
//        super.postLeave(player)
//        WornItemManager.update(player)
//    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    fun onFlyingChunkUnload(event: ChunkUnloadEvent) {
//        flying?.let { flying ->
//            val chunk = flying.location.chunk
//            if (event.chunk.x == chunk.x && event.chunk.z == chunk.z) {
//                event.isCancelled = true
//            }
//        }
//    }

    inner class SoarinBench(
        private val x: Double
    ) : FlatrideCar<ArmorStand>(arrayOfNulls(6)) {

        private val matrix = Matrix4x4()

        fun syncMusic(time: Long) {
            for (entity in entities) {
                val player = entity.passengers.firstOrNull() as? Player
                if (player != null)
                    AudioServerApi.addAndSync("soarin_waffle_onride", player, time)
            }
        }

        fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, roll: Float, forceTeleport: Boolean) {
//            logcat { "yaw=${yaw.format(2)} pitch=${pitch.format(2)} roll=${roll.format(2)}" }
            matrix.setIdentity()
            matrix.rotateYawPitchRoll(pitch, yaw, roll)
            teleport(x, y, z, yaw, pitch, forceTeleport)
        }

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            val ridePart = ticksInState / 30
//            val shaking = state == State.RIDING && ridePart % 6 == 0

            for (i in entities.indices) {
                val offset = matrix.transformPoint(Vector3((i - 2.5).toDouble(), 0.0, 0.0))
//                logcat { "Offset $i ${Vector(offset.x, offset.y, offset.z).asString()}" }
                if (entities[i] == null || !entities[i].isValid) {
                    entities[i] = area.loc1.world!!.spawn(
                        Location(
                            area.loc1.world,
                            this.x + offset.x,
                            y + offset.y,
                            z + offset.z,
                            0f,
                            0f
                        ), ArmorStand::class.java
                    )
                    entities[i]!!.isPersistent = false
                    entities[i].setOwner(this@SoarinWaffle)
                    entities[i].setGravity(false)
                    entities[i].setBasePlate(false)
                    entities[i].isVisible = false
                    entities[i].customName = rideName
                    entities[i].addDisabledSlots(*EquipmentSlot.values())
                    if (i == 3)
                        entities[i].setHelmet(MaterialConfig.SOARIN)
                } else {//if (entities[i].getPassenger() != null || forceTeleport) {
                    EntityUtils.teleport(
                        entities[i],
                        this.x + offset.x,
                        y + offset.y,// + (if (shaking) Math.random() * 0.25 else 0.0),
                        z + offset.z,
                        0f,
                        0f
                    )
                    //                    entities[i].setHeadPose(new EulerAngle((left ? -pitch : pitch) + Math.PI, 0, 0));
                }

                if (i == 3) {
                    val quat = TransformUtils.getArmorStandPose(matrix.rotation)
                    entities[i].headPose = EulerAngle(
                        Math.toRadians(quat.x),
                        Math.toRadians(quat.y),
                        Math.toRadians(quat.z)
                    )
                }

                if (forceModelUpdate) {
                    if (i == 3)
                        entities[i].setHelmet(MaterialConfig.SOARIN)
                }
            }
        }
    }

    companion object {
        private const val BENCH_ANIMATION_TIME_SECONDS = 5.0
        private const val TOTAL_RIDE_DURATION_SECONDS = 208.0
        private var _instance: SoarinWaffle? = null

        fun getInstance(): SoarinWaffle {
            if (_instance == null) {
                _instance = SoarinWaffle()
            }
            return _instance!!
        }
    }
}
