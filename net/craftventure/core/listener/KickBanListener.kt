package net.craftventure.core.listener

import org.bukkit.event.Listener


class KickBanListener : Listener {
//    @EventHandler
//    fun onPlayerKick(event: PlayerKickEvent) {
//        Logger.info("${event.player.name} kicked")
//        KickEffect(event.player).runTaskTimer(CraftventureCore.getInstance(), 1, 1)
//    }
//
//    class KickEffect(private val player: Player) : BukkitRunnable() {
//        private var height = 0.0
//        private var tick = 0
//        private var angle = 0.0
//
//        override fun run() {
//            height += 0.08
//
//            val location = player.location
//            location.world.spawnParticle(Particle.EXPLOSION_NORMAL,
//                    location.x + Math.cos(angle), location.y + height, location.z + Math.sin(angle),
//                    1,
//                    0.0, 0.0, 0.0,
//                    0.0)
//            location.world.spawnParticle(Particle.EXPLOSION_NORMAL,
//                    location.x + Math.cos(-angle), location.y + (20.0 * 1.5 * 0.08 - height), location.z + Math.sin(-angle),
//                    1,
//                    0.0, 0.0, 0.0,
//                    0.0)
//
//            angle += 0.5
//            tick++
//            if (tick > 20 * 1.5) {
//                cancel()
//            }
//        }
//    }
}
