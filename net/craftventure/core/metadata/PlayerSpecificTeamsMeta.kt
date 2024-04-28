package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BaseMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.manager.TeamsManager
import net.craftventure.core.npc.NpcEntity
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team


class PlayerSpecificTeamsMeta(
    val owner: Player
) : BaseMetadata() {
    private val teams = hashMapOf<String, Team>()

    init {
        addOrUpdate(owner, TeamsManager.getTeamDataFor(owner))
        Bukkit.getOnlinePlayers().forEach { player ->
            addOrUpdate(player, TeamsManager.getTeamDataFor(player))
        }
    }

    override fun debugComponent() = Component.text("team=$teams")

    private fun createTeam(teamData: TeamsManager.TeamData): Team {
        val team = owner.scoreboard.getTeam(teamData.internalId)
            ?: owner.scoreboard.registerNewTeam(teamData.internalId).apply {
                setCanSeeFriendlyInvisibles(false)
                setAllowFriendlyFire(true)
                setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
                if (!teamData.isPlayerTeam)
                    setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
//        team.displayName(displayName)
                prefix(teamData.displayName)
                color(teamData.namedColor)
            }
        return team
    }

    private fun requireTeam(teamData: TeamsManager.TeamData): Team {
        return teams.getOrPut(teamData.internalId) { createTeam(teamData) }
    }

    @Throws(IllegalStateException::class)
    fun setAllOption(option: Team.Option, status: Team.OptionStatus) {
        for (team in owner.scoreboard.teams) {
            team.setOption(option, status)
        }
    }

    fun addOrUpdate(entity: NpcEntity, targetTeamData: TeamsManager.TeamData) {
        requireTeam(targetTeamData).addEntry(entity.teamEntry)
    }

    fun remove(entity: NpcEntity) {
        owner.scoreboard.teams.forEach { it.removeEntry(entity.id) }
    }

    fun addOrUpdate(player: Player, targetTeamData: TeamsManager.TeamData) {
//        if (player.uniqueId == owner.uniqueId) return
        requireTeam(targetTeamData).addEntry(player.name)
//        Logger.debug("Adding or updating ${player.name} for ${owner.name} in team ${targetTeamData.internalId}")
    }

    fun remove(player: Player) {
        if (player.uniqueId == owner.uniqueId) return
//        Logger.debug("Removing ${player.name} for ${owner.name}")
        owner.scoreboard.teams.forEach { it.removeEntry(player.name) }
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { PlayerSpecificTeamsMeta(player) }
    }

    companion object {
        fun getOrCreate(player: Player) = player.getOrCreateMetadata { PlayerSpecificTeamsMeta(player) }
    }
}