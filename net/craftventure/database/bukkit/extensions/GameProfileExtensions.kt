package net.craftventure.database.bukkit.extensions

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.craftventure.bukkit.ktx.extension.toSkullItem
import net.craftventure.database.generated.cvdata.tables.pojos.CachedGameProfile
import net.craftventure.bukkit.ktx.extension.withV2Marker
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.util.*

val CachedGameProfile.skullItem: ItemStack?
    get() {
        return toPlayerProfile().toSkullItem()
    }

@JvmOverloads
fun CachedGameProfile.toPlayerProfile(uuid: UUID = UUID.randomUUID().withV2Marker()): PlayerProfile {
    val gameProfile = Bukkit.createProfile(uuid, name)
    if (value != null)
        gameProfile.setProperty(ProfileProperty("textures", value!!, signature))
    return gameProfile
}