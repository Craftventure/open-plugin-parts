package net.craftventure.bukkit.ktx.manager

import net.craftventure.bukkit.ktx.manager.BossBarMeta.Companion.get
import net.craftventure.bukkit.ktx.manager.BossBarMeta.Companion.getOrCreate
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

object BossBarManager {
    init {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PluginProvider.getInstance(), {
            for (player in Bukkit.getOnlinePlayers()) {
                updateMessagesForPlayer(player)
            }
        }, 20, 20)
    }

    fun trigger(player: Player) {
        updateMessagesForPlayer(player)
    }

    private fun updateMessagesForPlayer(player: Player) {
        val metaData = get(player)
        if (metaData != null) {
//            logcat { "Updating for meta" }
            metaData.removeInvalidMessages()
            metaData.displayCurrentMessages()
        }
    }

    @JvmStatic
    fun remove(player: Player, id: String) {
        val meta = get(player) ?: return
        if (meta.removeById(id)) {
            updateMessagesForPlayer(player)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun display(
        player: Player,
        message: Message,
        replace: Boolean = false,
        playNotificationOnMessageChange: Boolean = false,
        forcePlaySound: Boolean = false,
    ) {
        val metaData = getOrCreate(player)
        if (metaData.addMessage(
                message,
                replace,
                playNotificationOnMessageChange = playNotificationOnMessageChange,
                forcePlaySound = forcePlaySound,
            )
        ) {
//            logcat { "Trigger" }
            trigger(player)
        }
    }

    class Message(
        val id: String = UUID.randomUUID().toString(),
        val text: Array<Component>,
        /**
         * Higher value will display the topmost
         */
        val priority: Int,
        val untilMillis: Long,
        val since: Long = System.currentTimeMillis(),
    ) {
        fun isValid() = untilMillis > System.currentTimeMillis()

        companion object {
            val messageComparator: Comparator<Message> =
                compareByDescending<Message> { it.priority }.thenBy { it.untilMillis }
        }
    }

    object Priority {
        const val shutdown = 1000
        const val dressingRoom = 110
        const val queue = 100
    }
}