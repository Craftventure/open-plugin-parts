package net.craftventure.core.script.action

import com.comphenix.packetwrapper.WrapperPlayServerBlockAction
import com.comphenix.protocol.wrappers.BlockPosition
import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.extension.power
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.npc.tracker.NpcEntityTracker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World

@JsonClass(generateAdapter = true)
data class BlockActionAction(
    @Expose val world: String = "world",
    @Expose val x: Double = 0.0,
    @Expose val y: Double = 0.0,
    @Expose val z: Double = 0.0,
    @Expose val type: Int = 0,
    @Expose val fake: Boolean = false,
    @Expose val forceUpdate: Boolean = true,
    @Expose val applyPhysics: Boolean = true
) : ScriptAction() {
    @Transient
    private var bukkitWorld: World = Bukkit.getWorld(world)!!
    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
        val location = Location(bukkitWorld, x, y, z)
        var wrapperPlayServerBlockAction: WrapperPlayServerBlockAction? = null
        if (type == Type.CHEST_OPEN || type == Type.CHEST_CLOSE) {
            wrapperPlayServerBlockAction = WrapperPlayServerBlockAction()
            wrapperPlayServerBlockAction.location = BlockPosition(
                location.blockX,
                location.blockY,
                location.blockZ
            )
            wrapperPlayServerBlockAction.byte1 = 1
            wrapperPlayServerBlockAction.byte2 = if (type == Type.CHEST_OPEN) 1 else 0
            wrapperPlayServerBlockAction.blockType = location.block.type
        } else if (type == Type.OPEN_OPENABLE || type == Type.CLOSE_OPENABLE) {
            val open =
                type == Type.OPEN_OPENABLE
            val block = location.block
            if (block != null) {
                Bukkit.getScheduler().runTask(CraftventureCore.getInstance(), Runnable { block.open(open) })
            }
        } else if (type == Type.POWER_ON || type == Type.POWER_OFF) {
            val powered =
                type == Type.POWER_ON
            val block = location.block
            if (block != null) {
                Bukkit.getScheduler()
                    .runTask(CraftventureCore.getInstance(), Runnable { block.power(powered) })
            }
        }
        if (wrapperPlayServerBlockAction != null) {
            for (player in entityTracker!!.players) {
                try {
                    wrapperPlayServerBlockAction.sendPacket(player)
                } catch (e: Exception) {
                    capture(e)
                }
            }
        }
    }

    override val actionTypeId: Int
        get() = ScriptAction.Type.BLOCK_ACTION

    interface Type {
        companion object {
            const val CHEST_OPEN = 1
            const val CHEST_CLOSE = 2
            const val OPEN_OPENABLE = 3
            const val CLOSE_OPENABLE = 4
            const val POWER_ON = 5
            const val POWER_OFF = 6
        }
    }
}