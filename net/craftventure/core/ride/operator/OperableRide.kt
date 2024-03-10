package net.craftventure.core.ride.operator

import net.craftventure.bukkit.ktx.area.AreaTracker
import net.craftventure.core.ride.RideInstance
import net.craftventure.core.ride.operator.controls.OperatorControl
import org.bukkit.Location
import org.bukkit.entity.Player


interface OperableRide : RideInstance {
    val operatorAreaTracker: AreaTracker

    val isBeingOperated: Boolean

    val totalOperatorSpots: Int

    fun getOperatorForSlot(slot: Int): Player?

    /**
     * Gets the operator slot for the given player
     *
     * @param player
     * @return the operator slot of the player if he is an operator or -1 instead
     */
    fun getOperatorSlot(player: Player): Int

    /**
     * Sets the ride operator
     *
     * @param player
     * @return true if the given player was set as the operator for this ride
     */
    fun setOperator(player: Player, slot: Int): Boolean

    fun cancelOperating(slot: Int)

    fun provideControls(): List<OperatorControl>

    fun isInOperateableArea(location: Location): Boolean

    fun updateWhileOperated()
}
