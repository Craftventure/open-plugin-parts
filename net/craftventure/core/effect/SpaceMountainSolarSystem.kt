package net.craftventure.core.effect

import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.utils.ItemStackUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType

class SpaceMountainSolarSystem : BaseEffect("smsolarsystem") {
    private val center = Location(Bukkit.getWorld("world"), 272.6, 30.7, -749.9)
    private var earthOffset = 0.0
    private var earthMoonOffset = 0.0
    private var planet1Offset = 0.0
    private val areaTracker: NpcAreaTracker

    private val earthEntity: NpcEntity
    private val moonEntity: NpcEntity
    private val planetEntity: NpcEntity

    init {
        areaTracker = NpcAreaTracker(SimpleArea("world", 247.0, 18.0, -794.0, 311.0, 75.0, -732.0))

        earthEntity = NpcEntity("spaceMountainSolar", EntityType.ARMOR_STAND, center)
            //                .armorstandSmall(true)
            .invisible(true)
            .helmet(ItemStackUtils.fromString("earth2"))
        moonEntity = NpcEntity("spaceMountainSolar", EntityType.ARMOR_STAND, center)
            //                .armorstandSmall(true)
            .invisible(true)
            .helmet(ItemStackUtils.fromString("mercury2"))
        planetEntity = NpcEntity("spaceMountainSolar", EntityType.ARMOR_STAND, center)
            //                .armorstandSmall(true)
            .invisible(true)
            .helmet(ItemStackUtils.fromString("jupiter"))

        areaTracker.addEntity(earthEntity)
        areaTracker.addEntity(moonEntity)
        areaTracker.addEntity(planetEntity)
    }

    private fun setLampOn(location: Location, on: Boolean) {
        location.block.type = if (on) Material.GLOWSTONE else Material.BARRIER
    }

    override fun onStarted() {
        super.onStarted()
        areaTracker.startTracking()

        setLampOn(Location(Bukkit.getWorld("world"), 276.0, 27.0, -751.0), true)
    }

    override fun onStopped() {
        super.onStopped()
        areaTracker.stopTracking()

        setLampOn(Location(Bukkit.getWorld("world"), 276.0, 27.0, -751.0), false)
    }

    override fun update(tick: Int) {
        earthEntity.move(
            center.x + Math.cos(earthOffset) * 3.5,
            center.y + Math.cos(earthOffset) * 0.5,
            center.z + Math.sin(earthOffset) * 3.5
        )

        moonEntity.move(
            center.x + Math.cos(earthOffset) * 3.5 + Math.cos(earthMoonOffset) * (3.5 * 0.45),
            center.y + Math.cos(earthOffset) * 0.5 + Math.cos(earthMoonOffset) * (0.5 * 0.45),
            center.z + Math.sin(earthOffset) * 3.5 + Math.sin(earthMoonOffset) * (3.5 * 0.45)
        )

        planetEntity.move(
            center.x + Math.cos(planet1Offset) * 3.5 * 0.35,
            center.y + Math.cos(planet1Offset) * 0.5 * 0.35,
            center.z + Math.sin(planet1Offset) * 3.5 * 0.35
        )

        earthOffset += 0.15
        earthMoonOffset -= 0.23
        planet1Offset -= 0.08

        if (tick > 20 * 8) {
            stop()
        }
    }
}
