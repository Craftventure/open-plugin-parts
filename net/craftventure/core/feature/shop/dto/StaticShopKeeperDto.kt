package net.craftventure.core.feature.shop.dto

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.actor.RecordingEquipment
import net.craftventure.core.npc.json.EntityInteractorJson
import org.bukkit.Location
import org.bukkit.entity.EntityType

@JsonClass(generateAdapter = true)
class StaticShopKeeperDto(
    val id: String? = null,
    val location: Location,
    val entityType: EntityType,
    val offer: String? = null,
    val interactionRadius: Double?,
    val proximityRadius: Double?,
    val horizontalRotationSpeed: Double = 180.0,
    val verticalRotationSpeed: Double = 90.0,
    val equipment: RecordingEquipment? = null,
    val metadata: List<EntityInteractorJson<Any>> = emptyList(),
    val playerProfile: String? = null,
) {
    val proximityRadiusSquared = proximityRadius?.let { it * it }
}