package net.craftventure.audioserver.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.SimpleArea
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
data class AreaPart(
    var name: String? = null,
    @Json(name = "enabled")
    var isEnabled: Boolean = false,
    var world: String? = null,
    var corner1: AreaPartCorner? = null,
    var corner2: AreaPartCorner? = null,
    @Json(name = "area")
    var jsonArea: Area.Json? = null,
) {
    //    @field:Transient
    val area: Area by lazy {
        jsonArea?.create() ?: SimpleArea(
            Location(Bukkit.getWorld(world!!), corner1!!.x, corner1!!.y, corner1!!.z),
            Location(Bukkit.getWorld(world!!), corner2!!.x, corner2!!.y, corner2!!.z)
        )
    }

    @Transient
    val playerList = hashSetOf<Player>()

    val players: Set<Player>
        get() = playerList

    fun isInAreaPart(player: Player): Boolean {
        return playerList.contains(player)
    }

    fun update(player: Player, location: Location) {
        if (isInArea(location)) {
            //            Logger.console("Add " + player.getName() + " to " + name);
            addPlayer(player)
        } else {
            //            Logger.console("Remove " + player.getName() + " from " + name);
            removePlayer(player)
        }
    }

    fun removePlayer(player: Player) {
        playerList.remove(player)
        //            Logger.console("Remove " + player.getName() + " from " + name);

    }

    fun isInArea(player: Player): Boolean {
        return area.isInArea(player.location)
    }

    fun isInArea(location: Location): Boolean {
        return area.isInArea(location)
    }

    private fun addPlayer(player: Player) {
        if (!playerList.contains(player)) {
            playerList.add(player)
            //            Logger.console("Add " + player.getName() + " to " + name);
        }
    }
}
