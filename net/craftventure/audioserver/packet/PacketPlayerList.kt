package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class PacketPlayerList(
    val players: List<PlayerLocation>
) : BasePacket(PacketID.PLAYER_LOCATIONS) {

    @JsonClass(generateAdapter = true)
    class PlayerLocation(
        val uuid: String,
        val name: String,
        val x: Double?,
        val y: Double?,
        val z: Double?,
        val hidden: Boolean?
    ) {
        constructor(player: Player, hidden: Boolean = player.isSneaking) : this(
            player.uniqueId.toString(),
            player.name,
            player.location.x,
            player.location.y,
            player.location.z,
            hidden
        )
    }
}
