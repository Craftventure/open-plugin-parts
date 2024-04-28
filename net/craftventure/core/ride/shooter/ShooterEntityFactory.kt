package net.craftventure.core.ride.shooter

import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.actor.ActorPlayback
import net.craftventure.core.npc.actor.RecordingData
import net.craftventure.core.ride.shooter.config.ShooterSceneEntityConfig
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.World

class ShooterEntityFactory(
    val id: String,
    val context: ShooterRideContext,
    val data: RecordingData
) {
    fun createPlayback(world: World, config: ShooterSceneEntityConfig): ShooterEntity {
        return ShooterEntity(
            id,
            context,
            createPlaybackOnly(world),
            config
        )
    }

    fun createPlaybackOnly(world: World): ActorPlayback {
        val start = data.getFirstLocation(world)!!
        val npcEntity = NpcEntity(
            id,
            data.getPreferredType()!!,
            start,
            if (data.getGameProfile() != null) MainRepositoryProvider.cachedGameProfileRepository.findCached(data.getGameProfile())
            else null,
        )
        if (data.getEquipment() != null) {
            data.getEquipment().equip(npcEntity)
        }
        return ActorPlayback(npcEntity, data)
    }
}