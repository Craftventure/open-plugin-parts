package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.md_5.bungee.chat.ComponentSerializer

@JsonClass(generateAdapter = true)
data class ChatAction(
    @Expose val message: String?
) : ScriptAction() {
    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
        if (message == null) return
        if (message.startsWith("{") || message.startsWith("[")) {
            val components = ComponentSerializer.parse(message)
            for (player in entityTracker!!.players) {
                player.sendMessage(*components)
            }
        } else {
            for (player in entityTracker!!.players) {
                player.sendMessage(message)
            }
        }
    }

    override val actionTypeId: Int
        get() = Type.CHAT
}