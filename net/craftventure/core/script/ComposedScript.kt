package net.craftventure.core.script

import net.craftventure.bukkit.ktx.extension.setYawPitchDegrees
import net.craftventure.core.ktx.extension.forEachAllocationless
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.script.fixture.ComposedScene
import net.craftventure.core.script.fixture.fountain.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import java.io.File
import java.util.*


class ComposedScript(private val scriptFile: File) : Script(), BackgroundService.Animatable {
    override var repeats: Boolean = false

    private var hasLoaded = false
    private var hasStarted = false

    private var composedScene: ComposedScene? = null
    private var startTime: Long = -1
    private var lastTime: Long = -1

    private var spawnedBlocks = LinkedList<Pair<WaterParticle, NpcEntity>>()

    override val isValid: Boolean
        get() = true

    @Throws(ScriptControllerException::class)
    override fun onLoad() {
        super.onLoad()
        composedScene = ComposedScene()
        try {
            composedScene!!.load(scriptFile)
//            Logger.info("Loaded ${composedScene?.fixtures?.size} fixtures")
        } catch (e: Exception) {
            e.printStackTrace()
            throw ScriptControllerException("Failed to load the composed script " + scriptFile.path, e)
        }

        hasLoaded = true
    }

    @Throws(ScriptControllerException::class)
    override fun onStart() {
        super.onStart()
        if (!hasLoaded) {
            scriptController?.stop()
            return
        }

        startTime = System.currentTimeMillis()
        lastTime = startTime

        BackgroundService.remove(this)
        BackgroundService.add(this)

        hasStarted = true
    }

    fun setStartTimeTo(newStart: Long) {
        startTime = newStart
        lastTime = startTime
    }

    @Throws(ScriptControllerException::class)
    override fun onStop() {
        super.onStop()
        BackgroundService.remove(this)
        hasStarted = false

        spawnedBlocks.forEachAllocationless {
            val entity = it.second
            scriptController?.npcEntityTracker?.removeEntity(entity)
        }
        spawnedBlocks.clear()
    }

