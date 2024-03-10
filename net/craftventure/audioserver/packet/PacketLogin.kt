package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass
import net.craftventure.audioserver.ProtocolConfig

@JsonClass(generateAdapter = true)
class PacketLogin(
    var version: Int = ProtocolConfig.VERSION,
    var uuid: String,
    var auth: String
) : BasePacket(PacketID.LOGIN) {
}
