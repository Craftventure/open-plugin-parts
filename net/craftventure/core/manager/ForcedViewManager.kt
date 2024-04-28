package net.craftventure.core.manager

import com.comphenix.packetwrapper.WrapperPlayServerPosition
import net.craftventure.bukkit.ktx.extension.isDisconnected
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.util.Logger
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object ForcedViewManager {
    private var scheduledFuture: ScheduledFuture<*>? = null
    private val specs = ConcurrentHashMap<Player, ForcedViewSpecs>()
    private val fps = 60

    private val teleportFlagsWithoutRotation = setOf(
        WrapperPlayServerPosition.PlayerTeleportFlag.X,
        WrapperPlayServerPosition.PlayerTeleportFlag.Y,
        WrapperPlayServerPosition.PlayerTeleportFlag.Z,
        WrapperPlayServerPosition.PlayerTeleportFlag.X_ROT,
        WrapperPlayServerPosition.PlayerTeleportFlag.Y_ROT
    )

    private val teleportFlagsWithRotation = setOf(
        WrapperPlayServerPosition.PlayerTeleportFlag.X,
        WrapperPlayServerPosition.PlayerTeleportFlag.Y,
        WrapperPlayServerPosition.PlayerTeleportFlag.Z
    )

    private val teleportFlagsWithoutPitch = setOf(
        WrapperPlayServerPosition.PlayerTeleportFlag.X,
        WrapperPlayServerPosition.PlayerTeleportFlag.Y,
        WrapperPlayServerPosition.PlayerTeleportFlag.Z,
        WrapperPlayServerPosition.PlayerTeleportFlag.X_ROT
    )

    private val teleportFlagsWithoutYaw = setOf(
        WrapperPlayServerPosition.PlayerTeleportFlag.X,
        WrapperPlayServerPosition.PlayerTeleportFlag.Y,
        WrapperPlayServerPosition.PlayerTeleportFlag.Z,
        WrapperPlayServerPosition.PlayerTeleportFlag.Y_ROT
    )

    fun init() {
        scheduledFuture = CraftventureCore.getScheduledExecutorService().scheduleAtFixedRate({
            try {
                update()
            } catch (e: Exception) {
                Logger.capture(e)
            }
        }, 0, 1000 / fps.toLong(), TimeUnit.MILLISECONDS)
    }

    fun destroy() {
        scheduledFuture!!.cancel(true)
    }

    private fun update() {
        val iterator = specs.iterator()
        for (entry in iterator) {
            val key = entry.key
            val value = entry.value

            if (key.isDisconnected() || !key.isInsideVehicle) {
                iterator.remove()
                specs.remove(key)
                continue
            }

            val playerYaw = value.currentYaw % 360//key.location.yaw % 360
            val targetYaw = value.targetYaw

            var yawDifference = targetYaw - playerYaw
            while (yawDifference < -180) yawDifference += 360
            while (yawDifference > 180) yawDifference -= 360

            val playerPitch = key.location.pitch % 360
            val targetPitch = value.targetPitch

            val pitchDifference = targetPitch - playerPitch

            val updateYaw = yawDifference.absoluteValue > value.yawFreedom
            val updatePitch = pitchDifference.absoluteValue > value.pitchFreedom
            if (updateYaw || updatePitch) {
                val packet = WrapperPlayServerPosition()
                packet.flags = when {
                    updateYaw && updatePitch -> teleportFlagsWithRotation
                    updateYaw -> teleportFlagsWithoutPitch
                    updatePitch -> teleportFlagsWithoutYaw
                    else -> teleportFlagsWithoutRotation
                }
                packet.yaw = 0f
                packet.pitch = 0f
                if (updateYaw) {
                    val maxDegreeUpdate = value.maxDregreePerSecond / (fps.toFloat())
                    val update = yawDifference.clamp(-maxDegreeUpdate, maxDegreeUpdate)

                    if (Math.abs(update) > value.yawFreedom)
                        value.currentYaw += update
                }
                if (updatePitch) {
                    val maxDegreeUpdate = value.maxDregreePerSecond / (fps.toFloat())
                    val update = pitchDifference.clamp(-maxDegreeUpdate, maxDegreeUpdate)

                    if (Math.abs(update) > value.pitchFreedom)
                        value.currentPitch += update
                }
                while (value.currentYaw < -180) value.currentYaw += 360
                while (value.currentYaw > 180) value.currentYaw -= 360
                if (updateYaw)
                    packet.yaw = value.currentYaw

//                while (value.currentPitch < -180) value.currentPitch += 360
//                while (value.currentPitch > 180) value.currentPitch -= 360
                if (updatePitch) {
//                    Logger.info("${value.targetPitch.format(2)} ${pitchDifference.format(2)} ${value.currentPitch.format(2)}")
                    packet.pitch = value.currentPitch
                }

//                value.currentYaw += packet.yaw
//                while (value.currentYaw < -180) value.currentYaw += 360
//                while (value.currentYaw > 180) value.currentYaw -= 360
//                packet.yaw = value.currentYaw

//                Logger.info("yaw=${packet.yaw.format(2)}")

//                Logger.info("$updateYaw " +
//                        "player=${playerYaw.format(2)} " +
//                        "target=${targetYaw.format(2)} " +
//                        "diff=${yawDifference.format(2)} " +
//                        "delta=${packet.yaw.format(2)}")
                try {
                    packet.sendPacket(key)
                } catch (e: Exception) {
                }
            } else {
//                Logger.info("${playerYaw.format(2)} ${targetYaw.format(2)} ${yawDifference.format(2)}")
            }
        }
    }

    fun set(
        player: Player,
        targetYaw: Float,
        targetPitch: Float,
        maxDregreePerSecond: Float,
        yawFreedom: Float,
        pitchFreedom: Float
    ) {
//        Logger.info("Setting ${targetYaw.format(2)} ${targetPitch.format(2)}")
        val value = specs[player]
        if (value === null) {
            specs[player] = ForcedViewSpecs(
                targetYaw % 360,
                targetPitch,
                maxDregreePerSecond,
                yawFreedom % 360,
                pitchFreedom,
                player.location.yaw,
                player.location.pitch
            )
        } else {
            value.apply {
                this.targetYaw = targetYaw % 360
                this.targetPitch = targetPitch
                this.maxDregreePerSecond = maxDregreePerSecond
                this.yawFreedom = yawFreedom % 360
                this.pitchFreedom = pitchFreedom
//                this.currentYaw = player.location.yaw
            }
        }
    }

    fun remove(player: Player) {
        specs.remove(player)
    }

    data class ForcedViewSpecs(
        var targetYaw: Float,
        var targetPitch: Float,
        var maxDregreePerSecond: Float,
        var yawFreedom: Float,
        var pitchFreedom: Float,
        var currentYaw: Float,
        var currentPitch: Float
    )
}
