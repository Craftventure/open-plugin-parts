package net.craftventure.core.ride.operator

import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.AreaTracker
import net.craftventure.core.CraftventureCore
import org.bukkit.entity.Player

class OperatorAreaTracker(
    private val operableRide: OperableRide, area: Area
) : AreaTracker(area) {
    init {
        addListener(object : StateListener {
            override fun onEnter(areaTracker: AreaTracker, player: Player) {
                update(player, true)
            }

            override fun onLeave(areaTracker: AreaTracker, player: Player) {
                update(player, false)
            }
        })
        start()
    }

    private fun update(player: Player, visible: Boolean) {
        CraftventureCore.getOperatorManager().updateForcedVisibility(operableRide, player, visible = visible)
    }
}