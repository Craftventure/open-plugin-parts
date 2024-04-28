package net.craftventure.core.script.action

import net.craftventure.core.npc.tracker.NpcEntityTracker

abstract class ScriptAction {
    var groupId: String? = null
    var sceneName: String? = null

    constructor(groupId: String?, sceneName: String?) {
        this.groupId = groupId
        this.sceneName = sceneName
    }

    constructor()

    abstract fun validate(): Boolean
    fun setGroupIdAndName(groupId: String, name: String) {
        this.groupId = groupId
        this.sceneName = name
    }

    /**
     * This method is executed asynchronously!
     *
     * @param entityTracker
     */
    abstract fun execute(entityTracker: NpcEntityTracker?)

    abstract val actionTypeId: Int

    interface Type {
        companion object {
            const val PLACE_SCHEMATIC = 1
            const val BLOCK_CHANGE = 2
            const val STRIKE_LIGHTNING = 3
            const val CHAT = 4
            const val CHAT_AS = 5
            const val PLAY_SOUND = 6
            const val POTION_EFFECT = 7
            const val BLOCK_ACTION = 8
            const val FALLING_AREA = 9
            const val EFFECT = 10
            const val LIGHTING = 11
        }
    }
}