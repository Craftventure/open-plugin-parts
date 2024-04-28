package net.craftventure.core.metadata

import com.comphenix.packetwrapper.WrapperPlayClientSettings
import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BaseMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.receivePacket
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket
import org.bukkit.entity.Player


class ClientSettingsMetadata(
    val owner: Player
) : BaseMetadata() {
    private var packet: WrapperPlayClientSettings? = null
    private var sending = false

    override fun debugComponent() = Component.text("sending=$sending")

    @Synchronized
    fun update(packet: WrapperPlayClientSettings) {
        if (sending) return
        this.packet = packet
    }

    fun resend() {
        val packet = this.packet ?: return
        val shouldHideCape = owner.isInsideVehicle
        val nmsPacket = packet.handle.handle as ServerboundClientInformationPacket
        val newPacket = if (shouldHideCape) {
            ServerboundClientInformationPacket(
                nmsPacket.language,
                nmsPacket.viewDistance,
                nmsPacket.chatVisibility,
                nmsPacket.chatColors,
                nmsPacket.modelCustomisation and 0x1.inv(),
                nmsPacket.mainHand,
                nmsPacket.textFilteringEnabled,
                nmsPacket.allowsListing,
            )
        } else {
            nmsPacket
        }
//        Logger.debug("Receiving with cape=${newPacket.displayedSkinParts.toString(16)} shouldhide=$shouldHideCape for ${owner.name}")
        try {
            sending = true
            owner.receivePacket(newPacket)
//            ProtocolLibrary.getProtocolManager().recieveClientPacket(owner, newPacket.handle)
            sending = false
        } catch (e: Exception) {
            sending = false
        }
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { ClientSettingsMetadata(player) }
    }
}