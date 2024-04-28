package net.craftventure.bukkit.ktx.util

import net.craftventure.core.ktx.util.Permissions
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible
import java.util.*

object PermissionChecker {
    private val uuidJoeywp = UUID.fromString("43941a19-b78e-427d-adb9-fd5b57f44e29")

    fun isJoey(permissible: Permissible): Boolean = permissible is Player && permissible.uniqueId == uuidJoeywp

    fun isTrusted(permissible: Permissible): Boolean = permissible.hasPermission(Permissions.TRUSTED)

    fun isVIP(permissible: Permissible): Boolean = permissible.hasPermission(Permissions.VIP)

    fun isPayedVIP(permissible: Permissible): Boolean = permissible.hasPermission(Permissions.PAYED_VIP)

    fun isCrew(permissible: Permissible): Boolean = permissible.hasPermission(Permissions.CREW)

    fun isYouTuber(permissible: Permissible): Boolean = permissible.hasPermission(Permissions.YOUTUBE)

    fun isBuilder(permissible: Permissible): Boolean = permissible.hasPermission(Permissions.CREW)

    fun isOwner(permissible: Permissible): Boolean = permissible.hasPermission(Permissions.OWNER)

    fun isResettable(permissible: Permissible): Boolean = permissible.hasPermission(Permissions.RESETTABLE)
}