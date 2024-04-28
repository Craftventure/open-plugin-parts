package net.craftventure.audioserver.api

import net.craftventure.audioserver.AudioServer
import net.craftventure.audioserver.packet.PacketKeyValue
import net.craftventure.audioserver.websocket.AudioServerHandler
import org.bukkit.entity.Player

object AudioServerApi {
    fun enable(areaName: String): Boolean {
        return enabled(areaName, true)
    }

    fun disable(areaName: String): Boolean {
        return enabled(areaName, false)
    }

    fun sync(areaName: String, sync: Long): Boolean {
        try {
            val audioAreas = AudioServer.instance.audioServerConfig!!.areas
            for (i in audioAreas.indices) {
                val audioArea = audioAreas[i]
                if (audioArea.name == areaName) {
                    audioArea.sync(sync)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun syncPlayer(areaName: String, player: Player, sync: Long): Boolean {
        try {
            val audioAreas = AudioServer.instance.audioServerConfig!!.areas
            for (i in audioAreas.indices) {
                val audioArea = audioAreas[i]
                if (audioArea.name == areaName) {
                    audioArea.sync(player, sync)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun add(areaName: String, player: Player): Boolean {
        try {
            val audioAreas = AudioServer.instance.audioServerConfig!!.areas
            for (i in audioAreas.indices) {
                val audioArea = audioAreas[i]
                if (audioArea.name == areaName) {
                    audioArea.addPlayer(player)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun addAndSync(areaName: String, player: Player, sync: Long): Boolean {
        try {
            val audioAreas = AudioServer.instance.audioServerConfig!!.areas
            for (i in audioAreas.indices) {
                val audioArea = audioAreas[i]
                if (audioArea.name == areaName) {
                    audioArea.addPlayer(player)
                    audioArea.sync(player, sync)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun remove(areaName: String, player: Player): Boolean {
        try {
            val audioAreas = AudioServer.instance.audioServerConfig!!.areas
            for (i in audioAreas.indices) {
                val audioArea = audioAreas[i]
                if (audioArea.name == areaName) {
                    audioArea.removePlayer(player)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun sendKeyValue(player: Player?, key: String, value: Any): Boolean {
        if (player != null) {
            val channelMetaData = AudioServerHandler.getChannel(player)
            if (channelMetaData != null) {
                PacketKeyValue(key, value).send(channelMetaData)
                return true
            }
        }
        return false
    }

    fun enabled(areaName: String, enabled: Boolean): Boolean {
        try {
            val audioAreas = AudioServer.instance.audioServerConfig!!.areas
            for (i in audioAreas.indices) {
                val audioArea = audioAreas[i]
                if (audioArea.name == areaName) {
                    audioArea.isEnabled = enabled
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }
}
