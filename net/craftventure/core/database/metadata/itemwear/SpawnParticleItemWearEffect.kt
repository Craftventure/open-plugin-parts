package net.craftventure.core.database.metadata.itemwear

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.json.ParticleAdapter
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.EquippedItemsMeta
import net.craftventure.core.utils.ParticleSpawner
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.util.Vector

@JsonClass(generateAdapter = true)
class SpawnParticleItemWearEffect(
    val particle: Particle,
    val count: Int = 1,
    val offset: Vector = Vector(),
    val extra: Double = 0.0,
    val longDistance: Boolean = false,
    val range: Double = ParticleSpawner.DEFAULT_RANGE,
    val locationOffset: Vector = Vector(),
    val data: ParticleAdapter.ParticleOptionJson? = null,
    val useHeadMatrix: Boolean = false,
    val excludeSelf: Boolean = false,
) : ItemWearEffect() {
    override fun applyActual(
        player: Player,
        playerMatrix: Matrix4x4,
        headMatrix: Matrix4x4,
        data: EquipmentManager.EquippedItemData,
        meta: EquippedItemsMeta
    ) {
        val location = this.locationOffset.clone()
        val matrix = if (useHeadMatrix) headMatrix else playerMatrix
        matrix.transformPoint(location)

        val parsedData = this.data?.create()

        try {
            player.world.spawnParticleX(
                x = location.x,
                y = location.y,
                z = location.z,
                particle = particle,
                count = count,
                offsetX = this.offset.x,
                offsetY = this.offset.y,
                offsetZ = this.offset.z,
                extra = extra,
                longDistance = longDistance,
                range = range,
                data = parsedData,
                exclude = if (excludeSelf) setOf(player) else null,
            )
        } catch (e: Exception) {
            logcat(
                LogPriority.WARN,
                logToCrew = true
            ) { "Failed to display particle: ${e.message} (data=${parsedData?.javaClass?.name}, particle=$particle)" }
        }
    }
}