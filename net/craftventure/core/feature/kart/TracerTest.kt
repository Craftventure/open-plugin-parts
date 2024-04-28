package net.craftventure.core.feature.kart

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.spawn
import net.craftventure.core.utils.LookAtUtil
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import kotlin.math.floor

class TracerTest(val kart: Kart, val maxLength: Int = 25) {
    private val task = executeSync(1, 1, this::update)
    private var location = kart.location.clone()
    private val yawPitch = LookAtUtil.YawPitch()

    private val previousFrames = arrayOfNulls<ArmorStand>(maxLength)
    private var frameIndex = 0

    private fun update() {
        if (!kart.isValid()) {
            Bukkit.getScheduler().cancelTask(task)
            previousFrames.forEach { it?.remove() }
            return
        }

        val direction = kart.location.clone().subtract(location)
        val distance = direction.length()
        if (distance > 1) {
            val normalizedDirection = direction.normalize()
            LookAtUtil.getYawPitchFromRadian(location, kart.location, yawPitch)
            for (i in 0 until floor(distance).toInt()) {
                location.add(normalizedDirection)
                spawnNextFrame(location, Math.toDegrees(yawPitch.yaw) + 90, Math.toDegrees(yawPitch.pitch))
            }
        }
    }

    private fun spawnNextFrame(at: Vector, yaw: Double, pitch: Double) {
        val armorStand = at.toLocation(kart.world!!)
            .apply {
                this.yaw = yaw.toFloat()
                this.pitch = pitch.toFloat()
            }
            .add(0.0, -1.1, 0.0)
            .spawn<ArmorStand>()
        armorStand.isSilent = true
        armorStand.isVisible = false
        armorStand.setGravity(false)
        armorStand.headPose = EulerAngle(Math.toRadians(-pitch - 90), 0.0, 0.0)
        armorStand.setHelmet(MaterialConfig.KART_TRACER_TRACE)
        previousFrames[frameIndex]?.remove()
        previousFrames[frameIndex] = armorStand
        frameIndex++
        if (frameIndex >= maxLength) {
            frameIndex = 0
        }
    }
}