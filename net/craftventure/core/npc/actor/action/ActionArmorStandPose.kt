package net.craftventure.core.npc.actor.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.NpcEntity

@JsonClass(generateAdapter = true)
data class ActionArmorStandPose(
    @field:Expose val type: Int,
    @field:Expose val x: Float,
    @field:Expose val y: Float,
    @field:Expose val z: Float
) : ActorAction() {

    override fun executeAction(npcEntity: NpcEntity?) {
        when (type) {
            PoseType.BODY -> npcEntity!!.body(x, y, z)
            PoseType.HEAD -> npcEntity!!.head(x, y, z)
            PoseType.LEFT_ARM -> npcEntity!!.leftArm(x, y, z)
            PoseType.RIGHT_ARM -> npcEntity!!.rightArm(x, y, z)
            PoseType.LEFT_LEG -> npcEntity!!.leftLeg(x, y, z)
            PoseType.RIGHT_LEG -> npcEntity!!.rightLeg(x, y, z)
        }
    }

    override val actionTypeId: Int
        get() = Type.ARMORSTAND_POSE

    interface PoseType {
        companion object {
            const val LEFT_ARM = 1
            const val RIGHT_ARM = 2
            const val LEFT_LEG = 3
            const val RIGHT_LEG = 4
            const val BODY = 5
            const val HEAD = 6
        }
    }

}