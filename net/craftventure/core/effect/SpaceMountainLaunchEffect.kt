package net.craftventure.core.effect

import net.craftventure.core.CraftventureCore
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.material.Lever


class SpaceMountainLaunchEffect : BaseEffect("smlaunch") {

    private val lampLocations = ArrayList<Location>()
    private val tower1 = ArrayList<Location>()
    private val tower2 = ArrayList<Location>()
    private val tower3 = ArrayList<Location>()
    private val tower4 = ArrayList<Location>()
    private val allTowers = ArrayList<Location>()

    private var ringMode = 0
    private var flickerOn = true
    private var flickerTicks = 0
    private var ringsOn = 0
    private var currentRingsOn = 0
    private var ringsOnWaitingTicks = 0

    init {
        lampLocations.add(Location(Bukkit.getWorld("world"), 263.5, 67.0, -762.5))
        lampLocations.add(Location(Bukkit.getWorld("world"), 260.5, 66.0, -762.5))
        lampLocations.add(Location(Bukkit.getWorld("world"), 257.5, 64.0, -762.5))
        lampLocations.add(Location(Bukkit.getWorld("world"), 254.5, 62.0, -762.5))


        tower3.add(Location(Bukkit.getWorld("world"), 275.5, 75.0, -757.5))
        tower3.add(Location(Bukkit.getWorld("world"), 275.5, 77.0, -757.5))

        tower1.add(Location(Bukkit.getWorld("world"), 284.5, 74.0, -767.5))
        tower1.add(Location(Bukkit.getWorld("world"), 284.5, 76.0, -767.5))
        tower1.add(Location(Bukkit.getWorld("world"), 284.5, 78.0, -767.5))

        tower2.add(Location(Bukkit.getWorld("world"), 276.5, 76.0, -770.5))
        tower2.add(Location(Bukkit.getWorld("world"), 276.5, 78.0, -770.5))

        tower4.add(Location(Bukkit.getWorld("world"), 283.5, 71.0, -756.5))
        tower4.add(Location(Bukkit.getWorld("world"), 283.5, 73.0, -756.5))

        allTowers.addAll(tower3)
        allTowers.addAll(tower1)
        allTowers.addAll(tower2)
        allTowers.addAll(tower4)
    }

    private fun ringOn(location: Location, on: Boolean) {
        val block = location.block
        if (on) {
            if (block.type != Material.SEA_LANTERN)
                block.type = Material.SEA_LANTERN
        } else {
            if (block.type != Material.LAPIS_BLOCK)
                block.type = Material.LAPIS_BLOCK
        }
    }

    private fun switchOn(location: Location, on: Boolean) {
        val block = location.block
        if (block != null) {
            if (block.state != null && block.state.data is Lever) {
                val blockState = block.state
                val lever = block.state.data as Lever
                if (lever.isPowered != on) {
                    lever.isPowered = on
                    blockState.data = lever
                    blockState.update(true, false)
                }
            }
        }
    }

    override fun onStarted() {
        super.onStarted()
        for (location in lampLocations)
            switchOn(location, true)
        for (location in allTowers)
            ringOn(location, true)

        currentRingsOn = allTowers.size
        ringsOn = allTowers.size

        ringMode = CraftventureCore.getRandom().nextInt(2)
    }

    override fun onStopped() {
        super.onStopped()
        for (location in lampLocations)
            switchOn(location, true)
        for (location in allTowers)
            ringOn(location, true)
    }

    override fun update(tick: Int) {
        if (tick < 20 * 6) {
            var index = 0
            for (location in lampLocations) {
                val lampTick = (index * 4 + tick) % (4 * lampLocations.size)
                if (index == 3)
                //                    Logger.console("LampTick " + index + " > " + lampTick);
                    switchOn(location, lampTick < 3)
                index++
            }
        } else if (tick == 20 * 6) {
            for (location in lampLocations)
                switchOn(location, false)
        } else if (tick >= 20 * 8) {
            var index = 0
            for (location in lampLocations) {
                switchOn(location, tick > 20 * 8 + index * 8)
                index++
            }
        }

        if (ringMode == 0) {
            if (ringsOnWaitingTicks <= 0) {
                if (currentRingsOn == ringsOn) {
                    ringsOn = CraftventureCore.getRandom().nextInt(allTowers.size)
                }

                if (currentRingsOn < ringsOn) {
                    if (currentRingsOn - 1 >= 0)
                        ringOn(allTowers[currentRingsOn - 1], true)
                    currentRingsOn++
                } else if (currentRingsOn > ringsOn) {
                    if (currentRingsOn - 1 >= 0)
                        ringOn(allTowers[currentRingsOn - 1], false)
                    currentRingsOn--
                }
                ringsOnWaitingTicks = 3
            }
            ringsOnWaitingTicks--
        } else {
            if (tick > 20 * 2) {
                flickerTicks--
                if (flickerTicks <= 0) {
                    flickerTicks = CraftventureCore.getRandom().nextInt(8)
                    flickerOn = CraftventureCore.getRandom().nextBoolean()
                }

                if (!flickerOn) {
                    for (location in allTowers)
                        ringOn(location, false)
                } else {
                    for (location in allTowers)
                        ringOn(location, true)
                }
            }
        }

        if (tick > 20 * 10) {
            stop()
        }
    }
}
