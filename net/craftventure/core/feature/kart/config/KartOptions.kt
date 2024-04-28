package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.feature.kart.KartProperties
import net.craftventure.core.feature.kart.NamedPart
import net.craftventure.core.feature.kart.resolve
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.utils.BoundingBox
import java.util.*

@JsonClass(generateAdapter = true)
data class KartOptions(
    override val id: String,
    override val displayName: String,
    override val extends: String? = null,
    val type: KartProperties.Type? = null,
    val colors: Optional<List<KartColorConfig>>? = null,
    val brakeId: Optional<String>? = null,
    val tireId: Optional<String>? = null,
    val engineId: Optional<String>? = null,
    val handlingId: Optional<String>? = null,
    val steerId: Optional<String>? = null,
    val zeppelinLifterId: Optional<String>? = null,
    val planeLifterId: Optional<String>? = null,
    val boundingBox: Optional<BoundingBox>? = null,
    val leftClickAction: Optional<KartActionConfig>? = null,
    val rightClickAction: Optional<KartActionConfig>? = null,
    val exitAction: Optional<KartExitAction>? = null,
    val seats: Optional<List<KartSeatConfig>>? = null,
    val legacyModelConfig: Optional<LegacyKartModelConfig>? = null,
    val wheels: Optional<List<KartWheelConfig>>? = null,
    val addons: Optional<List<String>>? = null,
    val armatureName: Optional<String>? = null,
) : KartPart, NamedPart {
    override fun isValid() = colors?.orElse() != null &&
            brakeId?.orElse() != null &&
            tireId?.orElse() != null &&
            engineId?.orElse() != null &&
            handlingId?.orElse() != null &&
            steerId?.orElse() != null &&
//            zeppelinLifterId?.orElse() != null &&
            boundingBox?.orElse() != null &&
            seats?.orElse() != null &&
            seats.orElse()!!.isNotEmpty() &&
            legacyModelConfig?.orElse() != null// &&
//            exitAction?.orElse() != null

    fun resolveBrakes(config: KartsConfig) =
        brakeId?.orElse()?.let { config.brake.resolve(it) }

    fun resolveTires(config: KartsConfig) =
        tireId?.orElse()?.let { config.tire.resolve(it) }

    fun resolveEngine(config: KartsConfig) =
        engineId?.orElse()?.let { config.engine.resolve(it) }

    fun resolveHandling(config: KartsConfig) =
        handlingId?.orElse()?.let { config.handling.resolve(it) }

    fun resolveSteer(config: KartsConfig) =
        steerId?.orElse()?.let { config.steer.resolve(it) }

    fun resolveZeppelinLifter(config: KartsConfig) =
        zeppelinLifterId?.orElse()?.let { config.zeppelinLifters.resolve(it) }

    fun resolvePlaneLifter(config: KartsConfig) =
        planeLifterId?.orElse()?.let { config.planeLifters.resolve(it) }
}