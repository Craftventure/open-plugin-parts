package net.craftventure.bukkit.ktx.util

import org.bukkit.Material
import org.bukkit.block.*
import org.reflections.Reflections

// Very basic tool to help us find the impact of often used tile entities on CV

object BlockEntityImpactHelper {
    val map = hashMapOf<String, Class<*>>()
    val offenders = listOf(
        CreatureSpawner::class.java,
        Chest::class.java,
        ShulkerBox::class.java,
        Campfire::class.java,
        Banner::class.java,
        Sign::class.java,
        Conduit::class.java,
        Bell::class.java,
        EnchantingTable::class.java,
        Skull::class.java,
        Beacon::class.java,
    )

    init {
        val reflections = Reflections("org.bukkit.block")
        val subtypes = reflections.getSubTypesOf(BlockState::class.java)
        subtypes.map {
            map[it.simpleName] = it
        }
    }

    fun impactScore(state: BlockState): Double? {
        return when (state) {
            is CreatureSpawner -> scoreByFps(6)
            is EnchantingTable -> scoreByFps(10)
            is Banner -> scoreByFps(10)
            is Campfire -> scoreByFps(15)
            is Chest, is EnderChest, is ShulkerBox -> scoreByFps(20)
            is Sign -> scoreByFps(25)
            is Skull -> {
                when (state.type) {
                    Material.DRAGON_HEAD, Material.DRAGON_WALL_HEAD -> scoreByFps(8)
                    else -> scoreByFps(25)
                }
            }

            is Conduit -> scoreByFps(40)
            is Bell -> scoreByFps(75)
            is Beacon -> scoreByFps(110)
            is Lectern -> scoreByFps(180)
            is BrewingStand -> scoreByFps(200)
            is CommandBlock -> scoreByFps(210)
            is Hopper, is Furnace, is Barrel, is Dispenser, is BlastFurnace, is Beehive -> scoreByFps(220)
            else -> return null
        }
    }

    private fun scoreByFps(fps: Int) = fps / 240.0
}