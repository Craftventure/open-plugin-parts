package net.craftventure.core.feature.kart

import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

data class KartSeat(
    var id: String = "default",
    var matrix: Matrix4x4,
    var shouldPlayerBeInvisible: Boolean,
    var allowItems: Boolean,
    var parentBone: String? = null,
    var passengerSeat: Boolean = true,
    var entityType: EntityType = EntityType.ARMOR_STAND,
    var enterPermission: String? = null,
    var matrixInterceptor: ((kart: Kart, matrix: Matrix4x4) -> Unit)? = null,
    var matrixPreInterceptor: ((kart: Kart, seat: Kart.Seat) -> Unit)? = null,
)