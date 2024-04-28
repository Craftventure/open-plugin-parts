package net.craftventure.bukkit.ktx.entitymeta

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.metadata.FixedMetadataValue

object Meta {
    private val plugin by lazy { Bukkit.getPluginManager().getPlugin("Craftventure")!! }

    @JvmStatic
    fun <T> getEntityMeta(entity: Entity, key: Key): T? {
        val data = entity.getMetadata(key.id).firstOrNull { it.owningPlugin === plugin } ?: return null
        return (data as FixedMetadataValue).value() as T
    }

    @JvmStatic
    fun <T> setEntityMeta(entity: Entity, key: Key, value: T) {
        if (key is TempKey)
            entity.setMetadata(key.id, FixedMetadataValue(plugin, value))
        else
            throw IllegalStateException("Unsupported key type ${key.javaClass.name}")
    }

    @JvmStatic
    fun <T> removeEntity(entity: Entity, key: Key) {
        entity.removeMetadata(key.id, plugin)
    }

    @JvmStatic
    fun createTempKey(id: String) = TempKey(id)

    abstract class Key(val id: String)

    class TempKey internal constructor(
        key: String
    ) : Key(key)
}