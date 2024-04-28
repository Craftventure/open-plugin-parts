package net.craftventure.bukkit.ktx.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import org.bukkit.entity.Player
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

// I had some issues with this one somewhere in the priority/override logic

object TitleManager {
    var disabled = false

    fun require(player: Player) {
        trigger(player)
    }

    fun trigger(player: Player) {
        updateMessagesForPlayer(player)
    }

    private fun updateMessagesForPlayer(player: Player) {
        val metaData = TitleMeta.get(player)
        if (metaData != null) {
            metaData.removeExpiredTitles()
            val messageList = metaData.getMessageList()
            val firstMessage = messageList.firstOrNull()

            handleTitleUpdate(firstMessage, player, metaData)
        }
    }

    private fun handleTitleUpdate(message: TitleData?, player: Player, metaData: TitleMeta) {
        if (disabled) return

        val currentTitle = metaData.lastTitleData
        val changed = message !== currentTitle

        if (changed) {
            metaData.lastTitleData = message
        }

        if (message != null) {
            if (changed) {
//                logcat { "Updating title ${message.title?.asPlainText()}/${message.subtitle?.asPlainText()}" }
                player.showTitle(
                    Title.title(
                        message.title ?: Component.empty(),
                        message.subtitle ?: Component.empty(),
                        message.actualTimes
                    )
                )
            }
        } else {
            if (changed) {
//                logcat { "Clearing title" }
                player.sendTitlePart(
                    TitlePart.TIMES, Title.Times.times(
                        Duration.ZERO,
                        Duration.ZERO,
                        currentTitle?.actualTimes?.fadeOut() ?: Duration.ZERO,
                    )
                )
//                player.showTitle(
//                    Title.title(
//                        Component.empty(), Component.empty(),
//                        Title.Times.times(
//                            Duration.ZERO,
//                            Duration.ZERO,
//                            Duration.ZERO
//                        )
//                    )
//                )
            }
        }
    }

    @JvmStatic
    fun remove(player: Player, id: String) {
        val meta = TitleMeta.get(player) ?: return
        if (meta.removeById(id)) {
            updateMessagesForPlayer(player)
        }
    }

    @JvmStatic
    fun remove(player: Player, type: Type) {
        val meta = TitleMeta.get(player) ?: return
        if (meta.removeByType(type)) {
            updateMessagesForPlayer(player)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun display(player: Player, message: TitleData, replace: Boolean = false) {
        val metaData = TitleMeta.get(player) ?: return
        if (metaData.addMessage(message, replace)) {
            trigger(player)
        }
    }

    fun Player.displayTitle(message: TitleData, replace: Boolean = false) = display(this, message, replace)

    enum class Type(val value: Int) {
        Shutdown(20000),
        Fade(10000),
        CoinBooster(9000),
        RideQueue(2000),
        Default(1000),
        Afk(100),
        Welcome(80),
    }

    data class TitleData(
        val id: String = UUID.randomUUID().toString(),
        val type: Type = Type.Default,
        val title: Component?,
        val subtitle: Component?,
        val times: Title.Times,
    ) {
        val since: Long = System.currentTimeMillis()
        val totalTimesDuration = times.stay().toMillis() + times.fadeIn().toMillis() + times.fadeOut().toMillis()
        val untilMillis = since + totalTimesDuration

        val timeLeft get() = max(0L, untilMillis - System.currentTimeMillis())

        val actualTimes: Title.Times
            get() {
                val timeLeft = timeLeft
                val stay = times.stay().toMillis()
                val fadeIn = times.fadeIn().toMillis()
                val fadeOut = times.fadeOut().toMillis()
                if (timeLeft - fadeIn - fadeOut > 0) {
                    val actualStay = timeLeft - fadeIn - fadeOut
                    return Title.Times.times(
                        Duration.of(max(0L, timeLeft - actualStay - fadeOut), ChronoUnit.MILLIS),
                        Duration.of(max(0L, timeLeft - fadeIn - fadeOut), ChronoUnit.MILLIS),
                        Duration.of(max(0L, timeLeft - fadeIn - actualStay), ChronoUnit.MILLIS),
                    ).apply {
//                        logcat { "Using shortened for ${this}" }
                    }
                }
                val halfFade = ceil((fadeIn + fadeOut) / 2.0).toLong()
                return Title.Times.times(
                    Duration.of(halfFade, ChronoUnit.MILLIS),
                    Duration.of(0, ChronoUnit.MILLIS),
                    Duration.of(halfFade, ChronoUnit.MILLIS),
                ).apply {
//                    logcat { "Using half title for ${this}" }
                }
            }

        fun hasExpired() = untilMillis < System.currentTimeMillis()

        companion object {
            val messageComparator: Comparator<TitleData> =
                compareByDescending<TitleData> { it.type.value }.thenBy { it.untilMillis }

            fun ofFade(
                id: String,
                type: Type = Type.Fade,
                times: Title.Times,
            ) = TitleData(
                id,
                type,
                title = Component.text("\uE06B", NamedTextColor.BLACK),
                subtitle = null,
                times = times,
            )

            fun ofFade(
                id: String,
                type: Type = Type.Fade,
                fadeIn: Duration,
                stay: Duration,
                fadeOut: Duration,
            ) = TitleData(
                id,
                type,
                title = Component.text("\uE06B", NamedTextColor.BLACK),
                subtitle = null,
                times = Title.Times.times(fadeIn, stay, fadeOut),
            )

            fun of(
                id: String,
                type: Type = Type.Default,
                title: Component?,
                subtitle: Component?,
                fadeIn: Duration,
                stay: Duration,
                fadeOut: Duration,
            ) = TitleData(
                id, type, title, subtitle, Title.Times.times(fadeIn, stay, fadeOut)
            )

            fun ofTicks(
                id: String,
                type: Type = Type.Default,
                title: Component?,
                subtitle: Component?,
                fadeInTicks: Long,
                stayTicks: Long,
                fadeOutTicks: Long,
            ) = TitleData(
                id,
                type,
                title,
                subtitle,
                Title.Times.times(
                    Duration.of(fadeInTicks * 50, ChronoUnit.MILLIS),
                    Duration.of(stayTicks * 50, ChronoUnit.MILLIS),
                    Duration.of(fadeOutTicks * 50, ChronoUnit.MILLIS)
                )
            )
        }
    }
}