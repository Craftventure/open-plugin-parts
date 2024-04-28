package net.craftventure.bukkit.ktx.entitymeta

import net.craftventure.bukkit.ktx.extension.isDisconnected
import net.craftventure.core.ktx.logging.logcat
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*

object MetaAnnotations {
    private val items = hashMapOf<UUID, HashMap<String, BaseMetadata>>()

    private fun getKey(clazz: Class<*>) = clazz.name

    private val registeredClasses: MutableSet<Class<out BaseMetadata>> = mutableSetOf()

    fun getRegisteredClasses(): Set<Class<out BaseMetadata>> = registeredClasses

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : BaseMetadata> removeMetadata(target: Entity, clazz: Class<T>) {
        if (!BaseMetadata::class.java.isAssignableFrom(clazz)) throw IllegalStateException("${clazz.name} is not a metadata class")
        synchronized(items) {
            items[target.uniqueId]?.apply {
                val key = getKey(clazz)
                get(key)?.onDestroy()
                remove(key)

                if (this.isEmpty()) {
                    items.remove(target.uniqueId)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : BaseMetadata> getMetadata(target: Entity, clazz: Class<T>): T? {
        if (!BaseMetadata::class.java.isAssignableFrom(clazz)) throw IllegalStateException("${clazz.name} is not a metadata class")
        synchronized(items) {
            return items[target.uniqueId]?.get(getKey(clazz)) as? T
        }
    }

    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun <T : BaseMetadata> getOrCreateMetadata(
        target: Entity,
        clazz: Class<T>,
        creator: () -> T = { clazz.getDeclaredConstructor().newInstance() }
    ): T {
        if (target is Player && target.isDisconnected()) {
            logcat(
                priority = LogPriority.ERROR,
                logToCrew = true
            ) { "[MemoryLeak] Tried to create metadata ${clazz.name} for disconnected player ${target.name}" }
        }
        registeredClasses.add(clazz)
        if (!BaseMetadata::class.java.isAssignableFrom(clazz)) throw IllegalStateException("${clazz.name} is not a metadata class")
        synchronized(items) {
            val existing = getMetadata(target, clazz)
            if (existing != null) {
//                logcat { "Returning existing for ${target?.name} ${clazz}" }
                return existing
            }

            val metadata = creator()
            val map = items[target.uniqueId] ?: HashMap()
            map[getKey(clazz)] = metadata
            if (items[target.uniqueId] !== map)
                items[target.uniqueId] = map
            return metadata
        }
    }

    fun getAll() = items

    fun get(target: Entity) = items[target.uniqueId]?.values?.toList() ?: emptyList()

    @JvmStatic
    fun cleanup(entity: Entity) {
        synchronized(items) {
            val data = items[entity.uniqueId]
            data?.values?.forEach { it.onDestroy() }
            items.remove(entity.uniqueId)
//            logcat { "items=${items.size} entity=${items[entity.uniqueId]?.size}" }
        }
    }
}

inline fun <reified T : BaseMetadata> Entity.removeMetadata(instance: T) =
    MetaAnnotations.removeMetadata(this, instance.javaClass)

inline fun <reified T : BaseMetadata> Entity.removeMetadata() = MetaAnnotations.removeMetadata(this, T::class.java)
inline fun <reified T : BaseMetadata> Entity.requireMetadata() = MetaAnnotations.getMetadata(this, T::class.java)!!
inline fun <reified T : BaseMetadata> Entity.getMetadata() = MetaAnnotations.getMetadata(this, T::class.java)
inline fun <reified T : BaseMetadata> Entity.getOrCreateMetadata(
    noinline creator: () -> T = {
        T::class.java.getDeclaredConstructor().newInstance()
    }
) =
    MetaAnnotations.getOrCreateMetadata(this, T::class.java, creator)
