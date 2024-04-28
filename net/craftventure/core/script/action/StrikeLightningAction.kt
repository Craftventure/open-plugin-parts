package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.strikeLightningEffect
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.npc.tracker.NpcEntityTracker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound

@JsonClass(generateAdapter = true)
data class StrikeLightningAction(
    @Expose val fake: Boolean = true,
    @Expose val world: String = "world",
    @Expose val x: Double = 0.0,
    @Expose val y: Double = 0.0,
    @Expose val z: Double = 0.0,
) : ScriptAction() {
    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
        val strikeLocation = Location(Bukkit.getWorld(world), x, y, z)
        if (!fake) {
            strikeLocation.world.strikeLightningEffect(strikeLocation)
        } else {
            for (player in entityTracker!!.players) {
                player.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 100f, 1f)
                try {
                    player.strikeLightningEffect(strikeLocation)
                } catch (e: Exception) {
                    capture(e)
                }
            }
        }
    }

    override val actionTypeId: Int
        get() = Type.STRIKE_LIGHTNING
}