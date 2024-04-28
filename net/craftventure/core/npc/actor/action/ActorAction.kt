package net.craftventure.core.npc.actor.action

import net.craftventure.core.npc.NpcEntity

abstract class ActorAction {
    abstract fun executeAction(npcEntity: NpcEntity?)
    abstract val actionTypeId: Int

    interface DoubleSettingType {
        companion object {
            const val x = 1
            const val y = 2
            const val z = 3
            const val yaw = 4
            const val pitch = 5
            const val leftArmX = 6
            const val leftArmY = 7
            const val leftArmZ = 8
            const val rightArmX = 9
            const val rightArmY = 10
            const val rightArmZ = 11
            const val leftLegX = 12
            const val leftLegY = 13
            const val leftLegZ = 14
            const val rightLegX = 15
            const val rightLegY = 16
            const val rightLegZ = 17
            const val bodyX = 18
            const val bodyY = 19
            const val bodyZ = 20
            const val headX = 21
            const val headY = 22
            const val headZ = 23
        }
    }

    interface Type {
        companion object {
            const val MOVE = 1
            const val ARMORSTAND_POSE = 4
            const val EQUIPMENT = 5
            const val ANIMATED_MOVE = 6
            const val STATE_FLAG = 7
            const val DOUBLE_SETTING = 8
        }
    }
}