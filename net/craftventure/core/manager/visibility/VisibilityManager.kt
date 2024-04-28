package net.craftventure.core.manager.visibility

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.CraftventureCore
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

object VisibilityManager {
    private val invisibleJoins = mutableSetOf<UUID>()

    fun updateAll() {
        Bukkit.getOnlinePlayers().forEach { player ->
            updateTo(player)
        }
    }

    fun requestVanishJoin(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            val meta = player.getOrCreateMetadata { VisibilityMeta(player) }
            meta.setVanishedIfAllowed()
//            Logger.debug("$uuid vanished")
        } else {
//            Logger.debug("$uuid vanish join")
            invisibleJoins.add(uuid)
        }
    }

    /**
     * @return true if the login should be silent, false otherwise
     */
    @JvmStatic
    fun handleLoginOf(player: Player): Boolean {
        val invisibleJoin = invisibleJoins.contains(player.uniqueId)
//        Logger.debug("${player.name} vanish join: $invisibleJoin")
        if (invisibleJoin) {
            requestVanishJoin(player.uniqueId)
            invisibleJoins.remove(player.uniqueId)
        }
        broadcastChangesFrom(player)
        updateTo(player)
        return player.getMetadata<VisibilityMeta>()?.vanished == true
    }

    @JvmStatic
    fun broadcastChangesFrom(player: Player) {
//        val visibilityMeta = player.getMetadata<VisibilityMeta>()
//        Logger.info("Broadcast visibility change of ${player.name} (hasMeta? ${visibilityMeta != null})")
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer === player)
                continue

            updateTo(onlinePlayer, player)
        }
    }

    @JvmStatic
    fun updateTo(subject: Player, other: Player) {
        val subjectVisiblityMeta = subject.getMetadata<VisibilityMeta>()
        val visibilityMeta = other.getMetadata<VisibilityMeta>()

        val wantsToSee = subjectVisiblityMeta?.wantsToSee(other) ?: true
        val canSee = visibilityMeta?.canBeSeenBy(subject) ?: true

        subject.setCanSee(other, canSee && wantsToSee)
    }

    @JvmStatic
    fun forceUpdate(subject: Player) {
        updateTo(subject)
        broadcastChangesFrom(subject)
    }

    @JvmStatic
    fun updateTo(subject: Player) {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer === subject)
                continue

            updateTo(subject, onlinePlayer)
        }
    }

    fun Player.setCanSee(other: Player, show: Boolean = true) {
//        Logger.info("Updating player visibility: Showing ${other.name} to ${this.name}? $show")
        val shouldUpdate = canSee(other) != show
        if (shouldUpdate) {
//            Logger.info("Updating player visibility so that ${other.name} can be seen by ${this.name} == $show")
            if (show)
                showPlayer(CraftventureCore.getInstance(), other)
            else
                hidePlayer(CraftventureCore.getInstance(), other)
        }
    }
}