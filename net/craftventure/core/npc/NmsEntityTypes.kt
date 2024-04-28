package net.craftventure.core.npc

import net.craftventure.core.ktx.util.Logger
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.player.Player
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld
import org.bukkit.craftbukkit.v1_20_R1.util.CraftNamespacedKey
import org.bukkit.entity.EntityType

object NmsEntityTypes {
    private val entityTypeFactoryField by lazy {
        val factoryField =
            net.minecraft.world.entity.EntityType::class.java.declaredFields.first { it.type == net.minecraft.world.entity.EntityType.EntityFactory::class.java }
        factoryField.isAccessible = true
        factoryField
    }

    val entityTypeToClassMap = EntityType.values().filter { it != EntityType.UNKNOWN }.associateWith {
        val nmsClass = it.toNmsClass()
        val nmsType = it.toNmsType()
        if (nmsClass == null)
            Logger.warn("No NMS class for $it")
        if (nmsType == null)
            Logger.warn("No NMS type for $it")
//        logcat { "Converting $it (${it.key}) type=${nmsType?.id} class=${nmsClass?.name}" }
        EntityData(nmsType, nmsClass)
    }

    fun EntityType.toNmsType(): net.minecraft.world.entity.EntityType<out Entity>? = when (this) {
        EntityType.UNKNOWN -> null
        else -> {
            val key = CraftNamespacedKey.toMinecraft(this.key)
            BuiltInRegistries.ENTITY_TYPE.get(key)
        }
    }

    fun EntityType.toNmsClass() = when (this) {
        EntityType.UNKNOWN -> null
        EntityType.PLAYER -> Player::class.java
        EntityType.LIGHTNING -> LightningBolt::class.java
        else -> try {
            val type = this.toNmsType()
            val factory =
                entityTypeFactoryField.get(type) as net.minecraft.world.entity.EntityType.EntityFactory<Entity>
            val entity = factory.create(
                type as net.minecraft.world.entity.EntityType<Entity>,
                (Bukkit.getWorlds()[0] as CraftWorld).handle
            )
            entity::class.java
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.warn("Failed to convert type $this to NMS type")
            null
        }
    }

    data class EntityData(
        val type: net.minecraft.world.entity.EntityType<out Entity>?,
        val clazz: Class<out Entity>?,
    )

    init {
        entityTypeToClassMap.entries.forEach { entry ->
            val type = entry.key
            val clazz = entry.value.clazz
            if (clazz != null)
                require(Entity::class.java.isAssignableFrom(clazz)) { "Invalid class for type $type (${clazz.name})" }
        }
    }
}