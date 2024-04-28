package net.craftventure.core.task

import net.craftventure.audioserver.packet.PacketPlayerList
import net.craftventure.audioserver.websocket.AudioServerHandler
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.isOwner
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.manager.visibility.VisibilityMeta
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object AudioServerPlayerMapTask {
    private var taskId: Int? = null

    fun start() {
        if (taskId != null) return
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(PluginProvider.getInstance(), {
            val players = Bukkit.getOnlinePlayers()
            for (player in players) {
                val channelMetaData = AudioServerHandler.getChannel(player)
                if (channelMetaData != null) {
                    if (player.isOwner()) {
                        forCrew(player).send(channelMetaData)
                    } else if (players.size > 30) {
                        justSelf(player).send(channelMetaData)
                    } else if (player.isCrew()) {
                        forCrew(player).send(channelMetaData)
                    } else {
                        forGuests(player).send(channelMetaData)
                    }
                }
            }
        }, 1L, 20 * 1L)

    }

    fun stop() {
        if (taskId == null) return
        Bukkit.getScheduler().cancelTask(taskId!!)
    }

    fun justSelf(packetReceiver: Player): PacketPlayerList {
        val meta = packetReceiver.getMetadata<VisibilityMeta>()
        return PacketPlayerList(
            listOf(
                PacketPlayerList.PlayerLocation(
                    packetReceiver,
                    (meta?.shouldHideOnAudioServer() ?: packetReceiver.isSneaking)
                )
            )
        )
    }

    fun forCrew(packetReceiver: Player): PacketPlayerList = PacketPlayerList(
        Bukkit.getOnlinePlayers()
            .map {
                val meta = it.player?.getMetadata<VisibilityMeta>()
                PacketPlayerList.PlayerLocation(it, (meta?.shouldHideOnAudioServer() ?: it.isSneaking))
            }
    )

    fun forGuests(packetReceiver: Player): PacketPlayerList = PacketPlayerList(
        Bukkit.getOnlinePlayers()
            .filter {
                if (it === packetReceiver) return@filter true
                if (!packetReceiver.canSee(it)) return@filter false
                if (it.isSneaking) return@filter false

                val meta = it.player?.getMetadata<VisibilityMeta>()
                if (meta != null) {
                    return@filter !meta.shouldHideOnAudioServer()
                }
                return@filter true
            }
            .map {
                PacketPlayerList.PlayerLocation(it)
            }
    )
}