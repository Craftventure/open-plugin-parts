package net.craftventure.core.feature.finalevent

import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.json.DurationJson
import org.bukkit.Location
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.EntityType
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Vector

object FinaleCinematicConfig {

    @JsonClass(generateAdapter = true)
    class MainConfig(
        val audioAreaName: String,
        val startAt: DurationJson? = null,
        val sceneList: List<String>,
        val initialActions: List<SceneAction>? = null,
        val timedActions: List<TimedSceneAction>? = null,
    )

    @JsonClass(generateAdapter = true)
    class SceneConfig(
        val duration: DurationJson,
        val itemDisplays: List<ItemDisplayConfig>? = null,
        val textDisplays: List<TextDisplayConfig>? = null,
        val cameraType: EntityType = EntityType.ARMOR_STAND,
        val cameraPaths: List<CameraPath>,
        val npcs: List<NpcData>? = null,
        val initialActions: List<SceneAction>? = null,
        val timedActions: List<TimedSceneAction>? = null,
    )

    @JsonClass(generateAdapter = true)
    class TimedSceneAction(
        val at: DurationJson,
        val action: SceneAction,
    )

    sealed class SceneAction {
        @JsonClass(generateAdapter = true)
        class FadeEnd(val duration: DurationJson) : SceneAction()

        @JsonClass(generateAdapter = true)
        class FadeStart(val duration: DurationJson) : SceneAction()

        @JsonClass(generateAdapter = true)
        class SetTime(val hour: Int, val minute: Int) : SceneAction()

        @JsonClass(generateAdapter = true)
        class AnimateTime(
            val startHour: Int,
            val startMinute: Int,
            val endHour: Int,
            val endMinute: Int,
            val fadeTime: DurationJson
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class PlaySpecialEffect(
            val effect: String,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class AssumeRunning(
            val group: String,
            val scene: String,
            val startedAt: DurationJson,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class ScriptStart(
            val group: String,
            val scene: String,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class ScriptStop(
            val group: String,
            val scene: String,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class DispatchRide(
            val ride: String,
            val station: String? = null,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class FadeInText(
            val id: String,
            val duration: DurationJson,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class FadeOutText(
            val id: String,
            val duration: DurationJson,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class HardcodedAction(
            val action: Type,
        ) : SceneAction() {
            enum class Type {
                SpinWheelOfFortune,
            }
        }

        @JsonClass(generateAdapter = true)
        class Mount(
            val passenger: String,
            val seat: String,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class Eating(
            val who: String,
            val eating: Boolean,
        ) : SceneAction()

        @JsonClass(generateAdapter = true)
        class Sit(
            val who: String,
            val sitting: Boolean,
            val yOffset: Double = 0.0,
        ) : SceneAction()
    }

    @JsonClass(generateAdapter = true)
    class CameraPath(
        val duration: DurationJson? = null,
        val cameraFrames: List<CameraFrame>,
    ) {
        val calculatedDuration = duration?.duration ?: cameraFrames.maxBy { it.at.duration }.at.duration
    }

    @JsonClass(generateAdapter = true)
    class CameraFrame(
        val at: DurationJson,
        val location: Location,
    )

    @JsonClass(generateAdapter = true)
    class NpcData(
        val id: String? = null,
        val startPlayingAt: DurationJson,
        val profile: String? = null,
        val npcFileName: String,
        val model: String? = null,
        val entityType: EntityType = EntityType.PLAYER,
        val hideNameTag: Boolean = true,
        val displayName: String? = null,
        val team: TeamType? = null,
        val balloon: String? = null,
        val isSelf: Boolean = false,
    ) {
        enum class TeamType {
            crew,
            vip
        }
    }

    @JsonClass(generateAdapter = true)
    class ItemDisplayConfig(
        val id: String,
        val model: String,
        val initialScale: Double = 1.0,
        val initialLocation: Location,
        val followCamDistance: Double? = null,
    )

    @JsonClass(generateAdapter = true)
    class TextDisplayConfig(
        val id: String,
        val initialScale: Double = 1.0,
        val initialLocation: Location,
        val initialOpacity: Double = 1.0,
        val followCamDistance: Double? = null,
        val text: String,
        val billboard: Billboard = Billboard.FIXED,
        val alignment: TextDisplay.TextAlignment = TextDisplay.TextAlignment.LEFT,
        val followOffset: Vector? = null,
        val font: String? = "minguarana",
        val useDefaultBackground: Boolean = false,
        val useVariables: Boolean = false,
        val updateTitleEverySecond: Boolean = false,
        val requireVariables: Set<String>? = null,
    )
}