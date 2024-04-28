package net.craftventure.core.ride.shooter

import net.craftventure.core.npc.actor.ActorPlayback
import net.craftventure.core.ride.shooter.config.ShooterSceneEntityConfig

class ShooterEntity(
    val id: String,
    val context: ShooterRideContext,
    val playback: ActorPlayback,
    val config: ShooterSceneEntityConfig
) {
    val hitActions = config.onHit.map { it.toAction() }
}