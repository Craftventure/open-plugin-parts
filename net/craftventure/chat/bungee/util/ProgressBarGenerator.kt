package net.craftventure.chat.bungee.util

import net.craftventure.chat.core.util.SpaceHelper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object ProgressBarGenerator {
    private val widths = listOf(
        "▏", "▎", "▍", "▌", "▋", "▊", "▉", "█",
    )

    fun blockStyle(
        widthInCharacterPixels: Int,
        progress: Double,
        progressColor: TextColor? = null,
        remainingColor: TextColor? = null,
        percentageText: Boolean = false,
        percentageTextColorOnProgress: TextColor = NamedTextColor.WHITE,
        percentageTextColorOnRemaining: TextColor = NamedTextColor.WHITE,
        progressTextProvider: () -> String = { "${floor(progress.coerceIn(0.0, 1.0) * 100).roundToInt()}%" },
        percentageTextCentered: Boolean = true,
    ): Component {
        val clampedProgress = progress.coerceIn(0.0, 1.0)
        val widthProgress = (widthInCharacterPixels * clampedProgress).roundToInt()
        val widthRemaining = widthInCharacterPixels - widthProgress

//        logcat { "Compiling ${clampedProgress.format(2)} $widthProgress $widthRemaining" }

        val component = Component.text()
        component.append(generateBar(widthProgress, progressColor))
        if (remainingColor != null)
            component.append(generateBar(widthRemaining, remainingColor))
        else
            component.append(Component.text(SpaceHelper.width(widthRemaining)))

        if (percentageText) {
            val progressText = progressTextProvider()
            val textWidth = FontUtils.width(progressText)
            if (percentageTextCentered) {
                val offset = ceil((widthInCharacterPixels - textWidth) * 0.5).roundToInt()
                component.append(
                    Component.text(
                        SpaceHelper.width(
                            -widthInCharacterPixels + offset,
                            noSplit = false
                        )
                    )
                )
                component.append(
                    Component.text(
                        progressText,
                        if (clampedProgress >= 0.5) percentageTextColorOnProgress else percentageTextColorOnRemaining
                    )
                )
                component.append(Component.text(SpaceHelper.width(widthInCharacterPixels - offset - textWidth)))
            } else {

//                if (widthProgress > widthRemaining) {
//                    val offset = ceil((widthProgress - textWidth) * 0.5).roundToInt()
//                    component.append(Component.text(SpaceHelper.width(-widthInCharacterPixels + offset, noSplit = false)))
//                    component.append(Component.text(progressText, percentageTextColorOnProgress))
//                    component.append(Component.text(SpaceHelper.width(widthInCharacterPixels - offset - textWidth)))
//                } else {
//                    val offset = ceil((widthProgress - textWidth) * 0.5).roundToInt() + widthProgress
//                    component.append(Component.text(SpaceHelper.width(-widthInCharacterPixels + offset, noSplit = false)))
//                    component.append(Component.text(progressText, percentageTextColorOnRemaining))
//                    component.append(Component.text(SpaceHelper.width(widthInCharacterPixels - offset - textWidth)))
//                }

                val padding = 2
                if (widthProgress > widthRemaining) {
                    component.append(
                        Component.text(
                            SpaceHelper.width(
                                -widthInCharacterPixels + widthProgress - textWidth - padding,
                                noSplit = false
                            )
                        )
                    )
                    component.append(Component.text(progressText, percentageTextColorOnProgress))
                    component.append(Component.text(SpaceHelper.width(widthInCharacterPixels - widthProgress + padding)))
                } else {
                    component.append(
                        Component.text(
                            SpaceHelper.width(
                                -widthInCharacterPixels + widthProgress + padding,
                                noSplit = false
                            )
                        )
                    )
                    component.append(Component.text(progressText, percentageTextColorOnRemaining))
                    component.append(Component.text(SpaceHelper.width(widthInCharacterPixels - widthProgress - textWidth - padding)))
                }
            }
        }

        return component.build()
    }

    fun generateBar(width: Int, color: TextColor?): Component {
        var pixelsLeftToFill = width
        val component = Component.text()
            .style(Style.style(color))
        while (pixelsLeftToFill > 0) {
            when {
                pixelsLeftToFill >= 8 -> {
                    component.append(Component.text(widths[7]))
                    pixelsLeftToFill -= 8
                }

                pixelsLeftToFill == 7 -> {
                    component.append(Component.text(widths[6]))
                    pixelsLeftToFill = 0
                }

                pixelsLeftToFill == 6 -> {
                    component.append(Component.text(widths[5]))
                    pixelsLeftToFill = 0
                }

                pixelsLeftToFill == 5 -> {
                    component.append(Component.text(widths[4]))
                    pixelsLeftToFill = 0
                }

                pixelsLeftToFill == 4 -> {
                    component.append(Component.text(widths[3]))
                    pixelsLeftToFill = 0
                }

                pixelsLeftToFill == 3 -> {
                    component.append(Component.text(widths[2]))
                    pixelsLeftToFill = 0
                }

                pixelsLeftToFill == 2 -> {
                    component.append(Component.text(widths[1]))
                    pixelsLeftToFill = 0
                }

                else -> {
                    component.append(Component.text(widths[0]))
                    pixelsLeftToFill = 0
                }
            }
            component.append(Component.text(SpaceHelper.width(-1)))
        }
        return component.build()
    }
}