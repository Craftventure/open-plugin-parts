package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.tracker.NpcEntityTracker
import org.bukkit.Bukkit
import org.bukkit.Location

@JsonClass(generateAdapter = true)
data class PlaySoundAction(
    @Expose val world: String = "world",
    @Expose val x: Double = 0.0,
    @Expose val y: Double = 0.0,
    @Expose val z: Double = 0.0,
    @Expose val soundName: String? = null,
    @Expose val volume: Float = 0f,
    @Expose val pitch: Float = 0f,
) : ScriptAction() {


    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
        val location = Location(Bukkit.getWorld(world), x, y, z)
        for (player in entityTracker!!.players) {
            player.playSound(location, soundName!!, volume, pitch)
        }
    }

    override val actionTypeId: Int
        get() = Type.PLAY_SOUND
}