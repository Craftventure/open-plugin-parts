package net.craftventure.core.feature.shop.dto

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.util.BoundingBoxProducer
import net.craftventure.core.npc.json.EntityInteractorJson
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot

@JsonClass(generateAdapter = true)
class ShopItemDto(
    val id: String,
    val type: EntityType,
    val location: Location,
    val boundingBoxProducers: Array<BoundingBoxProducer> = arrayOf(BoundingBoxProducer.SquareSizedProducer(1.0, false)),
    val interactionRadius: Double? = null,
    val equipmentSlot: EquipmentSlot = EquipmentSlot.HEAD,
    val metadata: List<EntityInteractorJson<Any>> = emptyList(),
)