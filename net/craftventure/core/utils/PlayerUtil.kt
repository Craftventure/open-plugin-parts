package net.craftventure.core.utils

import net.craftventure.core.manager.GameModeManager
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*


object PlayerUtil {
    fun setFlying(player: Player, flying: Boolean) {
        if (flying) {
            //            player.setVelocity(new Vector(0, 0.05, 0));
            player.allowFlight = true
            player.isFlying = true
            player.flySpeed = GameModeManager.defaultFlySpeed
        } else {
            player.isFlying = false
            player.allowFlight = false
        }
    }

    operator fun get(nameOrUuid: String, allowDatabaseLookups: Boolean): UUID? {
        val player = Bukkit.getPlayer(nameOrUuid)
        if (player != null)
            return player.uniqueId
        try {
            return UUID.fromString(nameOrUuid)
        } catch (e: Exception) {
            if (allowDatabaseLookups) {
                val guestStat = MainRepositoryProvider.guestStatRepository.getByNameOrUuid(nameOrUuid)
                if (guestStat != null)
                    try {
                        return guestStat.uuid
                    } catch (e1: Exception) {
                        e1.printStackTrace()
                    }

                //            e.printStackTrace();
            }
        }

        return null
    }

    fun getName(uuid: UUID, allowDatabaseLookups: Boolean): String {
        val player = Bukkit.getPlayer(uuid)
        if (player != null)
            return player.name

        if (allowDatabaseLookups) {
            val guestStat = MainRepositoryProvider.guestStatRepository.findSilent(uuid)
            if (guestStat != null) {
                return guestStat.lastKnownName!!
            }
        }
        return uuid.toString()
    }
}
