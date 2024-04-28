package net.craftventure.core.manager.visibility

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.core.config.AreaConfigManager
import net.craftventure.core.feature.minigame.MinigameManager
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player


class VisibilityMeta(
    val holder: Player
) : BasePlayerMetadata(holder) {
    var hiddenOnAudioMap = false
    var vanished = false
        private set(value) {
            if (field != value) {
                field = value
            }
        }
    var hideAllPlayers = false
        private set
    val isForceVanished: Boolean
        get() = AreaConfigManager.getInstance().isForceVanished(holder.location)

    override fun debugComponent() =
        Component.text("isForceVanished=$isForceVanished hideAllPlayers=$hideAllPlayers vanished=$vanished hiddenOnAudioMap=$hiddenOnAudioMap")

    fun shouldHideOnAudioServer() = holder.isSneaking || hiddenOnAudioMap

    /**
     * @return true if all players are hidden
     */
    fun toggleHideOnAudioMap(): Boolean {
        hiddenOnAudioMap = !hiddenOnAudioMap
        return hiddenOnAudioMap
    }

    /**
     * @return true if all players are hidden
     */
    fun toggleHideAllPlayers(): Boolean {
        hideAllPlayers = !hideAllPlayers
//        Logger.info("hideAllPlayers=$hideAllPlayers for ${holder.name}")
        VisibilityManager.updateTo(holder)
        return hideAllPlayers
    }

    /**
     * @return true if vanished
     */
    fun toggleVanish(): Boolean {
        if (holder.isCrew()) {
            vanished = !vanished
//            if (vanished) {
//                VanishEffect(holder).runTaskTimer(CraftventureCore.getInstance(), 1L, 1L)
//            }
//            Logger.info("vanished=$vanished for ${holder.name}")
            VisibilityManager.broadcastChangesFrom(holder)
            return vanished
        }
        return false
    }

    fun setVanishedIfAllowed(): Boolean {
        if (holder.isCrew()) {
            vanished = true
//            if (vanished) {
//                VanishEffect(holder).runTaskTimer(CraftventureCore.getInstance(), 1L, 1L)
//            }
//            Logger.info("vanished=$vanished for ${holder.name}")
            VisibilityManager.broadcastChangesFrom(holder)
            return vanished
        }
        return false
    }

    fun wantsToSee(player: Player): Boolean {
        if (MinigameManager.getParticipatingGame(player) != null)
            return true

        val canViewOtherPlayers = FeatureManager.isFeatureEnabled(FeatureManager.Feature.VIEW_OTHER_PLAYERS)
        if (hideAllPlayers || !canViewOtherPlayers) {
            if (!player.isCrew()) {
                return false
            }
        }
        return true
    }

    fun canBeSeenBy(player: Player): Boolean {
        if (MinigameManager.getParticipatingGame(holder) != null)
            return true

        if (isForceVanished)
            return false

        if (vanished) {
            if (!player.isCrew())
                return false
        }
        return true
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { VisibilityMeta(player) }
    }
}