package net.craftventure.core.utils

import io.sentry.Sentry
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeMain
import net.craftventure.core.ktx.util.Permissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import java.time.LocalDateTime
import java.util.*

class CraftventureLogcatLogger(minPriority: LogPriority = LogPriority.DEBUG) : LogcatLogger {
    var recentErrors = mutableListOf<Pair<String, LocalDateTime>>()
        private set

    private val minPriorityInt: Int = minPriority.priorityInt

    override fun isLoggable(priority: LogPriority): Boolean = priority.priorityInt >= minPriorityInt

    override fun log(
        priority: LogPriority,
        tag: String,
        message: String,
        logToCrew: Boolean,
    ) {
        val priorityColor = when (priority) {
            LogPriority.VERBOSE -> verboseColor
            LogPriority.DEBUG -> debugColor
            LogPriority.INFO -> infoColor
            LogPriority.WARN -> warningColor
            LogPriority.ERROR -> errorColor
            LogPriority.ASSERT -> assertColor
        }
        val component = Component.text("[$tag] $message", priorityColor)
        Bukkit.getConsoleSender().sendMessage(component)
//        Bukkit.getServer().logger.log(level, message)

        if (logToCrew)
            Bukkit.broadcast(priorityColor + message, Permissions.CREW)


    }

    override fun logException(priority: LogPriority, throwable: Throwable) {
        if (PluginProvider.isNonProductionServer()) {
            throwable.printStackTrace()
            val crew = Bukkit.getOnlinePlayers().filter { it.isCrew() }
            if (crew.isNotEmpty()) {
                val component = Component.text(
                    "Error: ${throwable.message} (click to copy)",
                    CVTextColor.serverError
                )
                    .clickEvent(
                        net.kyori.adventure.text.event.ClickEvent.copyToClipboard(
                            Base64.getEncoder().encodeToString(throwable.stackTraceToString().encodeToByteArray())
                        )
                    )
                executeMain {
                    crew.forEach { it.sendMessage(component) }
                }
            }
            recentErrors = recentErrors.takeLast(500).toMutableList()
        } else {
            recentErrors = recentErrors.takeLast(50).toMutableList()
        }

        Sentry.captureException(throwable)
    }

    companion object {
        val debugColor = TextColor.fromHexString("#2786BB")!!
        val verboseColor = TextColor.fromHexString("#6D6D6D")!!
        val infoColor = TextColor.fromHexString("#B4B4B4")!!
        val warningColor = TextColor.fromHexString("#B49204")!!
        val errorColor = TextColor.fromHexString("#FF1217")!!
        val assertColor = TextColor.fromHexString("#FF6B68")!!
    }
}