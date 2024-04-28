package net.craftventure.bukkit.ktx.manager

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.chat.bungee.util.CVTextColor
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.*


class MessageBarMeta(
    val player: Player
) : BasePlayerMetadata(player) {
    private val messageList = LinkedList<MessageBarManager.Message>()

    fun sortMessages() {
        messageList.sortWith(MessageBarManager.Message.messageComparator)
    }

    fun removeById(id: String): Boolean {
        return messageList.removeAll { it.id == id }.also {
//            logcat { "Removed invalid messages" }
        }
    }

    fun removeByType(type: MessageBarManager.Type): Boolean {
        return messageList.removeAll { it.type == type }.also {
//            logcat { "Removed invalid messages" }
        }
    }

    fun removeInvalidMessages() {
        messageList.removeAll { !it.isValid() }.also {
//            if (it)
//                logcat { "Removed invalid messages" }
        }
    }

    fun getMessageList(): MutableList<MessageBarManager.Message> {
        return messageList
    }

    fun addMessage(message: MessageBarManager.Message, replace: Boolean = false): Boolean {
//        logcat { "Add message ${message.id} replace=$replace" }
        if (replace) {
            val replaceIndex = messageList.indexOfFirst { it.id == message.id }
            if (replaceIndex >= 0) {
                val existingItem = messageList[replaceIndex]
                messageList[replaceIndex] = message
                return existingItem.text != message.text
            } else {
                messageList.removeAll { it.id == message.id }
                return messageList.add(message).also {
                    if (it)
                        sortMessages()
                }
            }
        } else {
            if (messageList.any { it.id == message.id }) {
//                logcat { "Returning early" }
                return false
            }
            return messageList.add(message).also {
//                logcat { "Added $it" }
                if (it)
                    sortMessages()
            }
        }
    }

    override fun debugComponent() =
        Component.text("count=${messageList.size} ids=${messageList.joinToString { it.id }}", CVTextColor.serverNotice)

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { MessageBarMeta(player) }
    }

    companion object {
        fun get(player: Player) = player.getMetadata<MessageBarMeta>()
    }
}
