package net.craftventure.core.listener

import net.craftventure.database.MainRepositoryProvider
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent

class DeathListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        event.deathMessage(null)

        val player = event.player
        val lastDamageCause = player.lastDamageCause
        val repo = MainRepositoryProvider.achievementProgressRepository

        when (lastDamageCause?.cause) {
            EntityDamageEvent.DamageCause.PROJECTILE ->
                repo.increaseCounter(event.player.uniqueId, "death_projectile")
            EntityDamageEvent.DamageCause.SUFFOCATION ->
                repo.increaseCounter(event.player.uniqueId, "death_suffocation")
            EntityDamageEvent.DamageCause.FALL ->
                repo.increaseCounter(event.player.uniqueId, "death_fall")
            EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK ->
                repo.increaseCounter(event.player.uniqueId, "death_fire")
            EntityDamageEvent.DamageCause.LAVA ->
                repo.increaseCounter(event.player.uniqueId, "death_lava")
            EntityDamageEvent.DamageCause.DROWNING ->
                repo.increaseCounter(event.player.uniqueId, "death_drown")
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ->
                repo.increaseCounter(event.player.uniqueId, "death_explode")
            EntityDamageEvent.DamageCause.VOID ->
                repo.increaseCounter(event.player.uniqueId, "death_void")
            EntityDamageEvent.DamageCause.LIGHTNING ->
                repo.increaseCounter(event.player.uniqueId, "death_lightning")
            EntityDamageEvent.DamageCause.POISON ->
                repo.increaseCounter(event.player.uniqueId, "death_poison")
            EntityDamageEvent.DamageCause.FLY_INTO_WALL ->
                repo.increaseCounter(event.player.uniqueId, "death_fly_wall")
            EntityDamageEvent.DamageCause.HOT_FLOOR ->
                repo.increaseCounter(event.player.uniqueId, "death_hot_floor")
            EntityDamageEvent.DamageCause.FREEZE ->
                repo.increaseCounter(event.player.uniqueId, "death_frozen")
            EntityDamageEvent.DamageCause.CONTACT ->
                repo.increaseCounter(event.player.uniqueId, "death_contact")
            else -> {}
        }
        repo.increaseCounter(event.player.uniqueId, "death_general")
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val repo = MainRepositoryProvider.achievementProgressRepository
        repo.increaseCounter(event.player.uniqueId, "respawn")
    }
}
