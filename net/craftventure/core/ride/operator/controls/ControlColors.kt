package net.craftventure.core.ride.operator.controls

import net.craftventure.bukkit.ktx.extension.colorFromHex
import org.bukkit.Color

object ControlColors {
    @JvmStatic
    val NEUTRAL_DARK = colorFromHex("#7b7b7b")

    @JvmStatic
    val NEUTRAL = Color.WHITE

    @JvmStatic
    val GREEN_BRIGHT = colorFromHex("#21b11a")

    @JvmStatic
    val GREEN = colorFromHex("#495b24")

    @JvmStatic
    val RED_BRIGHT = colorFromHex("#b92424")

    @JvmStatic
    val RED = colorFromHex("#8e2020")

    @JvmStatic
    val BLUE = colorFromHex("#2e3483")
}