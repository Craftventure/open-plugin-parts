package net.craftventure.core.ride.tracklessride.transport.car

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.core.extension.spawn
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ride.tracklessride.BaseTagContainer
import net.craftventure.core.ride.tracklessride.TagContainer
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.config.CarGroupCarConfig
import net.craftventure.core.ride.tracklessride.config.CarGroupConfig
import net.craftventure.core.ride.tracklessride.navigation.PathPosition
import net.craftventure.core.ride.tracklessride.property.DoubleProperty
import net.craftventure.core.utils.EntityUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.io.File

class TestTracklessRideCar(
    position: PathPosition,
    idInGroup: Int,
    tracklessRide: TracklessRide,
    tagContainer: TagContainer = BaseTagContainer(),
    groupConfig: CarGroupConfig,
    carConfig: CarGroupCarConfig,
) : TracklessRideCar(
    position = position,
    idInGroup = idInGroup,
    tracklessRide = tracklessRide,
    tagContainer = tagContainer,
    groupConfig = groupConfig,
    carConfig = carConfig,
) {
    private val model = dataItem(Material.DIAMOND_SWORD, 67)
    private var entityModel: ArmorStand? = null
    private var entityDebug1: ArmorStand? = null
    private var entityDebug2: ArmorStand? = null
    private var entityDebug3: ArmorStand? = null

    private val yawProperty = DoubleProperty("yaw", 0.0)
    private val pitchProperty = DoubleProperty("pitch", 0.0)

    override fun getDoubleProperty(id: String): DoubleProperty? {
        return when (id) {
            yawProperty.id -> yawProperty
            pitchProperty.id -> pitchProperty
            else -> super.getDoubleProperty(id)
        }
    }

    override fun destroy() {
        super.destroy()
        entityModel?.remove()
        entityDebug1?.remove()
        entityDebug2?.remove()
        entityDebug3?.remove()
    }

    override fun hasPlayers(): Boolean = false
    override val playerPassengers: Collection<Player>
        get() = listOfNotNull(
            entityModel?.passengers?.filterIsInstance<Player>(),
            entityDebug1?.passengers?.filterIsInstance<Player>(),
            entityDebug2?.passengers?.filterIsInstance<Player>(),
            entityDebug3?.passengers?.filterIsInstance<Player>(),
        ).flatten()

    override fun update() {
        super.update()
        val world = Bukkit.getWorld("world")!!
//        pathPosition.location.spawnParticleX(world, Particle.END_ROD)
        val baseLocation = Location(
            world,
            pathPosition.location.x,
            pathPosition.location.y - EntityConstants.ArmorStandHeadOffset,
            pathPosition.location.z,
            yawProperty.value.toFloat(),
            pitchProperty.value.toFloat()
        )
        val hasModel = entityModel != null
        entityModel %= baseLocation
        entityModel!!.apply {
            if (equipment.helmet == null || equipment.helmet?.type == Material.AIR || !hasModel)
                equipment.helmet = model
        }

//        val debugLocation1 = baseLocation.clone().add(0.0, 0.6, 0.0)
//        entityDebug1 %= debugLocation1
//        entityDebug1!!.apply {
//            isCustomNameVisible = true
//            customName =
//                "${group.groupId}/$idInGroup Global [${group.get(TagContext.GLOBAL).joinToString(", ")}]"
//        }
//
//        val debugLocation2 = baseLocation.clone().add(0.0, 0.8, 0.0)
//        entityDebug2 %= debugLocation2
//        entityDebug2!!.apply {
//            isCustomNameVisible = true//tags.isNotEmpty()
//            customName = "Scene [${get(TagContext.SCENE).joinToString(", ")}]"
//        }
//
//        val debugLocation3 = baseLocation.clone().add(0.0, 1.0, 0.0)
//        entityDebug3 %= debugLocation3
//        entityDebug3!!.apply {
//            val programPart = trackedRide.controller.getProgramPartForCar(this@TestTracklessRideCar)
//            isCustomNameVisible = true//programPart != null
//            if (programPart != null) {
//                val state = trackedRide.controller.getState(this@TestTracklessRideCar, programPart)
//                customName = "${programPart.javaClass.simpleName} (${programPart.getCurrentProgress(state).format(2)})"
//            } else {
//                customName = "Program done"
//            }
//        }


        val debugLocation1 = baseLocation.clone().add(0.0, 0.6, 0.0)
        entityDebug1 %= debugLocation1
        entityDebug1!!.apply {
            isCustomNameVisible = true
            customName = "Yaw ${yawProperty.value.format(2)}"
        }

        val debugLocation2 = baseLocation.clone().add(0.0, 0.8, 0.0)
        entityDebug2 %= debugLocation2
        entityDebug2!!.apply {
            isCustomNameVisible = true//tags.isNotEmpty()
            customName = "Pitch ${pitchProperty.value.format(2)}"
        }
    }

    inline operator fun <reified T : Entity> T?.rem(location: Location): T {
        if (this != null) {
            EntityUtils.teleport(
                this,
                location.x,
                location.y,
                location.z,
                location.yaw,
                location.pitch
            )
            return this
        } else {
            return location.spawn<T>().apply {
                isPersistent = false
                if (this is ArmorStand) {
                    isVisible = false
                }
            }
        }
    }

    override fun tryToEnter(player: Player, seatEntityId: Int): Boolean = false
    override fun putPassenger(player: Player): Boolean = false

    @JsonClass(generateAdapter = true)
    class Config : CarConfig() {
        override fun createFactory(directory: File): CarFactory = Factory()

        companion object {
            const val type = "test"
        }
    }

    class Factory : CarFactory() {
        override fun produce(
            position: PathPosition,
            groupId: Int,
            groupConfig: CarGroupConfig,
            carId: Int,
            carConfig: CarGroupCarConfig,
            trackedRide: TracklessRide,
            tagContainer: TagContainer
        ): TracklessRideCar = TestTracklessRideCar(position, carId, trackedRide, tagContainer, groupConfig, carConfig)
    }
}