package net.craftventure.bukkit.ktx.manager

import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.logging.logcat
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.*


class BossBarMeta(
    val player: Player
) : BasePlayerMetadata(player) {
    private val messageList = LinkedList<BossBarManager.Message>()
    private val bossBars = LinkedList<BossBar>()

    private var lastDisplayedMessages: List<BossBarManager.Message> = emptyList()

    fun displayCurrentMessages() {
//        logcat { "Displaying" }
        val messages = messageList.take(3)
        if (messages == lastDisplayedMessages) {
//            logcat { "Returning (not changed)" }
            return
        }
        lastDisplayedMessages = messages

        bossBars.forEach { player.hideBossBar(it) }
        bossBars.clear()
        messages.forEach { message ->
            if (message.text.isEmpty()) {
//            logcat { "Returning (null || empty)" }
                return@forEach
            }

//        logcat { "DIsplaying" }
            message.text.forEach { text ->
                BossBar.bossBar(text, 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
                    .apply {
                        bossBars.add(this)
                        player.showBossBar(this)
                    }
            }
        }
    }

    fun sortMessages() {
        messageList.sortWith(BossBarManager.Message.messageComparator)
    }

    fun removeById(id: String): Boolean {
        return messageList.removeAll { it.id == id }.also {
//            logcat { "Removed invalid messages" }
        }
    }

    fun removeInvalidMessages() {
        messageList.removeAll { !it.isValid() }.also {
//            if (it)
//                logcat { "Removed invalid messages" }
        }
    }

    fun getMessageList(): MutableList<BossBarManager.Message> {
        return messageList
    }

    fun addMessage(
        message: BossBarManager.Message,
        replace: Boolean = false,
        playNotificationOnMessageChange: Boolean = false,
        forcePlaySound: Boolean = false,
    ): Boolean {
        logcat { "Add message ${message.id} replace=$replace" }
        if (replace) {
            val existing = messageList.find { it.id == message.id }
            val removed = messageList.removeAll { it.id == message.id }
            if (forcePlaySound || !removed || (playNotificationOnMessageChange && message.text.contentEquals(existing?.text))) {
                playNotification()
            }
            return messageList.add(message).also {
                if (it)
                    sortMessages()
            }
        } else {
            if (messageList.any { it.id == message.id }) {
                logcat { "Returning early" }
                return false
            }
            return messageList.add(message).also {
                logcat { "Added $it" }
                playNotification()
                if (it)
                    sortMessages()
            }
        }
    }

    private fun playNotification() {
        logcat { "Playing sound" }
        player.playSound(player.location, "minecraft:gui.messageroller", 1f, 1f)
    }

    override fun debugComponent() =
        Component.text("count=${messageList.size} ids=${messageList.joinToString { it.id }}", CVTextColor.serverNotice)

    companion object {
        fun getOrCreate(player: Player) = player.getOrCreateMetadata { BossBarMeta(player) }
        fun get(player: Player) = player.getMetadata<BossBarMeta>()
    }
}
