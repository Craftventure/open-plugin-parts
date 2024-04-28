package net.craftventure.core.npc.actor.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.NpcEntity

@JsonClass(generateAdapter = true)
class ActionDoubleSetting constructor(
    @field:Expose val value: Double
) : ActorAction() {
    @Expose
    var type = 0

    fun withType(type: Int): ActionDoubleSetting {
        this.type = type
        return this
    }

    override fun executeAction(npcEntity: NpcEntity?) { //        npcEntity.move(x, y, z, yaw, pitch);
//        npcEntity.moveHead(headYaw);
    }

    override val actionTypeId: Int
        get() = Type.ANIMATED_MOVE

}