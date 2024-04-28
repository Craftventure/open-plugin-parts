package net.craftventure.core.utils

import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.chat.bungee.extension.plus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration
import java.time.temporal.ChronoUnit

@Deprecated("Use TitleManager instead")
object TitleUtil {
    @JvmStatic
    @Deprecated("Use TitleManager instead")
    fun Player.sendTitleWithTicks(
        `in`: Int = 10,
        stay: Int,
        out: Int = 20,
        title: Component? = null,
        subtitle: Component? = null,
    ) {
        val times = Title.Times.times(
            Duration.of(`in` * 50L, ChronoUnit.MILLIS),
            Duration.of(stay * 50L, ChronoUnit.MILLIS),
            Duration.of(out * 50L, ChronoUnit.MILLIS)
        )

        displayTitle(
            TitleManager.TitleData(
                times = times,
                title = title,
                subtitle = subtitle,
            )
        )

//        val title = Title.title(title ?: Component.empty(), subtitle ?: Component.empty(), times)
//        this.showTitle(title)
    }

    @JvmStatic
    @Deprecated("Use components where possible")
    fun Player.sendTitleWithTicks(
        `in`: Int = 10,
        stay: Int,
        out: Int = 20,
        titleColor: TextColor = NamedTextColor.GOLD,
        title: String? = null,
        subtitleColor: TextColor = NamedTextColor.YELLOW,
        subtitle: String? = null,
    ) = sendTitleWithTicks(
        `in`,
        stay,
        out,
        title?.let { Component.text("", titleColor) + LegacyComponentSerializer.legacySection().deserialize(it) },
        subtitle?.let { Component.text("", subtitleColor) + LegacyComponentSerializer.legacySection().deserialize(it) },
    )
}
