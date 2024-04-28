package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.json.ParticleAdapter
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.utils.ParticleSpawner
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.util.Vector

@JsonClass(generateAdapter = true)
class SpawnParticleItemUseEffect(
    val particle: Particle,
    val count: Int = 1,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val offsetZ: Double = 0.0,
    val extra: Double = 0.0,
    val longDistance: Boolean = false,
    val range: Double = ParticleSpawner.DEFAULT_RANGE,
    val locationOffset: Vector? = null,
    val data: ParticleAdapter.ParticleOptionJson? = null,
    val selfOnly: Boolean = false
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val location = location.clone()
        if (locationOffset != null)
            location.add(locationOffset)

        val parsedData = this.data?.create()

//        val requiredDataType = particle.dataType
//        if (parsedData != null) {
//            if (requiredDataType !== parsedData.javaClass) {
//
//            }
//        } else if (requiredDataType !== Void::class.java) {
//            logcat { "Particle ${particle} requires datatype ${requiredDataType}" }
//        }

        try {
            location.spawnParticleX(
                particle = particle,
                count = count,
                offsetX = offsetX,
                offsetY = offsetY,
                offsetZ = offsetZ,
                extra = extra,
                longDistance = longDistance,
                range = range,
                data = parsedData,
                players = if (selfOnly) setOf(player) else null,
            )
        } catch (e: Exception) {
            logcat(
                LogPriority.WARN,
                logToCrew = true
            ) { "Failed to display particle: ${e.message} (data=${parsedData?.javaClass?.name}, particle=$particle)" }
        }
    }
}