package net.craftventure.core.utils

import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.database.type.OperatorKind
import org.bukkit.entity.Player


object OperatorUtils {
    fun isAllowedToOperate(operableRide: OperableRide, player: Player): Boolean {
        val ride = operableRide.ride
        if (ride != null) {
            if (ride.operatorKind == OperatorKind.OWNER && PermissionChecker.isOwner(player)) {
                return true
            } else if (ride.operatorKind == OperatorKind.CREW && PermissionChecker.isCrew(player)) {
                return true
            } else if (ride.operatorKind == OperatorKind.VIP && PermissionChecker.isVIP(player)) {
                return true
            } else if (ride.operatorKind == OperatorKind.ANYONE) {
                return true
            }
        }
        return false
    }

    fun isBeingOperated(instance: Any): Boolean {
        return (instance as? OperableRide)?.isBeingOperated ?: false
    }

    fun getOperatorForSlot(instance: Any, slot: Int): Player? {
        return if (instance is OperableRide) {
            instance.getOperatorForSlot(slot)
        } else null
    }
}
