package net.craftventure.core.ride.tracklessride.config

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.tracklessride.navigation.GraphNode
import net.craftventure.core.ride.tracklessride.programpart.action.ActionData
import net.craftventure.core.ride.tracklessride.scene.SceneData
import net.craftventure.core.ride.tracklessride.track.PathPart
import net.craftventure.core.ride.tracklessride.transport.car.CarConfig
import org.bukkit.Location

@JsonClass(generateAdapter = true)
data class TracklessRideConfig(
    val id: String,
    val finishAchievement: String,
    val area: Area.Json,
    val exitLocation: Location,
    val settings: SettingsConfig,
    val startActionsForAllCars: List<ActionData> = emptyList(),
    val groups: Map<Int, CarGroupConfig>,
    val scenes: Map<String, SceneData>,
    val carConfig: Map<String, CarConfig>,
    val ejectLocations: Map<String, List<Location>>?,
    var queues: List<RideQueue.TracklessRideJson>? = null,
)

@JsonClass(generateAdapter = true)
data class SettingsConfig(
    val groupSize: Int
)

@JsonClass(generateAdapter = true)
data class CarGroupConfig(
    val scene: String,
    val colors: Map<String, String> = emptyMap(),
    val cars: Map<Int, CarGroupCarConfig>
)

@JsonClass(generateAdapter = true)
data class CarGroupCarConfig(
    val startNode: String,
    val colors: Map<String, String> = emptyMap(),
    val startActions: List<ActionData>
)

@JsonClass(generateAdapter = true)
data class GraphConfig(
    val nodes: List<GraphNode.Json>,
    val parts: List<PathPart.Data>
)