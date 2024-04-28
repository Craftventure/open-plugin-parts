package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.util.PermissionChecker
import org.bukkit.GameMode
import org.bukkit.entity.Player

object GameModeManager {
    const val defaultFlySpeed = 0.1f
    const val defaultWalkSpeed = 0.2f

    @JvmStatic
    fun setDefaults(player: Player) {
        setDefaultGamemode(player)
        setDefaultFly(player)
    }

    @JvmStatic
    fun setDefaultGamemode(player: Player) {
        if (PermissionChecker.isCrew(player)) {
            player.gameMode = GameMode.CREATIVE
        } else {
            player.gameMode = GameMode.ADVENTURE
        }
    }

    @JvmStatic
    fun setDefaultWalkSpeed(player: Player) {
        player.walkSpeed = defaultWalkSpeed
    }

    @JvmStatic
    fun setDefaultFly(player: Player) {
        if (player.isVIP() || player.isCrew() || player.hasPermission("craftventure.fly.self")) {
            player.allowFlight = true
            player.isFlying = true
            player.flySpeed = defaultFlySpeed
        } else {
            player.allowFlight = false
            player.isFlying = false
        }
    }

    @JvmStatic
    fun setDefaultFlySpeed(player: Player) {
        player.flySpeed = defaultFlySpeed
    }
}
