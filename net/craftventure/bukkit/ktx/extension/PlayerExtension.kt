package net.craftventure.bukkit.ktx.extension

import net.craftventure.bukkit.ktx.util.PermissionChecker
import org.bukkit.permissions.Permissible

fun Permissible.isResettableAccount(): Boolean = PermissionChecker.isJoey(this)
fun Permissible.isJoey(): Boolean = PermissionChecker.isJoey(this)
fun Permissible.isVIP(): Boolean = PermissionChecker.isVIP(this)
fun Permissible.isPayedVIP(): Boolean = PermissionChecker.isPayedVIP(this)
fun Permissible.isYouTuber(): Boolean = PermissionChecker.isYouTuber(this)
fun Permissible.isCrew(): Boolean = PermissionChecker.isCrew(this)
fun Permissible.isBuilder(): Boolean = PermissionChecker.isBuilder(this)
fun Permissible.isOwner(): Boolean = PermissionChecker.isOwner(this)
fun Permissible.isTrusted(): Boolean = PermissionChecker.isTrusted(this)