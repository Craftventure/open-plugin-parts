package net.craftventure.bukkit.ktx.extension

import net.craftventure.bukkit.ktx.util.fastForEach
import net.craftventure.core.ktx.util.Logger
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ServerGamePacketListener
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

fun Player.isConnected(): Boolean = !isDisconnected()

fun Player.isDisconnected(): Boolean = (this as? CraftPlayer)?.handle?.connection?.isDisconnected ?: true

fun LivingEntity.removeAllPotionEffects(predicate: (PotionEffect) -> Boolean = { true }) {
    val effects = this.activePotionEffects
    for (effect in effects) {
        if (predicate(effect))
            removePotionEffect(effect.type)
    }
}

fun LivingEntity.renewPotionEffect(
    potionEffectType: PotionEffectType,
    duration: Int,
    amplifier: Int = 0,
    ambient: Boolean = true,
    particles: Boolean = false,
    icon: Boolean = false
) {
    addPotionEffect(PotionEffect(potionEffectType, duration, amplifier, ambient, particles, icon))
}

fun Player.receivePacket(packet: Packet<ServerGamePacketListener>) {
    packet.handle((this as CraftPlayer).handle.connection)
}

fun Player.receivePacketIgnoreError(packet: Packet<ServerGamePacketListener>) {
    try {
        packet.handle((this as CraftPlayer).handle.connection)
    } catch (e: Exception) {
        Logger.capture(e)
    }
}

fun Player.sendPacket(packet: Packet<ClientGamePacketListener>) {
    (this as CraftPlayer).handle.connection.send(packet)
}

fun Player.sendPacketIgnoreError(packet: Packet<ClientGamePacketListener>, log: Boolean = true) {
    try {
        (this as CraftPlayer).handle.connection.send(packet)
    } catch (e: Exception) {
        Logger.capture(e)
    }
}

fun List<Player>.sendPacket(packet: Packet<ClientGamePacketListener>) {
    fastForEach {
        it.sendPacket(packet)
    }
}

fun Collection<Player>.sendPacket(packet: Packet<ClientGamePacketListener>) {
    forEach {
        it.sendPacket(packet)
    }
}

fun List<Player>.sendPacketIgnoreError(packet: Packet<ClientGamePacketListener>, log: Boolean = true) {
    fastForEach {
        it.sendPacketIgnoreError(packet, log)
    }
}

fun Collection<Player>.sendPacketIgnoreError(packet: Packet<ClientGamePacketListener>, log: Boolean = true) {
    forEach {
        it.sendPacketIgnoreError(packet, log)
    }
}