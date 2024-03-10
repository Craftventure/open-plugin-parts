package net.craftventure.core.ride.tracklessride.scene

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.controls.OperatorControl
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class TracklessRideScene(
    val id: String,
    val tracklessRide: TracklessRide,
    val sceneData: SceneData
) : OperatorControl.ControlListener {
    private val queue: Queue<TracklessRideCarGroup> = LinkedList()
    val actions = sceneData.program.mapValues { it.value.map { it.toPart(this) } }
    var currentGroup: TracklessRideCarGroup? = null
        private set(value) {
            field = value
            onCurrentGroupUpdated()
        }

    protected open fun onCurrentGroupUpdated() {}

    open fun onNotifyEnteredRide(player: Player, car: TracklessRideCar) {}
    open fun onNotifyExitedRide(player: Player, car: TracklessRideCar) {}
    open fun provideControls(): List<OperatorControl> = emptyList()
    open fun onOperatorsChanged() {}
    open fun destroy() {}
    open fun provideEjectOverride(): TracklessRide.EjectLocationProvider? = null

    override fun onClick(
        operableRide: OperableRide,
        player: Player?,
        operatorControl: OperatorControl,
        operatorSlot: Int?
    ) {
    }

    open fun createOrUpdateDebugItem(itemStack: ItemStack?): ItemStack {
        val item = itemStack ?: ItemStack(Material.STONE).apply {
            displayName(Component.text("Scene $id", CVTextColor.MENU_DEFAULT_TITLE))
        }
        return item.loreWithBuilder {
            labeled("currentGroup", if (currentGroup != null) "${currentGroup!!.groupId}" else "None")
            labeled("queue", if (queue.isNotEmpty()) queue.joinToString(", ") { it.groupId.toString() } else "Empty")
        }
    }

    fun allowEnter(group: TracklessRideCarGroup): Boolean =
        currentGroup == null && queue.peek() === group || queue.isEmpty()

    fun allowExit(group: TracklessRideCarGroup): Boolean = currentGroup === group

    open fun update() {}
    open fun updateAsync() {}

    fun enter(group: TracklessRideCarGroup): Boolean {
        if (!allowEnter(group)) return false
        queue.remove(group)
        currentGroup = group
        return true
    }

    fun exit(group: TracklessRideCarGroup): Boolean {
        if (!allowExit(group)) return false
        currentGroup = null
        return true
    }

    fun isQueued(group: TracklessRideCarGroup) = group in queue

    fun queue(group: TracklessRideCarGroup): Boolean {
        if (isQueued(group)) return true
        return queue.add(group)
    }

    fun dequeue(group: TracklessRideCarGroup): Boolean = queue.remove(group)
}