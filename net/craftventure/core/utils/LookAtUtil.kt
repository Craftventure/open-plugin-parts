package net.craftventure.core.utils

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.util.Vector
import kotlin.math.atan2

object LookAtUtil {
    fun makePlayerLookAt(player: Player, target: Vector?) {
        val direction = player.eyeLocation.toVector().subtract(target!!).normalize()
        val x = direction.x
        val y = direction.y
        val z = direction.z
        // Now change the angle
        val changed = player.location.clone()
        changed.yaw = (180 - Math.toDegrees(Math.atan2(x, z))).toFloat()
        changed.pitch = (90 - Math.toDegrees(Math.acos(y))).toFloat()
        player.teleport(changed, PlayerTeleportEvent.TeleportCause.PLUGIN)
    }

    fun getYawToTarget(source: Vector, target: Vector): Double? {
        if (source == target) return null

        val direction = target.clone().subtract(source)
        return -Math.toDegrees(atan2(direction.x, direction.z))
    }

    @JvmStatic
    fun getYawPitchFromRadian(
        from: Vector,
        to: Vector,
        yawPitch: YawPitch
    ) {
        val dX = from.x - to.x
        val dY = from.y - to.y
        val dZ = from.z - to.z
        yawPitch.yaw = Math.atan2(dZ, dX)
        yawPitch.pitch = Math.atan2(Math.sqrt(dZ * dZ + dX * dX), dY) + Math.PI
    }

    @JvmStatic
    fun getYawPitchFromRadian(
        fromX: Double,
        fromY: Double,
        fromZ: Double,
        toX: Double,
        toY: Double,
        toZ: Double,
        yawPitch: YawPitch
    ) {
        val dX = fromX - toX
        val dY = fromY - toY
        val dZ = fromZ - toZ
        yawPitch.yaw = Math.atan2(dZ, dX)
        yawPitch.pitch = Math.atan2(Math.sqrt(dZ * dZ + dX * dX), dY)
    }

    class YawPitch(
        var yaw: Double = 0.0,
        var pitch: Double = 0.0,
    )
}