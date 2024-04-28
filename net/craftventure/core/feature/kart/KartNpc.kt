package net.craftventure.core.feature.kart

import net.craftventure.database.generated.cvdata.tables.pojos.CachedGameProfile
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

data class KartNpc(
    val id: String = "default",
    var matrix: Matrix4x4,
    var useHeadRotation: Boolean,
    var parentBone: String? = null,
    var model: ItemStack? = null,
    var entityType: EntityType = EntityType.ARMOR_STAND,
    var cachedGameProfile: CachedGameProfile? = null,
    var matrixInterceptor: ((kart: Kart, matrix: Matrix4x4) -> Unit)? = null,
    var matrixPreInterceptor: ((kart: Kart, seat: Kart.VisualFakeSeat) -> Unit)? = null,
)