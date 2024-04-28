package net.craftventure.bukkit.ktx.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import net.craftventure.bukkit.ktx.extension.colorFromHex
import net.craftventure.bukkit.ktx.extension.toHexColor
import org.bukkit.Color

class ColorAdapter {
    @FromJson
    fun fromJson(json: String) = colorFromHex(json)

    @ToJson
    fun toJson(instance: Color) = instance.toHexColor()
}