package net.craftventure.core.effect

import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.core.animation.VerticalDoor
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.utils.SimpleInterpolator
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import penner.easing.Quad
import kotlin.math.min


class MatShopExitDoor : BaseEffect("mat_shop_exit_door") {
    private val door: VerticalDoor

    private var lastStateChange = 0L
    private var state = State.Closed
        set(value) {
            field = value
            lastStateChange = System.currentTimeMillis()
        }

    init {
        val world = Bukkit.getWorld("world")
        val verticalDoorTracker =
            NpcAreaTracker(SimpleArea("world", 2.0, 35.0, -774.0, 37.0, 42.0, -740.0))
        verticalDoorTracker.startTracking()

        val blocks = arrayOf(
            Location(world, 29.5, 36.0, -761.5),
            Location(world, 29.5, 37.0, -761.5),
            Location(world, 29.5, 38.0, -761.5),
            Location(world, 29.5, 39.0, -761.5),
            Location(world, 29.5, 40.0, -761.5),
        ) + arrayOf(
            Location(world, 28.5, 36.0, -762.5),
            Location(world, 28.5, 37.0, -762.5),
            Location(world, 28.5, 38.0, -762.5),
            Location(world, 28.5, 39.0, -762.5),
            Location(world, 28.5, 40.0, -762.5),
            Location(world, 28.5, 41.0, -762.5),
        ) + arrayOf(
            Location(world, 27.5, 36.0, -763.5),
            Location(world, 27.5, 37.0, -763.5),
            Location(world, 27.5, 38.0, -763.5),
            Location(world, 27.5, 39.0, -763.5),
            Location(world, 27.5, 40.0, -763.5),
            Location(world, 27.5, 41.0, -763.5),
        ) + arrayOf(
            Location(world, 26.5, 36.0, -764.5),
            Location(world, 26.5, 37.0, -764.5),
            Location(world, 26.5, 38.0, -764.5),
            Location(world, 26.5, 39.0, -764.5),
            Location(world, 26.5, 40.0, -764.5),
        )

        door = VerticalDoor(
            Material.POLISHED_DEEPSLATE.createBlockData(),
            4.0,
            blocks,
            SimpleInterpolator(Quad::easeInOut),
            verticalDoorTracker
        ) { location, y ->
//            if (location.x == 29.0 || location.x == 26.0) {
            min(y, 41.0)
//                max(min(4.0, 41.0 - location.y), 0.0)
//            } else
//                4.0
        }
    }

    override fun update(tick: Int) {
        when (state) {
            State.Opening -> {
                if (!door.isRunning)
                    state = State.Open
            }
            State.Open -> {
                if (System.currentTimeMillis() - lastStateChange > 8000) {
                    state = State.Closing
                    door.close(20 * 3)
                }
            }
            State.Closing -> {
                if (!door.isRunning)
                    stop()
            }
            State.Closed -> {
                if (tick <= 2) {
                    state = State.Opening
                    door.open(20 * 3)
                } else {
                    this.stop()
                }
            }
        }
    }

    override fun play() {
        super.play()
        state = State.Closed
    }

    override fun stop() {
        super.stop()
        state = State.Closed
    }

    enum class State {
        Opening,
        Open,
        Closing,
        Closed
    }
}