package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger.warn
import net.craftventure.core.npc.tracker.NpcEntityTracker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.data.BlockData

data class BlockChangeAction(
    @Expose private val x: Int = 0,
    @Expose private val y: Int = 0,
    @Expose private val z: Int = 0,
    @Expose private val world: String = "world",
    @Expose private val data: String? = null,
    @Expose private val fake: Boolean = true,
    @Expose private val physics: Boolean = true
) : ScriptAction() {
    @Transient
    var blockData: BlockData? = null
        get() {
            if (field == null) {
                try {
                    field = Bukkit.getServer().createBlockData(data!!)
                } catch (e: Throwable) {
                    warn("Failed to parse blockdata %s", false, data)
                    e.printStackTrace()
                }
            }
            return field
        }
        private set

    override fun validate(): Boolean {
        if (blockData == null) return false
        return !(x == 0 && y == 0 && z == 0)
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
        val blockData = blockData
        if (blockData != null) {
            if (fake) {
                for (player in entityTracker!!.players) {
                    player.sendBlockChange(Location(player.world, x.toDouble(), y.toDouble(), z.toDouble()), blockData)
                }
            } else {
                Bukkit.getScheduler().runTask(CraftventureCore.getInstance(), Runnable {
                    val world = Bukkit.getWorld(world)
                    if (world != null) {
                        val block = world.getBlockAt(x, y, z)
                        block.setBlockData(blockData, physics)
                    }
                })
            }
        }
    }

    override val actionTypeId: Int
        get() = Type.BLOCK_CHANGE
}