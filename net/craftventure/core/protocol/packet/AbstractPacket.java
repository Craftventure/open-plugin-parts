package net.craftventure.core.protocol.packet;

import org.bukkit.entity.Player;


public abstract class AbstractPacket {
    public abstract void sendPlayer(Player player);
}
