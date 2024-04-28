package net.craftventure.core.utils

import com.comphenix.packetwrapper.WrapperPlayServerEntityStatus
import net.craftventure.bukkit.ktx.extension.packAllReflection
import net.craftventure.bukkit.ktx.extension.sendPacketIgnoreError
import net.craftventure.bukkit.ktx.extension.withV2Marker
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.spawn
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftFirework
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.projectiles.ProjectileSource
import org.bukkit.util.Vector
import java.util.*


object FireworkUtils {
    fun spawn(
        world: World,
        x: Double,
        y: Double,
        z: Double,
        itemStack: ItemStack,
        velocityX: Double = 0.0,
        velocityY: Double = 0.0,
        velocityZ: Double = 0.0,
        lifeTimeInSeconds: Double? = null,
        players: List<Player>? = null,
        shooter: ProjectileSource? = null,
    ): Firework? {
        try {
            val meta = itemStack.itemMeta as? FireworkMeta ?: return null
//            Logger.debug("Spawning fireworks with $velocityX $velocityY $velocityZ")

            if (players != null) {
                val entityId = Bukkit.getUnsafe().nextEntityId()
                val spawnPacket = ClientboundAddEntityPacket(
                    entityId,
                    UUID.randomUUID().withV2Marker(),
                    x,
                    y,
                    z,
                    0f,
                    0f,
                    net.minecraft.world.entity.EntityType.FIREWORK_ROCKET,
                    0,
                    Vec3(velocityX, velocityY, velocityZ),
                    0.0,
                )

                val data = SynchedEntityData(NpcEntity.getFakeEntity(Location(world, x, y, z)))
                data.define(EntityMetadata.Fireworks.item.accessor, CraftItemStack.asNMSCopy(itemStack))

                val metadataPacket = ClientboundSetEntityDataPacket(entityId, data.packAllReflection())

                for (player in players) {
                    player.sendPacketIgnoreError(spawnPacket)
                    player.sendPacketIgnoreError(metadataPacket)
                }

                executeSync((20 * lifeTimeInSeconds!!).toLong()) {
//                    val explodePacket = ClientboundEntityEventPacket()
                    val explodePacket = WrapperPlayServerEntityStatus()
                    explodePacket.entityID = entityId
                    explodePacket.entityStatus = 17
                    //                explodePacket.effectID =
                    val removePacket = ClientboundRemoveEntitiesPacket(entityId)

                    for (player in players) {
                        try {
                            explodePacket.sendPacket(player)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        player.sendPacketIgnoreError(removePacket)
                    }
                }

            } else {
                val firework = Location(world, x, y, z).spawn<Firework>()
                firework.fireworkMeta = meta
                firework.velocity = Vector(velocityX, velocityY, velocityZ)
                firework.shooter = shooter
                if (lifeTimeInSeconds != null && firework is CraftFirework) {
                    firework.handle.lifetime = (20 * lifeTimeInSeconds).toInt()
                }
                return firework
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun spawn(
        location: Location,
        itemStack: ItemStack,
        velocityX: Double = 0.0,
        velocityY: Double = 0.0,
        velocityZ: Double = 0.0,
        lifeTimeInSeconds: Double? = null,
        players: List<Player>? = null,
        shooter: ProjectileSource? = null,
    ) = spawn(
        world = location.world!!,
        x = location.x,
        y = location.y,
        z = location.z,
        itemStack = itemStack,
        velocityX = velocityX,
        velocityY = velocityY,
        velocityZ = velocityZ,
        lifeTimeInSeconds = lifeTimeInSeconds,
        players = players,
        shooter = shooter,
    )
}