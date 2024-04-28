package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.manager.EquipmentManager
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class BalloonTrackerMeta(
    val player: Player
) : BasePlayerMetadata(player) {
    var previouslyAppliedBalloon: EquipmentManager.EquippedItemData? = null
        private set

    fun clear() {
        previouslyAppliedBalloon = null
    }

    fun update(balloon: EquipmentManager.EquippedItemData?): Boolean {
        val changed = previouslyAppliedBalloon === null && balloon !== null ||
                previouslyAppliedBalloon !== null && balloon === null ||
                balloon?.id != previouslyAppliedBalloon?.id

//        logcat { "Updating changed=$changed (${previouslyAppliedBalloon?.id} vs ${balloon?.id})" }
        if (changed) previouslyAppliedBalloon = balloon

        return changed
    }

    override fun debugComponent() =
        Component.text("previouslyAppliedBalloon=${previouslyAppliedBalloon?.id}")

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { BalloonTrackerMeta(player) }
    }
}