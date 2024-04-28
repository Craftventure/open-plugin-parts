package net.craftventure.core.ride.shooter.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata
import org.bukkit.entity.EntityType
import org.bukkit.util.BoundingBox

@JsonClass(generateAdapter = true)
data class ShooterHitboxConfig(
    val boundingBoxEntityOverride: EntityType? = null,
    val width: Double? = null,
    val height: Double? = null,
) {
    fun boundingBox(): BoundingBox? {
        boundingBoxEntityOverride?.let {
            return EntityMetadata.getBoundingBox(it)
        }
        if (width != null && height != null) {
            return BoundingBox(
                -width,
                0.0,
                -width,
                width,
                height,
                width
            )
        }
        return null
    }
}

