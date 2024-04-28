package net.craftventure.core.npc.tracker

import org.bukkit.entity.Player

class ManualNpcTracker(val player: Player) : NpcEntityTracker() {
    override fun onStartTracking() {
        super.onStartTracking()
        addPlayer(player)
    }
}