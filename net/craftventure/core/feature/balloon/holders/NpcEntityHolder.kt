package net.craftventure.core.feature.balloon.holders

import net.craftventure.core.npc.NpcEntity
import org.bukkit.Location
import org.bukkit.util.Vector


class NpcEntityHolder(
    override val tracker: TrackerInfo,
    val holder: NpcEntity,
    private val validCheck: () -> Boolean = { true },
    private val offset: Vector = Vector(0.0, 0.8, 0.0)
) : BalloonHolder() {
    override val leashHolderEntityId: Int = holder.entityId
    override val anchorLocation: Location
        get() = holder.getLocation()
    override val ownerCenterLocation: Location
        get() = holder.getLocation().clone().add(offset)
    override val isValid: Boolean
        get() = validCheck()
}
