package net.craftventure.core.ride.shooter

import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.script.particle.ParticleMap
import net.craftventure.core.script.particle.ParticlePlayback

class ShooterParticlePathFactory(
    val id: String,
    val context: ShooterRideContext,
    val data: ParticleMap
) {
    fun createPlayback(tracker: NpcAreaTracker): List<ParticlePlayback> {
        return data.paths.map { ParticlePlayback(it, tracker, 0) }
    }
}