    override fun onAnimationUpdate() {
        val now = System.currentTimeMillis()
        val previousTime = (lastTime - startTime) / 1000.0
        val time = (now - startTime) / 1000.0

        if (time > (composedScene?.settings?.value?.orElse()?.duration ?: 0.0) / 1000.0) {
            onStop()
            return
        }

        val unevenFrame = time % 0.1 > 0.05
//        Logger.info("Uneven $unevenFrame")

        composedScene?.fixtures?.forEachAllocationless { fixture ->
            val playingTimeline = fixture.getTimeline("play")
            if (playingTimeline != null) {
                if (playingTimeline.valueAt(time) < 1.0 && fixture !is SuperShooter) {
                    return@forEachAllocationless
                }
            }

            when (fixture) {
                is Shooter -> {
                    val pressureTimeline = fixture.getTimeline("pressure")

                    val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                    if (pressure > 0) {
                        if (pressure < 1 && unevenFrame)
                            return@forEachAllocationless

                        val particle = WaterParticle(
                            fixture.location.y,
                            Vector(fixture.location.x, fixture.location.y, fixture.location.z),
                            Vector(0.0, pressure, 0.0)
                        )
                        add(particle)
                    }
                }

                is SuperShooter -> {
                    val shootTime = fixture.getTimeline("shots")?.getFrameTimeBetween(previousTime, time)

                    if (shootTime != null) {
                        val pressureTimeline = fixture.getTimeline("pressure")
                        val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                        if (pressure > 0) {
                            if (pressure < 1 && unevenFrame)
                                return@forEachAllocationless

                            val height = fixture.getTimeline("height")?.valueAt(shootTime / 1000.0)
                            if (height != null) {
                                for (i in 0 until height.toInt()) {
                                    val particle = WaterParticle(
                                        fixture.location.y,
                                        Vector(fixture.location.x, fixture.location.y + i, fixture.location.z),
                                        Vector(0.0, pressure, 0.0)
                                    )
                                    add(particle)
                                }
                            }
                        }
                    }
                }

                is OarsmanJet -> {
                    val pressureTimeline = fixture.getTimeline("pressure")

                    val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                    if (pressure > 0) {
                        val yaw = fixture.getTimeline("heading")?.valueAt(time) ?: 0.0
                        val pitch = fixture.getTimeline("pitch")?.valueAt(time) ?: 0.0

                        val motion = Vector().setYawPitchDegrees(yaw, pitch).multiply(pressure)
                        if (motion.lengthSquared() < 1 * 1 && unevenFrame)
                            return@forEachAllocationless

                        val particle = WaterParticle(
                            fixture.location.y,
                            Vector(fixture.location.x, fixture.location.y, fixture.location.z),
                            motion
                        )
                        add(particle)
                    }
                }

                is LillyJet -> {
                    val pressureTimeline = fixture.getTimeline("pressure")

                    val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                    if (pressure > 0) {
                        if (pressure < 1 && unevenFrame)
                            return@forEachAllocationless

                        val rays = fixture.getTimeline("rays")?.valueAt(time) ?: 0.0
                        val pitch = fixture.getTimeline("pitch")?.valueAt(time) ?: 0.0

                        val angleOffset = (360.0 / rays) * 10
                        var currentYaw = angleOffset * time
                        val yawIncreasePerFountain = 360 / rays

                        for (ray in 0 until Math.ceil(rays).toInt()) {
                            val motion = Vector().setYawPitchDegrees(currentYaw, pitch).multiply(pressure)
                            val particle = WaterParticle(
                                fixture.location.y,
                                Vector(fixture.location.x, fixture.location.y, fixture.location.z),
                                motion
                            )
                            add(particle)

                            currentYaw += yawIncreasePerFountain
                        }
                    }
                }

                is Bloom -> {
                    val pressureTimeline = fixture.getTimeline("pressure")

                    val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                    if (pressure > 0) {
                        if (pressure < 1 && unevenFrame)
                            return@forEachAllocationless

                        val rays = fixture.getTimeline("rays")?.valueAt(time) ?: 0.0
                        val pitch = fixture.getTimeline("pitch")?.valueAt(time) ?: 0.0

                        var yaw = 0.0
                        val yawIncreasePerFountain = 360 / rays

                        for (ray in 0 until Math.ceil(rays).toInt()) {
                            val motion = Vector().setYawPitchDegrees(yaw, pitch).multiply(pressure)
                            add(
                                WaterParticle(
                                    fixture.location.y,
                                    Vector(fixture.location.x, fixture.location.y, fixture.location.z),
                                    motion
                                )
                            )
                            yaw += yawIncreasePerFountain
                        }
                    }
                }
            }
        }

        val blockIterator = spawnedBlocks.iterator()
        while (blockIterator.hasNext()) {
            val it = blockIterator.next()
            val particle = it.first
            val entity = it.second

            particle.update()

            if (particle.shouldRemove()) {
                scriptController?.npcEntityTracker?.removeEntity(entity)
                blockIterator.remove()
            }
        }

        lastTime = now
    }

    private fun add(particle: WaterParticle) {
        if (!hasStarted)
            return

//        Logger.info("Adding entity")
        val entity = createWaterEntity(particle.location)
        spawnedBlocks.add(particle to entity)
        scriptController?.npcEntityTracker?.addEntity(entity)
        entity.velocity(particle.motion)
    }

    private fun createWaterEntity(
        position: Vector,
        blockData: BlockData = Material.BLUE_CONCRETE.createBlockData()
    ): NpcEntity {
        val location = position.toLocation(Bukkit.getWorlds().first())
        location.x += composedScene?.settings?.value?.orElse()?.x ?: 0.0
        location.y += composedScene?.settings?.value?.orElse()?.y ?: 0.0
        location.z += composedScene?.settings?.value?.orElse()?.z ?: 0.0
        return NpcEntity(entityType = EntityType.FALLING_BLOCK, location = location).setBlockData(blockData)
    }

    private inner class WaterParticle(
        val startY: Double,
        val location: Vector,
        val motion: Vector
    ) {
        private var lastUpdate = System.currentTimeMillis()

        fun shouldRemove() = location.y < startY

        fun update() {
//            val percentage = ((System.currentTimeMillis() - lastUpdate) / 50.0).clamp(0.0, 1.0)
            while (System.currentTimeMillis() > lastUpdate + 50) {
                location.x += motion.x
                location.y += motion.y
                location.z += motion.z

//                motion.x *= 0.699999988079071
//                motion.y *= 0.699999988079071
//                motion.z *= -0.5

                motion.x *= 0.9800000190734863
                motion.y *= 0.9800000190734863
                motion.z *= 0.9800000190734863

                motion.y -= 0.04

                lastUpdate += 50
            }
        }
    }
}
