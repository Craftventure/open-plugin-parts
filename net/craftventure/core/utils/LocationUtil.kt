package net.craftventure.core.utils

import com.comphenix.protocol.wrappers.BlockPosition
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block


object LocationUtil {
    fun toLocation(blockPosition: BlockPosition): Location =
        Location(
            Bukkit.getWorlds()[0],
            blockPosition.x.toDouble(),
            blockPosition.y.toDouble(),
            blockPosition.z.toDouble()
        )

    fun isZero(loc: Location): Boolean {
        return loc.x == 0.0 && loc.y == 0.0 && loc.z == 0.0
    }

    fun equals(location: Location, block: Block): Boolean = location.blockX == block.x &&
            location.blockY == block.y &&
            location.blockZ == block.z
}
