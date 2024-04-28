package net.craftventure.bukkit.ktx.entitymeta

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.google.common.collect.Sets
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.ktx.util.Logger.severe
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Vehicle
import org.bukkit.event.*
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.Plugin
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent
import java.lang.reflect.Method

// Based on an idea by (if I remember correctly) kennytv from PaperMC

object EntityEvents : Listener {
    private val plugin by lazy { Bukkit.getPluginManager().getPlugin("Craftventure")!! }
    private val registeredEvents: MutableSet<Class<out Event>> = HashSet()
    private val OTHER_EVENTS: Set<Class<out Event>> = Sets.newHashSet(
        VehicleEnterEvent::class.java,
        VehicleExitEvent::class.java,
        PlayerInteractEntityEvent::class.java,
        PlayerStopSpectatingEntityEvent::class.java,
        PlayerStartSpectatingEntityEvent::class.java,
        EntityMountEvent::class.java,
        EntityDismountEvent::class.java,
    )

    private fun getMetaKey(clazz: Class<*>) = Meta.createTempKey("__EntityEvent_" + clazz.name)

    fun cleanup(entity: Entity) {
        registeredEvents.forEach { event ->
            val metaKey = getMetaKey(event)
//            val listeners = Meta.getEntityMeta<ArrayList<EntityListener<*>>>(entity, metaKey) ?: return@forEach
//            if (entity is Snowball)
//                Logger.debug("Removing ${listeners.size}")
            Meta.removeEntity<Any?>(entity, metaKey)
        }
    }

    fun Entity.addListener(listener: Listener) = registerEntityListener(this, scope = listener)
    fun Entity.removeListener(listener: Listener) = unregisterEntityListener(this, scope = listener)

    fun Entity.registerListenerAsLongAsValid(listener: Listener) {
        val validListener = object : Listener {
            @EventHandler
            fun tick(event: ServerTickEndEvent) {
//                Logger.debug("Valid ticking")
                if (!isValid) {
                    HandlerList.unregisterAll(listener)
                    HandlerList.unregisterAll(this)
                }
            }
        }
        Bukkit.getPluginManager().registerEvents(listener, PluginProvider.getInstance())
        Bukkit.getPluginManager().registerEvents(validListener, PluginProvider.getInstance())
    }

    fun unregisterEntityListener(
        entity: Entity,
        scope: Listener
    ) {
        val clazz: Class<*> = scope.javaClass
        loop@ for (method in clazz.methods) {
            if (method.isAnnotationPresent(EventHandler::class.java)) {
                method.isAccessible = true
                var checkClass: Class<out Event>? = null
                val types = method.parameterTypes
                var fail = false
                if (types.size != 1) {
                    fail = true
                } else {
                    checkClass = types[0] as Class<out Event>
                }
                if (fail || checkClass == null) {
                    severe(clazz.name + " Invalid EntityEventHandler method signature \"" + method.toGenericString() + "\"")
                    continue
                }
                val metaKey = getMetaKey(checkClass as Class<*>)
                val listeners = Meta.getEntityMeta<ArrayList<EntityListener<*>>>(entity, metaKey) ?: return
                Meta.setEntityMeta(entity, metaKey, listeners.filter { it.scope !== scope })
            }
        }
    }

