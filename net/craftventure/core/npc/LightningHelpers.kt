package net.craftventure.core.npc

import net.craftventure.bukkit.ktx.coroutine.executeSync
import net.craftventure.bukkit.ktx.extension.sendPacket
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.*

fun Player.strikeLightningEffect(at: Location) = strikeLightningEffect(Vector(at.x, at.y, at.z))
fun Player.strikeLightningEffect(at: Vector) {
    val uuid = UUID.randomUUID()
    val entityId = Bukkit.getUnsafe().nextEntityId()

    val packet = ClientboundAddEntityPacket(
        entityId,
        uuid,
        at.x,
        at.y,
        at.z,
        0f,
        0f,
        NmsEntityTypes.entityTypeToClassMap[EntityType.LIGHTNING]!!.type!!,
        0,
        Vec3.ZERO,
        0.0,
    )

    try {
        this.sendPacket(packet)
        executeSync(20) { this.sendPacket(ClientboundRemoveEntitiesPacket(entityId)) } // Not sure if this is even needed?
    } catch (e: Exception) {
        e.printStackTrace()
    }
}