package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.resetLight
import net.craftventure.core.extension.setLightLevel
import net.craftventure.core.npc.tracker.NpcEntityTracker
import org.bukkit.Bukkit
import org.bukkit.Location


@JsonClass(generateAdapter = true)
data class LightingEffect(
    @Expose val type: Type? = null,
    @Expose val world: String = "world",
    @Expose val x: Double = 0.toDouble(),
    @Expose val y: Double = 0.toDouble(),
    @Expose val z: Double = 0.toDouble(),
    @Expose val level: Int? = null,
) : ScriptAction() {
    @delegate:Transient
    private val location by lazy { Location(Bukkit.getWorld(world), x, y, z) }

    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
//        Logger.debug("$type to $level at ${location.blockX} ${location.blockY} ${location.blockZ}")
        executeSync {
            when (type) {
                Type.SET_LIGHT -> {
                    val level = this.level ?: return@executeSync
                    location.block.setLightLevel(level)
                }
                Type.RELIGHT -> {
                    location.block.resetLight()
                }
                else -> {}
            }
        }
    }


    override val actionTypeId: Int
        get() = ScriptAction.Type.LIGHTING

    enum class Type {
        SET_LIGHT, RELIGHT
    }
}
