package net.craftventure.core.feature.casino

import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.dropNaturally
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.database.bukkit.extensions.itemRepresentation
import net.craftventure.database.type.BankAccountType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.util.Vector


object CasinoManager {
    private var areaTracker: NpcAreaTracker? = null
    private var winterAreaTracker: NpcAreaTracker? = null
    private var halloweenAreaTracker: NpcAreaTracker? = null
    var wheelOfFortune: WheelOfFortune? = null

    fun init() {
        //        SlotMachine.create(129, 42, -797, 10);
        // 121.50, 43, -796.50
        areaTracker = NpcAreaTracker(SimpleArea("world", 101.0, 39.0, -828.0, 145.0, 55.0, -786.0))
        areaTracker!!.startTracking()
        wheelOfFortune = WheelOfFortune(areaTracker)

        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 121.50, 43.0, -796.50), 10, "10vc")//.test();
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 121.50 + 4, 43.0, -796.50), 10, "10vc")
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 121.50 + 8, 43.0, -796.50), 10, "10vc")
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 121.50 - 4, 43.0, -796.50), 5, "5vc")
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 121.50 - 8, 43.0, -796.50), 5, "5vc")
//        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 121.50 - 12, 43.0, -796.50), 5, "5vc")

        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 113.50 + 16, 51.0, -796.50), 100, "100vc")
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 113.50 + 12, 51.0, -796.50), 250, "250vc")
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 113.50 + 8, 51.0, -796.50), 500, "500vc")
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 113.50 + 4, 51.0, -796.50), 1000, "1000vc")
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 113.50, 51.0, -796.50), 1000, "1000vc")

        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 138.50, 50.0, -786.50), 5000, "5000vc")
        SlotMachine(areaTracker, 0.0, Location(Bukkit.getWorld("world"), 134.50, 50.0, -786.50), 10000, "10000vc")
    }

    fun destroy() {
        areaTracker?.release()
        winterAreaTracker?.release()
        halloweenAreaTracker?.release()
    }

    fun spawnCoin(location: Location, velocity: Vector, lifeTimeTicks: Long) {
        val itemStack = BankAccountType.VC.itemRepresentation
        itemStack.durability = (Math.random() * Short.MAX_VALUE).toInt().toShort()
        val item = location.clone().dropNaturally(itemStack)
        item.pickupDelay = Int.MAX_VALUE
        item.velocity = velocity

        executeSync(lifeTimeTicks) { item.remove() }
    }
}
