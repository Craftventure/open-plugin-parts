package net.craftventure.core.feature.dressingroom

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.CombinedArea
import net.craftventure.core.npc.actor.RecordingEquipment
import net.craftventure.core.npc.json.EntityInteractorJson
import net.craftventure.database.type.EquippedItemSlot
import net.craftventure.database.type.ItemType
import org.bukkit.Location
import org.bukkit.entity.EntityType
import kotlin.math.max

@JsonClass(generateAdapter = true)
class DressingRoomDto(
    val shop: String,
    val previewSlots: Set<EquippedItemSlot> = setOf(
        EquippedItemSlot.HELMET,
        EquippedItemSlot.CHESTPLATE,
        EquippedItemSlot.LEGGINGS,
        EquippedItemSlot.BOOTS,
        EquippedItemSlot.COSTUME,
        EquippedItemSlot.HANDHELD,
    ),
    val items: Set<String> = emptySet(),
    val area: Array<Area.Json>,
    val intro: SimpleAnimationDto,
    val outro: SimpleAnimationDto,
    val cameraLocations: CameraLocationsDto,
    val previewLocation: Location,
    val balloonLocation: Location? = null,
    val leaveLocation: Location,
    val itemDisplays: Array<ItemDisplayDto> = emptyArray(),
    val npcs: Array<NpcData> = emptyArray(),
    val npcVisibilityArea: Array<Area.Json> = emptyArray(),
    val playerPreviewMountId: String? = null,
    val type: Type = Type.DressingRoom,
) {
    @Transient
    val triggerAreaCombined = CombinedArea(*area.map { it.create() }.toTypedArray())

    enum class Type {
        DressingRoom,
        Barber,
//        BiggiesCustoms,
    }

    @JsonClass(generateAdapter = true)
    data class NpcData(
        val id: String,
        val location: Location,
        val entityType: EntityType,
        val equipment: RecordingEquipment? = null,
        val metadata: List<EntityInteractorJson<Any>> = emptyList(),
    )

    @JsonClass(generateAdapter = true)
    data class SimpleAnimationDto(
        val duration: Double? = null,
        val player: List<TimedLocationDto>,
        val camera: List<TimedLocationDto>,
    ) {
        @Transient
        val durationInMs = (max(
            max(
                player.maxOf { it.at },
                camera.maxOf { it.at },
            ),
            duration ?: 0.0
        ) * 1000).toLong()
    }

    @JsonClass(generateAdapter = true)
    data class TimedLocationDto(
        val location: Location,
        val at: Double,
    )

    @JsonClass(generateAdapter = true)
    data class CameraLocationsDto(
        val default: ViewpointDataDto,
        val hat: ViewpointDataDto?,
    )

    @JsonClass(generateAdapter = true)
    data class ViewpointDataDto(
        val location: Location,
        val zoom: Int? = null,
    )

    @JsonClass(generateAdapter = true)
    data class ItemDisplayDto(
        val slot: String,
        val location: Location,
        val types: Set<ItemType>,
    )
}