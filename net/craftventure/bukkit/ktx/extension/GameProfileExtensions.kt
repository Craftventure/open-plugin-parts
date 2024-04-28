package net.craftventure.bukkit.ktx.extension

import com.destroystokyo.paper.profile.PlayerProfile
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

fun PlayerProfile.toSkullItem(): ItemStack {
    val skullItem = ItemStack(Material.PLAYER_HEAD)
    val meta = skullItem.itemMeta as SkullMeta
    meta.playerProfile = this

    skullItem.itemMeta = meta
    return skullItem
}

fun UUID.toSkullItem(): ItemStack {
    val skullItem = ItemStack(Material.PLAYER_HEAD)
    val meta = skullItem.itemMeta as SkullMeta
    meta.playerProfile = Bukkit.createProfile(this, null)

    skullItem.itemMeta = meta
    return skullItem
}

fun PlayerProfile.getTextureDataBase64(): String? = this.properties.firstOrNull { it.name == "textures" }?.value