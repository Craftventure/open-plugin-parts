package net.craftventure.core.protocol

import com.comphenix.packetwrapper.WrapperPlayServerAttachEntity
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.protocol.ProtocolHandler.addLeash
import net.craftventure.core.protocol.ProtocolHandler.removeLeash
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class ProtocolLeash(var entityId: Int, var ownerId: Int) {
    private val wrapperPlayServerAttachEntity: WrapperPlayServerAttachEntity
    var isCreated = false
        private set

    fun update(entityId: Int, ownerId: Int) {
//        Logger.console("Updating protocolleash");
        for (player in Bukkit.getOnlinePlayers()) {
            try {
                wrapperPlayServerAttachEntity.entityID = entityId
                wrapperPlayServerAttachEntity.vehicleId = -1
                wrapperPlayServerAttachEntity.sendPacket(player)

                wrapperPlayServerAttachEntity.entityID = entityId
                wrapperPlayServerAttachEntity.vehicleId = ownerId
                wrapperPlayServerAttachEntity.sendPacket(player)
            } catch (e: Exception) {
                capture(e)
            }
        }
        this.entityId = entityId
        this.ownerId = ownerId
    }

    fun `is`(spawnedEntityId: Int): Boolean {
        return spawnedEntityId == entityId || spawnedEntityId == ownerId
    }

    fun create() {
        if (!isCreated) {
//            Logger.console("Creating protocolleash");
            addLeash(this)
            wrapperPlayServerAttachEntity.vehicleId = ownerId
            for (player in Bukkit.getOnlinePlayers()) {
                try {
                    wrapperPlayServerAttachEntity.sendPacket(player)
                } catch (e: Exception) {
                    capture(e)
                }
            }
            isCreated = true
        }
    }

    fun spawn(player: Player?) {
        try {
//            Logger.console("Spawning protocolleash " + player.getName());
            wrapperPlayServerAttachEntity.sendPacket(player)
        } catch (e: Exception) {
            capture(e)
        }
    }

    fun destroy() {
        if (isCreated) {
//            Logger.console("Destroying protocolleash");
            removeLeash(this)
            wrapperPlayServerAttachEntity.vehicleId = -1
            for (player in Bukkit.getOnlinePlayers()) {
                try {
                    wrapperPlayServerAttachEntity.sendPacket(player)
                } catch (e: Exception) {
                    capture(e)
                }
            }
            isCreated = false
        }
    }

    init {
//        Logger.console("Initialising protocolleash");
        wrapperPlayServerAttachEntity = WrapperPlayServerAttachEntity()
        wrapperPlayServerAttachEntity.entityID = entityId
        wrapperPlayServerAttachEntity.vehicleId = ownerId
    }
}