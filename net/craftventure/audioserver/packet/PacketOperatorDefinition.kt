package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PacketOperatorDefinition(
    val rides: List<Ride>
) : BasePacket(PacketID.OPERATOR_DEFINITION) {

    @JsonClass(generateAdapter = true)
    class Ride(
        val id: String,
        val controls: List<OperatorControlModel>,
        var name: String? = null,
        @Json(name = "operator_slots")
        val operatorSlots: MutableList<OperatorSlot> = mutableListOf(),
        @Json(name = "force_display")
        val forceDisplay: Boolean
    )

    @JsonClass(generateAdapter = true)
    class OperatorSlot(
        @Json(name = "ride_id")
        val rideId: String,
        val slot: Int,
        val uuid: String?,
        val name: String?
    )


    @JsonClass(generateAdapter = true)
    class OperatorControlModel(
        val id: String,
        val name: String?,
        @Json(name = "ride_id")
        val rideId: String?,
        val kind: String?,
        @Json(name = "is_enabled")
        val isEnabled: Boolean?,
        val data: Map<String, Any>?,
        val sort: Int?,
        val group: String?,
        @Json(name = "group_display")
        val groupDisplay: String?
    )
}
