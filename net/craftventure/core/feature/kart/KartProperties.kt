package net.craftventure.core.feature.kart

import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.feature.kart.actions.ExitHandler
import net.craftventure.core.feature.kart.addon.KartAddon
import net.craftventure.core.feature.kart.config.*
import net.craftventure.core.feature.kart.physicscontroller.PhysicsController
import net.craftventure.core.utils.BoundingBox
import org.bukkit.inventory.ItemStack


data class KartProperties(
    val seats: Array<KartSeat>,
    val boundingBox: BoundingBox,
    val leftClickAction: KartAction?,
    val rightClickAction: KartAction?,
    val kartNpcs: Array<KartNpc>,
    val type: Type = Type.KART,
    val exitHandler: ExitHandler?,
    val wheels: Array<WheelConfig>,

    val steer: KartSteer,
    val brakes: KartBrakes,
    val engine: KartEngine,
    val tires: KartTires,
    val handling: KartHandling,
    val zeppelinLifter: KartZeppelinLifter?,
    val planeLifter: KartPlaneLifter?,

    val physicsController: Class<out PhysicsController>,
    val addons: Set<KartAddon>,
    val armature: Armature
) {
    val clonedArmature: Armature
        get() = armature.clone()

    data class WheelConfig(
        val source: KartWheelConfig,
        val matrix: Matrix4x4,
        val model: ItemStack? = null,
        val radius: Double? = null,
        val isLeftSide: Boolean,
        val hasBrakes: Boolean,
        val isSteered: Boolean,
        val steerAngle: Double? = null,
        val forceCustomParticle: Boolean = false,
    ) {
        val diameter = radius?.let { it * 2 }
        val circumference = diameter?.let { it * Math.PI }
    }

    enum class Type(val isFlying: Boolean) {
        KART(false),
        ZEPPELIN(true),
        AIRPLANE(true),
    }
}

