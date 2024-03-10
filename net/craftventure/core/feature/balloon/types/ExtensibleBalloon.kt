package net.craftventure.core.feature.balloon.types

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.CraftventureCore
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.holders.PlayerHolder
import net.craftventure.core.feature.balloon.physics.BalloonPhysics
import net.craftventure.core.feature.balloon.physics.DefaultBalloonPhysics
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import org.bukkit.Bukkit
import org.bukkit.Location

class ExtensibleBalloon(
    val id: String,
    private val physics: BalloonPhysics = DefaultBalloonPhysics(),
    val ownableItem: OwnableItem?,
    private vararg val extensions: Extension,
) : Balloon {
    var angle: Float
        get() = balloonLocation!!.yaw
        set(value) {
            balloonLocation!!.yaw = value
        }
    override var balloonHolder: BalloonHolder? = null
        private set
    override var balloonLocation: Location? = null
    private var oldLocation: Location? = null

    private var tracker: NpcEntityTracker? = null
//    override val leashLength: Double get() = 2.2

    override fun spawn(balloonHolder: BalloonHolder) {
        this.balloonHolder = balloonHolder

        balloonLocation = balloonHolder.anchorLocation.clone()
        balloonLocation!!.yaw = CraftventureCore.getRandom().nextFloat() * 360f
        oldLocation = balloonLocation!!.clone()

        tracker = balloonHolder.tracker.tracker

        extensions.forEach {
            if (it.enabled)
                try {
                    it.spawn(this, balloonHolder, tracker!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
        }
        if (balloonHolder.tracker.shouldHandleManagement)
            tracker!!.startTracking()
    }

    override fun update() {
        val balloonHolder = this.balloonHolder ?: return
        val holderLocation = balloonHolder.anchorLocation.clone()

        val prePhysicsBalloonLocation = balloonLocation!!.toVector()
        physics.update(this, holderLocation, balloonLocation!!, oldLocation!!)
//        oldLocation = balloonHolderlocation!!.clone()

        extensions.forEach {
            if (it.enabled)
                try {
                    it.update(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
        }
        oldLocation!!.set(prePhysicsBalloonLocation)

        val playerHolder = balloonHolder as? PlayerHolder
        if (playerHolder != null && oldLocation!!.distanceSquared(balloonLocation!!) > 25 * 25) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                CraftventureCore.getInstance(),
                { tracker!!.forceRespawn(playerHolder.holder) },
                5
            )
        }
    }

    override fun despawn(withEffects: Boolean) {
        extensions.forEach {
            if (it.enabled)
                try {
                    it.despawn(this, withEffects, tracker!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
        }
        if (balloonHolder?.tracker?.shouldHandleManagement == true)
            this.tracker?.release()

        this.balloonHolder = null
        this.tracker = null
    }

    abstract class Extension {
        // Extensions can disable themselves when they're not valid in the current situation
        open var enabled: Boolean = true
            protected set

        open fun spawn(balloon: ExtensibleBalloon, balloonHolder: BalloonHolder, tracker: NpcEntityTracker) {}
        open fun update(balloon: ExtensibleBalloon) {}
        open fun despawn(balloon: ExtensibleBalloon, withEffects: Boolean, tracker: NpcEntityTracker) {}

        abstract class Json {
            abstract fun toExtension(): Extension
        }
    }

    @JsonClass(generateAdapter = true)
    data class Json(
        val physics: BalloonPhysics.Json = DefaultBalloonPhysics.Json(),
        val extensions: List<Extension.Json>
    ) {
        fun toBalloon(id: String, ownableItem: OwnableItem?) =
            ExtensibleBalloon(
                id = id,
                physics = physics.toPhysics(),
                extensions = extensions.map { it.toExtension() }.toTypedArray(),
                ownableItem = ownableItem,
            )
    }
}