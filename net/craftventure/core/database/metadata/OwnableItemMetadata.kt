package net.craftventure.core.database.metadata

import com.mojang.brigadier.StringReader
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.colorFromHex
import net.craftventure.bukkit.ktx.extension.named
import net.craftventure.bukkit.ktx.util.BoundingBoxProducer
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.database.metadata.itemuse.AlcoholicConsumptionEffect
import net.craftventure.core.database.metadata.itemuse.ItemUseEffect
import net.craftventure.core.database.metadata.itemwear.ItemWearEffect
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.feature.instrument.InstrumentType
import net.craftventure.core.feature.kart.KartManager
import net.craftventure.core.feature.minigame.lasergame.gun.LaserGameGunType
import net.craftventure.core.feature.minigame.lasergame.turret.LaserGameTurretType
import net.craftventure.core.feature.nbsplayback.NbsFileManager
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.extension.takeIfNotBlank
import net.craftventure.core.ktx.extension.toOptional
import net.craftventure.core.ktx.logging.logException
import net.kyori.adventure.text.format.TextColor
import net.minecraft.commands.arguments.CompoundTagArgument
import org.bukkit.Color
import java.util.*

@JsonClass(generateAdapter = true)
data class OwnableItemMetadata(
    val kart: Optional<KartMetadata>? = null,
    val laserGun: Optional<LaserGunMetadata>? = null,
    val laserTurret: Optional<LaserTurretMetadata>? = null,
    val mainColor: Optional<String>? = null,
    val kartId: Optional<String>? = null,
    val limitedness: Limitedness? = null,
    val sourceDescription: String? = null,
    val compoundTag: String? = null,
    val extensibleBalloon: Optional<ExtensibleBalloon.Json>? = null,
    val tebexPackageId: String? = null,
    val consumptionMeta: ConsumptionMeta? = null,
    val musicSheet: MusicSheetMeta? = null,
    val item: ItemUseMeta? = null,
    val itemWear: ItemWearMeta? = null,
    val descriptions: List<String>? = null,
    val bounds: BoundingBoxProducer? = null,
) {
    val parsedMainColor = mainColor?.orElse()?.let { parseColor(it) }
    val isRandomColor = mainColor?.orElse() == "random"
    val parsedCompoundTag = compoundTag?.let {
        try {
            CompoundTagArgument.compoundTag().parse(StringReader(it))
        } catch (e: Exception) {
            logException(e)
            null
        }
    }

    //other?.kart?.let { it.orElse() } kart ?. orElse ()?.mergedWith(other?.kart?.orElse())?.toOptional()
    infix fun mergedWith(other: OwnableItemMetadata?): OwnableItemMetadata = this.copy(
        kart = merge(kart, other?.kart),
        laserGun = merge(laserGun, other?.laserGun),
        laserTurret = merge(laserTurret, other?.laserTurret),
        mainColor = other?.mainColor ?: this.mainColor,
//        kartId = other?.kartId ?: this.kartId,
//        extensibleBalloon = other?.extensibleBalloon ?: this.extensibleBalloon,
//        compoundTag = other?.compoundTag ?: this.compoundTag,
//        tebexPackageId = other?.tebexPackageId ?: this.tebexPackageId,
    )

    @JsonClass(generateAdapter = true)
    data class KartMetadata(
        val steer: Optional<String>? = null,
    ) : Mergeable<KartMetadata>, Describeable {
        override fun mergedWith(other: KartMetadata?): KartMetadata = copy(
            steer = other?.steer ?: steer,
        )

        override fun describe(builder: ComponentBuilder.LoreBuilder) {
            steer?.orElse()?.let { "Steering: ${it}" }?.let(builder::textOnNewLine)
        }
    }

    @JsonClass(generateAdapter = true)
    data class LaserGunMetadata(
        val type: Optional<LaserGameGunType>? = null
    ) : Mergeable<LaserGunMetadata>, Describeable {
        override fun mergedWith(other: LaserGunMetadata?): LaserGunMetadata = copy(
            type = other?.type ?: type
        )

        override fun describe(builder: ComponentBuilder.LoreBuilder) {
            type?.orElse()?.let { "Gun type: ${it.displayName}" }?.let(builder::textOnNewLine)
        }
    }

    @JsonClass(generateAdapter = true)
    data class LaserTurretMetadata(
        val type: Optional<LaserGameTurretType>? = null
    ) : Mergeable<LaserTurretMetadata>, Describeable {
        override fun mergedWith(other: LaserTurretMetadata?): LaserTurretMetadata = copy(
            type = other?.type ?: type
        )

        override fun describe(builder: ComponentBuilder.LoreBuilder) {
            type?.orElse()?.let { "Turret type: ${it.displayName}" }?.let(builder::textOnNewLine)
        }
    }

    @JsonClass(generateAdapter = true)
    data class ConsumptionMeta(
        val effects: List<ItemUseEffect>,
        val foodLevelDelta: Int = 1,
        val saturationLevelDelta: Float = 3f,
    ) : Describeable {
        override fun describe(builder: ComponentBuilder.LoreBuilder) {
            if (effects.any { it is AlcoholicConsumptionEffect }) builder.textOnNewLine("Alcoholic consumption")
        }
    }

    @JsonClass(generateAdapter = true)
    data class MusicSheetMeta(
        val song: String,
        val instruments: Set<InstrumentType>,
    ) : Describeable {
        override fun describe(builder: ComponentBuilder.LoreBuilder) {
            NbsFileManager.getSong(song)?.let { file ->
                builder.emptyLines()
                builder.textOnNewLine("${file.name}")

                file.originalAuthor?.let {
                    builder.textOnNewLine("By ${file.originalAuthor}")
                }

                file.author?.let {
                    builder.textOnNewLine("Composed by ${file.author}")
                }

                file.description?.let {
                    builder.textOnNewLine(file.description!!)
                }

                builder.emptyLines(1)
                builder.text("For the following instruments:")

                instruments.map {
                    builder.textOnNewLine("- ${it.displayName}")
                }
            }
        }
    }

    @JsonClass(generateAdapter = true)
    data class ItemUseMeta(
        val category: ItemUseCategory = ItemUseCategory.Default,
        val effects: List<ItemUseEffect>,
        val useTimeout: Long = 3000,
        val allowWhileAfk: Boolean = false,
        val allowWhileInVehicle: Boolean = false,
        val requireNonSpectator: Boolean = true,
        val deleteOnUseIfConsumable: Boolean = true,
        val activateWithLeftClick: Boolean = true,
        val activateWithRightClick: Boolean = true,
    ) : Describeable {
        enum class ItemUseCategory {
            Default,
        }
    }

    @JsonClass(generateAdapter = true)
    data class ItemWearMeta(
        val effects: List<ItemWearEffect>,
    ) : Describeable

    //    override fun describe(builder:ComponentBuilder.LoreBuilder) {
    fun describe(source: ComponentBuilder.LoreBuilder) {
        when (limitedness) {
            Limitedness.LIMITED_EDITION_EVENT -> "Limited edition item (event)".let(source::textOnNewLine)
            Limitedness.LIMITED_EDITION -> "Limited edition item".let(source::textOnNewLine)
            null -> {}
        }

        sourceDescription?.takeIfNotBlank()?.let { "Introduced: $it" }?.let(source::textOnNewLine)

        if (isRandomColor) "Primary color: random".let(source::textOnNewLine)
        else parsedMainColor?.let {
            "Primary color: ${it.named().name}".let(source::textOnNewLine)
            source.text(" ▉", TextColor.color(it.asRGB()))
        }

        kartId?.orElse()?.let { kartId ->
            val kartProperties = KartManager.kartPropertiesFromConfig(kartId)
            if (kartProperties != null) {
                null
            } else {
                "Kart configuration is invalid".let(source::textOnNewLine)
            }
        }

        laserGun?.orElse()?.describe(source)
        laserTurret?.orElse()?.describe(source)
        consumptionMeta?.describe(source)
        item?.describe(source)
        musicSheet?.describe(source)
        descriptions?.map { it.parseWithCvMessage() }
    }

    enum class Limitedness {
        LIMITED_EDITION_EVENT,
        LIMITED_EDITION,
    }

    interface Describeable {
        fun describe(builder: ComponentBuilder.LoreBuilder) {}
    }

    interface Mergeable<T> {
        fun mergedWith(other: T?): T
    }

    companion object {
        private fun <T : Mergeable<T>> merge(
            parent: Optional<T>?,
            child: Optional<T>?,
            merger: (parent: T, child: T) -> T = { parent, child -> parent.mergedWith(child) },
        ): Optional<T>? {
            return when {
                child == null -> parent
                parent == null -> child
                child.orElse() == null -> Optional.ofNullable<T>(null)
                parent.orElse() == null -> child
                else -> {
                    val parentValue = parent.orElse()!!
                    val childValue = child.orElse()!!
                    merger(parentValue, childValue).toOptional()
                }
            }
        }

        private fun parseColor(color: String): Color? {
            if (color.startsWith("#")) {
                try {
                    return colorFromHex(color)
                } catch (e: Exception) {
                }
            }
            return color.toIntOrNull()?.let { Color.fromRGB(it) }
        }
    }

}