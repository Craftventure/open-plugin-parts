package net.craftventure.bukkit.ktx.manager

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.coroutine.executeSync
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.chat.bungee.util.CVTextColor
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*


class TitleMeta(
    val player: Player
) : BasePlayerMetadata(player) {
    private val titles = LinkedList<TitleManager.TitleData>()
    private var task: Int? = null
    var lastTitleData: TitleManager.TitleData? = null

    private fun updateTask() {
        if (titles.isEmpty()) {
            if (task == null) {
//                logcat { "Clearing update task" }
            }
            task?.let { Bukkit.getScheduler().cancelTask(it) }
            task = null
        } else {
            if (task == null) {
//                logcat { "Setup task" }
                task = executeSync(20, 20) {
                    TitleManager.trigger(player)
                }
            }
        }
    }

    private fun sortTitles() {
        titles.sortWith(TitleManager.TitleData.messageComparator)
        updateTask()
    }

    fun removeAll() {
        titles.clear()
        lastTitleData = null
    }

    fun removeById(id: String): Boolean {
        return titles.removeAll { it.id == id }.also {
            if (it)
                updateTask()
//            logcat { "Removed invalid messages" }
        }
    }

    fun removeByType(type: TitleManager.Type): Boolean {
        return titles.removeAll { it.type == type }.also {
            if (it)
                updateTask()
//            logcat { "Removed invalid messages" }
        }
    }

    fun removeExpiredTitles() {
        titles.removeAll { it.hasExpired() }.also {
            if (it) {
//                logcat { "Removed invalid messages" }
                updateTask()
            }
        }
    }

    fun getMessageList(): MutableList<TitleManager.TitleData> {
        return titles
    }

    fun addMessage(message: TitleManager.TitleData, replace: Boolean = false): Boolean {
//        logcat { "Add title ${message.id} replace=$replace" }
        if (replace) {
            val replaceIndex = titles.indexOfFirst { it.id == message.id }
            if (replaceIndex >= 0) {
                val existingItem = titles[replaceIndex]
                titles[replaceIndex] = message
                val changed =
                    existingItem.title != message.title || existingItem.subtitle != message.subtitle || existingItem.times != message.times
                if (changed) {
                    updateTask()
                }
                return changed
            } else {
                titles.removeAll { it.id == message.id }
                return titles.add(message).also {
                    if (it) {
                        sortTitles()
                        updateTask()
                    }
                }
            }
        } else {
            if (titles.any { it.id == message.id }) {
//                logcat { "Returning early" }
                return false
            }
            return titles.add(message).also {
//                logcat { "Added $it" }
                if (it) {
                    sortTitles()
                    updateTask()
                }
            }
        }
    }

    override fun debugComponent() =
        Component.text("count=${titles.size} ids=${titles.joinToString { it.id }}", CVTextColor.serverNotice)

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { TitleMeta(player) }
    }

    companion object {
        fun get(player: Player) = player.getMetadata<TitleMeta>()
    }
}
