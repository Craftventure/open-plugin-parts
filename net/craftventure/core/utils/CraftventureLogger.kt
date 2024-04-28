package net.craftventure.core.utils

import io.sentry.Sentry
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.plugin.Environment
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.BukkitLogger.Companion.color
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.api.CvApi
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeMain
import net.craftventure.core.ktx.logging.asLog
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.Permissions
import net.kyori.adventure.text.Component
import okhttp3.MultipartBody
import okhttp3.Request
import org.bukkit.Bukkit
import java.time.LocalDateTime
import java.util.*

@Deprecated(message = "Use logcat")
class CraftventureLogger : Logger.Tree {
    var recentErrors = mutableListOf<Pair<String, LocalDateTime>>()
        private set
    private val locale by lazy { Locale.US }//Locale("nl", "NL") }
    override fun captureException(throwable: Throwable) {
        recentErrors.add(throwable.asLog() to LocalDateTime.now())

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
//        if (CraftventureCore.getEnvironment() == CraftventureCore.Environment.PRODUCTION)
        Sentry.captureException(throwable)
    }

    override fun doLog(tag: String, level: Logger.Level, log: String, logToCrew: Boolean, vararg params: Any?) {
        if (level == Logger.Level.DEBUG && PluginProvider.environment == Environment.PRODUCTION) return

        val message = level.color + "[$tag] " + prepareMessage(log, *params)
        Bukkit.getConsoleSender().sendMessage(message)
//        Bukkit.getServer().logger.log(level, message)
        if (logToCrew)
            Bukkit.broadcast(level.color /*+ "[LOG/${level.tag}] "*/ + prepareMessage(log, *params), Permissions.CREW)
    }

    private fun prepareMessage(log: String, vararg params: Any? = emptyArray()): String {
        return try {
            if (params.isNotEmpty())
                log.format(locale = locale, args = *params)
            else
                log
        } catch (e: Exception) {
//            e.printStackTrace()
            log
        }
    }

    companion object {
        fun logToDiscordAndIngame(
            message: String,
            level: Logger.Level = Logger.Level.INFO,
            url: String = CraftventureCore.getSettings().aegisWebhookUrl!!
        ) {
            Logger.log(level, message, true)

            executeAsync {
                try {
                    val call = CvApi.okhttpClient.newCall(
                        Request.Builder()
                            .url(url)
                            .post(
                                MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("content", "`${message.trim()}`")
                                    .build()
                            )
                            .build()
                    )
                    call.execute().close()
                } catch (e: Exception) {
                }
            }
        }
    }

}