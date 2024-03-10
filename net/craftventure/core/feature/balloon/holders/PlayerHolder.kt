package net.craftventure.core.feature.balloon.holders

import net.craftventure.core.feature.balloon.BalloonManager
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.core.npc.tracker.PlayerCoupledEntityTracker
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.MainHand
import kotlin.math.cos
import kotlin.math.sin


class PlayerHolder(
    val holder: Player,
) : BalloonHolder() {
    private val meta = holder.equippedItemsMeta()
    override val leashHolderEntityId: Int = holder.entityId
    override val tracker = TrackerInfo(
        PlayerCoupledEntityTracker(holder, true),
        true,
    )
    override val anchorLocation: Location
        get() {
            val location = holder.location.clone()
            location.pitch = 0f
            val yawRadian = -Math.toRadians(location.yaw + (if (holder.mainHand == MainHand.RIGHT) 90.0 else -90.0))
            return location.clone().add(sin(yawRadian) * 0.5, holder.eyeHeight * 0.5, cos(yawRadian) * 0.5)
        }
    override val ownerCenterLocation: Location
        get() = holder.location.clone().add(0.0, holder.eyeHeight * 0.5, 0.0)

    override val isValid: Boolean
        get() = BalloonManager.isAllowedToHoldBalloon(holder) &&
                meta?.appliedEquippedItems?.balloonItem != null
}
