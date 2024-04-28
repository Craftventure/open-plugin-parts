package net.craftventure.bukkit.ktx.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.bukkit.potion.PotionEffectType

class PotionEffectTypeAdapter {
    @FromJson
    fun fromJson(json: String) = PotionEffectType.getByName(json)

    @ToJson
    fun toJson(instance: PotionEffectType) = instance.name
}