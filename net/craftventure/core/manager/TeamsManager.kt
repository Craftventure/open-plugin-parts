package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.isOwner
import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.extension.isYouTuber
import net.craftventure.chat.bungee.extension.append
import net.craftventure.core.metadata.PlayerSpecificTeamsMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible

object TeamsManager {
    private val ownerTeam =
        TeamData(true, "1owner", Component.text("Owner "), /*CVTextColor.serverNoticeAccent,*/ NamedTextColor.GOLD)
        { it.isOwner() }
    private val crewTeam =
        TeamData(true, "2crew", Component.text("Crew "), /*CVTextColor.crew,*/ NamedTextColor.DARK_AQUA)
        { it.isCrew() }
    private val vipTeam =
        TeamData(true, "3vip", Component.text("VIP "), /*CVTextColor.vip,*/ NamedTextColor.DARK_PURPLE)
        { it.isVIP() }
    private val youtubeTeam =
        TeamData(true, "4youtube", Component.text("YouTube "), /*CVTextColor.youtube,*/ NamedTextColor.RED)
        { it.isYouTuber() }
    private val defaultTeam =
        TeamData(true, "5guest", Component.text("Guest "), /*CVTextColor.guest,*/ NamedTextColor.GRAY)
        { true }
    private val npcTeam =
        TeamData(true, "9npc", Component.text("NPC "), /*CVTextColor.guest,*/ NamedTextColor.WHITE)
        { false }

    private val teams = listOf(ownerTeam, crewTeam, youtubeTeam, vipTeam, defaultTeam, npcTeam)
    private val colorTeams = hashMapOf<Int, TeamData>()

    fun getNpcTeam() = npcTeam
    fun getTeamDataFor(player: Player) = teams.firstOrNull { it.matches(player) } ?: defaultTeam
    fun allTeams() = teams

    fun vipTeam() = vipTeam
    fun crewTeam() = crewTeam

    fun getTeamDataFor(color: NamedTextColor) = colorTeams.getOrPut(color.value()) {
        TeamData(
            false,
            "clr_${color.value()}",
            Component.empty(),
            color
        ) { false }
    }

    init {
        for (player in Bukkit.getOnlinePlayers()) update(player)
    }

//    fun addNpc(entity: NpcEntity, teamData: TeamData) {
//        Bukkit.getOnlinePlayers().forEach {
//            it.getMetadata<PlayerSpecificTeamsMeta>()?.addOrUpdate(entity, teamData)
//        }
//    }
//
//    fun removeEntity(entity: NpcEntity) {
//        Bukkit.getOnlinePlayers().forEach {
//            it.getMetadata<PlayerSpecificTeamsMeta>()?.remove(entity)
//        }
//    }

    fun remove(player: Player) {
        Bukkit.getOnlinePlayers().forEach {
            it.getMetadata<PlayerSpecificTeamsMeta>()?.remove(player)
        }
//        teams.forEach { it.removeEntry(player.name) }
    }

    fun update(player: Player) {
//        remove(player)

        val teamData = getTeamDataFor(player)
        player.playerListName(
            Component.text("", teamData.namedColor)
                .append(teamData.displayName)
                .append(player.name)
        )

        Bukkit.getOnlinePlayers().forEach {
            it.getMetadata<PlayerSpecificTeamsMeta>()?.addOrUpdate(player, teamData)
        }
    }

    class TeamData(
        val isPlayerTeam: Boolean,
        val internalId: String,
        val displayName: Component,
//        var color: TextColor,
        val namedColor: NamedTextColor,
        val matches: (Permissible) -> Boolean,
    ) {
//        val coloredDisplayName = displayName.color(color)
    }
}
