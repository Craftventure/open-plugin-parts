package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.core.npc.tracker.NpcEntityTracker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.material.Openable

@JsonClass(generateAdapter = true)
data class OpenOpenableAction(
    @Expose val open: Boolean = false,
    @Expose val world: String = "world",
    @Expose val x: Double = 0.0,
    @Expose val y: Double = 0.0,
    @Expose val z: Double = 0.0,
) : ScriptAction() {
    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
        val location = Location(Bukkit.getWorld(world), x, y, z)
        val block = location.block
        if (block.state.data is Openable) {
            block.open(open)
        }
    }

    override val actionTypeId: Int
        get() = Type.BLOCK_ACTION
}