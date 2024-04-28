package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger.severe
import net.craftventure.core.npc.tracker.NpcEntityTracker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@JsonClass(generateAdapter = true)
data class PotionEffectAction(
    @Expose val x: Double = 0.0,
    @Expose val y: Double = 0.0,
    @Expose val z: Double = 0.0,
    @Expose val world: String = "world",
    @Expose val radius: Double? = null,
    @Expose val potionEffectType: String? = null,
    @Expose val duration: Int = 0,
    @Expose val amplifier: Int = 0,
    @Expose val ambient: Boolean = false,
    @Expose val particles: Boolean = false,
    @Expose val color: Int? = null,
) : ScriptAction() {

    @Transient
    private var potionEffect: PotionEffect? = null
    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
        if (potionEffect == null) {
            val potionEffectType =
                PotionEffectType.getByName(potionEffectType!!)
            if (potionEffectType == null) {
                severe("PotionEffectType " + this.potionEffectType + " not found!")
                return
            }
            potionEffect = if (color != null) PotionEffect(
                potionEffectType,
                duration,
                amplifier,
                ambient,
                particles,
                false
            ) else PotionEffect(potionEffectType, duration, amplifier, ambient, particles)
        }
        Bukkit.getScheduler().runTask(CraftventureCore.getInstance(), Runnable {
            val world = Bukkit.getWorld(world)
            if (world != null) {
                if (radius != null) {
                    val location = Location(world, x, y, z)
                    //                        Logger.console("Executing potioneffect " + entityTracker.getPlayers().size());
                    for (player in entityTracker!!.players) {
                        if (player.location.distanceSquared(location) < radius * radius) {
                            player.addPotionEffect(potionEffect!!)
                            //                                Logger.console("Sent to " + player.getName());
                        }
                    }
                } else { //                        Logger.console("Executing potioneffect " + entityTracker.getPlayers().size());
                    for (player in entityTracker!!.players) {
                        player.addPotionEffect(potionEffect!!)
                        //                            Logger.console("Send to " + player.getName());
                    }
                }
            }
        })
    }

    override val actionTypeId: Int
        get() = Type.POTION_EFFECT
}