    fun registerEntityListener(
        entity: Entity,
        plugin: Plugin = EntityEvents.plugin,
        scope: Listener
    ) {
        if (!entity.isValid) return
        val clazz: Class<*> = scope.javaClass
        val pluginManager = Bukkit.getPluginManager()
        loop@ for (method in clazz.methods) {
            if (method.isAnnotationPresent(EventHandler::class.java)) {
                method.isAccessible = true
                var checkClass: Class<out Event>? = null
                val types = method.parameterTypes
                var fail = false
                if (types.size != 1) {
                    fail = true
                } else {
                    checkClass = types[0] as Class<out Event>
                    if (!EntityEvent::class.java.isAssignableFrom(checkClass) && !OTHER_EVENTS.any {
                            it.isAssignableFrom(
                                checkClass
                            )
                        }) {
                        fail = true
                    }
                }
                if (fail || checkClass == null) {
                    severe(clazz.name + " Invalid EntityEventHandler method signature \"" + method.toGenericString() + "\"")
                    continue
                }
                val metaKey = getMetaKey(checkClass as Class<*>)
                val listeners = Meta.getEntityMeta<ArrayList<EntityListener<*>>>(entity, metaKey) ?: arrayListOf()
                val listener = when {
                    checkClass == VehicleEnterEvent::class.java -> VehicleEnteredListener(
                        scope,
                        method,
                        entity is Vehicle
                    )
                    checkClass == VehicleExitEvent::class.java -> VehicleExitedListener(
                        scope,
                        method,
                        entity is Vehicle
                    )
                    checkClass == PlayerStartSpectatingEntityEvent::class.java -> PlayerStartSpectatingListener(
                        scope,
                        method,
                        true
                    )
                    checkClass == PlayerStopSpectatingEntityEvent::class.java -> PlayerStopSpectatingListener(
                        scope,
                        method,
                        true
                    )
                    checkClass == EntityMountEvent::class.java -> EntityMountListener(
                        scope,
                        method,
                        true
                    )
                    checkClass == EntityDismountEvent::class.java -> EntityDismountListener(
                        scope,
                        method,
                        true
                    )
                    PlayerInteractEntityEvent::class.java.isAssignableFrom(checkClass) -> PlayerInteractEntityListener(
                        scope,
                        method,
                        true
                    )
                    EntityEvent::class.java.isAssignableFrom(checkClass) -> EntityEventListener(scope, method)
                    else -> {
                        severe("how did this happen?")
                        continue@loop
                    }
                } as EntityListener<Event>
                listeners.add(listener)
                Meta.setEntityMeta(entity, metaKey, listeners)
//                val listeners2 = Meta.getEntityMeta<ArrayList<EntityListener<*>>>(entity, metaKey)
//                Logger.debug("Set = ${listeners.size} Get = ${listeners2?.size}")
                if (!registeredEvents.contains(checkClass)) {
                    val executor =
                        EventExecutor { scope1: Listener, event: Event ->
                            val ent = listener.getEntity(event) ?: return@EventExecutor
//                            Logger.debug("Handling event for $ent $event")
                            val listeners1 = Meta.getEntityMeta<ArrayList<EntityListener<*>>>(ent, metaKey)
                            if (listeners1 != null) {
//                                Logger.debug("Has listeners")
                                for (entityListener in listeners1) {
                                    entityListener.call(event)
                                }
                            }
                        }
                    pluginManager.registerEvent(
                        checkClass,
                        scope,
                        EventPriority.HIGHEST,
                        executor,
                        plugin,
                        true
                    )
                    registeredEvents.add(checkClass)
                }
            }
        }
    }

    internal class ForcedEntityEventListener(scope: Listener, method: Method, val entity: Entity) :
        EntityListener<ServerTickEndEvent>(scope, method) {
        override fun getEntity(event: ServerTickEndEvent): Entity {
            return entity
        }
    }

    internal class EntityEventListener(scope: Listener, method: Method) :
        EntityListener<EntityEvent>(scope, method) {
        override fun getEntity(event: EntityEvent): Entity {
            return event.entity
        }
    }

    internal class VehicleEnteredListener(
        scope: Listener,
        method: Method,
        private val targetVehicle: Boolean
    ) : EntityListener<VehicleEnterEvent>(scope, method) {
        override fun getEntity(event: VehicleEnterEvent): Entity {
            return if (targetVehicle) event.vehicle else event.entered
        }
    }

    internal class VehicleExitedListener(
        scope: Listener,
        method: Method,
        private val targetVehicle: Boolean
    ) : EntityListener<VehicleExitEvent>(scope, method) {
        override fun getEntity(event: VehicleExitEvent): Entity {
            return if (targetVehicle) event.vehicle else event.exited
        }
    }

    internal class PlayerInteractEntityListener(
        scope: Listener,
        method: Method,
        private val targetClicked: Boolean
    ) : EntityListener<PlayerInteractEntityEvent>(scope, method) {
        override fun getEntity(event: PlayerInteractEntityEvent): Entity {
            return if (targetClicked) event.rightClicked else event.player
        }
    }

    internal class PlayerStopSpectatingListener(
        scope: Listener,
        method: Method,
        private val target: Boolean
    ) : EntityListener<PlayerStopSpectatingEntityEvent>(scope, method) {
        override fun getEntity(event: PlayerStopSpectatingEntityEvent): Entity {
            return if (target) event.spectatorTarget else event.player
        }
    }

    internal class PlayerStartSpectatingListener(
        scope: Listener,
        method: Method,
        private val target: Boolean
    ) : EntityListener<PlayerStartSpectatingEntityEvent>(scope, method) {
        override fun getEntity(event: PlayerStartSpectatingEntityEvent): Entity {
            return if (target) event.newSpectatorTarget else event.player
        }
    }

    internal class EntityMountListener(
        scope: Listener,
        method: Method,
        private val target: Boolean
    ) : EntityListener<EntityMountEvent>(scope, method) {
        override fun getEntity(event: EntityMountEvent): Entity {
            return if (target) event.mount else event.entity
        }
    }

    internal class EntityDismountListener(
        scope: Listener,
        method: Method,
        private val target: Boolean
    ) : EntityListener<EntityDismountEvent>(scope, method) {
        override fun getEntity(event: EntityDismountEvent): Entity {
            return if (target) event.dismounted else event.entity
        }
    }

    internal abstract class EntityListener<T : Event>(
        val scope: Listener,
        val method: Method
    ) {
        abstract fun getEntity(event: T): Entity?
        fun call(event: Event) {
            try {
                method.invoke(scope, event)
            } catch (ex: Exception) {
                capture(
                    IllegalStateException(
                        "Method " + scope.javaClass.name +
                                ":" + method.name, ex
                    )
                )
            }
        }
    }
}