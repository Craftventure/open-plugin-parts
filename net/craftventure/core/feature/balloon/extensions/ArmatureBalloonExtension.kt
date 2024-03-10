package net.craftventure.core.feature.balloon.extensions

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.add
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.animation.armature.ArmatureAnimator
import net.craftventure.core.animation.armature.Joint
import net.craftventure.core.animation.dae.DaeLoader
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.json.EntityInteractorJson
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.utils.ItemStackUtils
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

class ArmatureBalloonExtension(
    val data: Json,
    armatures: Array<Armature>,
) : ExtensibleBalloon.Extension() {
    private val animators = armatures.map { ArmatureAnimator(it) }
    private var npcsHolders = emptyList<NpcHolder>()

    override fun spawn(balloon: ExtensibleBalloon, balloonHolder: BalloonHolder, tracker: NpcEntityTracker) {
        npcsHolders = data.attachments.flatMap { attachment ->
            val bone =
                animators.firstNotNullOf { it.allJoints.firstOrNull { joint -> joint.name == attachment.boneName } }

            attachment.npcs.map { npcAttachment ->
                val npc =
                    NpcEntity(
                        "armatureBalloon",
                        npcAttachment.entityType,
                        balloon.balloonLocation!!.clone().add(npcAttachment.offset)
                            .add(
                                0.0,
                                if (npcAttachment.compensateForArmorstand && npcAttachment.entityType == EntityType.ARMOR_STAND) -EntityConstants.ArmorStandHeadOffset else 0.0,
                                0.0
                            )
                    )
                npcAttachment.equipment.forEach { slot, itemName ->
                    val item = ItemStackUtils.fromString(itemName)
                    if (item != null) {
                        npc.setSlot(slot, item)
                    }
                }
                npcAttachment.metadata.forEach { it.applyTo(npc) }
                tracker.addEntity(npc)
                NpcHolder(
                    npc,
                    bone,
                    npcAttachment
                )
            }
        }
    }

    override fun update(balloon: ExtensibleBalloon) {
        animators.forEach { animator ->
            val armature = animator.armature
            animator.setTime(0.0)

            val location = balloon.balloonLocation!!
            armature.animatedTransform.setIdentity()
            armature.animatedTransform.translate(location.x, location.y, location.z)
            armature.animatedTransform.rotateYawPitchRoll(location.pitch, location.yaw, 0f)
            armature.animatedTransform.multiply(armature.transform)

            armature.applyAnimatedTransformsRecursively()

//            Logger.debug(
//                "From ${location.toVector().asString()} to ${
//                    armature.animatedTransform.toVector().asString()
//                }"
//            )
        }

        npcsHolders.forEach { npcHolder ->
            if (npcHolder.data.mountTo != null) return@forEach
            val position = npcHolder.bone.animatedTransform.toVector().add(npcHolder.data.offset)
                .add(
                    0.0,
                    if (npcHolder.data.compensateForArmorstand && npcHolder.npc.entityType == EntityType.ARMOR_STAND) -EntityConstants.ArmorStandHeadOffset else 0.0,
                    0.0
                )
            val rotation = npcHolder.bone.animatedTransform.rotation

//            Logger.debug("Position ${position.asString()} ${rotation.yaw.format(2)} ${rotation.pitch.format(2)} for bone ${npcHolder.bone.id}/${npcHolder.bone.name}")

            npcHolder.npc.move(
                position.x,
                position.y,
                position.z,
                rotation.yaw.toFloat(),
                rotation.pitch.toFloat(),
                rotation.yaw.toFloat()
            )
        }
    }

    override fun despawn(balloon: ExtensibleBalloon, withEffects: Boolean, tracker: NpcEntityTracker) {
        npcsHolders.forEach { npcHolder -> tracker.removeEntity(npcHolder.npc) }
        npcsHolders = emptyList()
    }

    private class NpcHolder(
        val npc: NpcEntity,
        val bone: Joint,
        val data: Json.NpcAttachment,
    )

    @JsonClass(generateAdapter = true)
    data class Json(
        val armatureName: String = "base.dae",
        val attachments: List<BoneAttachment>,
    ) : ExtensibleBalloon.Extension.Json() {
        override fun toExtension(): ExtensibleBalloon.Extension {
            val daeFile = File(PluginProvider.plugin.dataFolder, "data/balloon/$armatureName")
            if (!daeFile.exists()) {
                throw IOException("Failed to find balloon armature for $armatureName ($daeFile)")
            }

            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(daeFile)
            val armatures = DaeLoader.load(doc, daeFile.name)
            return ArmatureBalloonExtension(this, armatures)
        }

        @JsonClass(generateAdapter = true)
        data class BoneAttachment(
            val armatureName: String? = null,
            val boneName: String = "Bone",
            val npcs: List<NpcAttachment>,
        )


        @JsonClass(generateAdapter = true)
        data class NpcAttachment(
            val name: String? = null,
            val entityType: EntityType = EntityType.ARMOR_STAND,
            val compensateForArmorstand: Boolean = true,
            val offset: Vector = Vector(0.0, 0.0, 0.0),
            val equipment: Map<EquipmentSlot, String>,
            val metadata: List<EntityInteractorJson<Any>>,
            val mountTo: String? = null,
        )

        companion object {
            const val type = "armature"
        }
    }
}