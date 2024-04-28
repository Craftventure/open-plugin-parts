package net.craftventure.core.utils

import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.util.field
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player


object EntityUtils {

    fun setupArmorStandUpdates() {
        val armorStand = EntityType.ARMOR_STAND
        //https://nms.screamingsandals.org/1.19.4/net/minecraft/world/entity/EntityType.html > updateInterval
        val field = armorStand.field("bI")!!
        field.set(armorStand, 1)
//        armorStand.updateInterval()
    }

    val Player.nmsHandle: ServerPlayer get() = (this as CraftPlayer).handle
    val Entity.nmsHandle: net.minecraft.world.entity.Entity get() = (this as CraftEntity).handle

    fun Entity.setInstantUpdate(): Boolean {
        if (!isValid) return false
        if (this is ArmorStand) return true
        try {
            val tracker = (this.world as CraftWorld).handle.chunkSource.chunkMap
            val entityTracker = tracker.entityMap[this.entityId]!!
            val trackerEntryField =
                entityTracker::class.java.declaredFields.firstOrNull { it.type == ServerEntity::class.java }
                    ?: return false
            trackerEntryField.isAccessible = true
            val entry = trackerEntryField.get(entityTracker) as? ServerEntity ?: run {
                executeSync(10) {
                    this.setInstantUpdate()
                }
                return false
            }
            //https://nms.screamingsandals.org/1.19.4/net/minecraft/server/level/ServerEntity.html > updateInterval
            val field = entry.field("e"/*"updateInterval"*/)!!
//            Logger.debug("d=${field.get(entry)}")
            field.set(entry, 1)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun teleport(target: Entity, loc: Location): Boolean {
        return teleport(target, loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
    }

    fun teleport(target: Entity, x: Double, y: Double, z: Double): Boolean {
        val entity = (target as CraftEntity).handle
        return teleport(target, x, y, z, entity.yRot, entity.xRot)
    }

    fun teleport(target: Entity, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        val entity = (target as CraftEntity).handle
        if (entity.x != x || entity.y != y || entity.z != z || entity.yRot != yaw || entity.xRot != pitch) {
            entity.moveTo(x, y, z, yaw, pitch)
            return true
        }
        return false
    }

    fun move(target: Entity, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        val entity = (target as CraftEntity).handle
        if (entity.x != x || entity.y != y || entity.z != z || entity.yRot != yaw || entity.xRot != pitch) {
//            entity.move(MoverType.PLAYER, Vec3(x - entity.x, y - entity.y, z - entity.z))
            entity.moveTo(x, y, z, yaw, entity.xRot)
            return true
        }
        return false
    }

    fun setLocation(target: Entity, loc: Location): Boolean {
        return setLocation(target, loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
    }

    fun setLocation(target: Entity, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        val entity = (target as CraftEntity).handle
        if (entity.x != x || entity.y != y || entity.z != z || entity.yRot != yaw || entity.xRot != pitch) {
            entity.moveTo(x, y, z, yaw, pitch)
            return true
        }
        return false
    }

    fun forceTeleport(target: Entity, loc: Location) {
        forceTeleport(target, loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
    }

    fun forceTeleport(target: Entity, x: Double, y: Double, z: Double) {
        val entity = (target as CraftEntity).handle
        forceTeleport(target, x, y, z, entity.yRot, entity.xRot)
    }

    fun forceTeleport(target: Entity, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        val entity = (target as CraftEntity).handle
        entity.moveTo(x, y, z, yaw, pitch)
    }

    //    public static boolean teleport(Player player, Location dest) {
    //        if (player != null) {
    //            player.teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
    ////            CraftPlayer entityPlayer = (CraftPlayer) player;
    ////            entityPlayer.getHandle().playerConnection.teleport(dest);
    //            return true;
    //        }
    //        return false;
    //    }

    fun teleportWithHeadYaw(target: Entity, location: Location) {
        teleportWithHeadYaw(target, location.x, location.y, location.z, location.yaw, location.pitch)
    }

    fun teleportWithHeadYaw(target: Entity, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        teleport(target, x, y, z, yaw, pitch)
        yaw(target, yaw)
    }

    fun yaw(target: Entity, yaw: Float) {
        val entity = (target as CraftEntity).handle
        // 1.11.2: method h is located near getHeadRotation
        entity?.yHeadRot = yaw
    }

    fun entityYaw(target: Entity, yaw: Float) {
        val entity = (target as CraftEntity).handle
        entity.moveTo(entity.x, entity.y, entity.z, yaw, entity.xRot)
    }

//    fun noClip(target: Entity, noclip: Boolean) {
//        (target as CraftEntity).handle.noclip = noclip
//    }

    fun find(id: Int): Entity? {
        val worlds = Bukkit.getWorlds()
        for (i in worlds.indices) {
            val world = worlds[i]
            val entities = world.entities
            for (i1 in entities.indices) {
                val entity = entities[i1]
                if (entity.entityId == id) {
                    return entity
                }
            }
        }
        return null
    }

    fun getFirstPassenger(entity: Entity?): Entity? {
        if (entity != null) {
            if (entity.passengers.size >= 1) {
                return entity.passengers[0]
            }
        }
        return null
    }

    fun hasPlayerPassengers(entity: Entity?): Boolean {
        if (entity != null) {
            val passengers = entity.passengers
            for (i in passengers.indices) {
                val passenger = passengers[i]
                if (passenger is Player) {
                    return true
                }
            }
        }
        return false
    }
}
