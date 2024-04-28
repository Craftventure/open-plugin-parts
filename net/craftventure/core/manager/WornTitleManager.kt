package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.event.PlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.async.executeSync
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.PlayerCoupledEntityTracker
import net.craftventure.database.generated.cvdata.tables.pojos.Title
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPoseChangeEvent
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent
import kotlin.collections.set

class WornTitleManager : Listener {
    private val titleHolderHashMap = HashMap<Player, TitleHolder>()

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityMount(event: EntityMountEvent) {
        if (event.entity is Player) {
            update(event.entity as Player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDismount(event: EntityDismountEvent) {
        if (event.entity is Player) {
            update(event.entity as Player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onToggleSneak(event: EntityPoseChangeEvent) {
        val entity = event.entity
        if (entity is Player) update(entity, pose = event.pose)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerLocationChanged(event: PlayerLocationChangedEvent) {
        executeSync { update(event.player, location = event.to) }
    }

    fun setTitle(player: Player, title: Title?) {
        if (!ENABLED)
            return
        //        Logger.info("Setting title " + (title != null ? title.getId() : "<null>") + " for " + player.getName());
        if (player.isConnected() && !player.isInsideVehicle && title != null && title.enabled!!) {
            var titleHolder: TitleHolder? = titleHolderHashMap[player]
            if (titleHolder == null) {
                //                Logger.info("Created titleholder");
                titleHolder = TitleHolder(player, title)
            } else {
                //                Logger.info("Updated title holder");
                titleHolder.setTitle(title)
            }
            titleHolderHashMap[player] = titleHolder
            update(player)
        } else {
            //            Logger.info("Failed setting title");
            remove(player)
        }
    }

    fun update(player: Player, location: Location = player.location, pose: Pose = player.pose) {
        if (!ENABLED)
            return
        if (player.isConnected() && !player.isInsideVehicle) {
            val titleHolder = titleHolderHashMap[player]
            titleHolder?.update(
                pose != Pose.SNEAKING && !player.isInsideVehicle,
                location.clone().add(0.0, player.eyeHeight + 0.5, 0.0)
            )
        }
    }

    fun remove(player: Player) {
        if (!ENABLED)
            return
        //        Logger.info("Removing " + player.getName());
        val titleHolder = titleHolderHashMap[player]
        titleHolder?.remove()
        //        else
        //            Logger.info("Failed to remove titleholder for " + player.getName());
        titleHolderHashMap.remove(player)
    }

    class TitleHolder(player: Player, private var title: Title) {
        private val entityTracker: PlayerCoupledEntityTracker = PlayerCoupledEntityTracker(player, false)
        private val titleEntity: NpcEntity = NpcEntity(
            "title",
            EntityType.ARMOR_STAND,
            Location(player.world, player.location.x, player.eyeHeight + 0.5, player.location.z)
        )
        private var visible: Boolean = true
            set(value) {
                if (field != value) {
                    field = value
                    if (value) {
                        titleEntity.updateMetadata {
                            customName(title.title?.parseWithCvMessage())
                            customNameVisible(true)
                        }
                    } else {
                        titleEntity.updateMetadata {
                            customName(null as? Component)
                            customNameVisible(false)
                        }
                    }
                }
            }

        init {
            titleEntity.updateMetadata {
                customName(title.title?.parseWithCvMessage())
                customNameVisible(true)
            }

            titleEntity.noGravity(true)
            titleEntity.invisible(true)
            titleEntity.marker(true)
            titleEntity.forceTeleport = true

            entityTracker.addEntity(titleEntity)
            entityTracker.startTracking()
        }

        fun getTitle(): Title {
            return title
        }

        fun setTitle(title: Title) {
            this.title = title
            titleEntity.customName(title.title?.parseWithCvMessage())
        }

        fun update(visible: Boolean, location: Location) {
            this.visible = visible
            titleEntity.move(location.x, location.y, location.z)
        }

        fun remove() {
            entityTracker.release()
        }
    }

    companion object {
        private val ENABLED = true

        val Player.titleLocation: Location
            get() = location.clone().add(0.0, this.eyeHeight + 0.5, 0.0)
    }
}
