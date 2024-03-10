package net.craftventure.core.feature.balloon.extensions

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.protocol.ProtocolLeash
import org.bukkit.entity.EntityType

// Will disable itself if the holder doesn't support leashes
class LeashExtension : ExtensibleBalloon.Extension() {
    private var protocolLeash: ProtocolLeash? = null
    private var leashHolder: NpcEntity? = null

    override fun spawn(balloon: ExtensibleBalloon, balloonHolder: BalloonHolder, tracker: NpcEntityTracker) {
        val holderEntityId = balloonHolder.leashHolderEntityId

        if (holderEntityId == null) {
            enabled = false
            return
        } else {
            enabled = true
        }

        leashHolder = NpcEntity(
            "balloonLeash",
            EntityType.TURTLE,
            balloon.balloonLocation!!.clone().add(0.0, 0.0, 0.0)
        )
        leashHolder!!.setMetadata(EntityMetadata.Ageable.baby, true)
//        leashHolder!!.marker(true)
        leashHolder!!.invisible(true)
        tracker.addEntity(leashHolder!!)

        protocolLeash = ProtocolLeash(leashHolder!!.entityId, holderEntityId)
        protocolLeash!!.create()
    }

    override fun update(balloon: ExtensibleBalloon) {
        super.update(balloon)
        leashHolder!!.move(balloon.balloonLocation!!.clone().add(0.0, 0.0, 0.0).apply {
            yaw = 0f
            pitch = 0f
        })
    }

    override fun despawn(balloon: ExtensibleBalloon, withEffects: Boolean, tracker: NpcEntityTracker) {
        if (leashHolder != null)
            tracker.removeEntity(leashHolder!!)
        protocolLeash?.destroy()
        protocolLeash = null
    }

    @JsonClass(generateAdapter = true)
    class Json : ExtensibleBalloon.Extension.Json() {
        override fun toExtension(): ExtensibleBalloon.Extension = LeashExtension()

        companion object {
            const val type = "leash"
        }
    }
}