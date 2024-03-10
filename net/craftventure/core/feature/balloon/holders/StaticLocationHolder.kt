package net.craftventure.core.feature.balloon.holders

import org.bukkit.Location


class StaticLocationHolder(
    override val leashHolderEntityId: Int?,
    override val tracker: TrackerInfo,
    override val anchorLocation: Location,
    override val ownerCenterLocation: Location = anchorLocation,
    override val maxLeashLength: Double? = null,
    private val validCheck: () -> Boolean = { true },
) : BalloonHolder() {
    override val isValid: Boolean
        get() = validCheck()
}
