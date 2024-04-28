package net.craftventure.core.feature.finalevent

import com.squareup.moshi.JsonClass
import net.craftventure.core.utils.ItemStackUtils
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.inventory.EquipmentSlot

@JsonClass(generateAdapter = true)
class PlayerRecordingDto(
    val initialActions: List<PlayerRecordingActionDto>,
    val actions: List<TimedActionDto>,
)

sealed class PlayerRecordingActionDto {
    @JsonClass(generateAdapter = true)
    class Location(
        val location: org.bukkit.Location,
    ) : PlayerRecordingActionDto()

    @JsonClass(generateAdapter = true)
    class Pose(
        val pose: org.bukkit.entity.Pose,
    ) : PlayerRecordingActionDto()

    @JsonClass(generateAdapter = true)
    class PlayerAnimation(
        val animation: PlayerAnimationType,
    ) : PlayerRecordingActionDto()

    @JsonClass(generateAdapter = true)
    class Item(
        val slot: EquipmentSlot,
        val itemBase64: String?,
    ) : PlayerRecordingActionDto() {
        @delegate:Transient
        val item by lazy { ItemStackUtils.fromString(itemBase64) }
    }
}

@JsonClass(generateAdapter = true)
class TimedActionDto(
    val at: Long,
    val action: PlayerRecordingActionDto,
)