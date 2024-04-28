package net.craftventure.bukkit.ktx.util

import net.craftventure.core.ktx.logging.logcat
import net.minecraft.core.registries.BuiltInRegistries
import org.bukkit.craftbukkit.v1_20_R1.util.CraftNamespacedKey
import org.bukkit.entity.EntityType

object EntityTable {
    private val entityIds = hashMapOf<EntityType, Int>()

    init {
        val entityRegistry = BuiltInRegistries.ENTITY_TYPE
        org.bukkit.entity.EntityType.values().forEach bukkitType@{ bukkitEntityType ->
            if (bukkitEntityType == EntityType.UNKNOWN) return@bukkitType
            val key = CraftNamespacedKey.toMinecraft(bukkitEntityType.key)
            val id = entityRegistry.getId(entityRegistry.get(key))
            entityIds[bukkitEntityType] = id
//            logcat { "$bukkitEntityType = $id" }
        }
//        logcat { "Loaded ${entityIds.size} entityIds" }
    }

    fun getTypeId(type: EntityType): Int? = entityIds[type] ?: run {
        logcat(LogPriority.WARN) { "$type: not found" }
        null
    }
}