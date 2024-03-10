package net.craftventure.core.feature.balloon.holders

import net.craftventure.core.npc.tracker.NpcEntityTracker
import org.bukkit.Location


abstract class BalloonHolder {
    abstract val anchorLocation: Location
    open val ownerCenterLocation: Location get() = anchorLocation
    abstract val isValid: Boolean
    abstract val leashHolderEntityId: Int?
    abstract val tracker: TrackerInfo
    open val maxLeashLength: Double? = null

    data class TrackerInfo(
        val tracker: NpcEntityTracker,
        val shouldHandleManagement: Boolean,
    )
}
