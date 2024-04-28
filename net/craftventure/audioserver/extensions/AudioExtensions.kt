package net.craftventure.audioserver.extensions

import net.craftventure.audioserver.websocket.AudioServerHandler
import net.craftventure.audioserver.websocket.ChannelMetaData
import org.bukkit.entity.Player

fun Player.getAudioChannelMeta(): ChannelMetaData? = AudioServerHandler.getChannel(this)