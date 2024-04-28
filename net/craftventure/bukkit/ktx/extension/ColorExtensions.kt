package net.craftventure.bukkit.ktx.extension

import net.craftventure.bukkit.ktx.util.ColorNamer
import org.bukkit.Color
import java.util.*

fun colorFromHex(colorString: String): Color {
    if (colorString.isEmpty()) {
        throw IllegalArgumentException("Invalid color specification")
    }

    var color = colorString.toLowerCase(Locale.ROOT)

    if (color.startsWith("#")) {
        color = color.substring(1)
    } else {
        throw IllegalStateException("Failed to parse color $colorString")
    }

    val len = color.length

    try {
        val r: Int
        val g: Int
        val b: Int

        when (len) {
            3 -> {
                r = Integer.parseInt(color.substring(0, 1), 16)
                g = Integer.parseInt(color.substring(1, 2), 16)
                b = Integer.parseInt(color.substring(2, 3), 16)
                return Color.fromRGB((r / 15.0).toInt(), (g / 15.0).toInt(), (b / 15.0).toInt())
            }
            4 -> {
                r = Integer.parseInt(color.substring(0, 1), 16)
                g = Integer.parseInt(color.substring(1, 2), 16)
                b = Integer.parseInt(color.substring(2, 3), 16)
                return Color.fromRGB((r / 15.0).toInt(), (g / 15.0).toInt(), (b / 15.0).toInt())
            }
            6 -> {
                r = Integer.parseInt(color.substring(0, 2), 16)
                g = Integer.parseInt(color.substring(2, 4), 16)
                b = Integer.parseInt(color.substring(4, 6), 16)
                return Color.fromRGB(r, g, b)
            }
            8 -> {
                r = Integer.parseInt(color.substring(0, 2), 16)
                g = Integer.parseInt(color.substring(2, 4), 16)
                b = Integer.parseInt(color.substring(4, 6), 16)
                return Color.fromRGB(r, g, b)
            }
            else -> {
            }
        }
    } catch (nfe: NumberFormatException) {
    }


    throw IllegalArgumentException("Invalid color specification")
}

fun Color.distanceTo(other: Color): Int {
    val redDifference = this.red - other.red
    val greenDifference = this.green - other.green
    val blueDifference = this.blue - other.blue

    return redDifference * redDifference + greenDifference * greenDifference + blueDifference * blueDifference
}

fun Color.named() = ColorNamer.nameForColor(this)

fun Color.toHexColor() = "#" + Integer.toHexString(this.asRGB())
fun Int.toHexColor() = "#" + Integer.toHexString(this)

/**
 * amount: 0-255
 */
fun Color.multiply(amount: Int): Color = multiply(amount / 255.0)
fun Color.multiply(multiplier: Double): Color {
    return Color.fromRGB((red * multiplier).toInt(), (green * multiplier).toInt(), (blue * multiplier).toInt())
}